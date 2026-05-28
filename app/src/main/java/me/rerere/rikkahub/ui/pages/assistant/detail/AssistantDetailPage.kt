@file:OptIn(ExperimentalMaterial3Api::class)

package me.rerere.rikkahub.ui.pages.assistant.detail

import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.BookOpen01
import me.rerere.hugeicons.stroke.Brain02
import me.rerere.hugeicons.stroke.ArrowRight01
import me.rerere.hugeicons.stroke.Code
import me.rerere.hugeicons.stroke.Message02
import me.rerere.hugeicons.stroke.Settings03
import me.rerere.hugeicons.stroke.Puzzle
import me.rerere.hugeicons.stroke.Wrench01
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.files.FilesManager
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.Avatar
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.components.ui.UIAvatar
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.hooks.heroAnimation
import me.rerere.rikkahub.ui.pages.assistant.detail.TavernCharacterCard
import me.rerere.rikkahub.utils.CardExporter
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun AssistantDetailPage(id: String) {
    val vm: AssistantDetailVM = koinViewModel(
        parameters = {
            parametersOf(id)
        }
    )
    val assistant by vm.assistant.collectAsStateWithLifecycle()
    val navController = LocalNavController.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        text = assistant.name.ifBlank {
                            stringResource(R.string.assistant_page_default_assistant)
                        },
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    BackButton()
                },
                actions = {
                    // 导出按钮
                    if (assistant.tavernData != null) {
                        var showExport by remember { mutableStateOf(false) }
                        TextButton(onClick = { showExport = true }) {
                            Text("导出", style = MaterialTheme.typography.labelMedium)
                        }
                        if (showExport) {
                            ExportCardDialog(
                                assistant = assistant,
                                onDismiss = { showExport = false },
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding + PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AssistantHeader(
                    assistant = assistant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 16.dp)
                )
            }

            // 酒馆角色卡结构化信息
            if (assistant.tavernData != null) {
                item {
                    TavernCharacterCard(
                        assistant = assistant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }

            item {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    item(
                        onClick = { navController.navigate(Screen.AssistantBasic(id)) },
                        leadingContent = { Icon(HugeIcons.Settings03, null) },
                        supportingContent = { Text(stringResource(R.string.assistant_detail_basic_desc)) },
                        headlineContent = { Text(stringResource(R.string.assistant_page_tab_basic)) },
                        trailingContent = { Icon(HugeIcons.ArrowRight01, null) },
                    )
                    item(
                        onClick = { navController.navigate(Screen.AssistantPrompt(id)) },
                        leadingContent = { Icon(HugeIcons.Message02, null) },
                        supportingContent = { Text(stringResource(R.string.assistant_detail_prompt_desc)) },
                        headlineContent = { Text(stringResource(R.string.assistant_page_tab_prompt)) },
                        trailingContent = { Icon(HugeIcons.ArrowRight01, null) },
                    )
                    item(
                        onClick = { navController.navigate(Screen.AssistantInjections(id)) },
                        leadingContent = { Icon(HugeIcons.Puzzle, null) },
                        supportingContent = { Text(stringResource(R.string.assistant_detail_extensions_desc)) },
                        headlineContent = { Text(stringResource(R.string.assistant_page_tab_extensions)) },
                        trailingContent = { Icon(HugeIcons.ArrowRight01, null) },
                    )
                    item(
                        onClick = { navController.navigate(Screen.AssistantMemory(id)) },
                        leadingContent = { Icon(HugeIcons.Brain02, null) },
                        supportingContent = { Text(stringResource(R.string.assistant_detail_memory_desc)) },
                        headlineContent = { Text(stringResource(R.string.assistant_page_tab_memory)) },
                        trailingContent = { Icon(HugeIcons.ArrowRight01, null) },
                    )
                    item(
                        onClick = { navController.navigate(Screen.AssistantRequest(id)) },
                        leadingContent = { Icon(HugeIcons.Code, null) },
                        supportingContent = { Text(stringResource(R.string.assistant_detail_request_desc)) },
                        headlineContent = { Text(stringResource(R.string.assistant_page_tab_request)) },
                        trailingContent = { Icon(HugeIcons.ArrowRight01, null) },
                    )
                    item(
                        onClick = { navController.navigate(Screen.AssistantMcp(id)) },
                        leadingContent = { Icon(HugeIcons.Wrench01, null) },
                        supportingContent = { Text(stringResource(R.string.assistant_detail_mcp_desc)) },
                        headlineContent = { Text(stringResource(R.string.assistant_page_tab_mcp)) },
                        trailingContent = { Icon(HugeIcons.ArrowRight01, null) },
                    )
                    item(
                        onClick = { navController.navigate(Screen.AssistantLocalTool(id)) },
                        leadingContent = { Icon(HugeIcons.BookOpen01, null) },
                        supportingContent = { Text(stringResource(R.string.assistant_detail_local_tools_desc)) },
                        headlineContent = { Text(stringResource(R.string.assistant_page_tab_local_tools)) },
                        trailingContent = { Icon(HugeIcons.ArrowRight01, null) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AssistantHeader(
    assistant: Assistant,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        UIAvatar(
            value = assistant.avatar,
            name = assistant.name.ifBlank { stringResource(R.string.assistant_page_default_assistant) },
            onUpdate = null,
            modifier = Modifier
                .size(100.dp)
                .heroAnimation("assistant_${assistant.id}")
        )

        Text(
            text = assistant.name.ifBlank { stringResource(R.string.assistant_page_default_assistant) },
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (assistant.systemPrompt.isNotBlank()) {
            Text(
                text = assistant.systemPrompt.take(100) + if (assistant.systemPrompt.length > 100) "..." else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExportCardDialog(
    assistant: Assistant,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current

    // 头像选取（当没有头像时手动选）
    val pngImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { pickedUri: Uri? ->
        if (pickedUri == null) return@rememberLauncherForActivityResult
        doPngExport(context, scope, toaster, assistant, pickedUri, onDismiss)
    }

    // 已设头像的 URI
    val avatarUri = runCatching {
        val url = (assistant.avatar as? Avatar.Image)?.url ?: return@runCatching null
        url.toUri()
    }.getOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出角色卡") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("导出到「下载」文件夹：", style = MaterialTheme.typography.bodyMedium)
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                val json = CardExporter.buildV3CardJson(assistant)
                                val fileName = "RikkaHub_${assistant.name.replace(" ", "_")}_${System.currentTimeMillis()}.json"
                                saveToDownloads(context, fileName, "application/json", json.toByteArray())
                                toaster.show("已导出 JSON: $fileName")
                            } catch (e: Exception) {
                                toaster.show("导出失败: ${e.message}")
                            }
                        }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📄 JSON 文件")
                }
                TextButton(
                    onClick = {
                        if (avatarUri != null) {
                            // 有头像 → 直接用
                            doPngExport(context, scope, toaster, assistant, avatarUri, onDismiss)
                        } else {
                            // 没头像 → 选图片
                            pngImagePicker.launch("image/*")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val label = if (avatarUri != null) "🖼️ PNG 嵌入（使用当前头像）"
                                 else "🖼️ PNG 嵌入（需选择头像图片）"
                    Text(label)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

private fun doPngExport(
    context: Context,
    scope: kotlinx.coroutines.CoroutineScope,
    toaster: com.dokar.sonner.ToasterState,
    assistant: Assistant,
    imageUri: Uri,
    onDismiss: () -> Unit,
) {
    scope.launch {
        try {
            val json = CardExporter.buildV3CardJson(assistant)
            val pngBytes = CardExporter.embedCardToPng(imageUri, context, json)
                ?: error("嵌入PNG失败，请确认选择的图片是PNG格式")
            val fileName = "RikkaHub_${assistant.name.replace(" ", "_")}_${System.currentTimeMillis()}.png"
            saveToDownloads(context, fileName, "image/png", pngBytes)
            toaster.show("已导出 PNG: $fileName")
        } catch (e: Exception) {
            toaster.show("导出失败: ${e.message}")
        }
        onDismiss()
    }
}

/**
 * 通过 MediaStore 将字节写入「下载」文件夹
 */
private suspend fun saveToDownloads(context: Context, fileName: String, mimeType: String, data: ByteArray) {
    withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { output ->
                    output.write(data)
                }
            } ?: error("无法创建下载文件")
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = java.io.File(downloadsDir, fileName)
            file.writeBytes(data)
        }
    }
}
