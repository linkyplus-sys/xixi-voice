package com.linky.voiceclone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ========== 全局统一间距 ==========
val TopBarContentSpacing = 12.dp
val CardSpacing = 12.dp
val BottomNavContentPadding = 80.dp

// TopBar 半透明背景（不用 drawBackdrop，避免与页面 layerBackdrop 同实例父子 SIGSEGV）
private val TopBarBgColor = Color(0xFF0A0A0A).copy(alpha = 0.8f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(TopBarBgColor)
    ) {
        CenterAlignedTopAppBar(
            title = { Text(title, fontWeight = FontWeight.Bold) },
            navigationIcon = navigationIcon,
            actions = actions,
            windowInsets = WindowInsets.statusBars,
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = Color.Transparent
            )
        )
    }
}
