package com.linky.voiceclone.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.linky.voiceclone.api.MiMoTtsApi
import com.linky.voiceclone.data.SettingsDataStore
import com.linky.voiceclone.data.VoiceDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

enum class SynthMode { CLONE, DESIGN }
enum class SynthState { IDLE, LOADING, DONE, ERROR }

@HiltViewModel
class SynthViewModel @Inject constructor(
    app: Application,
    private val settings: SettingsDataStore,
    private val voiceDao: VoiceDao
) : AndroidViewModel(app) {

    private val api = MiMoTtsApi()
    private val outputDir = File(app.filesDir, "audio/output").also { it.mkdirs() }

    val mode = MutableStateFlow(SynthMode.CLONE)
    val voiceDesc = MutableStateFlow("")
    val text = MutableStateFlow("")
    val state = MutableStateFlow(SynthState.IDLE)
    val errorMsg = MutableStateFlow("")
    val resultFile = MutableStateFlow<File?>(null)
    val synthProgress = MutableStateFlow(0f)

    init {
        viewModelScope.launch {
            combine(settings.apiKey, settings.baseUrl) { k, u -> k to u }
                .collect { (k, u) -> api.updateConfig(k, u) }
        }
    }

    fun synthesize(selectedVoiceId: String?, sampleDir: File) {
        val t = text.value.trim()
        if (t.isBlank()) { errorMsg.value = "请输入文本"; return }
        viewModelScope.launch {
            state.value = SynthState.LOADING
            errorMsg.value = ""
            resultFile.value = null
            synthProgress.value = 0f
            try {
                val progressJob = launch {
                    while (synthProgress.value < 0.9f) {
                        kotlinx.coroutines.delay(300)
                        synthProgress.value = (synthProgress.value + 0.03f).coerceAtMost(0.9f)
                    }
                }
                val result = when (mode.value) {
                    SynthMode.CLONE -> {
                        val vid = selectedVoiceId ?: throw Exception("请先选择音色")
                        val sample = sampleDir.listFiles()?.firstOrNull { it.name.startsWith(vid) }
                            ?: throw Exception("样本文件不存在")
                        // 标记音色为最近使用
                        voiceDao.updateLastUsed(vid)
                        api.synthesizeClone(sample, t)
                    }
                    SynthMode.DESIGN -> {
                        val desc = voiceDesc.value.trim()
                        if (desc.isBlank()) throw Exception("请输入音色描述")
                        api.synthesizeDesign(desc, t)
                    }
                }
                progressJob.cancel()
                synthProgress.value = 1f
                val tag = if (mode.value == SynthMode.CLONE) selectedVoiceId else "design"
                val ext = result.format.ifBlank { "wav" }
                val outFile = File(outputDir, "${tag}_${System.currentTimeMillis()}.$ext")
                outFile.writeBytes(result.audioBytes)
                resultFile.value = outFile
                state.value = SynthState.DONE
            } catch (e: Exception) {
                errorMsg.value = e.message ?: "未知错误"
                state.value = SynthState.ERROR
            }
        }
    }

    fun getHistory(): List<File> {
        return outputDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun deleteHistory(f: File) { f.delete() }
}
