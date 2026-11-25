package com.aibox.launcher.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.ui.glassSurface
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
            .glassSurface(shape)
            .padding(14.dp)
    ) {
        content()
    }
}

/** Размер крупных цифр в виджетах: на низких экранах (AI Box, телефон в ландшафте) меньше. */
@Composable
fun widgetNumberSize(): TextUnit {
    val heightDp = LocalConfiguration.current.screenHeightDp
    return if (heightDp < 480) 36.sp else 52.sp
}
