package me.rerere.rikkahub.data.ai.transformers

import me.rerere.ai.ui.UIMessage
import me.rerere.rikkahub.data.files.SkillManager
import me.rerere.rikkahub.data.files.SkillMetadata
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Skill 自动触发转换器 — Claude Code 风格
 * 扫描对话上下文中的触发关键词，匹配到的 skill 自动注入 SKILL.md body。
 * 无需 LLM 手动调用 use_skill tool。
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
        val autoTriggered = mutableListOf<SkillMetadata>()
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
                        else -> afterSystem.add(skill)  // 默认 after_system
                    }
                }
            }
        }

        if (autoTriggered.isEmpty() && beforeSystem.isEmpty() && afterSystem.isEmpty() && inChat.isEmpty()) {
            return messages
        }

        // 构建注入
        val result = mutableListOf<UIMessage>()

        // Before system
        beforeSystem.forEach { skill ->
            val body = skillManager.readSkillBody(skill.name) ?: return@forEach
            result.add(UIMessage.system("[Skill: ${skill.name}]\n$body"))
        }

        // Original messages (system prompt first, then rest)
        if (messages.isNotEmpty()) {
            result.add(messages.first())  // system prompt
        }

        // After system
        afterSystem.forEach { skill ->
            val body = skillManager.readSkillBody(skill.name) ?: return@forEach
            result.add(UIMessage.user("[Skill: ${skill.name}]\n$body"))
        }

        // Rest of messages
        if (messages.size > 1) {
            result.addAll(messages.drop(1))
        }

        // In-chat skills (inserted near the end, before last user message)
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
