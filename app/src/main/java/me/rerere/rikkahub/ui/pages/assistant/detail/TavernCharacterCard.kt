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
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.TavernBookEntry
import me.rerere.rikkahub.data.model.TavernEmbeddedBook
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowRight01
import me.rerere.hugeicons.stroke.Book01
import me.rerere.rikkahub.ui.theme.CustomColors
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer

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
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(200),
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CustomColors.listItemColors.containerColor
        ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column {
            // 紧凑头部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    HugeIcons.ArrowRight01,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { rotationZ = rotationAngle },
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    HugeIcons.Book01,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "角色卡",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = tav.spec.removePrefix("chara_card_").uppercase(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                    // 统计行
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
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
            }

            // 展开内容
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )

                    // 标签
                    if (tav.tags.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            tav.tags.forEach { tag ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(6.dp),
                                )
                            }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 14.dp),
                        )
                    }

                    // 可编辑字段
                    if (tav.systemPrompt.isNotBlank()) {
                        EditableField("系统提示词", tav.systemPrompt) { v: String ->
                            val newTav = tav.copy(systemPrompt = v)
                            onAssistantUpdate?.invoke(assistant.copy(tavernData = newTav))
                        }
                    }
                    EditableField("描述", tav.description) { v: String ->
                        val newTav = tav.copy(description = v)
                        onAssistantUpdate?.invoke(assistant.copy(tavernData = newTav))
                    }
                    EditableField("性格", tav.personality) { v: String ->
                        val newTav = tav.copy(personality = v)
                        onAssistantUpdate?.invoke(assistant.copy(tavernData = newTav))
                    }
                    EditableField("场景", tav.scenario) { v: String ->
                        val newTav = tav.copy(scenario = v)
                        onAssistantUpdate?.invoke(assistant.copy(tavernData = newTav))
                    }
                    EditableField("示例消息", tav.mesExample) { v: String ->
                        val newTav = tav.copy(mesExample = v)
                        onAssistantUpdate?.invoke(assistant.copy(tavernData = newTav))
                    }
                    if (tav.postHistoryInstructions.isNotBlank()) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 14.dp),
                        )
                        EditableField("历史后续指令", tav.postHistoryInstructions) { v: String ->
                            val newTav = tav.copy(postHistoryInstructions = v)
                            onAssistantUpdate?.invoke(assistant.copy(tavernData = newTav))
                        }
                    }
                    if (tav.firstMessage.isNotBlank()) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 14.dp),
                        )
                        EditableField("开场白", tav.firstMessage, previewLines = 1) { v: String ->
                            val newTav = tav.copy(firstMessage = v)
                            onAssistantUpdate?.invoke(assistant.copy(tavernData = newTav))
                        }
                    }

                    // 备选开场白
                    if (tav.alternateGreetings.isNotEmpty()) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                        Text(
                            text = "备选开场白 (${tav.alternateGreetings.size})",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        tav.alternateGreetings.forEachIndexed { i, greeting ->
                            EditableField("G${i + 1}", greeting) { v: String ->
                                val newGreetings = tav.alternateGreetings.toMutableList().apply { set(i, v) }
                                val newTav = tav.copy(alternateGreetings = newGreetings)
                                onAssistantUpdate?.invoke(assistant.copy(tavernData = newTav))
                            }
                        }
                    }

                    // 群聊专用开场白
                    if (tav.groupOnlyGreetings.isNotEmpty()) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                        Text(
                            text = "群聊专用开场白 (${tav.groupOnlyGreetings.size})",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        tav.groupOnlyGreetings.forEachIndexed { i, greeting ->
                            EditableField("G${i + 1}", greeting) { v: String ->
                                val newGreetings = tav.groupOnlyGreetings.toMutableList().apply { set(i, v) }
                                val newTav = tav.copy(groupOnlyGreetings = newGreetings)
                                onAssistantUpdate?.invoke(assistant.copy(tavernData = newTav))
                            }
                        }
                    }

                    // 内嵌世界书
                    tav.embeddedBook?.let { book ->
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
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
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                        EditableField("${tav.creator.ifBlank { "作者" }} 的备注", tav.creatorNotes) { v: String ->
                            val newTav = tav.copy(creatorNotes = v)
                            onAssistantUpdate?.invoke(assistant.copy(tavernData = newTav))
                        }
                    }

                    // 作者信息
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (tav.creator.isNotBlank()) {
                            Text(
                                text = "作者: ${tav.creator}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (tav.characterVersion.isNotBlank()) {
                            Text(
                                text = "版本: v${tav.characterVersion}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, active: Boolean) {
    val bgColor = if (active)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceContainerHigh
    val textColor = if (active)
        MaterialTheme.colorScheme.onPrimaryContainer
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
        )
    }
}

/**
 * 可编辑字段组件 — 折叠预览3行，展开后OutlinedTextField可编辑
 */
@Composable
private fun EditableField(
    label: String,
    value: String,
    onSave: (String) -> Unit,
    previewLines: Int = 2,
) {
    if (value.isBlank()) return
    var expanded by remember { mutableStateOf(false) }
    var editText by remember(value) { mutableStateOf(value) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(200),
    )
    // 折叠时自动保存
    LaunchedEffect(expanded) {
        if (!expanded && editText != value) {
            onSave(editText)
        }
    }

    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                HugeIcons.ArrowRight01,
                contentDescription = null,
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer { rotationZ = rotationAngle },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f),
            )
        }
        if (expanded) {
            OutlinedTextField(
                value = editText,
                onValueChange = { editText = it },
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 4.dp),
                textStyle = MaterialTheme.typography.bodySmall,
                minLines = 3,
            )
        } else {
            Text(
                text = value.lines().take(previewLines).joinToString("\n")
                    .let { if (it.length < value.length) "$it…" else it },
                modifier = Modifier.padding(start = 20.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = previewLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun EmbeddedBookSummary(
    book: TavernEmbeddedBook,
    onEntryUpdate: (TavernBookEntry) -> Unit = {},
) {
    var showEntries by remember { mutableStateOf(false) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (showEntries) 90f else 0f,
        animationSpec = tween(200),
    )
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showEntries = !showEntries },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                HugeIcons.ArrowRight01,
                contentDescription = null,
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer { rotationZ = rotationAngle },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                HugeIcons.Book01,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(6.dp))
            Text("内嵌世界书", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Text(
                    text = "${book.entries.size}",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            if (book.name.isNotBlank()) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "· ${book.name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        HugeIcons.ArrowRight01, contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
    var sticky by remember(entry) { mutableStateOf(entry.sticky.toString()) }
    var cooldown by remember(entry) { mutableStateOf(entry.cooldown.toString()) }
    var depth by remember(entry) { mutableStateOf(entry.depth.toString()) }
    var caseSensitive by remember(entry) { mutableStateOf(entry.caseSensitive) }
    var useRegex by remember(entry) { mutableStateOf(entry.useRegex) }
    var groupStr by remember(entry) { mutableStateOf(entry.group) }
    var groupWeight by remember(entry) { mutableStateOf(entry.groupWeight.toString()) }
    var groupOverride by remember(entry) { mutableStateOf(entry.groupOverride) }
    var scanDepthStr by remember(entry) { mutableStateOf(entry.scanDepth.toString()) }
    var keysStr by remember(entry) { mutableStateOf(entry.keys.joinToString(", ")) }
    var secondaryKeysStr by remember(entry) { mutableStateOf(entry.secondaryKeys.joinToString(", ")) }
    var commentStr by remember(entry) { mutableStateOf(entry.comment) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 顶部：标题 + 收起
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = entry.keys.firstOrNull() ?: entry.comment.ifBlank { "Entry #${entry.id}" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = onCollapse, modifier = Modifier.size(28.dp)) {
                Icon(HugeIcons.ArrowRight01, contentDescription = "收起",
                    modifier = Modifier.size(16.dp).graphicsLayer { rotationZ = 90f })
            }
        }

        // 状态切换行
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = enabled,
                onClick = { enabled = !enabled },
                label = { Text(if (enabled) "启用" else "禁用", style = MaterialTheme.typography.labelSmall) },
            )
            FilterChip(
                selected = constant,
                onClick = { constant = !constant },
                label = { Text(if (constant) "常驻" else "非常驻", style = MaterialTheme.typography.labelSmall) },
            )
            FilterChip(
                selected = selective,
                onClick = { selective = !selective },
                label = { Text(if (selective) "关键词" else "向量", style = MaterialTheme.typography.labelSmall) },
            )
        }

        // 条目名称
        Text("名称 (comment)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = commentStr,
            onValueChange = { commentStr = it },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall,
            singleLine = true,
        )

        // 触发关键词
        Text("主关键词 (keys, 逗号分隔)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = keysStr,
            onValueChange = { keysStr = it },
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodySmall,
            singleLine = true,
        )

        // 次级关键词
        if (selective) {
            Text("次级关键词 (secondary_keys, 逗号分隔)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = secondaryKeysStr,
                onValueChange = { secondaryKeysStr = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodySmall,
                singleLine = true,
            )
        }

        // 概率 + 位置
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("位置 (position)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // 用 FlowRow 自动换行
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val posOptions = listOf("角色前", "角色后", "用户前", "用户后", "@D深度")
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
        val advRotation by animateFloatAsState(
            targetValue = if (showAdvanced) 90f else 0f, animationSpec = tween(200),
        )
        Row(
            modifier = Modifier.fillMaxWidth().clickable { showAdvanced = !showAdvanced }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(HugeIcons.ArrowRight01, null, modifier = Modifier.size(14.dp).graphicsLayer { rotationZ = advRotation },
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Text(if (showAdvanced) "收起高级设置" else "展开高级设置",
                 style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (showAdvanced) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KeywordInput("优先级 (order)", priority, onValueChange = { priority = it }, modifier = Modifier.weight(1f))
                    KeywordInput("深度 (depth)", depth, onValueChange = { depth = it }, modifier = Modifier.weight(1f))
                    KeywordInput("冷却 (cooldown)", cooldown, onValueChange = { cooldown = it }, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("粘性 (轮)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(value = sticky, onValueChange = { sticky = it },
                            textStyle = MaterialTheme.typography.bodySmall, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                    FilterChip(selected = caseSensitive, onClick = { caseSensitive = !caseSensitive },
                        label = { Text("区分大小写", style = MaterialTheme.typography.labelSmall) })
                    FilterChip(selected = useRegex, onClick = { useRegex = !useRegex },
                        label = { Text("正则", style = MaterialTheme.typography.labelSmall) })
                    FilterChip(selected = groupOverride, onClick = { groupOverride = !groupOverride },
                        label = { Text("覆盖同组", style = MaterialTheme.typography.labelSmall) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("分组 (group)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(value = groupStr, onValueChange = { groupStr = it },
                            textStyle = MaterialTheme.typography.bodySmall, singleLine = true)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("组权重", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        KeywordInput("", groupWeight, onValueChange = { groupWeight = it }, modifier = Modifier.weight(1f))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("扫描深度", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        KeywordInput("", scanDepthStr, onValueChange = { scanDepthStr = it }, modifier = Modifier.weight(1f))
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("seletiveLogic", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                         modifier = Modifier.padding(end = 8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
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
        }

        // 保存按钮
        TextButton(
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
                    sticky = sticky.toIntOrNull() ?: 0,
                    cooldown = cooldown.toIntOrNull() ?: 0,
                    depth = depth.toIntOrNull() ?: 4,
                    scanDepth = scanDepthStr.toIntOrNull() ?: 1000,
                    caseSensitive = caseSensitive,
                    useRegex = useRegex,
                    group = groupStr,
                    groupWeight = groupWeight.toIntOrNull() ?: 100,
                    groupOverride = groupOverride,
                    keys = keysStr.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    secondaryKeys = secondaryKeysStr.split(",").map { it.trim() }.filter { it.isNotBlank() },
                    comment = commentStr,
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
private fun KeywordInput(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
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
