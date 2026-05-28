package me.rerere.rikkahub.data.files

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Claude Code plugin.json 格式支持
 * 与 SKILL.md 互补——plugin.json 提供元数据，commands 目录下的 .md 文件提供指令
 */
@Serializable
data class PluginManifest(
    val name: String = "",
    val description: String = "",
    val version: String = "",
    val author: PluginAuthor? = null,
    val category: String? = null,
    val triggers: List<String> = emptyList(),
    val injectPosition: String? = null,
    val allowedTools: List<String> = emptyList(),
    val mcpServers: List<PluginMcpServer> = emptyList(),
)

@Serializable
data class PluginAuthor(
    val name: String = "",
    val email: String = "",
)

/**
 * Skill 在 plugin.json 中声明的 MCP 服务器
 * 安装 skill 时会显示这些服务器信息，可在 MCP 设置页一键注册
 */
@Serializable
data class PluginMcpServer(
    val name: String = "",
    val transport: String = "sse",     // "sse" or "streamable_http"
    val url: String = "",
)

/**
 * 解析 plugin.json（如果存在）
 */
fun parsePluginManifest(skillDir: File): PluginManifest? {
    val file = skillDir.resolve("plugin.json")
    if (!file.exists()) return null
    return try {
        Json { ignoreUnknownKeys = true }.decodeFromString(
            PluginManifest.serializer(), file.readText()
        )
    } catch (_: Exception) { null }
}

/**
 * 列出 commands/ 目录下的 .md 文件
 */
fun listCommands(skillDir: File): List<CommandFile> {
    val dir = skillDir.resolve("commands")
    if (!dir.isDirectory) return emptyList()
    return dir.listFiles()
        ?.filter { it.isFile && it.name.endsWith(".md") }
        ?.mapNotNull { file ->
            val content = file.readText()
            val frontmatter = SkillFrontmatterParser.parse(content)
            CommandFile(
                name = file.nameWithoutExtension,
                description = frontmatter["description"] ?: file.nameWithoutExtension,
                allowedTools = frontmatter["allowed-tools"]?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList(),
                argumentHint = frontmatter["argument-hint"] ?: "",
                disableModelInvocation = frontmatter["disable-model-invocation"]?.toBooleanStrictOrNull() ?: false,
                content = SkillFrontmatterParser.extractBody(content),
                filePath = "commands/${file.name}",
            )
        }
        ?: emptyList()
}

data class CommandFile(
    val name: String,
    val description: String,
    val allowedTools: List<String> = emptyList(),
    val argumentHint: String = "",
    val disableModelInvocation: Boolean = false,
    val content: String,
    val filePath: String,
)
