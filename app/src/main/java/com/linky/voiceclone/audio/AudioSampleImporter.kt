package com.linky.voiceclone.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

enum class SupportedAudioFormat(
    val extension: String,
    val mimeType: String,
) {
    WAV("wav", "audio/wav"),
    MP3("mp3", "audio/mpeg"),
}

data class ImportedAudioSample(
    val file: File,
    val originalName: String,
    val format: SupportedAudioFormat,
    val byteSize: Long,
    val durationMs: Long?,
)

class AudioImportException(message: String) : Exception(message)

/**
 * Imports voice samples without trusting the picker-provided extension.
 * MiMo voice-clone samples must be WAV/MP3 and no larger than 10 MiB after Base64 encoding.
 */
@Singleton
class AudioSampleImporter @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    suspend fun importUri(
        uri: Uri,
        destinationDir: File,
        baseName: String,
    ): ImportedAudioSample = withContext(Dispatchers.IO) {
        val originalName = queryDisplayName(uri) ?: "audio_sample"
        val input = context.contentResolver.openInputStream(uri)
            ?: throw AudioImportException("无法读取所选音频，请重新选择")
        input.use {
            importStream(it, originalName, destinationDir, baseName)
        }
    }

    suspend fun importFile(
        source: File,
        destinationDir: File,
        baseName: String,
    ): ImportedAudioSample = withContext(Dispatchers.IO) {
        if (!source.isFile || source.length() == 0L) {
            throw AudioImportException("音频文件为空或不存在")
        }
        FileInputStream(source).use {
            importStream(it, source.name, destinationDir, baseName)
        }
    }

    private fun importStream(
        rawInput: InputStream,
        originalName: String,
        destinationDir: File,
        baseName: String,
    ): ImportedAudioSample {
        destinationDir.mkdirs()
        val input = BufferedInputStream(rawInput)
        input.mark(HEADER_SIZE)
        val header = ByteArray(HEADER_SIZE)
        val headerLength = input.read(header)
        input.reset()

        val format = detectFormat(header, headerLength)
            ?: throw AudioImportException("MiMo 音色克隆仅支持 WAV 和 MP3 文件")
        val destination = File(destinationDir, "$baseName.${format.extension}")

        try {
            var totalBytes = 0L
            FileOutputStream(destination).buffered().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read == 0) continue
                    totalBytes += read
                    if (base64EncodedSize(totalBytes) > MAX_BASE64_BYTES) {
                        throw AudioImportException("音频过大：Base64 编码后不能超过 10MB")
                    }
                    output.write(buffer, 0, read)
                }
            }
            if (totalBytes == 0L) throw AudioImportException("音频文件为空")

            return ImportedAudioSample(
                file = destination,
                originalName = originalName,
                format = format,
                byteSize = totalBytes,
                durationMs = readDuration(destination),
            )
        } catch (error: Exception) {
            destination.delete()
            throw error
        }
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        }
    }.getOrNull()

    private fun readDuration(file: File): Long? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    companion object {
        const val MAX_BASE64_BYTES: Long = 10L * 1024L * 1024L
        private const val HEADER_SIZE = 12

        fun base64EncodedSize(rawBytes: Long): Long = 4L * ((rawBytes + 2L) / 3L)

        fun detectFormat(header: ByteArray, length: Int = header.size): SupportedAudioFormat? {
            if (length >= 12 &&
                header.copyOfRange(0, 4).contentEquals("RIFF".encodeToByteArray()) &&
                header.copyOfRange(8, 12).contentEquals("WAVE".encodeToByteArray())
            ) {
                return SupportedAudioFormat.WAV
            }
            if (length >= 3 &&
                header[0] == 'I'.code.toByte() &&
                header[1] == 'D'.code.toByte() &&
                header[2] == '3'.code.toByte()
            ) {
                return SupportedAudioFormat.MP3
            }
            if (length >= 2 &&
                (header[0].toInt() and 0xFF) == 0xFF &&
                (header[1].toInt() and 0xE0) == 0xE0
            ) {
                return SupportedAudioFormat.MP3
            }
            return null
        }
    }
}
