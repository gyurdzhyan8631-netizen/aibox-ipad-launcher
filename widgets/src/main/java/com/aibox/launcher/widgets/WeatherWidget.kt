package com.aibox.launcher.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.ui.theme.LocalLauncherTokens

/** Готовые тексты для виджета погоды. */
data class WeatherWidgetInfo(
    val temp: String = "",
    val condition: String = "",
    val place: String = "",
    val note: String = ""
)

@Composable
fun WeatherWidget(info: WeatherWidgetInfo, modifier: Modifier = Modifier) {
    val tokens = LocalLauncherTokens.current
    GlassCard(modifier = modifier) {
        Column {
            Text(
                text = "Погода",
                color = tokens.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = info.temp.ifEmpty { "—" },
                color = tokens.textPrimary,
                fontSize = widgetNumberSize(),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = info.condition,
                color = tokens.textPrimary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val line = listOf(info.place, info.note).filter { it.isNotEmpty() }.joinToString(" · ")
            if (line.isNotEmpty()) {
                Text(
                    text = line,
                    color = tokens.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
