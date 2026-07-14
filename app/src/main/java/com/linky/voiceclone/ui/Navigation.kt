package com.linky.voiceclone.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.linky.voiceclone.ui.history.HistoryScreen
import com.linky.voiceclone.ui.home.HomeScreen
import com.linky.voiceclone.ui.settings.SettingsScreen
import com.linky.voiceclone.ui.theme.AppBackground
import com.linky.voiceclone.ui.theme.GlassBase
import com.linky.voiceclone.ui.voices.VoicesScreen

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Screen("home", "合成", Icons.Default.Home)
    data object Voices : Screen("voices", "音色", Icons.Default.Mic)
    data object History : Screen("history", "记录", Icons.Default.History)
    data object Settings : Screen("settings", "设置", Icons.Default.Home)
}

private val bottomItems = listOf(Screen.Home, Screen.Voices, Screen.History)

@Composable
fun Navigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var lastNavigationTime by remember { mutableLongStateOf(0L) }
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val navigationBarVisible = currentRoute != Screen.Settings.route && !imeVisible

    fun navigateTo(route: String) {
        val now = System.currentTimeMillis()
        if (now - lastNavigationTime < 250L || navController.currentDestination?.route == route) return
        lastNavigationTime = now
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppBackground,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { EnterTransition.None },
                exitTransition = { ExitTransition.None },
                popEnterTransition = { EnterTransition.None },
                popExitTransition = { ExitTransition.None },
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onSettings = { navController.navigate(Screen.Settings.route) },
                        onVoices = { navigateTo(Screen.Voices.route) },
                    )
                }
                composable(Screen.Voices.route) { VoicesScreen() }
                composable(Screen.History.route) { HistoryScreen() }
                composable(Screen.Settings.route) {
                    SettingsScreen(onBack = { navController.popBackStack() })
                }
            }

            AnimatedVisibility(
                visible = navigationBarVisible,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 220,
                        delayMillis = 60,
                        easing = FastOutSlowInEasing,
                    ),
                ) + slideInVertically(
                    animationSpec = tween(
                        durationMillis = 260,
                        delayMillis = 40,
                        easing = FastOutSlowInEasing,
                    ),
                    initialOffsetY = { it / 2 },
                ) + scaleIn(
                    animationSpec = tween(
                        durationMillis = 260,
                        delayMillis = 40,
                        easing = FastOutSlowInEasing,
                    ),
                    initialScale = 0.94f,
                    transformOrigin = TransformOrigin(0.5f, 1f),
                ),
                exit = fadeOut(
                    animationSpec = tween(durationMillis = 100),
                ) + slideOutVertically(
                    animationSpec = tween(
                        durationMillis = 140,
                        easing = FastOutLinearInEasing,
                    ),
                    targetOffsetY = { it / 3 },
                ) + scaleOut(
                    animationSpec = tween(durationMillis = 140),
                    targetScale = 0.96f,
                    transformOrigin = TransformOrigin(0.5f, 1f),
                ),
            ) {
                val selectedIndex = bottomItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
                LiquidBottomBar(
                    selectedIndex = selectedIndex,
                    onSelected = { navigateTo(bottomItems[it].route) },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .fillMaxWidth(0.82f)
                        .widthIn(min = 240.dp, max = 330.dp),
                )
            }
        }
    }
}

@Composable
private fun LiquidBottomBar(
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val capsuleShape = RoundedCornerShape(percent = 50)
    Surface(
        modifier = modifier
            .height(64.dp)
            .shadow(
                elevation = 20.dp,
                shape = capsuleShape,
                ambientColor = Color.Black.copy(alpha = 0.42f),
                spotColor = Color.Black.copy(alpha = 0.28f),
            ),
        shape = capsuleShape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(capsuleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.13f),
                            GlassBase.copy(alpha = 0.76f),
                            GlassBase.copy(alpha = 0.90f),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.30f),
                            Color.White.copy(alpha = 0.07f),
                        ),
                    ),
                    shape = capsuleShape,
                ),
        ) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.68f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.34f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            Box(Modifier.fillMaxSize().padding(5.dp)) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val tabWidth = maxWidth / bottomItems.size
                    val indicatorPosition by animateFloatAsState(
                        targetValue = selectedIndex.toFloat(),
                        animationSpec = spring(dampingRatio = 0.78f, stiffness = 360f),
                        label = "liquidIndicator",
                    )

                    Box(
                        Modifier
                            .align(Alignment.CenterStart)
                            .width(tabWidth)
                            .height(44.dp)
                            .graphicsLayer { translationX = indicatorPosition * tabWidth.toPx() }
                            .blur(10.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.24f), capsuleShape),
                    )

                    Box(
                        Modifier
                            .width(tabWidth)
                            .height(54.dp)
                            .graphicsLayer { translationX = indicatorPosition * tabWidth.toPx() }
                            .clip(capsuleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.10f),
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.74f),
                                        Color.White.copy(alpha = 0.06f),
                                    ),
                                ),
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.28f),
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.62f),
                                    ),
                                ),
                                shape = capsuleShape,
                            ),
                    )

                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        bottomItems.forEachIndexed { index, item ->
                            LiquidTab(
                                item = item,
                                selected = index == selectedIndex,
                                onClick = { onSelected(index) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(54.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidTab(
    item: Screen,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 500f),
        label = "tabPress",
    )
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(percent = 50))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(23.dp),
        )
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(5.dp))
                Text(
                    item.label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}
