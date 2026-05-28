package me.rerere.rikkahub.ui.pages.assistant.detail

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.dokar.sonner.ToastType
import com.dokar.sonner.ToasterState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.rerere.ai.ui.UIMessage
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.Lorebook
import me.rerere.rikkahub.data.model.PromptInjection
import me.rerere.rikkahub.data.model.InjectionPosition
import me.rerere.rikkahub.data.model.MessageRole
import me.rerere.rikkahub.data.model.TavernCharacterData
import me.rerere.rikkahub.data.model.TavernEmbeddedBook
import me.rerere.rikkahub.data.model.TavernBookEntry
import me.rerere.rikkahub.data.model.TavernAsset
import me.rerere.rikkahub.data.model.SelectiveLogic
import me.rerere.rikkahub.data.files.FilesManager
import me.rerere.rikkahub.ui.components.ui.AutoAIIcon
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.utils.ImageUtils
import me.rerere.rikkahub.utils.jsonPrimitiveOrNull
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

/**
 * 酒馆导入结果
 */
data class TavernImportResult(
    val assistant: Assistant,
    val newLorebooks: List<Lorebook> = emptyList(),  // 从内嵌世界书创建的新Lorebook
)

@Composable
fun AssistantImporter(
    modifier: Modifier = Modifier,
    onImport: (TavernImportResult) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier,
    ) {
        SillyTavernImporter(onImport = onImport)
    }
}

@Composable
private fun SillyTavernImporter(
    onImport: (TavernImportResult) -> Unit
) {
    val context = LocalContext.current
    val filesManager: FilesManager = koinInject()
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    var isLoading by remember { mutableStateOf(false) }

    val pngPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { importFile(context, uri, onImport, filesManager, toaster, scope) { isLoading = it } }
    }

    val jsonPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { importFile(context, uri, onImport, filesManager, toaster, scope) { isLoading = it } }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { pngPickerLauncher.launch(arrayOf("image/png")) },
            enabled = !isLoading
        ) {
            AutoAIIcon(name = "tavern", modifier = Modifier.padding(end = 8.dp))
            Text(if (isLoading) stringResource(R.string.assistant_importer_importing)
                 else stringResource(R.string.assistant_importer_import_tavern_png))
        }
        OutlinedButton(
            onClick = { jsonPickerLauncher.launch(arrayOf("application/json")) },
            enabled = !isLoading
        ) {
            AutoAIIcon(name = "tavern", modifier = Modifier.padding(end = 8.dp))
            Text(if (isLoading) stringResource(R.string.assistant_importer_importing)
                 else stringResource(R.string.assistant_importer_import_tavern_json))
        }
    }
}

private fun importFile(
    context: Context, uri: Uri,
    onImport: (TavernImportResult) -> Unit,
    filesManager: FilesManager, toaster: ToasterState,
    scope: kotlinx.coroutines.CoroutineScope,
    setLoading: (Boolean) -> Unit
) {
    setLoading(true)
    scope.launch {
        try {
            runCatching {
                importFromUri(context, uri, filesManager, onImport, toaster)
            }.onFailure { e ->
                e.printStackTrace()
                toaster.show(e.message ?: context.getString(R.string.assistant_importer_import_failed))
            }
        } finally { setLoading(false) }
    }
}

private suspend fun importFromUri(
    context: Context, uri: Uri, filesManager: FilesManager,
    onImport: (TavernImportResult) -> Unit, toaster: ToasterState
) {
    val mime = withContext(Dispatchers.IO) { filesManager.getFileMimeType(uri) }
    val (jsonString, backgroundStr) = withContext(Dispatchers.IO) {
        when (mime) {
            "image/png" -> {
                val result = ImageUtils.getTavernCharacterMeta(context, uri)
                result.map { base64Data ->
                    val json = String(Base64.decode(base64Data, Base64.DEFAULT))
                    val bg = filesManager.createChatFilesByContents(listOf(uri)).first().toString()
                    json to bg
                }.getOrElse { throw it }
            }
            "application/json" -> {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()
                    .use { it?.readText() }
                    ?: error(context.getString(R.string.assistant_importer_read_json_failed))
                json to null
            }
            else -> error(context.getString(R.string.assistant_importer_unsupported_file_type, mime ?: "unknown"))
        }
    }
    val json = Json.parseToJsonElement(jsonString).jsonObject
    val spec = json["spec"]?.jsonPrimitive?.contentOrNull
        ?: error(context.getString(R.string.assistant_importer_missing_spec_field))

    val (assistant, lorebooks) = when (spec) {
        "chara_card_v2" -> parseV2Card(context, json, backgroundStr)
        "chara_card_v3" -> parseV3Card(context, json, backgroundStr)
        else -> error(context.getString(R.string.assistant_importer_unsupported_spec, spec))
    }

    toaster.show(context.getString(R.string.assistant_importer_import_success, assistant.name))
    onImport(TavernImportResult(assistant, lorebooks))
}

// ==================== V2 Parser ====================

private fun parseV2Card(context: Context, json: JsonObject, background: String?): Pair<Assistant, List<Lorebook>> {
    val data = json["data"]?.jsonObject ?: error(context.getString(R.string.assistant_importer_missing_data_field))
    val name = data["name"]?.jsonPrimitiveOrNull?.contentOrNull
        ?: error(context.getString(R.string.assistant_importer_missing_name_field))

    val tavData = TavernCharacterData(
        spec = "chara_card_v2",
        specVersion = json["spec_version"]?.jsonPrimitive?.contentOrNull ?: "",
        name = name,
        description = data["description"]?.jsonPrimitiveOrNull?.contentOrNull ?: "",
        personality = data["personality"]?.jsonPrimitiveOrNull?.contentOrNull ?: "",
        scenario = data["scenario"]?.jsonPrimitiveOrNull?.contentOrNull ?: "",
        firstMessage = data["first_mes"]?.jsonPrimitiveOrNull?.contentOrNull ?: "",
        alternateGreetings = parseStringArray(data["alternate_greetings"]),
        mesExample = data["mes_example"]?.jsonPrimitiveOrNull?.contentOrNull ?: "",
        systemPrompt = data["system_prompt"]?.jsonPrimitiveOrNull?.contentOrNull ?: "",
        creator = data["creator"]?.jsonPrimitiveOrNull?.contentOrNull ?: "",
        creatorNotes = data["creator_notes"]?.jsonPrimitiveOrNull?.contentOrNull ?: "",
        characterVersion = data["character_version"]?.jsonPrimitiveOrNull?.contentOrNull ?: "",
        tags = parseStringArray(data["tags"]),
        postHistoryInstructions = data["post_history_instructions"]?.jsonPrimitiveOrNull?.contentOrNull ?: "",
        embeddedBook = parseEmbeddedBook(data["character_book"]?.jsonObject),
    )

    val systemPrompt = buildTavernSystemPrompt(tavData)
    val presetMessages = buildPresetMessages(tavData)
    val lorebooks = buildEmbeddedLorebooks(tavData)

    val assistant = Assistant(
        name = name,
        systemPrompt = systemPrompt,
        presetMessages = presetMessages,
        background = background,
        tavernData = tavData,
    )

    return assistant to lorebooks
}

// ==================== V3 Parser ====================

private fun parseV3Card(context: Context, json: JsonObject, background: String?): Pair<Assistant, List<Lorebook>> {
    val data = json["data"]?.jsonObject ?: error(context.getString(R.string.assistant_importer_missing_data_field))
    val name = data["name"]?.jsonPrimitiveOrNull?.contentOrNull
        ?: error(context.getString(R.string.assistant_importer_missing_name_field))

    val tavData = TavernCharacterData(
        spec = "chara_card_v3",
        specVersion = json["spec_version"]?.jsonPrimitive?.contentOrNull ?: "",
        name = name,
        description = data["description"]?.jsonPrimitiveOrNull?.contentOrNull ?: "",
        personality = data["personality"]?.jsonPrimitiveOrNull?.contentOrNull ?: "",
        scenario = data["scenario"]?.jsonPrimitiveOrNull?.contentOrNull ?: "",
        firstMessage = data["first_mes"]?.jsonPrimitiveOrNull?.contentOrNull ?: "",
        alternateGreetings = parseStringArray(data["alternate_greetings"]),
        mesExample = data["mes_example"]?.jsonPrimitiveOrNull?.contentOrNull ?: "",
        systemPrompt = data["system_prompt"]?.jsonPrimitiveOrNull?.contentOrNull ?: "",
        creator = data["creator"]?.jsonPrimitiveOrNull?.contentOrNull ?: "",
        creatorNotes = data["creator_notes"]?.jsonPrimitiveOrNull?.contentOrNull ?: "",
        characterVersion = data["character_version"]?.jsonPrimitiveOrNull?.contentOrNull ?: "",
        tags = parseStringArray(data["tags"]),
        postHistoryInstructions = data["post_history_instructions"]?.jsonPrimitiveOrNull?.contentOrNull ?: "",
        extensions = parseExtensions(data["extensions"]?.jsonObject),
        assets = parseAssets(data["assets"]?.jsonArray),
        groupOnlyGreetings = parseStringArray(data["group_only_greetings"]),
        embeddedBook = parseEmbeddedBook(data["character_book"]?.jsonObject),
    )

    val systemPrompt = buildTavernSystemPrompt(tavData)
    val presetMessages = buildPresetMessages(tavData)
    val lorebooks = buildEmbeddedLorebooks(tavData)

    val assistant = Assistant(
        name = name,
        systemPrompt = systemPrompt,
        presetMessages = presetMessages,
        background = background,
        tavernData = tavData,
    )

    return assistant to lorebooks
}

// ==================== Helpers ====================

private fun parseStringArray(element: kotlinx.serialization.json.JsonElement?): List<String> {
    if (element == null) return emptyList()
    return try {
        element.jsonArray.map { it.jsonPrimitive.contentOrNull ?: "" }.filter { it.isNotBlank() }
    } catch (_: Exception) { emptyList() }
}

private fun parseExtensions(obj: JsonObject?): Map<String, String> {
    if (obj == null) return emptyMap()
    return obj.entries.associate { (k, v) -> k to (v.jsonPrimitive?.contentOrNull ?: v.toString()) }
}

private fun parseAssets(arr: kotlinx.serialization.json.JsonArray?): List<TavernAsset> {
    if (arr == null) return emptyList()
    return arr.mapNotNull { el ->
        try {
            val obj = el.jsonObject
            TavernAsset(
                type = obj["type"]?.jsonPrimitive?.contentOrNull ?: "",
                name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "",
                uri = obj["uri"]?.jsonPrimitive?.contentOrNull ?: "",
                ext = obj["ext"]?.jsonPrimitive?.contentOrNull ?: "",
            )
        } catch (_: Exception) { null }
    }
}

private fun parseEmbeddedBook(obj: JsonObject?): TavernEmbeddedBook? {
    if (obj == null) return null
    val entries = try {
        val entriesJson = obj["entries"]
        when {
            entriesJson == null -> emptyList()
            entriesJson is kotlinx.serialization.json.JsonArray -> parseEntriesArray(entriesJson)
            else -> parseEntriesMap(entriesJson.jsonObject)
        }
    } catch (_: Exception) { emptyList() }

    if (entries.isEmpty()) return null

    return TavernEmbeddedBook(
        name = obj["name"]?.jsonPrimitive?.contentOrNull ?: "",
        description = obj["description"]?.jsonPrimitive?.contentOrNull ?: "",
        scanDepth = obj["scan_depth"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
        tokenBudget = obj["token_budget"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
        recursiveScanning = obj["recursive_scanning"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull(),
        extensions = parseExtensions(obj["extensions"]?.jsonObject),
        entries = entries,
    )
}

private fun parseEntriesArray(arr: kotlinx.serialization.json.JsonArray): List<TavernBookEntry> {
    return arr.mapNotNull { el ->
        try {
            val e = el.jsonObject
            TavernBookEntry(
                id = e["id"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                keys = parseStringArray(e["keys"]) + parseStringArray(e["key"]),
                secondaryKeys = parseStringArray(e["secondary_keys"]),
                comment = e["comment"]?.jsonPrimitive?.contentOrNull ?: "",
                content = e["content"]?.jsonPrimitive?.contentOrNull ?: "",
                constant = e["constant"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
                selective = e["selective"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
                selectiveLogic = e["selectiveLogic"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                group = e["group"]?.jsonPrimitive?.contentOrNull ?: "",
                position = e["position"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1,
                priority = e["order"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                    ?: e["priority"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 100,
                disable = e["disable"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
                caseSensitive = e["caseSensitive"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
                useRegex = e["useRegex"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
                probability = e["probability"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 100,
                sticky = e["sticky"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
                cooldown = e["cooldown"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                depth = e["depth"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 4,
                scanDepth = e["scan_depth"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
                role = e["role"]?.jsonPrimitive?.contentOrNull ?: "system",
                groupWeight = e["group_weight"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 100,
                groupOverride = e["group_override"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
            )
        } catch (_: Exception) { null }
    }
}

private fun parseEntriesMap(obj: JsonObject): List<TavernBookEntry> {
    return obj.entries.mapNotNull { (idStr, el) ->
        try {
            val e = el.jsonObject
            TavernBookEntry(
                id = idStr.toIntOrNull() ?: 0,
                keys = parseStringArray(e["keys"]) + parseStringArray(e["key"]),
                secondaryKeys = parseStringArray(e["secondary_keys"]),
                comment = e["comment"]?.jsonPrimitive?.contentOrNull ?: "",
                content = e["content"]?.jsonPrimitive?.contentOrNull ?: "",
                constant = e["constant"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
                selective = e["selective"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
                selectiveLogic = e["selectiveLogic"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                group = e["group"]?.jsonPrimitive?.contentOrNull ?: "",
                position = e["position"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1,
                priority = e["order"]?.jsonPrimitive?.contentOrNull?.toIntOrNull()
                    ?: e["priority"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 100,
                disable = e["disable"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
                caseSensitive = e["caseSensitive"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
                useRegex = e["useRegex"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
                probability = e["probability"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 100,
                sticky = e["sticky"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
                cooldown = e["cooldown"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 0,
                depth = e["depth"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 4,
                scanDepth = e["scan_depth"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
                role = e["role"]?.jsonPrimitive?.contentOrNull ?: "system",
                groupWeight = e["group_weight"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 100,
                groupOverride = e["group_override"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
            )
        } catch (_: Exception) { null }
    }
}

/**
 * 将内嵌世界书条目转为Rikkahub的RegexInjection
 */
private fun tavernEntryToInjection(entry: TavernBookEntry): PromptInjection.RegexInjection {
    return PromptInjection.RegexInjection(
        id = Uuid.random(),
        name = entry.comment.ifEmpty { entry.keys.firstOrNull() ?: "Entry ${entry.id}" },
        enabled = !entry.disable,
        priority = entry.priority,
        position = mapTavernPosition(entry.position),
        injectDepth = entry.depth,
        content = entry.content,
        role = mapTavernRole(entry.role),
        keywords = entry.keys,
        secondaryKeys = entry.secondaryKeys,
        useRegex = entry.useRegex,
        caseSensitive = entry.caseSensitive,
        scanDepth = entry.scanDepth ?: 4,
        constantActive = entry.constant,
        selectiveLogic = mapSelectiveLogic(entry.selectiveLogic),
        group = entry.group,
        probability = entry.probability,
        sticky = entry.sticky,
        cooldown = entry.cooldown,
        groupWeight = entry.groupWeight,
        groupOverride = entry.groupOverride,
    )
}

private fun mapTavernPosition(pos: Int): InjectionPosition = when (pos) {
    0 -> InjectionPosition.BEFORE_SYSTEM_PROMPT
    1 -> InjectionPosition.AFTER_SYSTEM_PROMPT
    2 -> InjectionPosition.TOP_OF_CHAT      // before user (AN)
    3 -> InjectionPosition.BOTTOM_OF_CHAT   // after user (@D)
    4 -> InjectionPosition.AT_DEPTH
    else -> InjectionPosition.AFTER_SYSTEM_PROMPT
}

private fun mapTavernRole(role: String): me.rerere.ai.core.MessageRole = when (role.lowercase()) {
    "user" -> me.rerere.ai.core.MessageRole.USER
    "assistant" -> me.rerere.ai.core.MessageRole.ASSISTANT
    else -> me.rerere.ai.core.MessageRole.USER  // system/other → USER (injects as user message)
}

private fun mapSelectiveLogic(logic: Int): SelectiveLogic = when (logic) {
    0 -> SelectiveLogic.AND_ANY
    1 -> SelectiveLogic.OR_ANY
    2 -> SelectiveLogic.NOT_ANY
    3 -> SelectiveLogic.NOT_ALL
    else -> SelectiveLogic.AND_ANY
}

/**
 * 从内嵌世界书构建 Lorebook 列表
 */
private fun buildEmbeddedLorebooks(tavData: TavernCharacterData): List<Lorebook> {
    val book = tavData.embeddedBook ?: return emptyList()
    val injections = book.entries.map { tavernEntryToInjection(it) }
    if (injections.isEmpty()) return emptyList()

    return listOf(
        Lorebook(
            id = Uuid.random(),
            name = book.name.ifEmpty { "${tavData.name}的世界书" },
            description = book.description,
            enabled = true,
            entries = injections,
        )
    )
}

/**
 * 构建保留结构的 system prompt（原始各字段 + mes_example）
 */
private fun buildTavernSystemPrompt(d: TavernCharacterData): String {
    return buildString {
        appendLine("[Character: ${d.name}]")
        appendLine()

        if (d.systemPrompt.isNotBlank()) {
            appendLine(d.systemPrompt)
            appendLine()
        }

        if (d.description.isNotBlank()) {
            appendLine("## Description")
            appendLine(d.description)
            appendLine()
        }

        if (d.personality.isNotBlank()) {
            appendLine("## Personality")
            appendLine(d.personality)
            appendLine()
        }

        if (d.scenario.isNotBlank()) {
            appendLine("## Scenario")
            appendLine(d.scenario)
            appendLine()
        }

        if (d.mesExample.isNotBlank()) {
            appendLine("## Example Messages")
            appendLine(d.mesExample)
            appendLine()
        }

        if (d.postHistoryInstructions.isNotBlank()) {
            appendLine("## Instructions")
            appendLine(d.postHistoryInstructions)
        }
    }.trim()
}

/**
 * 构建 presetMessages（first_mes + alternate_greetings 合并为对话预设）
 */
private fun buildPresetMessages(d: TavernCharacterData): List<UIMessage> {
    val messages = mutableListOf<UIMessage>()
    if (d.firstMessage.isNotBlank()) {
        messages.add(UIMessage.assistant(d.firstMessage))
    }
    for (greeting in d.alternateGreetings) {
        if (greeting.isNotBlank() && greeting != d.firstMessage) {
            messages.add(UIMessage.assistant(greeting))
        }
    }
    return messages
}
