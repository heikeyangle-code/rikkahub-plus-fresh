package me.rerere.rikkahub.data.ai.transformers

import me.rerere.ai.ui.UIMessage
import me.rerere.rikkahub.data.files.SkillManager
import me.rerere.rikkahub.data.files.SkillMetadata
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Skill 自动触发转换器 — Claude Code 风格
 * 有关键词的：扫描对话匹配，命中后直接注入 SKILL.md body
 * 无关键词的：由 use_skill 工具处理（工具 systemPrompt 列出全部 skill）
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
            }
            // 无触发词的：由 use_skill 工具处理，不在此注入
        }

        if (beforeSystem.isEmpty() && afterSystem.isEmpty() && inChat.isEmpty()) {
            return messages
        }

        // 构建注入
        val result = mutableListOf<UIMessage>()

        // Before system
        beforeSystem.forEach { skill ->
            val body = skillManager.readSkillBody(skill.name) ?: return@forEach
            result.add(UIMessage.system("[Skill: ${skill.name}]\n$body"))
        }

        // System prompt
        if (messages.isNotEmpty()) {
            result.add(messages.first())
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
