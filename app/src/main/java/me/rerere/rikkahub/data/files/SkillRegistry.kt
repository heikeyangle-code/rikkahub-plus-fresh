package me.rerere.rikkahub.data.files

/**
 * 内置 Skill 注册表 — 可浏览安装的官方/社区 Skill
 */
object SkillRegistry {
    data class RegistryEntry(
        val name: String,
        val description: String,
        val category: String,
        val author: String,
        val version: String,
        val triggers: List<String> = emptyList(),
        val githubUrl: String,
    )

    val entries: List<RegistryEntry> = emptyList()

    fun search(query: String): List<RegistryEntry> {
        return emptyList()
    }

    fun byCategory(): Map<String, List<RegistryEntry>> = emptyMap()
}
