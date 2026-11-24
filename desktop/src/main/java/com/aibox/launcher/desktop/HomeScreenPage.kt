package com.aibox.launcher.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.model.DesktopWidget
import com.aibox.launcher.widgets.BatteryWidget
import com.aibox.launcher.widgets.ConnectionWidget
import com.aibox.launcher.widgets.DateWidget
import com.aibox.launcher.widgets.MusicWidget
import com.aibox.launcher.widgets.WeatherWidget
import com.aibox.launcher.widgets.WeatherWidgetInfo

/**
 * Рабочий стол (первый и второй устроены одинаково): ряд виджетов, выбранных в настройках.
 * Страница занимает весь экран; [topPadding] и [bottomPadding] оставляют место под шапку и нижние панели,
 * поэтому ничего не обрезается жёсткой границей.
 */
@Composable
fun HomeScreenPage(
    widgets: List<DesktopWidget>,
    weather: WeatherWidgetInfo,
    onOpenMusic: () -> Unit,
    topPadding: Dp,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    val area = modifier
        .fillMaxSize()
        .padding(start = 28.dp, end = 28.dp, top = topPadding, bottom = bottomPadding)

    if (widgets.isEmpty()) {
        Box(modifier = area, contentAlignment = Alignment.Center) {
            Text(
                text = "Рабочий стол пуст.\nДобавить виджеты: Настройки → Главная.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        Row(modifier = area, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            widgets.forEach { widget ->
                val cell = Modifier.weight(1f).fillMaxHeight()
                when (widget) {
                    DesktopWidget.DATE -> DateWidget(modifier = cell)
                    DesktopWidget.POWER -> BatteryWidget(modifier = cell)
                    DesktopWidget.NETWORK -> ConnectionWidget(modifier = cell)
                    DesktopWidget.WEATHER -> WeatherWidget(info = weather, modifier = cell)
                    DesktopWidget.MUSIC -> MusicWidget(onOpenPlayer = onOpenMusic, modifier = cell)
                }
            }
        }
    }
}
