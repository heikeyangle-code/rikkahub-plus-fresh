package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.model.Avatar
import me.rerere.rikkahub.data.model.Persona
import me.rerere.rikkahub.data.model.PersonaInjectionPosition
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.FormItem
import me.rerere.rikkahub.ui.theme.CustomColors
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

@Composable
fun PersonaPage() {
    val settingsStore: SettingsStore = koinInject()
    val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Persona · 用户人设") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // 当前激活提示
            item {
                val active = settings.personas.find { it.id == settings.activePersonaId }
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("👤", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (active != null) "当前: ${active.name}" else "未激活 Persona",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                text = if (active != null) "已注入到提示词中" else "选择一个 Persona 激活",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // Persona 列表
            items(settings.personas, key = { it.id }) { persona ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = persona.name.ifBlank { "未命名" },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                )
                                if (persona.description.isNotBlank()) {
                                    Text(
                                        text = persona.description.take(80),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
                            TextButton(
                                onClick = {
                                    val s = settings
                                    if (s.activePersonaId == persona.id) {
                                        settingsStore.update(s.copy(activePersonaId = null))
                                    } else {
                                        settingsStore.update(s.copy(activePersonaId = persona.id))
                                    }
                                }
                            ) {
                                Text(
                                    if (settings.activePersonaId == persona.id) "停用" else "激活",
                                    color = if (settings.activePersonaId == persona.id)
                                        MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }

            // 新建 Persona
            item {
                var showDialog by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { showDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("+ 创建 Persona")
                }

                if (showDialog) {
                    var name by remember { mutableStateOf("") }
                    var desc by remember { mutableStateOf("") }
                    var pos by remember { mutableStateOf(PersonaInjectionPosition.AFTER_SYSTEM) }

                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text("新建 Persona") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("名称") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = desc,
                                    onValueChange = { desc = it },
                                    label = { Text("描述 (外表/背景)") },
                                    minLines = 2,
                                    maxLines = 5,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Text(
                                    text = "注入位置: ${pos.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    PersonaInjectionPosition.entries.forEach { p ->
                                        FilterChip(
                                            selected = pos == p,
                                            onClick = { pos = p },
                                            label = { Text(p.name, style = MaterialTheme.typography.labelSmall) },
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (name.isNotBlank()) {
                                        val s = settings
                                        settingsStore.update(
                                            s.copy(personas = s.personas + Persona(
                                                id = Uuid.random(),
                                                name = name,
                                                description = desc,
                                                position = pos,
                                            ))
                                        )
                                        showDialog = false
                                    }
                                }
                            ) { Text("创建") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDialog = false }) { Text("取消") }
                        },
                    )
                }
            }
        }
    }
}
