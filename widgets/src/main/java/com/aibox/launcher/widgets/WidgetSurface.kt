package com.aibox.launcher.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aibox.launcher.core.ui.theme.LocalLauncherTokens
import com.aibox.launcher.core.ui.theme.LocalWidgetStyle

/**
 * Оболочка системного AppWidget. Само содержимое рисует RemoteViews провайдера,
 * здесь задаются только радиус, фон, кромка и мягкая тень выбранного стиля.
 */
@Composable
fun HostedWidgetSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val tokens = LocalLauncherTokens.current
    val style = LocalWidgetStyle.current
    val context = LocalContext.current
    val density = LocalDensity.current

    val radius: Dp = when (style) {
        com.aibox.launcher.core.model.WidgetStyle.IPAD -> tokens.cornerRadius
        com.aibox.launcher.core.model.WidgetStyle.PIXEL -> {
            val system = with(density) {
                runCatching {
                    context.resources.getDimensionPixelSize(
                        android.R.dimen.system_app_widget_background_radius
                    )
                }.getOrDefault(0).toDp()
            }
            if (system < 8.dp) 16.dp else system
        }
        com.aibox.launcher.core.model.WidgetStyle.ONE_UI -> 26.dp
    }
    val shape = RoundedCornerShape(radius)

    val styled = when (style) {
        // iPad: карточка с лёгкой кромкой и рассеянной тенью.
        com.aibox.launcher.core.model.WidgetStyle.IPAD -> modifier
            .shadow(4.dp, shape, ambientColor = Color(0x33000000), spotColor = Color(0x33000000))
            .clip(shape)
            .background(tokens.cardBackground, shape)
            .border(1.dp, tokens.panelBorder, shape)
        // Pixel: фон и скругление рисует сам системный виджет (edge-to-edge).
        com.aibox.launcher.core.model.WidgetStyle.PIXEL -> modifier.clip(shape)
        // One UI: сплошная спокойная подложка и мягкая тень, без яркой кромки.
        com.aibox.launcher.core.model.WidgetStyle.ONE_UI -> modifier
            .shadow(6.dp, shape, ambientColor = Color(0x2B000000), spotColor = Color(0x2B000000))
            .clip(shape)
            .background(tokens.cardBackground, shape)
    }

    Box(modifier = styled.fillMaxSize()) {
        content()
    }
}
