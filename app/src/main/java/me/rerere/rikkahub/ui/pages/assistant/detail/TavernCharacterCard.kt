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
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.TavernEmbeddedBook
import me.rerere.rikkahub.ui.theme.CustomColors

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

@Composable
private fun EmbeddedBookSummary(book: TavernEmbeddedBook) {
    var showEntries by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showEntries = !showEntries },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("📖 内嵌世界书", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(6.dp))
            Text(
                text = "${book.entries.size}条",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
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
                modifier = Modifier.padding(top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                book.entries.take(15).forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        // 触发词
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                entry.keys.firstOrNull()?.let { key ->
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    ) {
                                        Text(
                                            text = key,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                                if (entry.constant) Text("🔒", style = MaterialTheme.typography.labelSmall)
                            }
                            if (entry.comment.isNotBlank()) {
                                Text(
                                    text = entry.comment,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        // 内容预览
                        Text(
                            text = entry.content.take(60).replace("\n", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 140.dp),
                        )
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
}
