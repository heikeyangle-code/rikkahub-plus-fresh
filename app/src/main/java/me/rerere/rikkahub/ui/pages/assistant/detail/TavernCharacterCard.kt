@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package me.rerere.rikkahub.ui.pages.assistant.detail

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.dp
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
    onAssistantUpdate: ((Assistant) -> Unit)? = null,
) {
    val tav = assistant.tavernData ?: return
    var expanded by remember { mutableStateOf(false) }

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

                    // 可折叠字段卡片 — 替代 Tab 切换
                    if (tav.systemPrompt.isNotBlank()) {
                        CollapsibleField("💻 系统提示词", tav.systemPrompt)
                    }
                    CollapsibleField("📝 描述", tav.description)
                    CollapsibleField("🎭 性格", tav.personality)
                    CollapsibleField("🎬 场景", tav.scenario)
                    CollapsibleField("💬 示例消息", tav.mesExample)
                    if (tav.postHistoryInstructions.isNotBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))
                        CollapsibleField("📋 历史后续指令", tav.postHistoryInstructions)
                    }
                    if (tav.firstMessage.isNotBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp))
                        CollapsibleField("👋 开场白", tav.firstMessage)
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

                    // 群聊专用开场白
                    if (tav.groupOnlyGreetings.isNotEmpty()) {
                        HorizontalDivider()
                        Text(
                            text = "群聊专用开场白 (${tav.groupOnlyGreetings.size})",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        tav.groupOnlyGreetings.forEachIndexed { i, greeting ->
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
                        EmbeddedBookSummary(
                            book = book,
                            onEntryUpdate = { updated ->
                                val tav = assistant.tavernData ?: return@EmbeddedBookSummary
                                val oldBook = tav.embeddedBook ?: return@EmbeddedBookSummary
                                val newEntries = oldBook.entries.map {
                                    if (it.id == updated.id) updated else it
                                }
                                val newBook = oldBook.copy(entries = newEntries)
                                val newTav = tav.copy(embeddedBook = newBook)
                                onAssistantUpdate?.invoke(assistant.copy(tavernData = newTav))
                            },
                        )
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

/**
 * 可折叠字段组件 — 默认收起，点击展开显示全部
 */
@Composable
private fun CollapsibleField(label: String, content: String) {
    if (content.isBlank()) return
    var expanded by remember { mutableStateOf(false) }
    val previewLines = 3

    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (expanded) "▲ 收起" else "▼ 展开",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (expanded) content else content.lines().take(previewLines).joinToString("\n")
                .let { if (it.length < content.length) "$it…" else it },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            maxLines = if (expanded) Int.MAX_VALUE else previewLines,
            overflow = TextOverflow.Ellipsis,
        )
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
private fun EmbeddedBookSummary(
    book: TavernEmbeddedBook,
    onEntryUpdate: (TavernBookEntry) -> Unit = {},
) {
    var showEntries by remember { mutableStateOf(false) }
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showEntries = !showEntries },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("📖 内嵌世界书", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = false,
                onClick = { showEntries = !showEntries },
                label = {
                    Text(
                        text = "${book.entries.size}",
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
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
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                book.entries.forEach { entry ->
                    CollapsibleEntryCard(
                        entry = entry,
                        onUpdate = onEntryUpdate,
                    )
                }
            }
        }
    }
}

/**
 * 可折叠世界书条目卡片 — 收起显示 keys+预览，展开显示全部可编辑字段
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CollapsibleEntryCard(
    entry: TavernBookEntry,
    onUpdate: (TavernBookEntry) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // ======== 收起预览 ========
            if (!expanded) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (entry.keys.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                entry.keys.take(4).forEach { key ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(key, style = MaterialTheme.typography.labelSmall) },
                                        shape = RoundedCornerShape(4.dp),
                                    )
                                }
                                if (entry.keys.size > 4) {
                                    Text(
                                        text = "+${entry.keys.size - 4}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = entry.content.replace("\n", " ").take(120)
                                .let { if (it.length < entry.content.length) "$it…" else it },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "▼",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // ======== 展开编辑 ========
            if (expanded) {
                EntryEditor(entry = entry, onUpdate = onUpdate, onCollapse = { expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EntryEditor(
    entry: TavernBookEntry,
    onUpdate: (TavernBookEntry) -> Unit,
    onCollapse: () -> Unit,
) {
    // 所有字段的状态
    var enabled by remember(entry) { mutableStateOf(!entry.disable) }
    var content by remember(entry) { mutableStateOf(entry.content) }
    var probability by remember(entry) { mutableStateOf(entry.probability.toFloat()) }
    var position by remember(entry) { mutableStateOf(entry.position) }
    var priority by remember(entry) { mutableStateOf(entry.priority.toString()) }
    var role by remember(entry) { mutableStateOf(entry.role) }
    var constant by remember(entry) { mutableStateOf(entry.constant) }
    var selective by remember(entry) { mutableStateOf(entry.selective) }
    var selectiveLogic by remember(entry) { mutableStateOf(entry.selectiveLogic) }
    var sticky by remember(entry) { mutableStateOf(entry.sticky) }
    var cooldown by remember(entry) { mutableStateOf(entry.cooldown.toString()) }
    var depth by remember(entry) { mutableStateOf(entry.depth.toString()) }
    var caseSensitive by remember(entry) { mutableStateOf(entry.caseSensitive) }
    var useRegex by remember(entry) { mutableStateOf(entry.useRegex) }
    var groupStr by remember(entry) { mutableStateOf(entry.group) }
    var groupWeight by remember(entry) { mutableStateOf(entry.groupWeight.toString()) }
    var groupOverride by remember(entry) { mutableStateOf(entry.groupOverride) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 顶部：标题 + 收起
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = entry.keys.firstOrNull() ?: entry.comment.ifBlank { "Entry #${entry.id}" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TextButton(onClick = onCollapse) {
                Text("▲ 收起", style = MaterialTheme.typography.labelSmall)
            }
        }

        // 启用开关
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (enabled) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                modifier = Modifier.clickable { enabled = !enabled },
            ) {
                Text(
                    text = if (enabled) "✓ 启用" else "✗ 禁用",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (enabled) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { constant = !constant }) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (constant) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surface,
                ) {
                    Text(
                        text = if (constant) "🔵 常驻" else "⚪ 非常驻",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (constant) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.clickable { selective = !selective },
            ) {
                Text(
                    text = if (selective) "🟢 关键词" else "🔗 向量",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }

        // 概率 + 位置
        Row {
            Column(modifier = Modifier.weight(1f)) {
                Text("触发概率", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = probability,
                    onValueChange = { probability = it },
                    valueRange = 0f..100f,
                    steps = 99,
                )
                Text("${probability.toInt()}%", style = MaterialTheme.typography.labelSmall)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("位置 (position)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val posOptions = listOf("角色前", "角色后", "用户前", "用户后", "@D深度")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    posOptions.forEachIndexed { i, label ->
                        FilterChip(
                            selected = position == i,
                            onClick = { position = i },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
        }

        // 内容
        Text("内容 (content)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 200.dp),
            textStyle = MaterialTheme.typography.bodySmall,
        )

        // 高级设置
        var showAdvanced by remember { mutableStateOf(false) }
        TextButton(onClick = { showAdvanced = !showAdvanced }) {
            Text(if (showAdvanced) "▲ 收起高级设置" else "▼ 展开高级设置",
                 style = MaterialTheme.typography.labelSmall)
        }
        if (showAdvanced) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row {
                    KeywordInput("优先级 (order)", priority) { priority = it }
                    KeywordInput("深度 (depth)", depth) { depth = it }
                    KeywordInput("冷却 (cooldown)", cooldown) { cooldown = it }
                }
                Row {
                    FilterChip(selected = sticky, onClick = { sticky = !sticky },
                        label = { Text("粘性", style = MaterialTheme.typography.labelSmall) })
                    FilterChip(selected = caseSensitive, onClick = { caseSensitive = !caseSensitive },
                        label = { Text("区分大小写", style = MaterialTheme.typography.labelSmall) })
                    FilterChip(selected = useRegex, onClick = { useRegex = !useRegex },
                        label = { Text("正则", style = MaterialTheme.typography.labelSmall) })
                    FilterChip(selected = groupOverride, onClick = { groupOverride = !groupOverride },
                        label = { Text("覆盖同组", style = MaterialTheme.typography.labelSmall) })
                }
                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("分组 (group)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(value = groupStr, onValueChange = { groupStr = it },
                            textStyle = MaterialTheme.typography.bodySmall, singleLine = true)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("组权重", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        KeywordInput("", groupWeight) { groupWeight = it }
                    }
                }
                Text("角色 (role)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val roleLabels = listOf("system", "user", "assistant")
                    roleLabels.forEachIndexed { _, label ->
                        FilterChip(
                            selected = role == label,
                            onClick = { role = label },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
                Row {
                    Text("seletiveLogic", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val logicLabels = listOf("AND", "OR", "NOT_ANY", "NOT_ALL")
                    logicLabels.forEachIndexed { i, label ->
                        FilterChip(
                            selected = selectiveLogic == i,
                            onClick = { selectiveLogic = i },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
        }

        // 保存按钮
        Button(
            onClick = {
                onUpdate(entry.copy(
                    disable = !enabled,
                    content = content,
                    probability = probability.toInt(),
                    position = position,
                    priority = priority.toIntOrNull() ?: 100,
                    role = role,
                    constant = constant,
                    selective = selective,
                    selectiveLogic = selectiveLogic,
                    sticky = sticky,
                    cooldown = cooldown.toIntOrNull() ?: 0,
                    depth = depth.toIntOrNull() ?: 4,
                    caseSensitive = caseSensitive,
                    useRegex = useRegex,
                    group = groupStr,
                    groupWeight = groupWeight.toIntOrNull() ?: 100,
                    groupOverride = groupOverride,
                ))
                onCollapse()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("保存修改")
        }
    }
}

@Composable
private fun KeywordInput(label: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        if (label.isNotBlank()) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodySmall,
            singleLine = true,
        )
    }
}
