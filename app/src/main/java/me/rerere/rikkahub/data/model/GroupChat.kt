package me.rerere.rikkahub.data.model

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * 群聊配置
 */
@Serializable
data class GroupChat(
    val id: Uuid = Uuid.random(),
    val name: String = "",
    val memberIds: List<Uuid> = emptyList(),    // 引用 assistants
    val autoSpeakerMode: GroupSpeakerMode = GroupSpeakerMode.ROUND_ROBIN,
    val enabled: Boolean = true,
)

@Serializable
enum class GroupSpeakerMode {
    ROUND_ROBIN,   // 轮流转，每人一轮
    MANUAL,        // 用户手动选择下一个发言人
}

/**
 * 群聊会话 — 扩展 Conversation 支持多发言者
 */
@Serializable
data class GroupConversation(
    val id: Uuid = Uuid.random(),
    val groupId: Uuid,
    val title: String = "",
    val speakerQueue: List<Int> = emptyList(),  // member index queue for round robin
    val currentSpeakerIndex: Int = 0,
    val lastSpeakerId: Uuid? = null,
)
