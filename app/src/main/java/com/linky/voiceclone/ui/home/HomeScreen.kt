package com.linky.voiceclone.ui.home

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linky.voiceclone.R
import com.linky.voiceclone.data.Voice
import com.linky.voiceclone.ui.AppTopBar
import com.linky.voiceclone.ui.components.GlassSurface
import com.linky.voiceclone.ui.components.StaticWaveform
import com.linky.voiceclone.ui.components.VoiceAvatar
import com.linky.voiceclone.ui.theme.BrandBlue
import com.linky.voiceclone.ui.theme.BrandViolet
import com.linky.voiceclone.util.PlayerManager
import com.linky.voiceclone.viewmodel.SynthMode
import com.linky.voiceclone.viewmodel.SynthStage
import com.linky.voiceclone.viewmodel.SynthState
import com.linky.voiceclone.viewmodel.SynthViewModel
import com.linky.voiceclone.viewmodel.VoiceViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSettings: () -> Unit,
    onVoices: () -> Unit = {},
) {
    val synthViewModel: SynthViewModel = hiltViewModel()
    val voiceViewModel: VoiceViewModel = hiltViewModel()
    val voices by voiceViewModel.voices.collectAsStateWithLifecycle()
    val mode by synthViewModel.mode.collectAsStateWithLifecycle()
    val state by synthViewModel.state.collectAsStateWithLifecycle()
    val stage by synthViewModel.stage.collectAsStateWithLifecycle()
    val error by synthViewModel.errorMsg.collectAsStateWithLifecycle()
    val resultFile by synthViewModel.resultFile.collectAsStateWithLifecycle()
    val text by synthViewModel.text.collectAsStateWithLifecycle()
    val voiceDescription by synthViewModel.voiceDesc.collectAsStateWithLifecycle()
    var selectedVoiceId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(voices) {
        if (voices.none { it.id == selectedVoiceId }) {
            selectedVoiceId = voices.firstOrNull()?.id
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(
            title = "嘻嘻配音",
            navigationIcon = {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "嘻嘻配音",
                    modifier = Modifier.padding(start = 16.dp).size(30.dp).clip(CircleShape),
                )
            },
            actions = {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "设置")
                }
            },
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("让声音成为作品", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "克隆熟悉的声音，或设计一种全新的表达",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                ModeSelector(
                    mode = mode,
                    onSelected = { synthViewModel.mode.value = it },
                )
            }

            if (mode == SynthMode.CLONE) {
                item {
                    if (voices.isEmpty()) {
                        GlassSurface(Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("还没有可用音色", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "先录制或导入一段 WAV / MP3 样本",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                TextButton(onClick = onVoices) { Text("去添加音色") }
                            }
                        }
                    } else {
                        VoiceSelector(
                            voices = voices,
                            selectedId = selectedVoiceId,
                            onSelected = { selectedVoiceId = it },
                        )
                    }
                }
            } else {
                item {
                    GlassSurface(Modifier.fillMaxWidth()) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.AutoAwesome, null, tint = BrandViolet)
                                Spacer(Modifier.width(8.dp))
                                Text("设计你的音色", fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedTextField(
                                value = voiceDescription,
                                onValueChange = { synthViewModel.voiceDesc.value = it },
                                placeholder = { Text("例如：青年女性，声线清亮，语速自然，温柔但有力量") },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth(),
                                colors = transparentTextFieldColors(),
                            )
                        }
                    }
                }
            }

            item {
                GlassSurface(Modifier.fillMaxWidth()) {
                    Column {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("配音文本", fontWeight = FontWeight.SemiBold)
                            Text(
                                "${text.length} 字",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedTextField(
                            value = text,
                            onValueChange = { synthViewModel.text.value = it },
                            placeholder = { Text("输入想让它说的话…") },
                            minLines = 5,
                            maxLines = 10,
                            modifier = Modifier.fillMaxWidth(),
                            colors = transparentTextFieldColors(),
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = { synthViewModel.synthesize(selectedVoiceId) },
                    enabled = state != SynthState.LOADING,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .background(
                            Brush.horizontalGradient(listOf(BrandBlue, BrandViolet)),
                            RoundedCornerShape(18.dp),
                        ),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    if (state == SynthState.LOADING) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("正在生成")
                    } else {
                        Icon(Icons.Outlined.GraphicEq, null)
                        Spacer(Modifier.width(8.dp))
                        Text("开始生成配音")
                    }
                }
            }

            if (state == SynthState.LOADING) {
                item {
                    GlassSurface(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stageLabel(stage), fontWeight = FontWeight.Medium)
                                Text(
                                    "生成时间会受文本长度和网络状况影响",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = synthViewModel::cancelSynthesis) {
                                Icon(Icons.Default.Close, contentDescription = "取消生成")
                            }
                        }
                    }
                }
            }

            if (error.isNotBlank() && state != SynthState.LOADING) {
                item {
                    GlassSurface(Modifier.fillMaxWidth()) {
                        Text(
                            error,
                            color = if (state == SynthState.CANCELLED) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            resultFile?.let { file ->
                item { ResultCard(file, text) }
            }
        }
    }
}

@Composable
private fun ModeSelector(mode: SynthMode, onSelected: (SynthMode) -> Unit) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        contentPadding = PaddingValues(4.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            ModeTab(
                title = "音色克隆",
                icon = { Icon(Icons.Default.Mic, null, Modifier.size(18.dp)) },
                selected = mode == SynthMode.CLONE,
                onClick = { onSelected(SynthMode.CLONE) },
                modifier = Modifier.weight(1f),
            )
            ModeTab(
                title = "音色设计",
                icon = { Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(18.dp)) },
                selected = mode == SynthMode.DESIGN,
                onClick = { onSelected(SynthMode.DESIGN) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ModeTab(
    title: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) BrandBlue.copy(alpha = 0.24f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(7.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceSelector(
    voices: List<Voice>,
    selectedId: String?,
    onSelected: (String) -> Unit,
) {
    var sheetVisible by remember { mutableStateOf(false) }
    val selected = voices.firstOrNull { it.id == selectedId }

    GlassSurface(
        modifier = Modifier.fillMaxWidth().clickable { sheetVisible = true },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VoiceAvatar(
                seed = selected?.id ?: "empty",
                label = selected?.name ?: "声",
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("当前音色", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(selected?.name ?: "选择一个音色", fontWeight = FontWeight.SemiBold)
                selected?.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "选择音色")
        }
    }

    if (sheetVisible) {
        ModalBottomSheet(onDismissRequest = { sheetVisible = false }) {
            Text(
                "选择音色",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            voices.forEach { voice ->
                val isSelected = voice.id == selectedId
                ListItem(
                    headlineContent = { Text(voice.name, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                    supportingContent = {
                        Text(
                            voice.description.ifBlank { "${voice.sampleFileName.substringAfterLast('.').uppercase()} 音频样本" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = {
                        VoiceAvatar(voice.id, voice.name, Modifier.size(42.dp))
                    },
                    trailingContent = {
                        if (isSelected) Icon(Icons.Default.CheckCircle, null, tint = BrandBlue)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable {
                        onSelected(voice.id)
                        sheetVisible = false
                    },
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun ResultCard(file: File, text: String) {
    val context = LocalContext.current
    val source = file.absolutePath
    var playing by remember(source) { mutableStateOf(false) }
    var playbackError by remember(source) { mutableStateOf("") }
    DisposableEffect(source) { onDispose { PlayerManager.release(source) } }

    GlassSurface(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("生成完成", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text(
                        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified())),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    file.extension.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            StaticWaveform(file.name, Modifier.fillMaxWidth().height(44.dp))
            if (playbackError.isNotBlank()) {
                Text(playbackError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        if (playing) {
                            PlayerManager.stop(source)
                            playing = false
                        } else {
                            PlayerManager.play(
                                source,
                                onStarted = { playing = true },
                                onStopped = { playing = false },
                                onError = { playbackError = it; playing = false },
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (playing) "停止播放" else "播放结果")
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file,
                    )
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = if (file.extension.equals("mp3", true)) "audio/mpeg" else "audio/wav"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            },
                            "分享音频",
                        ),
                    )
                }) {
                    Icon(Icons.Default.Share, contentDescription = "分享")
                }
            }
        }
    }
}

@Composable
private fun transparentTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
)

private fun stageLabel(stage: SynthStage): String = when (stage) {
    SynthStage.PREPARING -> "正在检查配置和音频"
    SynthStage.REQUESTING -> "MiMo 正在生成语音"
    SynthStage.SAVING -> "正在保存生成结果"
    else -> "正在准备"
}
