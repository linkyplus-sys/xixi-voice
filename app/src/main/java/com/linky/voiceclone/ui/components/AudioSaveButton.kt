package com.linky.voiceclone.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AudioSaveButton(
    file: File,
    format: String = file.extension,
    timestamp: Long = file.lastModified(),
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val mimeType = audioMimeType(format)
    val suggestedName = remember(file.absolutePath, format, timestamp) {
        buildAudioExportName(file, format, timestamp)
    }
    var saving by remember(file.absolutePath) { mutableStateOf(false) }
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(mimeType),
    ) { destination ->
        if (destination == null) return@rememberLauncherForActivityResult
        scope.launch {
            saving = true
            val error = try {
                withContext(Dispatchers.IO) { copyAudioFile(context, file, destination) }
                null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                failure
            }
            saving = false
            Toast.makeText(
                context,
                if (error == null) "音频已保存" else error.message ?: "保存失败，请重试",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    IconButton(
        onClick = {
            if (file.isFile) {
                saveLauncher.launch(suggestedName)
            } else {
                Toast.makeText(context, "音频文件已丢失", Toast.LENGTH_SHORT).show()
            }
        },
        enabled = !saving,
        modifier = modifier,
    ) {
        if (saving) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Icon(Icons.Outlined.SaveAlt, contentDescription = "保存到文件夹")
        }
    }
}

fun audioMimeType(format: String): String = when (format.lowercase(Locale.ROOT).trimStart('.')) {
    "mp3" -> "audio/mpeg"
    "wav" -> "audio/wav"
    "pcm", "pcm16" -> "audio/L16"
    else -> "audio/*"
}

private fun buildAudioExportName(file: File, format: String, timestamp: Long): String {
    val extension = format
        .lowercase(Locale.ROOT)
        .trimStart('.')
        .takeIf { it.matches(Regex("[a-z0-9]+")) }
        ?: file.extension.lowercase(Locale.ROOT).ifBlank { "wav" }
    val exportTime = timestamp.takeIf { it > 0L } ?: System.currentTimeMillis()
    val timeLabel = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(exportTime))
    return "嘻嘻配音_$timeLabel.$extension"
}

private fun copyAudioFile(context: Context, source: File, destination: Uri) {
    check(source.isFile) { "音频文件已丢失" }
    val output = context.contentResolver.openOutputStream(destination, "wt")
        ?: error("无法写入所选位置")
    source.inputStream().buffered().use { input ->
        output.buffered().use { target -> input.copyTo(target) }
    }
}
