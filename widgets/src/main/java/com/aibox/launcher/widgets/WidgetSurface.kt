package com.aibox.launcher.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.ui.glassSurface
import com.aibox.launcher.core.ui.theme.LocalLauncherTokens

/** Единая оболочка всех виджетов: тёмное стекло и мягкая кромка, как у системных виджетов iPad. */
@Composable
fun WidgetSurface(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(if (compact) 20.dp else 24.dp)
    Box(
        modifier = modifier
            .background(Color(0xD9101116), shape)
            .glassSurface(shape, strength = 1.05f)
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
