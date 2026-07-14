package com.linky.voiceclone.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linky.voiceclone.audio.AudioSampleImporter
import com.linky.voiceclone.data.Voice
import com.linky.voiceclone.data.VoiceDao
import com.linky.voiceclone.util.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

sealed interface VoiceImportState {
    data object Idle : VoiceImportState
    data object Importing : VoiceImportState
    data class Success(val voice: Voice) : VoiceImportState
    data class Error(val message: String) : VoiceImportState
}

@HiltViewModel
class VoiceViewModel @Inject constructor(
    app: Application,
    private val dao: VoiceDao,
    private val audioImporter: AudioSampleImporter,
) : AndroidViewModel(app) {

    private val sampleDir = File(app.filesDir, "audio/samples").also { it.mkdirs() }

    val voices = dao.getAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    private val _importState = MutableStateFlow<VoiceImportState>(VoiceImportState.Idle)
    val importState: StateFlow<VoiceImportState> = _importState.asStateFlow()

    fun importVoice(name: String, description: String, sourceUri: Uri) {
        viewModelScope.launch {
            _importState.value = VoiceImportState.Importing
            val id = UUID.randomUUID().toString().take(8)
            try {
                val imported = audioImporter.importUri(sourceUri, sampleDir, id)
                val voice = Voice(
                    id = id,
                    name = name.trim().ifBlank { "未命名" },
                    description = description.trim(),
                    sampleFileName = imported.file.name,
                    sampleSize = imported.byteSize,
                )
                try {
                    dao.insert(voice)
                } catch (error: Exception) {
                    imported.file.delete()
                    throw error
                }
                _importState.value = VoiceImportState.Success(voice)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _importState.value = VoiceImportState.Error(error.message ?: "音频导入失败")
            }
        }
    }

    fun importRecordedVoice(name: String, description: String, sourceFile: File) {
        viewModelScope.launch {
            _importState.value = VoiceImportState.Importing
            val id = UUID.randomUUID().toString().take(8)
            try {
                val imported = audioImporter.importFile(sourceFile, sampleDir, id)
                val voice = Voice(
                    id = id,
                    name = name.trim().ifBlank { "未命名" },
                    description = description.trim(),
                    sampleFileName = imported.file.name,
                    sampleSize = imported.byteSize,
                )
                try {
                    dao.insert(voice)
                } catch (error: Exception) {
                    imported.file.delete()
                    throw error
                }
                _importState.value = VoiceImportState.Success(voice)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                _importState.value = VoiceImportState.Error(error.message ?: "录音保存失败")
            }
        }
    }

    fun clearImportState() {
        _importState.value = VoiceImportState.Idle
    }

    fun updateVoice(voice: Voice) {
        viewModelScope.launch { dao.update(voice) }
    }

    fun deleteVoice(voice: Voice) {
        viewModelScope.launch {
            File(sampleDir, voice.sampleFileName).let {
                PlayerManager.release(it.absolutePath)
                if (it.exists()) it.delete()
            }
            dao.delete(voice)
        }
    }

    fun markUsed(voiceId: String) {
        viewModelScope.launch { dao.updateLastUsed(voiceId) }
    }

    fun getSampleDir(): File = sampleDir
}
