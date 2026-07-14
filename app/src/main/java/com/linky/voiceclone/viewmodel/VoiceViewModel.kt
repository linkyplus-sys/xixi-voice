package com.linky.voiceclone.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linky.voiceclone.data.Voice
import com.linky.voiceclone.data.VoiceDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    app: Application,
    private val dao: VoiceDao
) : AndroidViewModel(app) {

    private val sampleDir = File(app.filesDir, "audio/samples").also { it.mkdirs() }

    val voices = dao.getAll().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addVoice(name: String, description: String, sourceFile: File) {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString().take(8)
            val ext = sourceFile.extension.lowercase().let {
                if (it in listOf("wav","mp3")) it else "wav"
            }
            val dest = File(sampleDir, "$id.$ext")
            sourceFile.copyTo(dest, overwrite = true)
            dao.insert(Voice(id = id, name = name, description = description,
                sampleFileName = "$id.$ext", sampleSize = dest.length()))
        }
    }

    fun updateVoice(voice: Voice) {
        viewModelScope.launch { dao.update(voice) }
    }

    fun deleteVoice(voice: Voice) {
        viewModelScope.launch {
            File(sampleDir, voice.sampleFileName).let { if (it.exists()) it.delete() }
            dao.delete(voice)
        }
    }

    fun markUsed(voiceId: String) {
        viewModelScope.launch { dao.updateLastUsed(voiceId) }
    }

    fun getSampleDir() = sampleDir
}
