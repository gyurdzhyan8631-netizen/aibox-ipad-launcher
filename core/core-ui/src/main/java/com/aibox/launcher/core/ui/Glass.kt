package com.aibox.launcher.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * Лёгкое «матовое стекло»: полупрозрачная заливка с мягким переходом и тонкая светлая кромка,
 * на которой свет падает с верхнего левого угла. Работает без размытия, поэтому не нагружает GPU.
 */
fun Modifier.glassSurface(
    shape: Shape,
    tint: Color = Color.White,
    strength: Float = 1f
): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(
                tint.copy(alpha = 0.16f * strength),
                tint.copy(alpha = 0.07f * strength)
            )
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.40f * strength),
                Color.White.copy(alpha = 0.06f * strength),
                Color.White.copy(alpha = 0.18f * strength)
            )
        ),
        shape = shape
    )

/** Масштабирует всё содержимое (размеры в dp и текст в sp) в [scale] раз. */
@Composable
fun ScaledContent(scale: Float, content: @Composable () -> Unit) {
    val base = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(base.density * scale, base.fontScale)
    ) {
        content()
    }
}
