package com.linky.voiceclone.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.linky.voiceclone.ui.theme.BrandBlue
import com.linky.voiceclone.ui.theme.BrandViolet
import com.linky.voiceclone.ui.theme.GlassBase

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.11f), GlassBase, Color(0xE60D1420)),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.24f), Color.White.copy(alpha = 0.06f)),
                ),
                shape = shape,
            )
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun VoiceAvatar(
    seed: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    val palette = remember(seed) {
        val palettes = listOf(
            listOf(Color(0xFF4F7CFF), Color(0xFF8B5CF6)),
            listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)),
            listOf(Color(0xFFF973A6), Color(0xFF8B5CF6)),
            listOf(Color(0xFF10B981), Color(0xFF06B6D4)),
        )
        palettes[(seed.hashCode() and Int.MAX_VALUE) % palettes.size]
    }
    Box(
        modifier = modifier
            .background(Brush.linearGradient(palette), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.26f), CircleShape),
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
            brush = Brush.verticalGradient(listOf(BrandViolet.copy(alpha = 0.75f), color)),
            topLeft = Offset(index * (barWidth + gap), top),
            size = Size(barWidth, barHeight),
            cornerRadius = CornerRadius(barWidth / 2f),
        )
    }
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
