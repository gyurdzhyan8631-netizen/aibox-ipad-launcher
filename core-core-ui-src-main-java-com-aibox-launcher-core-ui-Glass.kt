package com.aibox.launcher.core.ui

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/**
 * Стеклянная поверхность в духе Apple Liquid Glass / iPadOS:
 * прозрачная подложка, размытие, преломление кромки, блик и мягкая глубина.
 */
fun Modifier.glassSurface(
    shape: Shape,
    tint: Color = Color.White,
    strength: Float = 1f
): Modifier = this
    // Глубина: мягкая тень под панелью, рисуется до clip(), поэтому видна снаружи.
    .drawBehind {
        val shadowPaint = Paint().apply {
            color = Color.Black.copy(alpha = (0.32f * strength).coerceIn(0f, 1f))
            asFrameworkPaint().maskFilter = BlurMaskFilter(18f, BlurMaskFilter.Blur.NORMAL)
        }
        val radius = minOf(size.width, size.height) / 4f
        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.translate(0f, 8.dp.toPx())
            canvas.drawRoundRect(0f, 0f, size.width, size.height, radius, radius, shadowPaint)
            canvas.restore()
        }
    }
    .clip(shape)
    // Размытие подложки: рассеянный внутренний ореол вместо резкой заливки.
    .drawBehind {
        val paint = Paint().apply {
            color = tint.copy(alpha = (0.07f * strength).coerceIn(0f, 1f))
            asFrameworkPaint().maskFilter = BlurMaskFilter(18f, BlurMaskFilter.Blur.NORMAL)
        }
        val radius = minOf(size.width, size.height) / 4f
        drawIntoCanvas { canvas ->
            canvas.drawRoundRect(0f, 0f, size.width, size.height, radius, radius, paint)
        }
    }
    // Прозрачность: полупрозрачный градиент, остаётся читаемым на фотообоях.
    .background(
        Brush.verticalGradient(
            listOf(
                tint.copy(alpha = (0.11f * strength).coerceIn(0f, 1f)),
                tint.copy(alpha = (0.05f * strength).coerceIn(0f, 1f)),
                tint.copy(alpha = (0.09f * strength).coerceIn(0f, 1f))
            )
        )
    )
    // Преломление и specular highlight — поверх подложки, но под контентом.
    .drawBehind {
        val edge = 1.dp.toPx()
        drawLine(
            color = Color(0xFF64D2FF).copy(alpha = (0.16f * strength).coerceIn(0f, 1f)),
            start = Offset(edge, size.height - edge),
            end = Offset(size.width - edge, size.height - edge),
            strokeWidth = edge
        )
        drawLine(
            color = Color(0xFFFF7AB5).copy(alpha = (0.12f * strength).coerceIn(0f, 1f)),
            start = Offset(size.width - edge, edge),
            end = Offset(size.width - edge, size.height - edge),
            strokeWidth = edge
        )
        drawLine(
            brush = Brush.linearGradient(
                listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = (0.42f * strength).coerceIn(0f, 1f)),
                    Color.Transparent
                )
            ),
            start = Offset(0f, size.height * 0.18f),
            end = Offset(size.width * 0.86f, 0f),
            strokeWidth = 1.5.dp.toPx()
        )
    }
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = (0.38f * strength).coerceIn(0f, 1f)),
                Color.White.copy(alpha = (0.08f * strength).coerceIn(0f, 1f)),
                Color.White.copy(alpha = (0.24f * strength).coerceIn(0f, 1f))
            )
        ),
        shape = shape
    )

/** Плавная реакция стеклянной поверхности на нажатие (Fluid Animation). */
@Composable
fun Modifier.fluidGlassSurface(
    shape: Shape,
    interactionSource: MutableInteractionSource,
    tint: Color = Color.White,
    strength: Float = 1f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glass_press"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            shadowElevation = (if (pressed) 4.dp else 10.dp).toPx()
        }
        .glassSurface(shape, tint, strength)
}

/** Масштабирует всё содержимое (размеры в dp и текст в sp) в [scale] раз. */
@Composable
fun ScaledContent(scale: Float, content: @Composable () -> Unit) {
    val base = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(base.density * scale, base.fontScale * scale)
    ) {
        content()
    }
}
