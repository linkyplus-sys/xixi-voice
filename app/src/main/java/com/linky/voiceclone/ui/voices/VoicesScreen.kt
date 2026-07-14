package com.linky.voiceclone.ui.voices

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.linky.voiceclone.ui.AppTopBar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linky.voiceclone.data.Voice
import com.linky.voiceclone.util.PlayerManager
import com.linky.voiceclone.util.WavRecorder
import com.linky.voiceclone.viewmodel.VoiceViewModel
import android.media.MediaPlayer
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicesScreen() {
    val vm: VoiceViewModel = hiltViewModel()
    val voices by vm.voices.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var voiceName by remember { mutableStateOf("") }
    var voiceDesc by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selectedUri = it; showAddDialog = true }
    }

    var isRecording by remember { mutableStateOf(false) }
    val wavRecorder = remember { WavRecorder() }
    var recordedFile by remember { mutableStateOf<File?>(null) }

    // 计时器
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(isRecording) {
        if (isRecording) {
            elapsedSeconds = 0
            while (isRecording) {
                kotlinx.coroutines.delay(1000)
                elapsedSeconds++
            }
        }
    }

    // 振幅（每帧刷新）
    var amplitude by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (isRecording) {
                amplitude = wavRecorder.amplitude()
                kotlinx.coroutines.delay(50) // ~20fps
            }
            amplitude = 0f
        }
    }

    DisposableEffect(Unit) { onDispose { if (wavRecorder.isRecording()) wavRecorder.stop() } }

    val recordPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val f = File(context.cacheDir, "record_${System.currentTimeMillis()}.wav")
            wavRecorder.start(f)
            recordedFile = f; isRecording = true
        }
    }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "音色管理")

        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("➕ 添加新音色", fontWeight = FontWeight.Medium, color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                if (isRecording) {
                                    wavRecorder.stop(); isRecording = false
                                    recordedFile?.let { showAddDialog = true }
                                } else {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        val f = File(context.cacheDir, "record_${System.currentTimeMillis()}.wav")
                                        wavRecorder.start(f)
                                        recordedFile = f; isRecording = true
                                    } else {
                                        recordPermission.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            }, modifier = Modifier.weight(1f)) {
                                Icon(if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord, null)
                                Spacer(Modifier.width(4.dp))
                                Text(if (isRecording) "停止录音" else "录音")
                            }
                            OutlinedButton(onClick = { filePicker.launch("audio/*") }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.FolderOpen, null)
                                Spacer(Modifier.width(4.dp))
                                Text("选择文件")
                            }
                        }

                        // 录音状态：计时 + 振幅动画
                        if (isRecording) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 振幅呼吸灯
                                RecordingIndicator(amplitude)
                                // 计时
                                val mm = elapsedSeconds / 60
                                val ss = elapsedSeconds % 60
                                Text(
                                    String.format(Locale.getDefault(), "%02d:%02d", mm, ss),
                                    fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    if (elapsedSeconds < 10) "建议录 10-30 秒"
                                    else if (elapsedSeconds <= 30) "✓ 时长合适"
                                    else "已超 30 秒，可以停止",
                                    fontSize = 12.sp,
                                    color = if (elapsedSeconds in 10..30) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text("样本质量影响克隆效果：干净人声、10-30秒、WAV 优先",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item {
                Text("已保存的音色", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (voices.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("暂无音色", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(voices, key = { it.id }) { voice ->
                VoiceManageCard(voice, sampleDir = vm.getSampleDir(),
                    onDelete = { vm.deleteVoice(voice) },
                    onEdit = { name, desc -> vm.updateVoice(voice.copy(name = name, description = desc)) }
                )
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false; selectedUri = null; recordedFile = null; voiceName = ""; voiceDesc = "" },
            title = { Text("保存音色") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(voiceName, { voiceName = it }, label = { Text("名称") },
                        placeholder = { Text("例如：我的声音") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(voiceDesc, { voiceDesc = it }, label = { Text("描述（可选）") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = voiceName.trim().ifBlank { "未命名" }
                    val src = when {
                        recordedFile != null -> recordedFile!!
                        selectedUri != null -> {
                            val tmp = File(context.cacheDir, "upload_${System.currentTimeMillis()}")
                            context.contentResolver.openInputStream(selectedUri!!)?.use { tmp.writeBytes(it.readBytes()) }
                            tmp
                        }
                        else -> return@TextButton
                    }
                    vm.addVoice(name, voiceDesc.trim(), src)
                    showAddDialog = false; selectedUri = null; recordedFile = null; voiceName = ""; voiceDesc = ""
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false; selectedUri = null; recordedFile = null; voiceName = ""; voiceDesc = "" }) {
                    Text("取消")
                }
            }
        )
    }
}

/** 录音指示器：红点 + 振幅缩放动画 */
@Composable
private fun RecordingIndicator(amplitude: Float) {
    // 基础呼吸动画
    val infiniteTransition = rememberInfiniteTransition(label = "breath")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse),
        label = "breathScale"
    )

    // 振幅叠加：基础缩放 + 振幅增量
    val targetScale = breathScale * (1f + amplitude * 0.5f)
    val smoothScale by animateFloatAsState(targetScale, tween(50), label = "smooth")

    Box(contentAlignment = Alignment.Center) {
        // 外圈光晕
        Box(Modifier
            .size(32.dp)
            .scale(smoothScale)
            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f), CircleShape)
        )
        // 红点
        Box(Modifier
            .size(16.dp)
            .background(MaterialTheme.colorScheme.error, CircleShape)
        )
    }
}

@Composable
private fun VoiceManageCard(voice: Voice, sampleDir: File, onDelete: () -> Unit, onEdit: (String, String) -> Unit) {
    var confirmDelete by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(voice.name) }
    var editDesc by remember { mutableStateOf(voice.description) }
    val format = voice.sampleFileName.substringAfterLast('.').uppercase()
    var previewPlaying by remember { mutableStateOf(false) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) { onDispose { previewPlayer?.release(); previewPlayer = null } }

    OutlinedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // 试听按钮
            IconButton(onClick = {
                val sampleFile = File(sampleDir, voice.sampleFileName)
                if (!sampleFile.exists()) return@IconButton
                if (previewPlaying) {
                    previewPlayer?.let { PlayerManager.pause(it) }
                    previewPlaying = false
                } else {
                    val p = MediaPlayer().apply {
                        setDataSource(sampleFile.absolutePath)
                        prepare()
                    }
                    previewPlayer = p
                    PlayerManager.play(p) { previewPlaying = false; previewPlayer = null }
                    previewPlaying = true
                }
            }) {
                Icon(
                    if (previewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    "试听"
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(voice.name, fontWeight = FontWeight.Medium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .height(16.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(3.dp)
                            )
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp))
                            .padding(horizontal = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(format,
                            fontSize = 9.sp, fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 9.sp
                        )
                    }
                    val meta = buildString {
                        if (voice.description.isNotBlank()) append(voice.description)
                        if (isNotEmpty()) append(" · ")
                        append("${(voice.sampleSize / 1024)}KB")
                    }
                    Text(meta, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            IconButton(onClick = { editName = voice.name; editDesc = voice.description; showEditDialog = true }) {
                Icon(Icons.Default.Edit, "编辑")
            }
            IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除音色") },
            text = { Text("确定删除「${voice.name}」？") },
            confirmButton = { TextButton(onClick = { onDelete(); confirmDelete = false }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
        )
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("编辑音色") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(editName, { editName = it }, label = { Text("名称") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(editDesc, { editDesc = it }, label = { Text("描述") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { TextButton(onClick = { onEdit(editName.trim(), editDesc.trim()); showEditDialog = false }) { Text("保存") } },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("取消") } }
        )
    }
}
