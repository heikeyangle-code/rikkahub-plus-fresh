package me.rerere.rikkahub.ui.pages.extensions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rerere.rikkahub.data.files.SkillFrontmatterParser
import me.rerere.rikkahub.data.files.SkillManager
import me.rerere.rikkahub.data.files.SkillMetadata
import me.rerere.rikkahub.data.files.SkillRegistry
import java.util.LinkedHashMap
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class SkillsVM(
    private val skillManager: SkillManager,
) : ViewModel() {
    private val _skills = MutableStateFlow<List<SkillMetadata>>(emptyList())
    val skills = _skills.asStateFlow()

    init {
        loadSkills()
    }

    private fun loadSkills() {
        viewModelScope.launch(Dispatchers.IO) {
            _skills.value = skillManager.listSkills()
        }
    }

    fun saveSkill(name: String, content: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = skillManager.saveSkill(name, content)
            _skills.value = skillManager.listSkills()
            withContext(Dispatchers.Main) {
                onResult(result != null)
            }
        }
    }

    fun deleteSkill(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            skillManager.deleteSkill(name)
            _skills.value = skillManager.listSkills()
        }
    }

    fun getSkillsDir() = skillManager.getSkillsDir()

    fun togglePin(name: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val skill = skillManager.listSkills().find { it.name == name } ?: return@launch
            val current = skillManager.readSkillContent(name) ?: return@launch
            val updated = current.replace(
                "pinned: ${!skill.pinned}",
                "pinned: ${skill.pinned}"
            ).let { if (skill.pinned) it else it.replace("pinned: true", "pinned: false") }
            // Toggle pinned in frontmatter
            if ("pinned:" !in current) {
                // Add pinned field after version or description
                val withPin = current.replaceFirst("version:", "pinned: true\nversion:")
                skillManager.saveSkill(name, withPin)
            } else {
                skillManager.saveSkill(name, updated)
            }
            _skills.value = skillManager.listSkills()
        }
    }

    /**
     * 从远程 URL 获取 marketplace.json 并导入
     */
    fun fetchRemoteMarketplace(url: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = downloadText(url) ?: run {
                    withContext(Dispatchers.Main) { onResult(false, "下载失败") }
                    return@launch
                }
                val marketplace = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                    .decodeFromString(MarketplaceData.serializer(), json)
                var count = 0
                marketplace.plugins.forEach { plugin ->
                    if (plugin.source != null) {
                        val fullUrl = if (plugin.source.startsWith("http")) plugin.source
                        else url.removeSuffix("marketplace.json") + plugin.source
                        importSkillFromGitHub(fullUrl) { success, _ ->
                            if (success) count++
                        }
                    }
                }
                _skills.value = skillManager.listSkills()
                withContext(Dispatchers.Main) { onResult(true, "导入完成: $count 个") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.message ?: "解析失败") }
            }
        }
    }

    /**
     * 从 ZIP 文件导入 skill 目录
     */
    fun importFromZip(uri: android.net.Uri, context: android.content.Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: run { withContext(Dispatchers.Main) { onResult(false, "无法读取文件") }; return@launch }
                val zipBytes = inputStream.readBytes()
                inputStream.close()

                val zipIn = java.util.zip.ZipInputStream(java.io.ByteArrayInputStream(zipBytes))
                val entries = mutableMapOf<String, ByteArray>()
                var entry = zipIn.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name.removePrefix("/")
                            .split("/").dropWhile { it.isEmpty() || it == "__MACOSX" }.joinToString("/")
                        if (name.isNotBlank() && !name.startsWith(".")) {
                            entries[name] = zipIn.readBytes()
                        }
                    }
                    entry = zipIn.nextEntry
                }
                zipIn.close()

                // 找到 SKILL.md
                val skillMdPath = entries.keys.find { it.endsWith("SKILL.md") }
                    ?: run { withContext(Dispatchers.Main) { onResult(false, "ZIP 中未找到 SKILL.md") }; return@launch }

                val skillName = SkillFrontmatterParser.parse(String(entries[skillMdPath]!!))["name"]
                    ?: run { withContext(Dispatchers.Main) { onResult(false, "SKILL.md 缺少 name") }; return@launch }

                // 保存所有文件
                val prefix = skillMdPath.removeSuffix("SKILL.md")
                val fileMap = entries.mapKeys { (k, _) ->
                    k.removePrefix(prefix)
                }.filterKeys { it.isNotBlank() }

                val saved = skillManager.saveSkillFilesAtomically(skillName, fileMap.mapValues { String(it.value) })
                _skills.value = skillManager.listSkills()
                withContext(Dispatchers.Main) {
                    onResult(saved, if (saved) skillName else "保存失败")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.message ?: "导入失败") }
            }
        }
    }

    @kotlinx.serialization.Serializable
    data class MarketplaceData(
        val plugins: List<MarketplacePlugin> = emptyList(),
    )
    @kotlinx.serialization.Serializable
    data class MarketplacePlugin(
        val name: String = "",
        val source: String? = null,
        val description: String = "",
        val category: String? = null,
    )

    /**
     * 从注册表条目下载并安装 skill
     */
    fun installFromRegistry(entry: SkillRegistry.RegistryEntry, onResult: (Boolean, String) -> Unit) {
        importSkillFromGitHub(entry.githubUrl, onResult)
    }

    /**
     * 从本地文件导入 (ZIP 或单个 SKILL.md)
     */
    fun importFromLocalFile(uri: android.net.Uri, context: android.content.Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fileName = uri.lastPathSegment?.lowercase() ?: ""
                // Route .zip to zip importer
                if (fileName.endsWith(".zip")) {
                    importFromZip(uri, context, onResult)
                    return@launch
                }
                if (!fileName.endsWith(".md")) {
                    withContext(Dispatchers.Main) { onResult(false, "不支持的格式，请选择 .zip 或 .md 文件") }
                    return@launch
                }
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
                    ?: run {
                        withContext(Dispatchers.Main) { onResult(false, "无法读取文件") }
                        return@launch
                    }
                val frontmatter = SkillFrontmatterParser.parse(content)
                val name = frontmatter["name"]?.takeIf { it.isNotBlank() }
                    ?: run {
                        withContext(Dispatchers.Main) { onResult(false, "无效的Skill文件，缺少name或description字段") }
                        return@launch
                    }
                val desc = frontmatter["description"]?.takeIf { it.isNotBlank() }
                    ?: run {
                        withContext(Dispatchers.Main) { onResult(false, "无效的Skill文件，缺少name或description字段") }
                        return@launch
                    }
                val saved = skillManager.saveSkill(name, content)
                _skills.value = skillManager.listSkills()
                withContext(Dispatchers.Main) {
                    onResult(saved != null, if (saved != null) name else "保存失败")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.message ?: "导入失败") }
            }
        }
    }

    /**
     * 从文件夹导入 (OpenDocumentTree)
     */
    fun importFromFolder(uri: android.net.Uri, context: android.content.Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val files = mutableMapOf<String, String>()

                fun readDir(dirUri: android.net.Uri, prefix: String) {
                    val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(
                        dirUri,
                        android.provider.DocumentsContract.getTreeDocumentId(dirUri)
                    )
                    val cursor = context.contentResolver.query(
                        childrenUri,
                        arrayOf(
                            android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                            android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE,
                            android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME
                        ),
                        null, null, null
                    )
                    cursor?.use { c ->
                        while (c.moveToNext()) {
                            val docId = c.getString(0)
                            val mime = c.getString(1)
                            val name = c.getString(2) ?: continue
                            val docUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(dirUri, docId)

                            if (android.provider.DocumentsContract.Document.MIME_TYPE_DIR == mime) {
                                readDir(docUri, "$prefix$name/")
                            } else {
                                val content = context.contentResolver.openInputStream(docUri)?.bufferedReader()?.readText()
                                if (content != null) {
                                    files["$prefix$name"] = content
                                }
                            }
                        }
                    }
                }
                readDir(uri, "")

                val skillMdPath = files.keys.find { it == "SKILL.md" }
                    ?: run { withContext(Dispatchers.Main) { onResult(false, "文件夹中未找到 SKILL.md") }; return@launch }

                val frontmatter = SkillFrontmatterParser.parse(files[skillMdPath]!!)
                val name = frontmatter["name"]?.takeIf { it.isNotBlank() }
                    ?: run { withContext(Dispatchers.Main) { onResult(false, "无效的Skill文件，缺少name或description字段") }; return@launch }
                val desc = frontmatter["description"]?.takeIf { it.isNotBlank() }
                    ?: run { withContext(Dispatchers.Main) { onResult(false, "无效的Skill文件，缺少name或description字段") }; return@launch }

                val saved = skillManager.saveSkillFilesAtomically(name, files)
                _skills.value = skillManager.listSkills()
                withContext(Dispatchers.Main) {
                    onResult(saved, if (saved) name else "保存失败")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.message ?: "导入失败") }
            }
        }
    }

    fun importSkillFromGitHub(repoUrl: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val info = parseGitHubUrl(repoUrl) ?: run {
                    withContext(Dispatchers.Main) { onResult(false, "无效的 GitHub 仓库链接") }
                    return@launch
                }

                // Collect all files recursively via GitHub Contents API
                val files = mutableListOf<Pair<String, String>>() // relativePath -> downloadUrl
                val listed = listFilesRecursively(info.owner, info.repo, info.branch, info.path, info.path, files)
                if (!listed) {
                    withContext(Dispatchers.Main) { onResult(false, "读取 GitHub 目录失败") }
                    return@launch
                }

                val skillMdEntry = files.find { it.first == "SKILL.md" } ?: run {
                    withContext(Dispatchers.Main) { onResult(false, "目录中未找到 SKILL.md") }
                    return@launch
                }

                val skillMdContent = downloadText(skillMdEntry.second) ?: run {
                    withContext(Dispatchers.Main) { onResult(false, "下载 SKILL.md 失败，请检查链接或网络") }
                    return@launch
                }

                val frontmatter = SkillFrontmatterParser.parse(skillMdContent)
                val name = frontmatter["name"]
                if (name.isNullOrBlank()) {
                    withContext(Dispatchers.Main) { onResult(false, "SKILL.md 格式错误：缺少 name 字段") }
                    return@launch
                }

                val fileContents = LinkedHashMap<String, String>()
                for ((relativePath, downloadUrl) in files) {
                    val content = downloadText(downloadUrl)
                    if (content == null) {
                        withContext(Dispatchers.Main) { onResult(false, "下载文件失败：$relativePath") }
                        return@launch
                    }
                    fileContents[relativePath] = content
                }

                val saved = skillManager.saveSkillFilesAtomically(name, fileContents)
                if (!saved) {
                    withContext(Dispatchers.Main) { onResult(false, "保存失败") }
                    return@launch
                }

                _skills.value = skillManager.listSkills()
                withContext(Dispatchers.Main) { onResult(true, name) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.message ?: "未知错误") }
            }
        }
    }

    private fun listFilesRecursively(
        owner: String,
        repo: String,
        branch: String,
        dirPath: String,
        basePath: String,
        result: MutableList<Pair<String, String>>,
    ): Boolean {
        val apiUrl = "https://api.github.com/repos/$owner/$repo/contents/$dirPath?ref=$branch"
        val json = downloadText(apiUrl) ?: return false
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val type = item.getString("type")
            val itemPath = item.getString("path")
            val relativePath = itemPath.removePrefix("$basePath/").removePrefix(basePath)
            when (type) {
                "file" -> {
                    val downloadUrl = item.optString("download_url").takeIf { it.isNotBlank() }
                        ?: return false
                    result.add(relativePath to downloadUrl)
                }

                "dir" -> {
                    val ok = listFilesRecursively(owner, repo, branch, itemPath, basePath, result)
                    if (!ok) return false
                }
            }
        }
        return true
    }

    private data class GitHubRepoInfo(
        val owner: String,
        val repo: String,
        val branch: String,
        val path: String,
    )

    private fun parseGitHubUrl(url: String): GitHubRepoInfo? {
        val trimmed = url.trim().trimEnd('/')
        // https://github.com/owner/repo
        // https://github.com/owner/repo/tree/branch
        // https://github.com/owner/repo/tree/branch/sub/path
        val regex = Regex("""https://github\.com/([^/]+)/([^/]+)(?:/tree/([^/]+)(/.*)?)?""")
        val match = regex.matchEntire(trimmed) ?: return null
        val owner = match.groupValues[1]
        val repo = match.groupValues[2]
        val branch = match.groupValues[3].ifBlank { "HEAD" }
        val subPath = match.groupValues[4].trimStart('/')
        return GitHubRepoInfo(owner, repo, branch, subPath)
    }

    private fun downloadText(url: String): String? {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("User-Agent", "Rikkahub/1.0")
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        return try {
            if (connection.responseCode == 200) connection.inputStream.bufferedReader().readText()
            else null
        } finally {
            connection.disconnect()
        }
    }
}
