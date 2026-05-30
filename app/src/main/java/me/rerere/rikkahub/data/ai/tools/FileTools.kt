package me.rerere.rikkahub.data.ai.tools

import kotlinx.serialization.json.*
import me.rerere.ai.core.InputSchema
import me.rerere.ai.core.Tool
import me.rerere.ai.ui.UIMessagePart
import java.io.File

/**
 * 文件操作工具 — file_read, file_write, file_list
 * AI 可直接读写 Android 文件系统中的文件。
 * skillDirs: 已启用的 skill 目录列表，用于解析相对路径（优先检索）
 */
fun createFileTools(skillDirs: List<String> = emptyList()): List<Tool> {
    val defaultDir = "/storage/emulated/0/Download"

    fun resolveFile(path: String): File {
        val f = File(path)
        if (f.exists() || path.startsWith("/")) return f
        // 相对路径 → 依次检索 skill 目录
        for (skillDir in skillDirs) {
            val candidate = File(skillDir, path)
            if (candidate.exists()) return candidate
        }
        // 兜底 Download
        return File(defaultDir, path)
    }

    return listOf(
        // ── file_read ──
        Tool(
            name = "file_read",
            description = "Read a file from the Android filesystem. Returns the file content as text. " +
                "Absolute paths work as-is. Relative paths are resolved against enabled skill directories first, then ${defaultDir}.",
            parameters = {
                InputSchema.Obj(
                    properties = buildJsonObject {
                        put("path", buildJsonObject {
                            put("type", "string")
                            put("description", "Absolute or relative file path.")
                        })
                    },
                    required = listOf("path"),
                )
            },
            execute = { args ->
                val path = args.jsonObject["path"]?.jsonPrimitive?.content
                    ?: error("path required")
                val file = resolveFile(path)
                if (!file.exists()) error("File not found: $path")
                if (!file.canRead()) error("Cannot read file: $path")
                if (file.isDirectory) error("Path is a directory, not a file: $path")
                if (file.length() > 5 * 1024 * 1024) error("文件超过 5MB，为防止内存溢出无法读取: $path")
                val content = file.readText()
                listOf(UIMessagePart.Text(content))
            },
        ),

        // ── file_write ──
        Tool(
            name = "file_write",
            description = "Create or overwrite a file on the Android filesystem. " +
                    "Relative paths go to ${defaultDir}.",
            parameters = {
                InputSchema.Obj(
                    properties = buildJsonObject {
                        put("path", buildJsonObject {
                            put("type", "string")
                            put("description", "File path. If relative (no leading /), saved under ${defaultDir}/")
                        })
                        put("content", buildJsonObject {
                            put("type", "string")
                            put("description", "Text content to write to the file")
                        })
                    },
                    required = listOf("path", "content"),
                )
            },
            execute = { args ->
                val obj = args.jsonObject
                val rawPath = obj["path"]?.jsonPrimitive?.content
                    ?: error("path required")
                val content = obj["content"]?.jsonPrimitive?.content
                    ?: error("content required")

                val path = if (rawPath.startsWith("/")) rawPath
                else "${defaultDir.trimEnd('/')}/${rawPath.trimStart('/')}"

                val file = File(path)
                file.parentFile?.mkdirs()
                file.writeText(content)
                listOf(UIMessagePart.Text("OK: wrote ${content.length} bytes to $path"))
            },
        ),

        // ── file_list ──
        Tool(
            name = "file_list",
            description = "List files and directories in a given directory. " +
                    "Default: ${defaultDir}/. " +
                    "Skill directories available: ${skillDirs.joinToString()}.",
            parameters = {
                InputSchema.Obj(
                    properties = buildJsonObject {
                        put("dir", buildJsonObject {
                            put("type", "string")
                            put("description", "Directory to list. Defaults to ${defaultDir} if empty.")
                        })
                    },
                    required = emptyList(),
                )
            },
            execute = { args ->
                val dirPath = args.jsonObject["dir"]?.jsonPrimitive?.content
                    ?.takeIf { it.isNotBlank() } ?: defaultDir

                val dir = resolveFile(dirPath)
                if (!dir.exists()) error("Directory not found: $dirPath")
                if (!dir.isDirectory) error("Not a directory: $dirPath")

                val entries = dir.listFiles()?.sortedBy { it.name } ?: emptyList()
                val listing = buildString {
                    appendLine("Contents of ${dir.absolutePath} (${entries.size} items):")
                    appendLine()
                    for (f in entries) {
                        val prefix = if (f.isDirectory) "📁" else "📄"
                        val size = if (f.isFile) " (${formatSize(f.length())})" else ""
                        appendLine("$prefix ${f.name}$size")
                    }
                }
                listOf(UIMessagePart.Text(listing))
            },
        ),
    )
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${bytes / (1024 * 1024)} MB"
}
