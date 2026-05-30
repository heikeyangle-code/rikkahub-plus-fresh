package me.rerere.rikkahub.ui.pages.extensions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import me.rerere.rikkahub.data.files.SkillFrontmatterParser
import me.rerere.rikkahub.data.files.SkillManager
import me.rerere.rikkahub.data.files.SkillMetadata
import me.rerere.rikkahub.data.files.SkillRegistry
import me.rerere.rikkahub.data.datastore.SettingsStore
import java.util.LinkedHashMap
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SkillsVM(
    private val skillManager: SkillManager,
) : ViewModel(), KoinComponent {
    private val settingsStore: SettingsStore by inject()
    private val _skills = MutableStateFlow<List<SkillMetadata>>(emptyList())
    val skills = _skills.asStateFlow()
    private val _downloadStatus = MutableStateFlow<String?>(null)
    val downloadStatus = _downloadStatus.asStateFlow()

    fun setDownloadStatus(status: String?) {
        _downloadStatus.value = status
    }

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
                var docContractFailed = false

                // 方式1: DocumentsContract API（标准 Android）
                try {
                    fun readDir(dirUri: android.net.Uri, prefix: String) {
                        val treeDocId = android.provider.DocumentsContract.getTreeDocumentId(dirUri)
                        val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(dirUri, treeDocId)
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
                                val docId = c.getString(0) ?: continue
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
                } catch (e: Exception) {
                    android.util.Log.e("SkillsVM", "DocumentsContract 扫描失败: ${e.message}", e)
                    docContractFailed = true
                }

                // 方式2: MIUI fallback — 从 URI 解析真实文件路径
                if (files.isEmpty()) {
                    android.util.Log.d("SkillsVM", "DocumentsContract 返回空，尝试 fallback, URI=$uri")
                    var realDir: java.io.File? = null

                    // 尝试1: 从 TreeDocumentId 解析
                    runCatching {
                        val docId = android.provider.DocumentsContract.getTreeDocumentId(uri)
                        android.util.Log.d("SkillsVM", "TreeDocumentId: $docId")
                        parsePathFromDocumentId(docId)
                    }.onSuccess { realDir = it }
                     .onFailure { android.util.Log.w("SkillsVM", "getTreeDocumentId 失败: ${it.message}") }

                    // 尝试2: 从 URI path 中提取 document ID
                    if (realDir == null) {
                        uri.path?.let { p ->
                            android.util.Log.d("SkillsVM", "URI path: $p")
                            // 匹配 /tree/xxx/document/yyy 格式
                            val parts = p.split("/")
                            val docIdx = parts.indexOfLast { it == "document" }
                            if (docIdx >= 0 && docIdx + 1 < parts.size) {
                                realDir = parsePathFromDocumentId(parts[docIdx + 1])
                            }
                        }
                    }

                    // 尝试3: 用 DocumentsContract.getDocumentId
                    if (realDir == null) {
                        runCatching {
                            val docId = android.provider.DocumentsContract.getDocumentId(uri)
                            android.util.Log.d("SkillsVM", "DocumentId: $docId")
                            parsePathFromDocumentId(docId)
                        }.onSuccess { realDir = it }
                    }

                    if (realDir != null && realDir.isDirectory) {
                        android.util.Log.d("SkillsVM", "Fallback: 扫描目录 $realDir")
                        realDir!!.walkTopDown().forEach { file ->
                            if (file.isFile) {
                                val relPath = file.relativeTo(realDir!!).path
                                if (file.length() > 20 * 1024 * 1024) {
                                    android.util.Log.w("SkillsVM", "跳过超大文件 $relPath (${file.length()} bytes)")
                                    return@forEach
                                }
                                try {
                                    files[relPath] = file.readText()
                                } catch (e: Exception) {
                                    android.util.Log.w("SkillsVM", "跳过文件 $relPath: ${e.message}")
                                }
                            }
                        }
                        android.util.Log.d("SkillsVM", "Fallback: 扫描完成，共 ${files.size} 个文件")
                    } else {
                        android.util.Log.e("SkillsVM", "Fallback: 无法获取目录路径, URI=$uri, realDir=$realDir")
                    }
                }

                if (files.isEmpty()) {
                    val detail = if (docContractFailed) "文件管理器不支持标准协议（如 MIUI），且无法解析目录路径"
                    else "未读取到任何文件"
                    android.util.Log.e("SkillsVM", "importFromFolder: $detail, URI=$uri")
                    withContext(Dispatchers.Main) { onResult(false, detail) }
                    return@launch
                }

                val skillMdPath = files.keys.find { it.endsWith("SKILL.md") }
                    ?: run {
                        val filesList = files.keys.take(10).joinToString()
                        android.util.Log.e("SkillsVM", "importFromFolder: 未找到 SKILL.md, 文件列表: $filesList")
                        withContext(Dispatchers.Main) { onResult(false, "文件夹中未找到 SKILL.md（已扫描到 ${files.size} 个文件）") }
                        return@launch
                    }

                // 以 SKILL.md 所在目录为根，调整所有文件路径
                val skillDirPrefix = skillMdPath.removeSuffix("SKILL.md")
                val adjustedFiles = files.mapKeys { (k, _) -> k.removePrefix(skillDirPrefix) }
                    .filterKeys { it.isNotBlank() }

                val frontmatter = SkillFrontmatterParser.parse(adjustedFiles[skillMdPath.removePrefix(skillDirPrefix)]!!)
                val name = frontmatter["name"]?.takeIf { it.isNotBlank() }
                    ?: run { withContext(Dispatchers.Main) { onResult(false, "无效的Skill文件，缺少name或description字段") }; return@launch }
                val desc = frontmatter["description"]?.takeIf { it.isNotBlank() }
                    ?: run { withContext(Dispatchers.Main) { onResult(false, "无效的Skill文件，缺少name或description字段") }; return@launch }

                val saved = skillManager.saveSkillFilesAtomically(name, adjustedFiles)
                _skills.value = skillManager.listSkills()
                withContext(Dispatchers.Main) {
                    onResult(saved, if (saved) name else "保存失败")
                }
            } catch (e: Exception) {
                android.util.Log.e("SkillsVM", "importFromFolder 异常: ${e.message}", e)
                withContext(Dispatchers.Main) { onResult(false, e.message ?: "导入失败") }
            }
        }
    }

    /** 从 DocumentsContract document ID 解析为真实 File */
    private fun parsePathFromDocumentId(docId: String): java.io.File? {
        // docId 格式: "primary:Download/myskill" 或 "home:Documents/myskill"
        val parts = docId.split(":", limit = 2)
        if (parts.size != 2) return null
        val storage = when (parts[0]) {
            "primary" -> android.os.Environment.getExternalStorageDirectory()
            "home" -> android.os.Environment.getExternalStorageDirectory()
            else -> {
                // 尝试 SD 卡等
                java.io.File("/storage/${parts[0]}")
                    .takeIf { it.exists() }
                    ?: return null
            }
        }
        val decodedPath = parts[1].replace("%2F", "/").replace("%3A", ":")
        return java.io.File(storage, decodedPath)
    }

    /**
     * 第一步: 扫描仓库中所有可用的 skill (Git Trees API, 1-2次请求)
     */
    fun scanSkillsFromGitHub(
        repoUrl: String,
        onResult: (Boolean, List<GitHubSkillInfo>, String) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val info = parseGitHubUrl(repoUrl) ?: run {
                    withContext(Dispatchers.Main) { onResult(false, emptyList(), "无效的 GitHub 仓库链接") }
                    return@launch
                }

                // 解析默认分支
                val branch = resolveBranch(info.owner, info.repo, info.branch)
                    ?: run {
                        withContext(Dispatchers.Main) { onResult(false, emptyList(), "无法获取仓库默认分支") }
                        return@launch
                    }

                // Git Trees API - 一次请求拿全部文件列表
                val treeJson = downloadText(
                    "https://api.github.com/repos/${info.owner}/${info.repo}/git/trees/$branch?recursive=1"
                ) ?: run {
                    withContext(Dispatchers.Main) { onResult(false, emptyList(), "读取仓库目录失败") }
                    return@launch
                }

                val tree = org.json.JSONObject(treeJson).getJSONArray("tree")
                val truncated = org.json.JSONObject(treeJson).optBoolean("truncated", false)

                if (truncated) {
                    withContext(Dispatchers.Main) { onResult(false, emptyList(), "仓库文件过多，暂不支持大仓库扫描") }
                    return@launch
                }

                // 找出所有 SKILL.md 的路径
                val skillMdPaths = mutableListOf<String>()
                for (i in 0 until tree.length()) {
                    val item = tree.getJSONObject(i)
                    val path = item.optString("path", "")
                    if (path.endsWith("SKILL.md") || path.endsWith("/SKILL.md")) {
                        skillMdPaths.add(path)
                    }
                }

                // 过滤: 如果 URL 指定了子路径，只保留该路径下的
                val filteredPaths = if (info.path.isBlank()) {
                    skillMdPaths
                } else {
                    skillMdPaths.filter { it.startsWith("${info.path}/") }
                }

                if (filteredPaths.isEmpty()) {
                    withContext(Dispatchers.Main) { onResult(false, emptyList(), "未找到 SKILL.md") }
                    return@launch
                }

                // 并发下载每个 SKILL.md 获取 name/description
                val results = coroutineScope {
                    filteredPaths.map { mdPath ->
                        async(Dispatchers.IO) {
                            val dirPath = mdPath.removeSuffix("SKILL.md").trimEnd('/')
                            val dlUrl = "https://raw.githubusercontent.com/${info.owner}/${info.repo}/$branch/$mdPath"
                            val content = downloadText(dlUrl) ?: return@async null
                            val fm = SkillFrontmatterParser.parse(content)
                            val name = fm["name"] ?: dirPath.split("/").last().ifBlank { "unknown" }
                            val desc = fm["description"] ?: ""
                            GitHubSkillInfo(name, desc, dirPath, mdPath)
                        }
                    }.awaitAll()
                }
                val skills = results.filterNotNull()

                withContext(Dispatchers.Main) {
                    onResult(true, skills, "${info.owner}/${info.repo}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, emptyList(), e.message ?: "扫描失败")
                }
            }
        }
    }

    /**
     * 第二步: 下载指定 skill 目录的全部文件
     */
    fun downloadSkillFromGitHub(
        repoUrl: String,
        skill: GitHubSkillInfo,
        onResult: (Boolean, String) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val info = parseGitHubUrl(repoUrl) ?: run {
                    withContext(Dispatchers.Main) { onResult(false, "无效的 GitHub 仓库链接") }
                    return@launch
                }

                val branch = resolveBranch(info.owner, info.repo, info.branch)
                    ?: run {
                        withContext(Dispatchers.Main) { onResult(false, "无法获取仓库默认分支") }
                        return@launch
                    }

                // Git Trees API - 一次拿全部文件路径
                val treeJson = downloadText(
                    "https://api.github.com/repos/${info.owner}/${info.repo}/git/trees/$branch?recursive=1"
                ) ?: run {
                    withContext(Dispatchers.Main) { onResult(false, "读取仓库目录失败") }
                    return@launch
                }
                val tree = org.json.JSONObject(treeJson).getJSONArray("tree")
                val dirPath = skill.dirPath.trimEnd('/')
                val prefix = if (dirPath.isBlank()) "" else "$dirPath/"

                val files = mutableListOf<Pair<String, String>>()
                for (i in 0 until tree.length()) {
                    val item = tree.getJSONObject(i)
                    val path = item.optString("path", "")
                    if (item.optString("type") == "blob" && path.startsWith(prefix)) {
                        val relativePath = path.removePrefix(prefix)
                        val downloadUrl = "https://raw.githubusercontent.com/${info.owner}/${info.repo}/$branch/$path"
                        files.add(relativePath to downloadUrl)
                    }
                }
                if (files.isEmpty()) {
                    withContext(Dispatchers.Main) { onResult(false, "目录中未找到文件") }
                    return@launch
                }

                val fileContents = LinkedHashMap<String, String>()
                val sem = Semaphore(5)
                val results = coroutineScope {
                    files.map { (path, url) ->
                        async(Dispatchers.IO) {
                            sem.withPermit { path to downloadText(url) }
                        }
                    }.awaitAll()
                }
                for ((relativePath, content) in results) {
                    if (content == null) {
                        withContext(Dispatchers.Main) { onResult(false, "下载文件失败：$relativePath") }
                        return@launch
                    }
                    fileContents[relativePath] = content
                }

                val saved = skillManager.saveSkillFilesAtomically(skill.name, fileContents)
                if (!saved) {
                    withContext(Dispatchers.Main) { onResult(false, "保存失败") }
                    return@launch
                }

                _skills.value = skillManager.listSkills()
                // 自动启用到当前助手
                val currentSettings = settingsStore.settingsFlow.value
                if (currentSettings.init) { /* dummy, 跳过 */ }
                else {
                    val updated = currentSettings.copy(
                        assistants = currentSettings.assistants.map { a ->
                            if (a.id == currentSettings.assistantId)
                                a.copy(enabledSkills = a.enabledSkills + skill.name)
                            else a
                        }
                    )
                    settingsStore.update(updated)
                }
                withContext(Dispatchers.Main) { onResult(true, skill.name) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.message ?: "下载失败") }
            }
        }
    }

    /** 兼容旧接口: 粘贴的链接正好指向一个 skill 目录时直接下载 */
    fun importSkillFromGitHub(repoUrl: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val info = parseGitHubUrl(repoUrl) ?: run {
                    withContext(Dispatchers.Main) { onResult(false, "无效的 GitHub 仓库链接") }
                    return@launch
                }
                val branch = resolveBranch(info.owner, info.repo, info.branch)
                    ?: run {
                        withContext(Dispatchers.Main) { onResult(false, "无法获取仓库默认分支") }
                        return@launch
                    }

                val files = mutableListOf<Pair<String, String>>()
                val listed = listFilesRecursively(info.owner, info.repo, branch, info.path, info.path, files)
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
                val sem = Semaphore(5)
                val results = coroutineScope {
                    files.map { (path, url) ->
                        async(Dispatchers.IO) {
                            sem.withPermit { path to downloadText(url) }
                        }
                    }.awaitAll()
                }
                for ((relativePath, content) in results) {
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
                // 自动启用到当前助手
                val currentSettings = settingsStore.settingsFlow.value
                if (!currentSettings.init) {
                    val updated = currentSettings.copy(
                        assistants = currentSettings.assistants.map { a ->
                            if (a.id == currentSettings.assistantId)
                                a.copy(enabledSkills = a.enabledSkills + name)
                            else a
                        }
                    )
                    settingsStore.update(updated)
                }
                withContext(Dispatchers.Main) { onResult(true, name) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.message ?: "未知错误") }
            }
        }
    }

    private data class GitHubRepoInfo(
        val owner: String,
        val repo: String,
        val branch: String?,  // null = 用户没指定，让 API 自行判断默认分支
        val path: String,
    )

    data class GitHubSkillInfo(
        val name: String,
        val description: String,
        val dirPath: String,  // 目录路径，如 "plugins/skill-creator/skills/skill-creator"
        val mdPath: String,   // SKILL.md 完整路径
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
        val branch = match.groupValues[3].takeIf { it.isNotBlank() }  // null = 用 API 默认分支
        val subPath = match.groupValues[4].trimStart('/')
        return GitHubRepoInfo(owner, repo, branch, subPath)
    }

    /** 解析分支: 用户指定了就用，没指定调 API 查默认分支 */
    private fun resolveBranch(owner: String, repo: String, userBranch: String?): String? {
        if (userBranch != null) return userBranch
        val repoJson = downloadText("https://api.github.com/repos/$owner/$repo") ?: return null
        return org.json.JSONObject(repoJson).optString("default_branch", null)
    }

    private fun listFilesRecursively(
        owner: String,
        repo: String,
        branch: String?,
        dirPath: String,
        basePath: String,
        result: MutableList<Pair<String, String>>,
    ): Boolean {
        val refParam = if (branch != null) "?ref=$branch" else ""
        val apiPath = dirPath.ifBlank { "" }
        val apiUrl = "https://api.github.com/repos/$owner/$repo/contents/$apiPath$refParam"
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
