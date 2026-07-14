package com.linky.voiceclone.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val AppBackground = Color(0xFF0B0F15)
val AppBackgroundRaised = Color(0xFF101722)
val GlassBase = Color(0xF5151C26)
val GlassHighlight = Color(0x14FFFFFF)
val BrandBlue = Color(0xFF86A5F8)
val BrandViolet = Color(0xFF9B9DBB)
val TextPrimary = Color(0xFFF2F4F8)
val TextSecondary = Color(0xFFB5BFCD)

private val DarkColors = darkColorScheme(
    primary = BrandBlue,
    onPrimary = Color(0xFF0C1830),
    primaryContainer = Color(0xFF263755),
    onPrimaryContainer = Color(0xFFE4EBFF),
    secondary = BrandViolet,
    onSecondary = Color(0xFF171824),
    background = AppBackground,
    onBackground = TextPrimary,
    surface = Color(0xFF141B25),
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF1C2532),
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF526176),
    outlineVariant = Color(0xFF2D3847),
    error = Color(0xFFFF8291),
    errorContainer = Color(0xFF48242C),
    onErrorContainer = Color(0xFFFFDCE0),
)

private val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun VoiceCloneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
