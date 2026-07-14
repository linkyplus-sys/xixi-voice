package com.linky.voiceclone.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A5F),
    background = Color(0xFF0F1117),
    surface = Color(0xFF1A1D27),
    surfaceVariant = Color(0xFF1E293B),   // 蓝灰
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color.White.copy(alpha = 0.15f),       // 输入框边框白色微透
    outlineVariant = Color.White.copy(alpha = 0.1f),  // 卡片边框白色
    error = Color(0xFFEF4444),
)

@Composable
fun VoiceCloneTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
