package com.aibox.launcher.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.ui.glassSurface
import com.aibox.launcher.core.ui.theme.LocalLauncherTokens

/**
 * Единая оболочка всех виджетов.
 *
 * Визуальный стиль Liquid Glass взят из tvOS-Launcher (Apache License 2.0):
 * тёплая тёмная подложка, верхний рассеянный блик и диагональная светящаяся кромка.
 * Поведение, размеры и данные виджетов не меняются.
 */
@Composable
fun WidgetSurface(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(if (compact) 22.dp else 28.dp)
    Box(
        modifier = modifier
            .background(Color(0xC4111218), shape)
            .glassSurface(shape, strength = 1.15f)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0x1FFFFFFF),
                        Color(0x04FFFFFF),
                        Color(0x00000000)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color(0x5CFFFFFF),
                        Color(0x1AFFFFFF),
                        Color(0x08FFFFFF),
                        Color(0x10FFFFFF),
                        Color(0x3BFFFFFF)
                    )
                ),
                shape = shape
            )
            .padding(if (compact) 12.dp else 16.dp)
    ) {
        content()
    }
}

@Composable
fun WidgetTitle(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = color,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun WidgetCaption(text: String, modifier: Modifier = Modifier) {
    val tokens = LocalLauncherTokens.current
    Text(
        text = text,
        color = tokens.textSecondary,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

@Composable
fun WidgetStatusPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
