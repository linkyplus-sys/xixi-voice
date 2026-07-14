package com.linky.voiceclone.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linky.voiceclone.ui.AppTopBar
import com.linky.voiceclone.ui.components.GlassSurface
import com.linky.voiceclone.viewmodel.ConfigTestState
import com.linky.voiceclone.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val savedKey by viewModel.savedApiKey.collectAsStateWithLifecycle()
    val savedUrl by viewModel.savedBaseUrl.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val testState by viewModel.testState.collectAsStateWithLifecycle()
    var apiKey by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }

    LaunchedEffect(savedKey) { if (apiKey.isBlank()) apiKey = savedKey }
    LaunchedEffect(savedUrl) { if (baseUrl.isBlank()) baseUrl = savedUrl }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "设置",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("MiMo API", style = MaterialTheme.typography.titleMedium)
            }
            item {
                GlassSurface(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it; viewModel.markEdited() },
                            label = { Text("API Key") },
                            singleLine = true,
                            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showKey = !showKey }) {
                                    Icon(
                                        if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (showKey) "隐藏 API Key" else "显示 API Key",
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = { baseUrl = it; viewModel.markEdited() },
                            label = { Text("Base URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text(
                            "API Key 仅保存在当前设备。测试连接会生成一条很短的测试音频，可能产生少量用量。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.test(apiKey.trim(), baseUrl.trim()) },
                                enabled = testState != ConfigTestState.Testing,
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) {
                                if (testState == ConfigTestState.Testing) {
                                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.size(7.dp))
                                }
                                Text(if (testState == ConfigTestState.Testing) "测试中" else "测试连接")
                            }
                            Button(
                                onClick = { viewModel.save(apiKey, baseUrl) },
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) {
                                if (saved) {
                                    Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp))
                                    Spacer(Modifier.size(7.dp))
                                }
                                Text(if (saved) "已保存" else "保存配置")
                            }
                        }
                    }
                }
            }

            when (val result = testState) {
                ConfigTestState.Success -> item {
                    ConnectionStatus(
                        title = "连接成功",
                        message = "API Key、接口地址和 MiMo TTS 权限均可用",
                        success = true,
                    )
                }
                is ConfigTestState.Error -> item {
                    ConnectionStatus(
                        title = "连接失败",
                        message = result.message,
                        success = false,
                    )
                }
                else -> Unit
            }

            item {
                Text("使用说明", style = MaterialTheme.typography.titleMedium)
            }
            item {
                GlassSurface(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("音色克隆", fontWeight = FontWeight.SemiBold)
                        Text(
                            "导入 WAV 或 MP3 样本，复刻已有声音。Base64 编码后不能超过 10MB。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("音色设计", fontWeight = FontWeight.SemiBold)
                        Text(
                            "通过年龄、性别、声音质感、节奏和情绪描述生成新的声线。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatus(title: String, message: String, success: Boolean) {
    GlassSurface(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (success) Icons.Default.CloudDone else Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.size(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
