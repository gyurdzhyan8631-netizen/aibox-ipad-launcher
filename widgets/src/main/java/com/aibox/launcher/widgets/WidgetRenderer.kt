package com.aibox.launcher.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Единая точка отрисовки: галерея, рабочий стол и умный стек используют одни и те же виджеты. */
@Composable
fun WidgetView(
    item: WidgetItem,
    weather: WidgetWeatherInfo,
    onOpenWidget: (WidgetType) -> Unit,
    modifier: Modifier = Modifier
) {
    val compact = item.size == WidgetSizeClass.SMALL
    when (item.type) {
        WidgetType.WEATHER -> WeatherCard(
            info = weather,
            compact = compact,
            onClick = { onOpenWidget(WidgetType.WEATHER) },
            modifier = modifier
        )
        WidgetType.MUSIC -> MusicCard(
            compact = compact,
            onClick = { onOpenWidget(WidgetType.MUSIC) },
            modifier = modifier
        )
        WidgetType.CALENDAR -> CalendarCard(
            compact = compact,
            onClick = { onOpenWidget(WidgetType.CALENDAR) },
            modifier = modifier
        )
        WidgetType.CLOCK -> ClockCard(
            compact = compact,
            onClick = { onOpenWidget(WidgetType.CLOCK) },
            modifier = modifier
        )
        WidgetType.BATTERY -> BatteryCard(
            onClick = { onOpenWidget(WidgetType.BATTERY) },
            modifier = modifier
        )
        WidgetType.NOTES -> NotesCard(
            onClick = { onOpenWidget(WidgetType.NOTES) },
            modifier = modifier
        )
        WidgetType.PHOTOS -> PhotosCard(
            onClick = { onOpenWidget(WidgetType.PHOTOS) },
            modifier = modifier
        )
        WidgetType.SMART_STACK -> SmartStackCard(
            size = item.size,
            weather = weather,
            onOpenWidget = onOpenWidget,
            modifier = modifier
        )
    }
}
