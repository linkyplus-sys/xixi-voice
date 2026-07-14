package com.linky.voiceclone.ui.home
import com.linky.voiceclone.R

import android.media.MediaPlayer
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import com.linky.voiceclone.ui.AppTopBar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linky.voiceclone.data.Voice
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import com.linky.voiceclone.util.PlayerManager
import com.linky.voiceclone.viewmodel.SynthMode
import com.linky.voiceclone.viewmodel.SynthState
import com.linky.voiceclone.viewmodel.SynthViewModel
import com.linky.voiceclone.viewmodel.VoiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen( onSettings: () -> Unit) {
    val synthVm: SynthViewModel = hiltViewModel()
    val voiceVm: VoiceViewModel = hiltViewModel()
    val voices by voiceVm.voices.collectAsStateWithLifecycle()
    val mode by synthVm.mode.collectAsStateWithLifecycle()
    val state by synthVm.state.collectAsStateWithLifecycle()
    val error by synthVm.errorMsg.collectAsStateWithLifecycle()
    val resultFile by synthVm.resultFile.collectAsStateWithLifecycle()
    val text by synthVm.text.collectAsStateWithLifecycle()
    val voiceDesc by synthVm.voiceDesc.collectAsStateWithLifecycle()
    val progress by synthVm.synthProgress.collectAsStateWithLifecycle()
    var selectedVoiceId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(voices) {
        if (selectedVoiceId == null && voices.isNotEmpty()) selectedVoiceId = voices.first().id
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = "嘻嘻配音",
            
            navigationIcon = {
                Box(Modifier.padding(start = 12.dp)) {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = "Logo",
                        modifier = Modifier.size(24.dp).clip(CircleShape)
                    )
                }
            },
            actions = { IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "设置") } }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 模式切换
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = mode == SynthMode.CLONE,
                        onClick = { synthVm.mode.value = SynthMode.CLONE },
                        label = { Text("🎤 音频克隆", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }, 
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2563EB).copy(alpha = 0.3f),
                            selectedLabelColor = Color.White
                        ))
                    FilterChip(selected = mode == SynthMode.DESIGN,
                        onClick = { synthVm.mode.value = SynthMode.DESIGN },
                        label = { Text("✨ 音色设计", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }, 
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF2563EB).copy(alpha = 0.3f),
                            selectedLabelColor = Color.White
                        ))
                }
            }

            // 克隆模式：音色下拉选择
            if (mode == SynthMode.CLONE) {
                item {
                    VoiceDropdown(
                        voices = voices,
                        selectedId = selectedVoiceId,
                        onSelect = { selectedVoiceId = it }
                    )
                }
            }

            // 设计模式
            if (mode == SynthMode.DESIGN) {
                item {
                    OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text("音色描述", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(4.dp))
                            OutlinedTextField(value = voiceDesc, onValueChange = { synthVm.voiceDesc.value = it },
                                placeholder = { Text("例如：年轻女性，声音甜美清亮") },
                                shape = RoundedCornerShape(8.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth(), minLines = 2)
                        }
                    }
                }
            }

            // 文本输入
            item {
                OutlinedTextField(value = text, onValueChange = { synthVm.text.value = it },
                    label = { Text("配音文本") },
                    placeholder = { Text("输入要合成的文本…") },
                    shape = MaterialTheme.shapes.medium,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth(), minLines = 3)
            }

            // 合成按钮 + 进度条
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { synthVm.synthesize(selectedVoiceId, voiceVm.getSampleDir()) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = state != SynthState.LOADING
                    ) {
                        if (state == SynthState.LOADING) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("生成中…")
                        } else {
                            Icon(Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(4.dp))
                            Text("开始配音")
                        }
                    }
                    // 进度条
                    if (state == SynthState.LOADING) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                if (progress < 0.3f) "正在连接 API…"
                                else if (progress < 0.7f) "正在合成语音…"
                                else "正在处理音频…",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${(progress * 100).toInt()}%",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 错误
            if (error.isNotBlank()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(12.dp)) {
                        Text(error, Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 14.sp)
                    }
                }
            }

            // 结果卡片（合成完成后一直显示，切页面不消失）
            if (resultFile != null) {
                item {
                    ResultCard(resultFile!!)

                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceDropdown(voices: List<Voice>, selectedId: String?, onSelect: (String) -> Unit) {
    var showSheet by remember { mutableStateOf(false) }
    val selectedVoice = voices.find { it.id == selectedId }

    // 选择器卡片
    OutlinedCard(Modifier.fillMaxWidth().clickable { showSheet = true }, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text("选择音色", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mic, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    if (selectedVoice != null) {
                        Text(selectedVoice.name, fontWeight = FontWeight.Medium)
                        if (selectedVoice.description.isNotBlank()) {
                            Text(selectedVoice.description, fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Text("请选择音色", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    // Bottom Sheet
    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Text("选择音色", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp))
            voices.forEach { voice ->
                val isSelected = voice.id == selectedId
                ListItem(
                    headlineContent = { Text(voice.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    supportingContent = {
                        if (voice.description.isNotBlank()) {
                            Text(voice.description, fontSize = 13.sp)
                        }
                    },
                    leadingContent = {
                        Icon(Icons.Default.Mic, null, Modifier.size(20.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingContent = {
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier.clickable { onSelect(voice.id); showSheet = false },
                    colors = if (isSelected) ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ) else ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
            // 底部留白给手势区域
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ResultCard(file: java.io.File, autoPlay: Boolean = false) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    val player = remember {
        MediaPlayer().apply {
            setDataSource(file.absolutePath)
            prepare()
            setOnCompletionListener { playing = false }
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    val dateStr = remember(file.lastModified()) {
        java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(file.lastModified()))
    }

    OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (playing) { PlayerManager.pause(player); playing = false }
                else { PlayerManager.play(player) { playing = false }; playing = true }
            }) {
                Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null)
            }
            Column(Modifier.weight(1f)) {
                Text(file.name, fontSize = 13.sp, maxLines = 1)
                Text("${(file.length() / 1024)}KB · $dateStr", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
                intent.type = "audio/wav"
                intent.putExtra(android.content.Intent.EXTRA_STREAM,
                    androidx.core.content.FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", file))
                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(android.content.Intent.createChooser(intent, "分享音频"))
            }) {
                Icon(Icons.Default.Share, "分享")
            }
        }
    }
}
