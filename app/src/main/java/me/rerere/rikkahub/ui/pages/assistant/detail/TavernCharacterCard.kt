package me.rerere.rikkahub.ui.pages.assistant.detail

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.TavernBookEntry
import me.rerere.rikkahub.data.model.TavernEmbeddedBook

/**
 * 酒馆角色卡信息面板 — 简洁高级，分层展示
 */
@Composable
fun TavernCharacterCard(
    assistant: Assistant,
    modifier: Modifier = Modifier,
) {
    val tav = assistant.tavernData ?: return
    var expanded by remember { mutableStateOf(false) }
    var activeTab by remember { mutableIntStateOf(0) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            // 紧凑头部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🧩 角色卡",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = tav.spec.removePrefix("chara_card_").uppercase(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (tav.creator.isNotBlank()) {
                            Text(
                                text = "· ${tav.creator}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (tav.characterVersion.isNotBlank()) {
                            Text(
                                text = "· v${tav.characterVersion}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    // 统计行
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatBadge("描述", tav.description.isNotBlank())
                        StatBadge("性格", tav.personality.isNotBlank())
                        StatBadge("场景", tav.scenario.isNotBlank())
                        StatBadge("示例", tav.mesExample.isNotBlank())
                        if (tav.embeddedBook != null) {
                            StatBadge("世界书·${tav.embeddedBook!!.entries.size}", true)
                        }
                        if (tav.alternateGreetings.isNotEmpty()) {
                            StatBadge("开场·${1 + tav.alternateGreetings.size}", true)
                        }
                    }
                }
                IconButton(onClick = { expanded = !expanded }) {
                    Text(
                        text = if (expanded) "▲" else "▼",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            // 展开内容
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    HorizontalDivider()

                    // 标签
                    if (tav.tags.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            tav.tags.forEach { tag ->
                                SuggestionChip(
                                    onClick = {},
                                    label = {
                                        Text(tag, style = MaterialTheme.typography.labelSmall)
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                )
                            }
                        }
                        HorizontalDivider()
                    }

                    // Tab 切换
                    val tabs = buildList {
                        if (tav.description.isNotBlank()) add("📝 描述")
                        if (tav.personality.isNotBlank()) add("🎭 性格")
                        if (tav.scenario.isNotBlank()) add("🎬 场景")
                        if (tav.mesExample.isNotBlank()) add("💬 示例")
                        if (tav.firstMessage.isNotBlank()) add("👋 开场")
                    }
                    if (tabs.size > 1) {
                        ScrollableTabRow(
                            selectedTabIndex = activeTab.coerceIn(0, tabs.size - 1),
                            modifier = Modifier.fillMaxWidth(),
                            edgePadding = 14.dp,
                            divider = {},
                        ) {
                            tabs.forEachIndexed { i, label ->
                                Tab(
                                    selected = activeTab == i,
                                    onClick = { activeTab = i },
                                    text = {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                        )
                                    },
                                )
                            }
                        }
                    }

                    // Tab 内容
                    val contentMap = buildMap {
                        if (tav.description.isNotBlank()) put("📝 描述", tav.description)
                        if (tav.personality.isNotBlank()) put("🎭 性格", tav.personality)
                        if (tav.scenario.isNotBlank()) put("🎬 场景", tav.scenario)
                        if (tav.mesExample.isNotBlank()) put("💬 示例", tav.mesExample)
                        if (tav.firstMessage.isNotBlank()) put("👋 开场", tav.firstMessage)
                    }
                    val selectedKey = tabs.getOrNull(activeTab) ?: tabs.firstOrNull() ?: ""

                    contentMap[selectedKey]?.let { content ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        ) {
                            Text(
                                text = content,
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                            )
                        }
                    }

                    // 备选开场白
                    if (tav.alternateGreetings.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            text = "备选开场白 (${tav.alternateGreetings.size})",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        tav.alternateGreetings.forEachIndexed { i, greeting ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 3.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            ) {
                                Text(
                                    text = "${i + 1}. ${greeting.take(120)}${if (greeting.length > 120) "…" else ""}",
                                    modifier = Modifier.padding(10.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    // 内嵌世界书
                    tav.embeddedBook?.let { book ->
                        HorizontalDivider()
                        EmbeddedBookSummary(book)
                    }

                    // creator notes
                    if (tav.creatorNotes.isNotBlank()) {
                        HorizontalDivider()
                        Text(
                            text = "📝 ${tav.creator} 的备注",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = tav.creatorNotes,
                            modifier = Modifier.padding(horizontal = 14.dp).padding(bottom = 14.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, active: Boolean) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = if (active) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EmbeddedBookSummary(book: TavernEmbeddedBook) {
    var showEntries by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<TavernBookEntry?>(null) }

    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showEntries = !showEntries },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("📖 内嵌世界书", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(8.dp))
            FilledTonalChip(
                onClick = { showEntries = !showEntries },
                label = {
                    Text(
                        text = "${book.entries.size}",
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                shape = RoundedCornerShape(8.dp),
            )
            if (book.name.isNotBlank()) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "· ${book.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = if (showEntries) "▲" else "▼",
                style = MaterialTheme.typography.labelSmall,
            )
        }

        AnimatedVisibility(visible = showEntries) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                book.entries.take(15).forEach { entry ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedEntry = entry },
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                entry.keys.firstOrNull()?.let { key ->
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (entry.group.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    ) {
                                        Text(
                                            text = entry.group,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = entry.content.replace("\n", " "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 200.dp),
                            )
                        }
                    }
                }
                if (book.entries.size > 15) {
                    Text(
                        text = "... 还有 ${book.entries.size - 15} 条",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selectedEntry = null },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        ) {
            EmbeddedEntryDetail(entry)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmbeddedEntryDetail(entry: TavernBookEntry) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Title
        Text(
            text = entry.comment.ifBlank { entry.keys.firstOrNull() ?: "未命名条目" },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))

        // Enable/disable badge
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (entry.disable) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
        ) {
            Text(
                text = if (entry.disable) "禁用" else "启用",
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                color = if (entry.disable) MaterialTheme.colorScheme.outline
                else MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.height(16.dp))

        // 📝 核心内容
        SectionDivider("📝 核心内容")
        Spacer(Modifier.height(8.dp))

        FieldLabel("主关键词")
        Spacer(Modifier.height(4.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            entry.keys.forEach { key ->
                SuggestionChip(
                    onClick = {},
                    label = { Text(key, style = MaterialTheme.typography.labelSmall) },
                    shape = RoundedCornerShape(6.dp),
                )
            }
        }

        if (entry.secondaryKeys.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FieldLabel("次要关键词")
            Spacer(Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                entry.secondaryKeys.forEach { key ->
                    SuggestionChip(
                        onClick = {},
                        label = { Text(key, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(6.dp),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        FieldLabel("次要关键词逻辑")
        Spacer(Modifier.height(4.dp))
        FilledTonalChip(
            onClick = {},
            label = {
                Text(
                    text = when (entry.selectiveLogic) {
                        0 -> "AND"
                        1 -> "OR"
                        2 -> "NOT_ANY"
                        3 -> "NOT_ALL"
                        else -> "AND"
                    },
                    style = MaterialTheme.typography.labelSmall,
                )
            },
            shape = RoundedCornerShape(6.dp),
        )

        Spacer(Modifier.height(8.dp))
        FieldLabel("内容")
        Spacer(Modifier.height(4.dp))
        var expanded by remember { mutableStateOf(false) }
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            val displayContent = if (!expanded && entry.content.lines().size > 10) {
                entry.content.lines().take(10).joinToString("\n") + "\n..."
            } else {
                entry.content
            }
            Text(
                text = displayContent,
                modifier = Modifier
                    .padding(12.dp)
                    .clickable { expanded = !expanded },
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(16.dp))

        // ⚙️ 触发设置
        SectionDivider("⚙️ 触发设置")
        Spacer(Modifier.height(8.dp))

        InfoRow("策略") {
            val (icon, label) = when {
                entry.constant -> "🔵" to "常驻"
                entry.selective -> "🟢" to "关键词触发"
                else -> "🔗" to "向量"
            }
            Text("$icon  $label", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(6.dp))
        InfoRow("触发概率") {
            Column {
                LinearProgressIndicator(
                    progress = { entry.probability / 100f },
                    modifier = Modifier.fillMaxWidth(0.6f),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${entry.probability}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        InfoRow("扫描深度") {
            Text(
                text = "${entry.scanDepth ?: entry.depth}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(6.dp))
        InfoRow("插入位置") {
            val posText = when (entry.position) {
                0 -> "角色前"
                1 -> "角色后"
                2 -> "用户前"
                3 -> "用户后"
                4 -> "@D 深度插入"
                else -> "未知"
            }
            Text(posText, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(6.dp))
        InfoRow("插入顺序") {
            Text("${entry.priority}", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(16.dp))

        // 🗂️ 分组
        SectionDivider("🗂️ 分组")
        Spacer(Modifier.height(8.dp))

        InfoRow("分组") {
            Text(
                text = entry.group.ifBlank { "无" },
                style = MaterialTheme.typography.bodyMedium,
                color = if (entry.group.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurface,
            )
        }

        Spacer(Modifier.height(6.dp))
        InfoRow("组权重") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${entry.groupWeight}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (entry.group.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = { entry.groupWeight / 100f },
                        modifier = Modifier.width(60.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        InfoRow("组覆盖") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (entry.groupOverride) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = "覆盖同组",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                } else {
                    Text(
                        text = "否",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        InfoRow("角色") {
            val roleText = when (entry.role) {
                "system" -> "System"
                "user" -> "User"
                "assistant" -> "Assistant"
                else -> entry.role
            }
            Text(roleText, style = MaterialTheme.typography.bodyMedium)
        }

        // ⏱️ 定时效果 (only show if any has value)
        val hasTiming = entry.sticky || entry.cooldown > 0 || entry.delayUntil > 0
        if (hasTiming) {
            Spacer(Modifier.height(16.dp))
            SectionDivider("⏱️ 定时效果")
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TimingChip("粘性", if (entry.sticky) "是" else "否", entry.sticky)
                if (entry.cooldown > 0) {
                    TimingChip("冷却", "${entry.cooldown}轮", true)
                }
                if (entry.delayUntil > 0) {
                    TimingChip("延迟", "${entry.delayUntil}轮", true)
                }
            }
        }

        // 底部留白
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionDivider(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(16.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(1.dp)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
    )
}

@Composable
private fun InfoRow(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
        Spacer(Modifier.height(2.dp))
        content()
    }
}

@Composable
private fun TimingChip(label: String, value: String, active: Boolean) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (active) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
        else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (active) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}
