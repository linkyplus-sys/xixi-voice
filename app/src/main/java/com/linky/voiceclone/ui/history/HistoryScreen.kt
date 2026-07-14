package com.linky.voiceclone.ui.history

import android.media.MediaPlayer
import com.linky.voiceclone.ui.AppTopBar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.linky.voiceclone.util.PlayerManager
import com.linky.voiceclone.viewmodel.SynthViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val vm: SynthViewModel = hiltViewModel()
    var files by remember { mutableStateOf(vm.getHistory()) }

    LaunchedEffect(Unit) { files = vm.getHistory() }

    Column(Modifier.fillMaxSize()) {
        AppTopBar(title = "生成记录")

        if (files.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("暂无记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(files, key = { it.name }) { file ->
                    HistoryItemCard(file, onDelete = {
                        vm.deleteHistory(file)
                        files = vm.getHistory()
                    })
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(file: File, onDelete: () -> Unit) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val player = remember {
        MediaPlayer().apply {
            try {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener { playing = false }
            } catch (_: Exception) {}
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    val dateStr = remember(file.lastModified()) {
        SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
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
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除记录") },
            text = { Text("确定删除「${file.name}」？此操作不可撤销。") },
            confirmButton = { TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("删除") } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } }
        )
    }
}
