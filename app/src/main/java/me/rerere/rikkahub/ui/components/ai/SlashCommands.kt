package me.rerere.rikkahub.ui.components.ai

import me.rerere.rikkahub.data.files.CommandFile

/**
 * 斜杠命令 — 来自 skill 的 commands/*.md
 * Claude Code 风格：每个 commands/xxx.md 自动变成 /xxx 命令
 */
data class SlashCommand(
    val name: String,                    // 命令名，如 "code-review"
    val description: String,             // 简短描述
    val allowedTools: List<String>,      // 允许的工具
    val argumentHint: String = "",       // 参数提示，如 "[project-name]"
    val disableModelInvocation: Boolean = false, // 纯脚本不调模型
    val content: String,                 // 指令正文
    val filePath: String,                // 源文件路径，如 "commands/code-review.md"
)

/**
 * 从已启用的 skills 中收集所有 commands 作为斜杠命令
 */
fun collectSlashCommands(
    enabledSkills: List<me.rerere.rikkahub.data.files.SkillMetadata>,
): List<SlashCommand> {
    return enabledSkills.flatMap { skill ->
        skill.commands.map { cmd ->
            SlashCommand(
                name = cmd.name,
                description = cmd.description,
                allowedTools = cmd.allowedTools,
                argumentHint = cmd.argumentHint,
                disableModelInvocation = cmd.disableModelInvocation,
                content = cmd.content,
                filePath = cmd.filePath,
            )
        }
    }
}

/**
 * 匹配用户输入的斜杠命令
 */
fun matchSlashCommand(
    input: String,
    commands: List<SlashCommand>,
): SlashCommand? {
    val trimmed = input.trimStart()
    if (!trimmed.startsWith("/")) return null
    val name = trimmed.substring(1).split(" ").first().lowercase()
    return commands.find { it.name.lowercase() == name }
}
