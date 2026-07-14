package com.linky.voiceclone.ui.history

import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linky.voiceclone.data.GenerationHistory
import com.linky.voiceclone.ui.AppTopBar
import com.linky.voiceclone.ui.components.GlassSurface
import com.linky.voiceclone.ui.components.StaticWaveform
import com.linky.voiceclone.ui.components.VoiceAvatar
import com.linky.voiceclone.util.PlayerManager
import com.linky.voiceclone.viewmodel.SynthViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen() {
    val viewModel: SynthViewModel = hiltViewModel()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val grouped = remember(history) { history.groupBy { dayLabel(it.createdAt) } }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "生成记录")
        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FolderOpen,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("还没有生成记录", fontWeight = FontWeight.Medium)
                    Text(
                        "完成一次配音后会自动保存在这里",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                grouped.forEach { (day, entries) ->
                    item(key = "day_$day") {
                        Text(
                            day,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                        )
                    }
                    entries.forEach { entry ->
                        item(key = entry.id) {
                            HistoryCard(
                                entry = entry,
                                file = viewModel.historyFile(entry),
                                onDelete = { viewModel.deleteHistory(entry) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(
    entry: GenerationHistory,
    file: File,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val source = file.absolutePath
    var playing by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    DisposableEffect(source) { onDispose { PlayerManager.release(source) } }

    GlassSurface(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VoiceAvatar(
                    seed = entry.voiceId ?: entry.voiceName,
                    label = entry.voiceName,
                    modifier = Modifier.size(42.dp),
                )
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry.voiceName, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${if (entry.mode == "clone") "音色克隆" else "音色设计"} · " +
                            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    entry.format.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Text(
                entry.text,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            StaticWaveform(
                seed = entry.id,
                modifier = Modifier.fillMaxWidth().height(36.dp),
            )
            if (playbackError.isNotBlank()) {
                Text(playbackError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalButton(
                    onClick = {
                        if (!file.isFile) {
                            playbackError = "音频文件已丢失"
                        } else if (playing) {
                            PlayerManager.stop(source)
                            playing = false
                        } else {
                            playbackError = ""
                            PlayerManager.play(
                                source = source,
                                onStarted = { playing = true },
                                onStopped = { playing = false },
                                onError = { playbackError = it; playing = false },
                            )
                        }
                    },
                ) {
                    Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                    Spacer(Modifier.size(6.dp))
                    Text(if (playing) "停止" else "播放")
                }
                Row {
                    IconButton(onClick = {
                        if (!file.isFile) {
                            playbackError = "音频文件已丢失"
                            return@IconButton
                        }
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file,
                        )
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = if (entry.format == "mp3") "audio/mpeg" else "audio/wav"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                },
                                "分享音频",
                            ),
                        )
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "分享")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除记录") },
            text = { Text("确定删除这条配音记录和音频文件吗？") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            },
        )
    }
}

private fun dayLabel(timestamp: Long): String {
    val target = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        sameDay(target, today) -> "今天"
        sameDay(target, yesterday) -> "昨天"
        else -> SimpleDateFormat("M月d日", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun sameDay(first: Calendar, second: Calendar): Boolean =
    first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
        first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
