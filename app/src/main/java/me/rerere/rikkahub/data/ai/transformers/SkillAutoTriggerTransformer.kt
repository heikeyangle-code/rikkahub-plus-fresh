package me.rerere.rikkahub.data.ai.transformers

import me.rerere.ai.ui.UIMessage
import me.rerere.rikkahub.data.files.SkillManager
import me.rerere.rikkahub.data.files.SkillMetadata
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Skill 自动触发转换器 — Claude Code 风格
 * 两条路径：
 * 1. 有关键词的：扫描对话匹配，命中后直接注入 SKILL.md body
 * 2. 无关键词的：注入 skill 列表描述到系统提示，LLM 自选 + use_skill 调用
 */
object SkillAutoTriggerTransformer : InputMessageTransformer, KoinComponent {

    private val skillManager: SkillManager by inject()

    override suspend fun transform(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> {
        val enabledNames = ctx.assistant.enabledSkills
        if (enabledNames.isEmpty()) return messages

        val allSkills = skillManager.listSkills()
        val enabledSkills = allSkills.filter { it.name in enabledNames }
        if (enabledSkills.isEmpty()) return messages

        // 拼接上下文用于匹配
        val context = messages.joinToString("\n") { it.toText() }

        // 分类 skill
        val beforeSystem = mutableListOf<SkillMetadata>()
        val afterSystem = mutableListOf<SkillMetadata>()
        val inChat = mutableListOf<SkillMetadata>()
        val selfSelectSkills = mutableListOf<SkillMetadata>()

        for (skill in enabledSkills) {
            // 有触发词的：检测是否匹配
            if (skill.triggers.isNotEmpty()) {
                val matched = skill.triggers.any { trigger ->
                    context.contains(trigger, ignoreCase = true)
                }
                if (matched) {
                    when (skill.injectPosition) {
                        "before_system" -> beforeSystem.add(skill)
                        "in_chat" -> inChat.add(skill)
                        else -> afterSystem.add(skill)
                    }
                }
            } else {
                // 无触发词的：LLM 自选模式
                selfSelectSkills.add(skill)
            }
        }

        // LLM 自选：注入可用 skill 列表到系统提示
        val selfSelectText = if (selfSelectSkills.isNotEmpty()) {
            buildString {
                appendLine("[Available Skills]")
                appendLine("You can use the use_skill tool to activate any of these skills when relevant:")
                selfSelectSkills.forEach { skill ->
                    appendLine("- ${skill.name}: ${skill.description}")
                }
            }
        } else ""

        if (beforeSystem.isEmpty() && afterSystem.isEmpty() && inChat.isEmpty() && selfSelectText.isEmpty()) {
            return messages
        }

        // 构建注入
        val result = mutableListOf<UIMessage>()

        // Before system
        beforeSystem.forEach { skill ->
            val body = skillManager.readSkillBody(skill.name) ?: return@forEach
            result.add(UIMessage.system("[Skill: ${skill.name}]\n$body"))
        }

        // System prompt（注入自选列表）
        if (messages.isNotEmpty()) {
            val sysMsg = messages.first()
            if (selfSelectText.isNotEmpty()) {
                val original = sysMsg.toText()
                result.add(UIMessage.system("$original\n\n$selfSelectText"))
            } else {
                result.add(sysMsg)
            }
        }

        // After system
        afterSystem.forEach { skill ->
            val body = skillManager.readSkillBody(skill.name) ?: return@forEach
            result.add(UIMessage.system("[Skill: ${skill.name}]\n$body"))
        }

        // Rest of messages
        if (messages.size > 1) {
            result.addAll(messages.drop(1))
        }

        // In-chat skills
        if (inChat.isNotEmpty()) {
            val insertIdx = (result.size - 1).coerceAtLeast(0)
            inChat.forEach { skill ->
                val body = skillManager.readSkillBody(skill.name) ?: return@forEach
                result.add(insertIdx, UIMessage.user("[Skill: ${skill.name}]\n$body"))
            }
        }

        return result
    }
}
