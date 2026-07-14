package com.linky.voiceclone.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linky.voiceclone.api.MiMoTtsApi
import com.linky.voiceclone.data.GenerationHistory
import com.linky.voiceclone.data.GenerationHistoryDao
import com.linky.voiceclone.data.SettingsDataStore
import com.linky.voiceclone.data.VoiceDao
import com.linky.voiceclone.util.PlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

enum class SynthMode { CLONE, DESIGN }
enum class SynthState { IDLE, LOADING, DONE, ERROR, CANCELLED }
enum class SynthStage { IDLE, PREPARING, REQUESTING, SAVING, COMPLETE }

@HiltViewModel
class SynthViewModel @Inject constructor(
    app: Application,
    private val settings: SettingsDataStore,
    private val voiceDao: VoiceDao,
    private val historyDao: GenerationHistoryDao,
) : AndroidViewModel(app) {
    private val api = MiMoTtsApi()
    private val sampleDir = File(app.filesDir, "audio/samples").also { it.mkdirs() }
    private val outputDir = File(app.filesDir, "audio/output").also { it.mkdirs() }
    private var synthesisJob: Job? = null

    val mode = MutableStateFlow(SynthMode.CLONE)
    val voiceDesc = MutableStateFlow("")
    val text = MutableStateFlow("")
    val state = MutableStateFlow(SynthState.IDLE)
    val stage = MutableStateFlow(SynthStage.IDLE)
    val errorMsg = MutableStateFlow("")
    val resultFile = MutableStateFlow<File?>(null)
    val history = historyDao.observeAll().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    init {
        viewModelScope.launch(Dispatchers.IO) { importLegacyHistory() }
    }

    fun synthesize(selectedVoiceId: String?) {
        val targetText = text.value.trim()
        if (targetText.isBlank()) {
            errorMsg.value = "请输入配音文本"
            state.value = SynthState.ERROR
            return
        }
        if (state.value == SynthState.LOADING) return

        synthesisJob = viewModelScope.launch {
            state.value = SynthState.LOADING
            stage.value = SynthStage.PREPARING
            errorMsg.value = ""
            resultFile.value = null
            try {
                api.updateConfig(settings.apiKey.first(), settings.baseUrl.first())

                val selectedMode = mode.value
                var historyVoiceId: String? = null
                var historyVoiceName = "设计音色"
                val result = when (selectedMode) {
                    SynthMode.CLONE -> {
                        val voiceId = selectedVoiceId ?: throw IllegalArgumentException("请先选择音色")
                        val voice = voiceDao.getById(voiceId)
                            ?: throw IllegalArgumentException("所选音色不存在")
                        val sample = File(sampleDir, voice.sampleFileName)
                        if (!sample.isFile) throw IllegalArgumentException("音色样本文件不存在")
                        historyVoiceId = voice.id
                        historyVoiceName = voice.name
                        stage.value = SynthStage.REQUESTING
                        api.synthesizeClone(sample, targetText).also {
                            voiceDao.updateLastUsed(voiceId)
                        }
                    }

                    SynthMode.DESIGN -> {
                        val description = voiceDesc.value.trim()
                        if (description.isBlank()) throw IllegalArgumentException("请输入音色描述")
                        stage.value = SynthStage.REQUESTING
                        api.synthesizeDesign(description, targetText)
                    }
                }

                stage.value = SynthStage.SAVING
                val tag = if (selectedMode == SynthMode.CLONE) selectedVoiceId else "design"
                val extension = result.format.lowercase().takeIf { it in OUTPUT_FORMATS } ?: "wav"
                val createdAt = System.currentTimeMillis()
                val output = File(outputDir, "${tag}_$createdAt.$extension")
                withContext(Dispatchers.IO) { output.writeBytes(result.audioBytes) }
                historyDao.insert(
                    GenerationHistory(
                        id = UUID.randomUUID().toString(),
                        text = targetText,
                        voiceId = historyVoiceId,
                        voiceName = historyVoiceName,
                        mode = selectedMode.name.lowercase(),
                        outputFileName = output.name,
                        format = extension,
                        fileSize = output.length(),
                        createdAt = createdAt,
                    ),
                )
                resultFile.value = output
                stage.value = SynthStage.COMPLETE
                state.value = SynthState.DONE
            } catch (cancelled: CancellationException) {
                stage.value = SynthStage.IDLE
                state.value = SynthState.CANCELLED
                errorMsg.value = "已取消生成"
            } catch (error: Exception) {
                stage.value = SynthStage.IDLE
                errorMsg.value = error.message ?: "生成失败，请重试"
                state.value = SynthState.ERROR
            } finally {
                synthesisJob = null
            }
        }
    }

    fun cancelSynthesis() {
        synthesisJob?.cancel()
    }

    fun historyFile(item: GenerationHistory): File = File(outputDir, item.outputFileName)

    fun deleteHistory(item: GenerationHistory) {
        viewModelScope.launch {
            val file = historyFile(item)
            PlayerManager.release(file.absolutePath)
            withContext(Dispatchers.IO) { file.delete() }
            historyDao.deleteById(item.id)
        }
    }

    private suspend fun importLegacyHistory() {
        val knownFiles = historyDao.existingFileNames().toHashSet()
        outputDir.listFiles()
            ?.filter { it.isFile && it.name !in knownFiles }
            ?.forEach { file ->
                val mode = if (file.name.startsWith("design_")) "design" else "clone"
                historyDao.insert(
                    GenerationHistory(
                        id = "legacy:${file.name}",
                        text = "旧版生成记录",
                        voiceId = null,
                        voiceName = if (mode == "design") "设计音色" else "历史音色",
                        mode = mode,
                        outputFileName = file.name,
                        format = file.extension.ifBlank { "wav" },
                        fileSize = file.length(),
                        createdAt = file.lastModified(),
                    ),
                )
            }
    }

    companion object {
        private val OUTPUT_FORMATS = setOf("wav", "mp3", "pcm", "pcm16")
    }
}
