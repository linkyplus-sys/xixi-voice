package com.linky.voiceclone.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.linky.voiceclone.ui.components.GlassSurface
import com.linky.voiceclone.ui.history.HistoryScreen
import com.linky.voiceclone.ui.home.HomeScreen
import com.linky.voiceclone.ui.settings.SettingsScreen
import com.linky.voiceclone.ui.theme.AppBackground
import com.linky.voiceclone.ui.theme.BrandBlue
import com.linky.voiceclone.ui.theme.BrandViolet
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
    val navigationBarVisible = currentRoute != Screen.Settings.route
    val navigationInset = WindowInsets.navigationBars.asPaddingValues()
        .calculateBottomPadding()

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

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(BrandBlue.copy(alpha = 0.12f), AppBackground),
                    radius = 1_100f,
                ),
            ),
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (navigationBarVisible) 84.dp + navigationInset else 0.dp),
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

        if (navigationBarVisible) {
            val selectedIndex = bottomItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
            LiquidBottomBar(
                selectedIndex = selectedIndex,
                onSelected = { navigateTo(bottomItems[it].route) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .fillMaxWidth(0.82f)
                    .widthIn(min = 240.dp, max = 330.dp),
            )
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
    GlassSurface(
        modifier = modifier
            .height(64.dp)
            .shadow(
                elevation = 20.dp,
                shape = capsuleShape,
                ambientColor = Color.Black.copy(alpha = 0.55f),
                spotColor = BrandBlue.copy(alpha = 0.35f),
            ),
        shape = capsuleShape,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(5.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val tabWidth = maxWidth / bottomItems.size
            val indicatorPosition by animateFloatAsState(
                targetValue = selectedIndex.toFloat(),
                animationSpec = spring(dampingRatio = 0.78f, stiffness = 360f),
                label = "liquidIndicator",
            )

            Box(
                Modifier
                    .width(tabWidth)
                    .height(54.dp)
                    .graphicsLayer { translationX = indicatorPosition * tabWidth.toPx() }
                    .clip(capsuleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(BrandBlue.copy(alpha = 0.30f), BrandViolet.copy(alpha = 0.22f)),
                        ),
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.16f), capsuleShape),
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
            tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
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
                    color = Color.White,
                )
            }
        }
    }
}
