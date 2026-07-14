package com.linky.voiceclone.ui.settings

import com.linky.voiceclone.ui.AppTopBar
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.linky.voiceclone.data.SettingsDataStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen( onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { SettingsDataStore(context) }
    val scope = rememberCoroutineScope()

    val savedKey by settings.apiKey.collectAsState(initial = "")
    val savedUrl by settings.baseUrl.collectAsState(initial = "https://token-plan-cn.xiaomimimo.com/v1")

    var apiKey by remember(savedKey) { mutableStateOf(savedKey) }
    var baseUrl by remember(savedUrl) { mutableStateOf(savedUrl) }
    var showKey by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "设置",
            
            navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
            }
        )

        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("API 配置", style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it; saved = false },
                label = { Text("API Key") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                    }
                }
            )

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it; saved = false },
                label = { Text("Base URL") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    scope.launch {
                        settings.setApiKey(apiKey.trim())
                        settings.setBaseUrl(baseUrl.trim())
                        saved = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (saved) "✅ 已保存" else "保存")
            }

            Spacer(Modifier.height(16.dp))

            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.12f)
            )) {
                Column(Modifier.padding(16.dp)) {
                    Text("💡 说明", fontWeight = FontWeight.Medium, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text("• 使用 MiMo-V2.5-TTS 系列 API\n" +
                         "• 克隆模式：上传音频样本复刻音色\n" +
                         "• 设计模式：文本描述生成音色\n" +
                         "• Token Plan URL: token-plan-cn.xiaomimimo.com",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}
