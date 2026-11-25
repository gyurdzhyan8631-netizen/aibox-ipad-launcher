package com.aibox.launcher.desktop

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aibox.launcher.core.model.InstalledApp
import com.aibox.launcher.icons.AppIconView
import com.aibox.launcher.widgets.BatteryWidget
import com.aibox.launcher.widgets.ConnectionWidget
import com.aibox.launcher.widgets.DateWidget
import com.aibox.launcher.widgets.GlassCard

/** Рабочие столы: виджеты и выбранные ярлыки на первом, все приложения на втором. */
@Composable
fun HomeScreenPage(
    pageIndex: Int,
    apps: List<InstalledApp>,
    onLaunchApp: (InstalledApp) -> Unit,
    appWidgetHost: AppWidgetHost,
    nativeWidgetIds: List<Int>,
    modifier: Modifier = Modifier,
    iconScale: Float = 1f,
    showDateWidget: Boolean = true,
    showPowerWidget: Boolean = true,
    showNetworkWidget: Boolean = true,
    showDateWidgetOnSecondScreen: Boolean = false,
    showPowerWidgetOnSecondScreen: Boolean = false,
    showNetworkWidgetOnSecondScreen: Boolean = false,
    homeScreenApps: Set<String> = emptySet(),
    customIcons: Map<String, String> = emptyMap(),
    iconCornerRadiusFraction: Float = 0.22f
) {
    val content = modifier
        .fillMaxSize()
        .padding(bottom = 22.dp)

    if (pageIndex == 0) {
        val shortcuts = apps.filter { it.packageName in homeScreenApps }
        val hasWidgets = showDateWidget || showPowerWidget || showNetworkWidget
        val sectionCount = listOf(hasWidgets, nativeWidgetIds.isNotEmpty(), shortcuts.isNotEmpty())
            .count { it }
            .coerceAtLeast(1)
        Column(
            modifier = content,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (hasWidgets) {
                HomeWidgetsRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f / sectionCount),
                    showDateWidget = showDateWidget,
                    showPowerWidget = showPowerWidget,
                    showNetworkWidget = showNetworkWidget
                )
            }
            if (nativeWidgetIds.isNotEmpty()) {
                HostedWidgetsGrid(
                    appWidgetHost = appWidgetHost,
                    widgetIds = nativeWidgetIds,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f / sectionCount)
                )
            }
            if (shortcuts.isNotEmpty()) {
                AppGrid(
                    apps = shortcuts,
                    onLaunchApp = onLaunchApp,
                    iconScale = iconScale,
                    showLabels = false,
                    customIcons = customIcons,
                    iconCornerRadiusFraction = iconCornerRadiusFraction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f / sectionCount)
                )
            }
        }
    } else {
        val hasWidgets = showDateWidgetOnSecondScreen ||
            showPowerWidgetOnSecondScreen || showNetworkWidgetOnSecondScreen
        val sectionCount = 1 + (if (hasWidgets) 1 else 0) +
            (if (nativeWidgetIds.isNotEmpty()) 1 else 0)
        Column(
            modifier = content,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (hasWidgets) {
                HomeWidgetsRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f / sectionCount),
                    showDateWidget = showDateWidgetOnSecondScreen,
                    showPowerWidget = showPowerWidgetOnSecondScreen,
                    showNetworkWidget = showNetworkWidgetOnSecondScreen
                )
            }
            if (nativeWidgetIds.isNotEmpty()) {
                HostedWidgetsGrid(
                    appWidgetHost = appWidgetHost,
                    widgetIds = nativeWidgetIds,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f / sectionCount)
                )
            }
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f / sectionCount),
                contentAlignment = Alignment.Center
            ) {
                val panelSize = minOf(maxWidth * 0.7f, maxHeight * 0.82f, 400.dp)
                GlassCard(modifier = Modifier.size(panelSize)) {
                    AppGrid(
                        apps = apps,
                        onLaunchApp = onLaunchApp,
                        iconScale = iconScale,
                        showLabels = false,
                        customIcons = customIcons,
                        iconCornerRadiusFraction = iconCornerRadiusFraction,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun HostedWidgetsGrid(
    appWidgetHost: AppWidgetHost,
    widgetIds: List<Int>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val manager = remember(context) { AppWidgetManager.getInstance(context) }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 240.dp),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(widgetIds, key = { it }) { id ->
            val info = remember(id) { manager.getAppWidgetInfo(id) }
            HostedWidgetCard(
                context = context,
                appWidgetHost = appWidgetHost,
                appWidgetId = id,
                info = info
            )
        }
    }
}

@Composable
private fun HostedWidgetCard(
    context: Context,
    appWidgetHost: AppWidgetHost,
    appWidgetId: Int,
    info: AppWidgetProviderInfo?
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        if (info == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Виджет недоступен",
                    color = androidx.compose.ui.graphics.Color.White,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            AndroidView(
                factory = { appWidgetHost.createView(context, appWidgetId, info) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(info.minHeight.coerceAtLeast(120).dp)
            )
        }
    }
}

@Composable
private fun HomeWidgetsRow(
    modifier: Modifier,
    showDateWidget: Boolean,
    showPowerWidget: Boolean,
    showNetworkWidget: Boolean
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (showDateWidget) DateWidget(modifier = Modifier.weight(1f).fillMaxHeight())
        if (showPowerWidget) BatteryWidget(modifier = Modifier.weight(1f).fillMaxHeight())
        if (showNetworkWidget) ConnectionWidget(modifier = Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun AppGrid(
    apps: List<InstalledApp>,
    onLaunchApp: (InstalledApp) -> Unit,
    iconScale: Float,
    showLabels: Boolean,
    customIcons: Map<String, String>,
    iconCornerRadiusFraction: Float,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 88.dp * iconScale),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(apps, key = { it.packageName }) { app ->
            AppIconView(
                app = app,
                iconSize = 56.dp * iconScale,
                showLabel = showLabels,
                customIconUri = customIcons[app.packageName],
                cornerRadiusFraction = iconCornerRadiusFraction,
                onClick = { onLaunchApp(app) }
            )
        }
    }
}
