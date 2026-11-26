package com.aibox.launcher.core.ui

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * Матовое стекло: мягко рассеянная светлая подложка, полупрозрачная заливка
 * с переходами и тонкая светлая кромка. Не размывает содержимое карточки,
 * поэтому остаётся лёгким для GPU.
 */
fun Modifier.glassSurface(
    shape: Shape,
    tint: Color = Color.White,
    strength: Float = 1f
): Modifier = this
    .clip(shape)
    .drawBehind {
        val alpha = ((0.16f * strength).coerceIn(0f, 1f))
        val paint = androidx.compose.ui.graphics.Paint().apply {
            color = Color.White.copy(alpha = alpha)
            asFrameworkPaint().maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
        }
        val radius = minOf(size.width, size.height) / 4f
        drawIntoCanvas { canvas ->
            canvas.drawRoundRect(0f, 0f, size.width, size.height, radius, radius, paint)
        }
    }
    .background(
        Brush.verticalGradient(
            listOf(
                tint.copy(alpha = (0.22f * strength).coerceIn(0f, 1f)),
                tint.copy(alpha = (0.08f * strength).coerceIn(0f, 1f)),
                tint.copy(alpha = (0.15f * strength).coerceIn(0f, 1f))
            )
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = (0.45f * strength).coerceIn(0f, 1f)),
                Color.White.copy(alpha = (0.05f * strength).coerceIn(0f, 1f)),
                Color.White.copy(alpha = (0.22f * strength).coerceIn(0f, 1f))
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
