package com.linky.voiceclone.ui.voices

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linky.voiceclone.data.Voice
import com.linky.voiceclone.ui.AppTopBar
import com.linky.voiceclone.ui.components.GlassSurface
import com.linky.voiceclone.ui.components.VoiceAvatar
import com.linky.voiceclone.ui.components.appTextFieldColors
import com.linky.voiceclone.util.PlayerManager
import com.linky.voiceclone.util.WavRecorder
import com.linky.voiceclone.viewmodel.VoiceImportState
import com.linky.voiceclone.viewmodel.VoiceViewModel
import java.io.File
import java.util.Locale

@Composable
fun VoicesScreen() {
    val viewModel: VoiceViewModel = hiltViewModel()
    val voices by viewModel.voices.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var recordedFile by remember { mutableStateOf<File?>(null) }
    var voiceName by remember { mutableStateOf("") }
    var voiceDescription by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var isFinalizing by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var amplitude by remember { mutableFloatStateOf(0f) }
    var screenError by remember { mutableStateOf("") }
    val recorder = remember { WavRecorder() }

    fun resetForm() {
        selectedUri = null
        recordedFile?.delete()
        recordedFile = null
        voiceName = ""
        voiceDescription = ""
        viewModel.clearImportState()
    }

    fun startRecording() {
        val file = File(context.cacheDir, "record_${System.currentTimeMillis()}.wav")
        runCatching { recorder.start(file) }
            .onSuccess {
                recordedFile?.delete()
                recordedFile = file
                screenError = ""
                isRecording = true
            }
            .onFailure {
                file.delete()
                screenError = it.message ?: "无法开始录音"
            }
    }

    fun finishRecording() {
        if (!isRecording || isFinalizing) return
        isRecording = false
        isFinalizing = true
        recorder.stop { error ->
            isFinalizing = false
            if (error != null) {
                recordedFile?.delete()
                recordedFile = null
                screenError = error
            } else if (recordedFile?.isFile == true) {
                showAddDialog = true
            } else {
                screenError = "录音文件生成失败"
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedUri = uri
            recordedFile?.delete()
            recordedFile = null
            screenError = ""
            showAddDialog = true
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording()
        else screenError = "需要麦克风权限才能录制音色"
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                kotlinx.coroutines.delay(50)
                amplitude = recorder.amplitude()
            }
        }
        amplitude = 0f
    }
    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsedSeconds = 0
            while (isRecording) {
                kotlinx.coroutines.delay(1_000)
                if (isRecording) elapsedSeconds++
            }
        }
    }
    LaunchedEffect(importState) {
        if (importState is VoiceImportState.Success) {
            showAddDialog = false
            resetForm()
        }
    }
    DisposableEffect(recorder) {
        onDispose {
            recorder.stop { recordedFile?.delete() }
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "我的音色")
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = 116.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("创建音色", style = MaterialTheme.typography.titleMedium)
            }
            item {
                GlassSurface(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    if (isRecording) finishRecording()
                                    else if (ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO,
                                        ) == PackageManager.PERMISSION_GRANTED
                                    ) startRecording()
                                    else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                },
                                enabled = !isFinalizing,
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) {
                                Icon(if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord, null)
                                Spacer(Modifier.width(7.dp))
                                Text(if (isFinalizing) "保存中" else if (isRecording) "停止" else "录制声音")
                            }
                            OutlinedButton(
                                onClick = { filePicker.launch("audio/*") },
                                enabled = !isRecording && !isFinalizing,
                                modifier = Modifier.weight(1f).height(48.dp),
                            ) {
                                Icon(Icons.Default.FolderOpen, null)
                                Spacer(Modifier.width(7.dp))
                                Text("导入音频")
                            }
                        }

                        if (isRecording || isFinalizing) {
                            RecordingPanel(amplitude, elapsedSeconds, isFinalizing)
                        } else {
                            Text(
                                "推荐 10–30 秒干净人声；MiMo 克隆仅支持 WAV / MP3",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (screenError.isNotBlank()) {
                            Text(screenError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("已保存音色", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${voices.size} 个",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (voices.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.MicNone,
                                null,
                                modifier = Modifier.size(42.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("还没有音色", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(voices, key = { it.id }) { voice ->
                    VoiceCard(
                        voice = voice,
                        sampleDir = viewModel.getSampleDir(),
                        onEdit = { name, description ->
                            viewModel.updateVoice(voice.copy(name = name, description = description))
                        },
                        onDelete = { viewModel.deleteVoice(voice) },
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        val importing = importState is VoiceImportState.Importing
        val importError = (importState as? VoiceImportState.Error)?.message
        AlertDialog(
            onDismissRequest = {
                if (!importing) {
                    showAddDialog = false
                    resetForm()
                }
            },
            title = { Text("保存音色") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = voiceName,
                        onValueChange = { voiceName = it },
                        label = { Text("音色名称") },
                        placeholder = { Text("例如：我的声音") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = voiceDescription,
                        onValueChange = { voiceDescription = it },
                        label = { Text("描述（可选）") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        "保存时会校验真实格式和 MiMo 10MB Base64 限制",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!importError.isNullOrBlank()) {
                        Text(importError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = voiceName.trim().ifBlank { "未命名" }
                        when {
                            recordedFile != null -> viewModel.importRecordedVoice(name, voiceDescription, recordedFile!!)
                            selectedUri != null -> viewModel.importVoice(name, voiceDescription, selectedUri!!)
                        }
                    },
                    enabled = !importing && (recordedFile != null || selectedUri != null),
                ) {
                    if (importing) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(if (importing) "校验中" else "保存")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddDialog = false; resetForm() },
                    enabled = !importing,
                ) { Text("取消") }
            },
        )
    }
}

@Composable
private fun RecordingPanel(amplitude: Float, elapsedSeconds: Int, finalizing: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RecordingIndicator(amplitude)
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                if (finalizing) "正在生成 WAV 文件" else String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    elapsedSeconds / 60,
                    elapsedSeconds % 60,
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                when {
                    finalizing -> "马上就好"
                    elapsedSeconds < 10 -> "建议至少录制 10 秒"
                    elapsedSeconds <= 30 -> "当前时长适合克隆"
                    else -> "样本已经足够，可以停止"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RecordingIndicator(amplitude: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "recordingPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulse",
    )
    val scale by animateFloatAsState(pulse * (1f + amplitude * 0.45f), tween(50), label = "amplitude")
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(38.dp)
                .scale(scale)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.18f), CircleShape),
        )
        Box(Modifier.size(16.dp).background(MaterialTheme.colorScheme.error, CircleShape))
    }
}

@Composable
private fun VoiceCard(
    voice: Voice,
    sampleDir: File,
    onEdit: (String, String) -> Unit,
    onDelete: () -> Unit,
) {
    val sample = remember(voice.sampleFileName, sampleDir) { File(sampleDir, voice.sampleFileName) }
    val source = sample.absolutePath
    var playing by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var editName by remember(voice.name) { mutableStateOf(voice.name) }
    var editDescription by remember(voice.description) { mutableStateOf(voice.description) }
    DisposableEffect(source) { onDispose { PlayerManager.release(source) } }

    GlassSurface(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            VoiceAvatar(voice.id, voice.name, Modifier.size(48.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(voice.name, fontWeight = FontWeight.SemiBold)
                Text(
                    buildString {
                        if (voice.description.isNotBlank()) append(voice.description).append(" · ")
                        append(voice.sampleFileName.substringAfterLast('.').uppercase())
                        append(" · ${voice.sampleSize / 1024}KB")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (playbackError.isNotBlank()) {
                    Text(playbackError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = {
                if (!sample.isFile) {
                    playbackError = "样本文件不存在"
                } else if (PlayerManager.isPlaying(source)) {
                    PlayerManager.stop(source)
                    playing = false
                } else {
                    playbackError = ""
                    PlayerManager.play(
                        source,
                        onStarted = { playing = true },
                        onStopped = { playing = false },
                        onError = { playbackError = it; playing = false },
                    )
                }
            }) {
                Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "试听")
            }
            IconButton(onClick = { editing = true }) {
                Icon(Icons.Default.Edit, contentDescription = "编辑")
            }
            IconButton(onClick = { deleting = true }) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("编辑音色") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("名称") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("描述") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = appTextFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onEdit(editName.trim().ifBlank { voice.name }, editDescription.trim())
                    editing = false
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("取消") } },
        )
    }
    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text("删除音色") },
            text = { Text("确定删除「${voice.name}」及其音频样本吗？") },
            confirmButton = {
                TextButton(onClick = { onDelete(); deleting = false }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text("取消") } },
        )
    }
}
