package com.linky.voiceclone.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.linky.voiceclone.ui.history.HistoryScreen
import com.linky.voiceclone.ui.home.HomeScreen
import com.linky.voiceclone.ui.settings.SettingsScreen
import com.linky.voiceclone.ui.voices.VoicesScreen
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Screen("home", "合成", Icons.Default.Home)
    data object Voices : Screen("voices", "音色", Icons.Default.Mic)
    data object History : Screen("history", "记录", Icons.Default.History)
    data object Settings : Screen("settings", "设置", Icons.Default.Home)
}

val bottomItems = listOf(Screen.Home, Screen.Voices, Screen.History)

private val ContainerColor = Color.White
private val ContainerHeight = 56.dp
private val IndicatorHeight = 48.dp
private val IconSize = 26.dp
private val ContainerPadding = 4.dp

@Composable
fun Navigation() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val dest = entry?.destination

    var lastNavTime by remember { mutableLongStateOf(0L) }

    fun navigate(route: String) {
        val now = System.currentTimeMillis()
        if (now - lastNavTime < 300L) return
        if (nav.currentDestination?.route == route) return
        lastNavTime = now
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0F1117))) {
        NavHost(
            nav, Screen.Home.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(Screen.Home.route) {
                HomeScreen( onSettings = { nav.navigate(Screen.Settings.route) })
            }
            composable(Screen.Voices.route) { VoicesScreen() }
            composable(Screen.History.route) { HistoryScreen() }
            composable(Screen.Settings.route) { SettingsScreen( onBack = { nav.popBackStack() }) }
        }

        val currentRoute = dest?.route
        if (currentRoute != Screen.Settings.route) {
            val selectedIndex = bottomItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
            GlassBottomBar(
                
                selectedIndex = selectedIndex,
                onTabSelected = { navigate(bottomItems[it].route) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.6f)
                    .padding(bottom = 40.dp)
            )
        }
    }
}

@Composable
private fun GlassBottomBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabsCount = bottomItems.size

    BoxWithConstraints(
        modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        val density = LocalDensity.current
        val tabWidth = with(density) {
            (constraints.maxWidth.toFloat() - 8.dp.toPx()) / tabsCount
        }

        val animationScope = rememberCoroutineScope()
        val indicatorPos = remember { Animatable(selectedIndex.toFloat()) }
        val pressScale = remember { Animatable(1f) }
        var currentIndex by remember { mutableIntStateOf(selectedIndex) }

        // 同步外部 selectedIndex
        LaunchedEffect(selectedIndex) {
            if (selectedIndex != currentIndex) {
                currentIndex = selectedIndex
                indicatorPos.animateTo(selectedIndex.toFloat(), tween(200))
            }
        }

        // ── 层1：容器壳 ──
        Row(
            Modifier
                .background(
                    
                    Color.Black.copy(alpha = 0.8f),
                    RoundedCornerShape(percent = 50)
                )
                .border(
                    2.dp,
                    Color(0xFF2563EB).copy(alpha = 0.5f),
                    RoundedCornerShape(percent = 50)
                )
                .height(ContainerHeight)
                .fillMaxWidth()
                .padding(ContainerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(tabsCount) { Box(Modifier.weight(1f)) }
        }

        // ── 层2：指示器（纯 Compose 样式，无 backdrop）──
        Box(
            Modifier
                .padding(horizontal = ContainerPadding)
                .graphicsLayer {
                    translationX = indicatorPos.value * tabWidth
                    scaleX = pressScale.value
                    scaleY = pressScale.value
                }
                .background(
                    Color(0xFF2563EB).copy(alpha = 0.12f),
                    RoundedCornerShape(percent = 50)
                )
                .border(
                    1.dp,
                    Color(0xFF2563EB).copy(alpha = 0.25f),
                    RoundedCornerShape(percent = 50)
                )
                .height(IndicatorHeight)
                .fillMaxWidth(1f / tabsCount)
        )

        // ── 层3：图标 + pointerInput ──
        Row(
            Modifier
                .height(ContainerHeight)
                .fillMaxWidth()
                .padding(ContainerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(tabsCount) { index ->
                val isSelected = index == currentIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(IndicatorHeight)
                        .pointerInput(animationScope) {
                            awaitEachGesture {
                                awaitFirstDown()
                                animationScope.launch {
                                    pressScale.animateTo(0.92f, spring(1f, 400f))
                                }
                                waitForUpOrCancellation()
                                animationScope.launch {
                                    pressScale.animateTo(1f, spring(1f, 300f))
                                }
                                onTabSelected(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = bottomItems[index].icon,
                        contentDescription = null,
                        tint = if (isSelected) Color(0xFF2563EB) else Color.Gray,
                        modifier = Modifier.size(IconSize)
                    )
                }
            }
        }
    }
}
