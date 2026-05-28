package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.model.InjectionPosition
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.TextArea
import me.rerere.rikkahub.ui.theme.CustomColors
import org.koin.compose.koinInject

@Composable
fun AuthorsNotePage() {
    val settingsStore: SettingsStore = koinInject()
    val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Author's Note") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Author's Note 文本
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📝 Author's Note",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "在对话中注入引导性文字。类似导演备注，控制 AI 输出方向。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = settings.authorNote,
                        onValueChange = { newValue ->
                            scope.launch {
                                settingsStore.update(settings.copy(authorNote = newValue))
                            }
                        },
                        label = { Text("内容") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // 插入位置
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📍 插入位置", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    val positions = listOf(
                        InjectionPosition.AFTER_SYSTEM_PROMPT to "系统提示词后",
                        InjectionPosition.TOP_OF_CHAT to "对话顶部",
                        InjectionPosition.BOTTOM_OF_CHAT to "最新消息前",
                        InjectionPosition.AT_DEPTH to "指定深度",
                    )
                    positions.forEach { (pos, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = settings.authorNotePosition == pos,
                                onClick = {
                                    scope.launch {
                                        settingsStore.update(settings.copy(authorNotePosition = pos))
                                    }
                                },
                            )
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // 深度
            if (settings.authorNotePosition == InjectionPosition.AT_DEPTH) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📏 插入深度: ${settings.authorNoteDepth}", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "距最新消息往前数 N 条处插入",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(8.dp))
                        Slider(
                            value = settings.authorNoteDepth.toFloat(),
                            onValueChange = {
                                scope.launch {
                                    settingsStore.update(settings.copy(authorNoteDepth = it.toInt()))
                                }
                            },
                            valueRange = 1f..20f,
                            steps = 18,
                        )
                    }
                }
            }

            // 频率
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("🎲 插入频率: ${(settings.authorNoteFrequency * 100).toInt()}%", style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = "低于100%时每次对话有概率跳过",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = settings.authorNoteFrequency,
                        onValueChange = {
                            scope.launch {
                                settingsStore.update(settings.copy(authorNoteFrequency = it))
                            }
                        },
                        valueRange = 0f..1f,
                        steps = 9,
                    )
                }
            }
        }
    }
}
