package com.aibox.launcher.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.model.DesktopWidget
import com.aibox.launcher.core.model.InstalledApp
import com.aibox.launcher.core.ui.ScaledContent
import com.aibox.launcher.icons.AppIconView
import com.aibox.launcher.widgets.AppWidgetController
import com.aibox.launcher.widgets.BatteryWidget
import com.aibox.launcher.widgets.ConnectionWidget
import com.aibox.launcher.widgets.DateWidget
import com.aibox.launcher.widgets.GlassCard
import com.aibox.launcher.widgets.HostedAppWidget
import com.aibox.launcher.widgets.MusicWidget
import com.aibox.launcher.widgets.WeatherWidget
import com.aibox.launcher.widgets.WeatherWidgetInfo

/** Префикс записи о виджете другого приложения: «aw:123». */
const val APP_WIDGET_PREFIX = "aw:"

/**
 * Рабочий стол (первый и второй устроены одинаково): ряд виджетов, выбранных в настройках —
 * встроенные и виджеты других приложений. Если виджетов нет, стол остаётся чистым, без надписей.
 * Страница занимает весь экран; [topPadding] и [bottomPadding] оставляют место под шапку и нижние панели.
 */
@Composable
fun HomeScreenPage(
    widgets: List<String>,
    weather: WeatherWidgetInfo,
    apps: List<InstalledApp>,
    onLaunchApp: (InstalledApp) -> Unit,
    onOpenMusic: () -> Unit,
    widgetController: AppWidgetController,
    iconScale: Float,
    widgetScale: Float,
    topPadding: Dp,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    val area = modifier
        .fillMaxSize()
        .padding(start = 28.dp, end = 28.dp, top = topPadding, bottom = bottomPadding)

    Box(modifier = area, contentAlignment = Alignment.TopStart) {
        if (widgets.isNotEmpty()) {
            ScaledContent(widgetScale) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    widgets.forEach { token ->
                        val builtin = DesktopWidget.entries.firstOrNull { it.id == token }
                        val appWidgetId = if (token.startsWith(APP_WIDGET_PREFIX)) {
                            token.removePrefix(APP_WIDGET_PREFIX).toIntOrNull()
                        } else {
                            null
                        }
                        when {
                            builtin == DesktopWidget.APPS -> AppsWidget(
                                apps = apps,
                                onLaunchApp = onLaunchApp,
                                iconScale = iconScale,
                                modifier = Modifier.weight(2f).fillMaxHeight()
                            )
                            builtin != null -> {
                                val cell = Modifier.weight(1f).fillMaxHeight()
                                when (builtin) {
                                    DesktopWidget.DATE -> DateWidget(modifier = cell)
                                    DesktopWidget.POWER -> BatteryWidget(modifier = cell)
                                    DesktopWidget.NETWORK -> ConnectionWidget(modifier = cell)
                                    DesktopWidget.WEATHER -> WeatherWidget(info = weather, modifier = cell)
                                    DesktopWidget.MUSIC -> MusicWidget(onOpenPlayer = onOpenMusic, modifier = cell)
                                    DesktopWidget.APPS -> Unit
                                }
                            }
                            appWidgetId != null -> HostedAppWidget(
                                controller = widgetController,
                                appWidgetId = appWidgetId,
                                modifier = Modifier.weight(1.5f).fillMaxHeight()
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Виджет «Все приложения»: прокручиваемая сетка иконок прямо на рабочем столе. */
@Composable
fun AppsWidget(
    apps: List<InstalledApp>,
    onLaunchApp: (InstalledApp) -> Unit,
    iconScale: Float,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 64.dp * iconScale),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(apps, key = { it.packageName }) { app ->
                AppIconView(
                    app = app,
                    onClick = { onLaunchApp(app) },
                    iconSize = 40.dp * iconScale,
                    labelSize = 10.sp
                )
            }
        }
    }
}
