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

    val entries: List<RegistryEntry> = listOf(
        RegistryEntry(
            name = "web-search",
            description = "搜索互联网获取实时信息。支持百度/Google等多种搜索引擎。",
            category = "搜索",
            author = "RikkaHub",
            version = "1.0",
            triggers = listOf("搜索", "百度", "查一下", "搜一下"),
            githubUrl = "https://github.com/rikkahub/skills/tree/main/web-search",
        ),
        RegistryEntry(
            name = "code-review",
            description = "自动代码审查：检查安全漏洞、代码质量、最佳实践。",
            category = "开发",
            author = "RikkaHub",
            version = "1.0",
            triggers = listOf("review", "审查", "code review", "检查代码"),
            githubUrl = "https://github.com/rikkahub/skills/tree/main/code-review",
        ),
        RegistryEntry(
            name = "translation",
            description = "高质量多语言翻译，支持中英日韩等主流语言。",
            category = "语言",
            author = "RikkaHub",
            version = "1.0",
            triggers = listOf("翻译", "translate", "译"),
            githubUrl = "https://github.com/rikkahub/skills/tree/main/translation",
        ),
        RegistryEntry(
            name = "summarization",
            description = "智能摘要：长文本、对话记录、会议纪要一键生成摘要。",
            category = "写作",
            author = "RikkaHub",
            version = "1.0",
            triggers = listOf("摘要", "总结", "概括", "summarize"),
            githubUrl = "https://github.com/rikkahub/skills/tree/main/summarization",
        ),
        RegistryEntry(
            name = "data-analysis",
            description = "数据分析：CSV/JSON数据探索、可视化建议、统计分析。",
            category = "数据分析",
            author = "RikkaHub",
            version = "1.0",
            triggers = listOf("分析", "数据", "统计", "可视化"),
            githubUrl = "https://github.com/rikkahub/skills/tree/main/data-analysis",
        ),
    )

    fun search(query: String): List<RegistryEntry> {
        val q = query.lowercase()
        return entries.filter {
            it.name.lowercase().contains(q) ||
            it.description.lowercase().contains(q) ||
            it.category.lowercase().contains(q) ||
            it.triggers.any { t -> t.lowercase().contains(q) }
        }
    }

    fun byCategory(): Map<String, List<RegistryEntry>> = entries.groupBy { it.category }
}
