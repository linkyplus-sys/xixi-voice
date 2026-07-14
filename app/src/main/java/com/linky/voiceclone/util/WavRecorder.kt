package com.linky.voiceclone.util

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.abs

class WavRecorder(
    private val sampleRate: Int = 44_100,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
) {
    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isStopping = false
    private var recordingError: String? = null

    @Volatile private var isRecording = false
    @Volatile private var currentAmplitude = 0f

    @Synchronized
    fun start(output: File) {
        check(!isRecording && !isStopping) { "录音设备正在使用中" }
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBufferSize <= 0) throw IllegalStateException("当前设备不支持录音参数")

        output.parentFile?.mkdirs()
        RandomAccessFile(output, "rw").use {
            it.setLength(0)
            it.write(ByteArray(WAV_HEADER_SIZE))
        }

        val audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBufferSize * 2,
            )
        } catch (_: SecurityException) {
            output.delete()
            throw IllegalStateException("麦克风权限已被拒绝")
        }
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            output.delete()
            throw IllegalStateException("无法初始化麦克风")
        }

        try {
            audioRecord.startRecording()
        } catch (error: Exception) {
            audioRecord.release()
            output.delete()
            throw IllegalStateException("无法开始录音：${error.message ?: "请检查麦克风权限"}")
        }

        recorder = audioRecord
        recordingError = null
        isRecording = true
        recordingThread = Thread({ recordLoop(audioRecord, output) }, "voice-wav-recorder").apply {
            start()
        }
    }

    /** Stops immediately and finalizes/releases the recorder off the main thread. */
    @Synchronized
    fun stop(onFinished: (error: String?) -> Unit = {}) {
        if ((!isRecording && recordingThread == null) || isStopping) return
        isRecording = false
        isStopping = true
        currentAmplitude = 0f

        val audioRecord = recorder
        val worker = recordingThread
        runCatching { audioRecord?.stop() }

        Thread({
            runCatching { worker?.join(STOP_TIMEOUT_MS) }
            runCatching { audioRecord?.release() }
            val error: String?
            synchronized(this) {
                recorder = null
                recordingThread = null
                isStopping = false
                error = recordingError
                recordingError = null
            }
            Handler(Looper.getMainLooper()).post { onFinished(error) }
        }, "voice-wav-finalizer").start()
    }

    fun isRecording(): Boolean = isRecording

    fun isStopping(): Boolean = isStopping

    fun amplitude(): Float = currentAmplitude

    private fun recordLoop(audioRecord: AudioRecord, output: File) {
        try {
            val buffer = ByteArray(4096)
            RandomAccessFile(output, "rw").use { raf ->
                raf.seek(WAV_HEADER_SIZE.toLong())
                while (isRecording) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        raf.write(buffer, 0, read)
                        updateAmplitude(buffer, read)
                    } else if (read < 0 && isRecording) {
                        throw IllegalStateException("录音读取失败：$read")
                    }
                }
                val dataSize = (raf.length() - WAV_HEADER_SIZE).coerceAtLeast(0L)
                raf.seek(0)
                raf.write(wavHeader(dataSize.toInt()))
            }
        } catch (error: Exception) {
            if (isRecording) recordingError = error.message ?: "录音失败"
            isRecording = false
        } finally {
            currentAmplitude = 0f
        }
    }

    private fun updateAmplitude(buffer: ByteArray, read: Int) {
        var maxSample = 0
        var index = 0
        while (index < read - 1) {
            val sample = (buffer[index].toInt() and 0xFF) or
                (buffer[index + 1].toInt() shl 8)
            maxSample = maxOf(maxSample, abs(sample.toShort().toInt()))
            index += 2
        }
        currentAmplitude = (maxSample.toFloat() / 32768f).coerceIn(0f, 1f)
    }

    private fun wavHeader(dataSize: Int): ByteArray {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        return ByteArray(WAV_HEADER_SIZE).also { header ->
            "RIFF".encodeToByteArray().copyInto(header, 0)
            writeInt(header, 4, dataSize + 36)
            "WAVE".encodeToByteArray().copyInto(header, 8)
            "fmt ".encodeToByteArray().copyInto(header, 12)
            writeInt(header, 16, 16)
            writeShort(header, 20, 1)
            writeShort(header, 22, channels)
            writeInt(header, 24, sampleRate)
            writeInt(header, 28, byteRate)
            writeShort(header, 32, blockAlign)
            writeShort(header, 34, bitsPerSample)
            "data".encodeToByteArray().copyInto(header, 36)
            writeInt(header, 40, dataSize)
        }
    }

    private fun writeInt(array: ByteArray, offset: Int, value: Int) {
        array[offset] = (value and 0xFF).toByte()
        array[offset + 1] = (value shr 8 and 0xFF).toByte()
        array[offset + 2] = (value shr 16 and 0xFF).toByte()
        array[offset + 3] = (value shr 24 and 0xFF).toByte()
    }

    private fun writeShort(array: ByteArray, offset: Int, value: Int) {
        array[offset] = (value and 0xFF).toByte()
        array[offset + 1] = (value shr 8 and 0xFF).toByte()
    }

    companion object {
        private const val WAV_HEADER_SIZE = 44
        private const val STOP_TIMEOUT_MS = 3_000L
    }
}
