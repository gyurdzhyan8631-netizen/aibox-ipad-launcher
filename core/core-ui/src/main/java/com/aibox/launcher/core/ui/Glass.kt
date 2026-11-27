package com.aibox.launcher.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * Матовое стекло без полос: сплошная полупрозрачная подложка и ровная светлая
 * кромка. Ранее здесь были BlurMaskFilter и два градиента с крошечной разницей
 * альфы — на 8-bit экране они давали заметный banding на всех капсулах.
 */
fun Modifier.glassSurface(
    shape: Shape,
    tint: Color = Color.White,
    strength: Float = 1f
): Modifier = this
    .clip(shape)
    .background(
        color = tint.copy(alpha = (0.07f * strength).coerceIn(0f, 1f)),
        shape = shape
    )
    .border(
        width = 1.dp,
        color = tint.copy(alpha = (0.20f * strength).coerceIn(0f, 1f)),
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
