package com.aibox.launcher.widgets

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.compose.ui.unit.Dp

/** Опции размера системного AppWidget: провайдер подстраивает RemoteViews под ячейку сетки. */
fun widgetOptions(size: WidgetSizeClass, cellWidth: Dp, cellHeight: Dp): Bundle {
    val width = (cellWidth.value * size.columns).toInt().coerceAtLeast(40)
    val height = (cellHeight.value * size.rows).toInt().coerceAtLeast(40)
    return Bundle().apply {
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, width)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
        putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, height)
    }
}
