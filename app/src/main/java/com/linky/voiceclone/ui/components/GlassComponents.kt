package com.linky.voiceclone.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicTextField
import kotlinx.coroutines.delay
import com.linky.voiceclone.ui.theme.BrandBlue
import com.linky.voiceclone.ui.theme.GlassBase

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = GlassBase,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun VoiceAvatar(
    seed: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val avatarColor = remember(seed) {
        val colors = listOf(
            Color(0xFF5E76A8),
            Color(0xFF477988),
            Color(0xFF776A8D),
            Color(0xFF58796D),
        )
        colors[(seed.hashCode() and Int.MAX_VALUE) % colors.size]
    }
    Box(
        modifier = modifier
            .background(avatarColor, CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label.trim().take(1).ifBlank { "声" },
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun StaticWaveform(
    seed: String,
    modifier: Modifier = Modifier,
    color: Color = BrandBlue,
    bars: Int = 28,
) {
    val heights = remember(seed, bars) {
        var value = seed.hashCode().toLong().let { if (it == 0L) 1L else it }
        List(bars) {
            value = (value * 1_103_515_245L + 12_345L) and 0x7FFFFFFF
            0.22f + ((value % 100L) / 100f) * 0.78f
        }
    }
    Canvas(modifier) {
        drawWaveform(heights, color)
    }
}

private fun DrawScope.drawWaveform(heights: List<Float>, color: Color) {
    if (heights.isEmpty()) return
    val gap = 3.dp.toPx()
    val barWidth = ((size.width - gap * (heights.size - 1)) / heights.size).coerceAtLeast(1.dp.toPx())
    heights.forEachIndexed { index, heightFactor ->
        val barHeight = size.height * heightFactor
        val top = (size.height - barHeight) / 2f
        drawRoundRect(
            color = color.copy(alpha = 0.82f),
            topLeft = Offset(index * (barWidth + gap), top),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barWidth / 2f),
        )
    }
}

@Composable
fun appTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = Color(0xFF0E151F),
    unfocusedContainerColor = Color(0xFF0E151F),
    disabledContainerColor = Color(0xFF111821),
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    minLines: Int = 5,
    maxLines: Int = 10,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val shape = RoundedCornerShape(18.dp)
    val borderColor = if (focused) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    LaunchedEffect(focused, imeVisible, value.length) {
        if (focused && imeVisible) {
            delay(140)
            bringIntoViewRequester.bringIntoView()
        }
    }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .background(Color(0xFF0E151F), shape)
            .border(if (focused) 2.dp else 1.dp, borderColor, shape)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
        ),
        minLines = minLines,
        maxLines = maxLines,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                innerTextField()
            }
        },
    )
}

@Composable
fun AudioSummary(
    title: String,
    subtitle: String,
    seed: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(42.dp)
                .background(BrandBlue.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.GraphicEq, null, tint = BrandBlue)
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StaticWaveform(seed, Modifier.size(width = 72.dp, height = 28.dp))
    }
}
