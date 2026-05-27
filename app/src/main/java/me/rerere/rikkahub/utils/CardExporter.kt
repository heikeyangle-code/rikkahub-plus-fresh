package me.rerere.rikkahub.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.zip.CRC32

/**
 * 角色卡导出工具 — PNG tEXt chunk 嵌入 / JSON 导出
 * 对齐 SillyTavern V3 spec
 */
object CardExporter {

    /**
     * 将角色卡 JSON 嵌入到已有图片的 PNG tEXt chunk 中
     * 返回 PNG 字节数组
     */
    fun embedCardToPng(imageUri: Uri, context: Context, cardJson: String): ByteArray? {
        val bitmap = try {
            context.contentResolver.openInputStream(imageUri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) { null } ?: return null

        // 将 bitmap 转为 PNG 字节
        val pngBytes = ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }

        // 在 PNG 中注入 tEXt chunk
        return injectTextChunk(pngBytes, "chara", Base64.encodeToString(cardJson.toByteArray(), Base64.NO_WRAP))
    }

    /**
     * 构建 V3 spec JSON
     */
    fun buildV3CardJson(assistant: me.rerere.rikkahub.data.model.Assistant): String {
        val tav = assistant.tavernData
        return buildJsonObject {
            put("spec", "chara_card_v3")
            put("spec_version", "3.0")
            putJsonObject("data") {
                put("name", tav?.name ?: assistant.name)
                put("description", tav?.description ?: "")
                put("personality", tav?.personality ?: "")
                put("scenario", tav?.scenario ?: "")
                put("first_mes", tav?.firstMessage ?: "")
                put("mes_example", tav?.mesExample ?: "")
                put("system_prompt", tav?.systemPrompt ?: assistant.systemPrompt)
                put("creator", tav?.creator ?: "")
                put("creator_notes", tav?.creatorNotes ?: "")
                put("character_version", tav?.characterVersion ?: "")
                put("post_history_instructions", tav?.postHistoryInstructions ?: "")
                putJsonArray("tags") { tav?.tags?.forEach { add(it) } }
                putJsonArray("alternate_greetings") { tav?.alternateGreetings?.forEach { add(it) } }
                if (tav?.extensions?.isNotEmpty() == true) {
                    putJsonObject("extensions") {
                        tav.extensions.forEach { (k, v) -> put(k, v) }
                    }
                }
                if (tav?.embeddedBook != null) {
                    putJsonObject("character_book") {
                        put("name", tav.embeddedBook.name)
                        put("description", tav.embeddedBook.description)
                        putJsonObject("entries") {
                            tav.embeddedBook.entries.forEach { entry ->
                                put(entry.id.toString(), buildJsonObject {
                                    putJsonArray("keys") { entry.keys.forEach { add(it) } }
                                    put("content", entry.content)
                                    put("comment", entry.comment)
                                    put("constant", entry.constant)
                                    put("selective", entry.selective)
                                    put("selectiveLogic", entry.selectiveLogic)
                                    put("position", entry.position)
                                    put("order", entry.priority)
                                    put("disable", entry.disable)
                                })
                            }
                        }
                    }
                }
            }
        }.toString()
    }

    /**
     * 在 PNG 字节流中注入 tEXt chunk
     * PNG 结构: [IHDR][...chunks...][tEXt][IEND]
     */
    private fun injectTextChunk(pngBytes: ByteArray, keyword: String, text: String): ByteArray {
        val output = ByteArrayOutputStream(pngBytes.size + keyword.length + text.length + 256)

        // 写入 PNG 签名
        output.write(pngBytes, 0, 8)

        // 找 IEND chunk 位置，在此之前插入 tEXt
        var pos = 8
        val iendPos = findIendChunk(pngBytes)
        val keywordBytes = keyword.toByteArray(Charsets.ISO_8859_1)
        val textBytes = text.toByteArray(Charsets.ISO_8859_1)

        // 复制 IEND 之前的所有 chunk
        while (pos < iendPos) {
            val chunkLen = readInt32BE(pngBytes, pos)
            output.write(pngBytes, pos, chunkLen + 12)
            pos += chunkLen + 12
        }

        // 写入 tEXt chunk
        val tEXtData = ByteArray(keywordBytes.size + 1 + textBytes.size)
        System.arraycopy(keywordBytes, 0, tEXtData, 0, keywordBytes.size)
        tEXtData[keywordBytes.size] = 0 // null separator
        System.arraycopy(textBytes, 0, tEXtData, keywordBytes.size + 1, textBytes.size)

        // Chunk length (4 bytes, big-endian)
        output.write(byteArrayOf(
            ((tEXtData.size shr 24) and 0xFF).toByte(),
            ((tEXtData.size shr 16) and 0xFF).toByte(),
            ((tEXtData.size shr 8) and 0xFF).toByte(),
            (tEXtData.size and 0xFF).toByte(),
        ))

        // Chunk type: "tEXt"
        output.write("tEXt".toByteArray())

        // Chunk data
        output.write(tEXtData)

        // CRC32 of type + data
        val crc = CRC32()
        crc.update("tEXt".toByteArray())
        crc.update(tEXtData)
        val crcVal = crc.value
        output.write(byteArrayOf(
            ((crcVal shr 24) and 0xFF).toByte(),
            ((crcVal shr 16) and 0xFF).toByte(),
            ((crcVal shr 8) and 0xFF).toByte(),
            (crcVal and 0xFF).toByte(),
        ))

        // 复制剩余的 IEND + 之后
        output.write(pngBytes, iendPos, pngBytes.size - iendPos)

        return output.toByteArray()
    }

    private fun findIendChunk(pngBytes: ByteArray): Int {
        var pos = 8
        while (pos + 12 <= pngBytes.size) {
            val len = readInt32BE(pngBytes, pos)
            val type = String(pngBytes, pos + 4, 4)
            if (type == "IEND") return pos
            pos += len + 12
        }
        return pngBytes.size - 12 // fallback
    }

    private fun readInt32BE(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 24) or
               ((bytes[offset + 1].toInt() and 0xFF) shl 16) or
               ((bytes[offset + 2].toInt() and 0xFF) shl 8) or
               (bytes[offset + 3].toInt() and 0xFF)
    }
}
