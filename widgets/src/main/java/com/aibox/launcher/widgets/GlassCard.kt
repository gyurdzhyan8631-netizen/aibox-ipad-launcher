package com.aibox.launcher.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aibox.launcher.core.ui.theme.LocalLauncherTokens

/** Стеклянная карточка — основа всех виджетов. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val tokens = LocalLauncherTokens.current
    val shape = RoundedCornerShape(tokens.cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(tokens.cardBackground)
            .border(1.dp, tokens.panelBorder, shape)
            .padding(18.dp)
    ) {
        content()
    }
}
