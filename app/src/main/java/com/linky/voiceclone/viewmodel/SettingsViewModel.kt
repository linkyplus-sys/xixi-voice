package com.linky.voiceclone.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linky.voiceclone.api.MiMoTtsApi
import com.linky.voiceclone.data.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ConfigTestState {
    data object Idle : ConfigTestState
    data object Testing : ConfigTestState
    data object Success : ConfigTestState
    data class Error(val message: String) : ConfigTestState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsDataStore,
) : ViewModel() {
    val savedApiKey: StateFlow<String> = settings.apiKey.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        "",
    )
    val savedBaseUrl: StateFlow<String> = settings.baseUrl.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        MiMoTtsApi.DEFAULT_BASE_URL,
    )
    val testState = MutableStateFlow<ConfigTestState>(ConfigTestState.Idle)
    val saved = MutableStateFlow(false)

    fun save(apiKey: String, baseUrl: String) {
        viewModelScope.launch {
            settings.setApiKey(apiKey.trim())
            settings.setBaseUrl(baseUrl.trim())
            saved.value = true
        }
    }

    /** Performs a tiny voicedesign request so success verifies auth, endpoint and TTS access. */
    fun test(apiKey: String, baseUrl: String) {
        if (testState.value == ConfigTestState.Testing) return
        viewModelScope.launch {
            testState.value = ConfigTestState.Testing
            try {
                MiMoTtsApi().apply { updateConfig(apiKey, baseUrl) }
                    .synthesizeDesign("清晰自然的中性普通话声音", "连接测试")
                testState.value = ConfigTestState.Success
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                testState.value = ConfigTestState.Error(error.message ?: "连接测试失败")
            }
        }
    }

    fun markEdited() {
        saved.value = false
        testState.value = ConfigTestState.Idle
    }
}
