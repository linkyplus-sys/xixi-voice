package com.linky.voiceclone.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioSampleImporterTest {
    @Test
    fun detectsWavFromRiffWaveHeader() {
        val header = ByteArray(12)
        "RIFF".encodeToByteArray().copyInto(header, 0)
        "WAVE".encodeToByteArray().copyInto(header, 8)

        assertEquals(SupportedAudioFormat.WAV, AudioSampleImporter.detectFormat(header))
    }

    @Test
    fun detectsMp3FromId3OrFrameSync() {
        assertEquals(
            SupportedAudioFormat.MP3,
            AudioSampleImporter.detectFormat(byteArrayOf('I'.code.toByte(), 'D'.code.toByte(), '3'.code.toByte())),
        )
        assertEquals(
            SupportedAudioFormat.MP3,
            AudioSampleImporter.detectFormat(byteArrayOf(0xFF.toByte(), 0xFB.toByte())),
        )
    }

    @Test
    fun doesNotTrustUnsupportedContainerHeader() {
        val mp4Header = byteArrayOf(0, 0, 0, 24, 'f'.code.toByte(), 't'.code.toByte(), 'y'.code.toByte(), 'p'.code.toByte())
        assertNull(AudioSampleImporter.detectFormat(mp4Header))
    }

    @Test
    fun calculatesBase64SizeWithoutReadingWholeFile() {
        assertEquals(0L, AudioSampleImporter.base64EncodedSize(0))
        assertEquals(4L, AudioSampleImporter.base64EncodedSize(1))
        assertEquals(4L, AudioSampleImporter.base64EncodedSize(3))
        assertEquals(8L, AudioSampleImporter.base64EncodedSize(4))
    }
}
