package com.linky.voiceclone.util

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.abs

class WavRecorder(
    private val sampleRate: Int = 44100,
    private val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    private val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
) {
    private var recorder: AudioRecord? = null
    private var recordingThread: Thread? = null
    @Volatile private var isRecording = false
    @Volatile private var currentAmplitude = 0f  // 0..1 normalized

    fun start(output: File) {
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate, channelConfig, audioFormat,
            bufferSize * 2
        )
        RandomAccessFile(output, "rw").use { it.write(ByteArray(44)) }
        isRecording = true
        recorder?.startRecording()

        recordingThread = Thread {
            val buffer = ByteArray(4096)
            RandomAccessFile(output, "rw").use { raf ->
                raf.seek(44)
                while (isRecording) {
                    val read = recorder?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        raf.write(buffer, 0, read)
                        // 计算振幅（取 PCM 16-bit 最大绝对值，归一化到 0..1）
                        var maxSample = 0
                        var i = 0
                        while (i < read - 1) {
                            val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
                            val absVal = abs(sample.toShort().toInt())
                            if (absVal > maxSample) maxSample = absVal
                            i += 2
                        }
                        currentAmplitude = (maxSample.toFloat() / 32768f).coerceIn(0f, 1f)
                    }
                }
                val dataSize = raf.length() - 44
                raf.seek(0)
                raf.write(wavHeader(dataSize.toInt()))
            }
        }.apply { start() }
    }

    fun stop() {
        isRecording = false
        try { recorder?.stop() } catch (_: Exception) {}
        recordingThread?.join(3000)
        recorder?.release()
        recorder = null
        recordingThread = null
        currentAmplitude = 0f
    }

    fun isRecording() = isRecording

    /** 当前录音振幅 0..1，每帧可读取 */
    fun amplitude() = currentAmplitude

    private fun wavHeader(dataSize: Int): ByteArray {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val totalSize = dataSize + 36
        return ByteArray(44).also { h ->
            h[0] = 'R'.code.toByte(); h[1] = 'I'.code.toByte()
            h[2] = 'F'.code.toByte(); h[3] = 'F'.code.toByte()
            writeInt(h, 4, totalSize)
            h[8] = 'W'.code.toByte(); h[9] = 'A'.code.toByte()
            h[10] = 'V'.code.toByte(); h[11] = 'E'.code.toByte()
            h[12] = 'f'.code.toByte(); h[13] = 'm'.code.toByte()
            h[14] = 't'.code.toByte(); h[15] = ' '.code.toByte()
            writeInt(h, 16, 16)
            writeShort(h, 20, 1); writeShort(h, 22, channels)
            writeInt(h, 24, sampleRate); writeInt(h, 28, byteRate)
            writeShort(h, 32, blockAlign); writeShort(h, 34, bitsPerSample)
            h[36] = 'd'.code.toByte(); h[37] = 'a'.code.toByte()
            h[38] = 't'.code.toByte(); h[39] = 'a'.code.toByte()
            writeInt(h, 40, dataSize)
        }
    }

    private fun writeInt(arr: ByteArray, offset: Int, value: Int) {
        arr[offset] = (value and 0xFF).toByte()
        arr[offset + 1] = (value shr 8 and 0xFF).toByte()
        arr[offset + 2] = (value shr 16 and 0xFF).toByte()
        arr[offset + 3] = (value shr 24 and 0xFF).toByte()
    }

    private fun writeShort(arr: ByteArray, offset: Int, value: Int) {
        arr[offset] = (value and 0xFF).toByte()
        arr[offset + 1] = (value shr 8 and 0xFF).toByte()
    }
}
