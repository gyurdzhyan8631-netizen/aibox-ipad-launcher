package com.aibox.launcher.desktop

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.aibox.launcher.core.model.InstalledApp
import com.aibox.launcher.core.model.WidgetCardStyle
import com.aibox.launcher.core.model.WidgetPlacement
import com.aibox.launcher.icons.AppIconView
import com.aibox.launcher.widgets.BatteryWidget
import com.aibox.launcher.widgets.ConnectionWidget
import com.aibox.launcher.widgets.DateWidget
import com.aibox.launcher.widgets.GlassCard
import kotlin.math.roundToInt

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
    iconCornerRadiusFraction: Float = 0.22f,
    widgetPlacements: Map<String, WidgetPlacement> = emptyMap(),
    onUpdateWidgetPlacement: (String, WidgetPlacement) -> Unit = { _, _ -> },
    widgetCardStyle: WidgetCardStyle = WidgetCardStyle.GLASS
) {
    val context = LocalContext.current
    val manager = remember(context) { AppWidgetManager.getInstance(context) }
    val shortcuts = if (pageIndex == 0) apps.filter { it.packageName in homeScreenApps } else apps
    val widgets = remember(
        pageIndex, nativeWidgetIds, showDateWidget, showPowerWidget, showNetworkWidget,
        showDateWidgetOnSecondScreen, showPowerWidgetOnSecondScreen, showNetworkWidgetOnSecondScreen,
        manager
    ) {
        buildList {
            val date = if (pageIndex == 0) showDateWidget else showDateWidgetOnSecondScreen
            val power = if (pageIndex == 0) showPowerWidget else showPowerWidgetOnSecondScreen
            val network = if (pageIndex == 0) showNetworkWidget else showNetworkWidgetOnSecondScreen
            if (date) add(HomeWidgetItem("date:$pageIndex", BuiltInWidget.DATE))
            if (power) add(HomeWidgetItem("power:$pageIndex", BuiltInWidget.POWER))
            if (network) add(HomeWidgetItem("network:$pageIndex", BuiltInWidget.NETWORK))
            nativeWidgetIds.forEach { id ->
                val info = manager.getAppWidgetInfo(id)
                add(
                    HomeWidgetItem(
                        key = "app-widget:$id",
                        appWidgetId = id,
                        appWidgetInfo = info,
                        minWidthDp = (info?.minWidth ?: 150).coerceAtLeast(150).dp,
                        minHeightDp = (info?.minHeight ?: 110).coerceAtLeast(100).dp
                    )
                )
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 22.dp)
    ) {
        val hasAppRow = pageIndex == 1 || shortcuts.isNotEmpty()
        val appAreaHeight = if (hasAppRow) maxHeight * 0.28f else 0.dp
        val widgetAreaHeight = (maxHeight - appAreaHeight).coerceAtLeast(1.dp)

        WidgetCanvas(
            widgets = widgets,
            appWidgetHost = appWidgetHost,
            placements = widgetPlacements,
            onUpdatePlacement = onUpdateWidgetPlacement,
            style = widgetCardStyle,
            modifier = Modifier
                .fillMaxWidth()
                .height(widgetAreaHeight)
        )

        if (hasAppRow) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(appAreaHeight)
            ) {
                AppGrid(
                    apps = shortcuts,
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

private enum class BuiltInWidget { DATE, POWER, NETWORK }

private data class HomeWidgetItem(
    val key: String,
    val builtInWidget: BuiltInWidget? = null,
    val appWidgetId: Int? = null,
    val appWidgetInfo: AppWidgetProviderInfo? = null,
    val minWidthDp: Dp = 150.dp,
    val minHeightDp: Dp = 100.dp
)

@Composable
private fun WidgetCanvas(
    widgets: List<HomeWidgetItem>,
    appWidgetHost: AppWidgetHost,
    placements: Map<String, WidgetPlacement>,
    onUpdatePlacement: (String, WidgetPlacement) -> Unit,
    style: WidgetCardStyle,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val heightPx = with(LocalDensity.current) { maxHeight.toPx() }
        var selectedKey by remember { mutableStateOf<String?>(null) }

        val builtInCount = widgets.count { it.builtInWidget != null }
        val builtInIndex = widgets.filter { it.builtInWidget != null }
            .mapIndexed { index, widget -> widget.key to index }
            .toMap()
        widgets.forEachIndexed { index, widget ->
            val minWidthFraction = (widget.minWidthDp / maxWidth).coerceIn(0.16f, 1f)
            val minHeightFraction = (widget.minHeightDp / maxHeight).coerceIn(0.18f, 1f)
            val placement = (
                placements[widget.key]
                    ?: defaultPlacement(index, widget, builtInCount, builtInIndex[widget.key])
                )
                .constrained(minWidthFraction, minHeightFraction)

            EditableWidget(
                widget = widget,
                placement = placement,
                minWidthFraction = minWidthFraction,
                minHeightFraction = minHeightFraction,
                containerWidth = maxWidth,
                containerHeight = maxHeight,
                selected = selectedKey == widget.key,
                onSelect = { selectedKey = widget.key },
                onPlacementChange = { key, value -> onUpdatePlacement(key, value) },
                appWidgetHost = appWidgetHost,
                style = style,
                containerWidthPx = widthPx,
                containerHeightPx = heightPx
            )
        }
    }
}

private fun defaultPlacement(
    index: Int,
    widget: HomeWidgetItem,
    builtInCount: Int,
    builtInIndex: Int?
): WidgetPlacement {
    if (widget.builtInWidget != null && builtInIndex != null) {
        val threeAcross = builtInCount >= 3
        val width = if (threeAcross) 0.31f else 0.47f
        val x = if (threeAcross) 0.02f + builtInIndex * 0.33f else if (builtInIndex == 0) 0.02f else 0.51f
        return WidgetPlacement(x, 0.04f, width, 0.42f)
    }

    val nativeIndex = index - builtInCount
    val column = nativeIndex % 2
    val row = nativeIndex / 2
    return WidgetPlacement(
        xFraction = if (column == 0) 0.02f else 0.51f,
        yFraction = (if (builtInCount == 0) 0.04f else 0.48f) + row * 0.44f,
        widthFraction = 0.47f,
        heightFraction = 0.42f
    )
}

@Composable
private fun EditableWidget(
    widget: HomeWidgetItem,
    placement: WidgetPlacement,
    minWidthFraction: Float,
    minHeightFraction: Float,
    containerWidth: Dp,
    containerHeight: Dp,
    selected: Boolean,
    onSelect: () -> Unit,
    onPlacementChange: (String, WidgetPlacement) -> Unit,
    appWidgetHost: AppWidgetHost,
    style: WidgetCardStyle,
    containerWidthPx: Float,
    containerHeightPx: Float
) {
    val density = LocalDensity.current
    val context = LocalContext.current
    var dragDelta by remember(widget.key) { mutableStateOf(Offset.Zero) }
    var resizeDelta by remember(widget.key) { mutableStateOf(Offset.Zero) }
    val itemWidth = containerWidth * placement.widthFraction +
        with(density) { resizeDelta.x.toDp() }
    val itemHeight = containerHeight * placement.heightFraction +
        with(density) { resizeDelta.y.toDp() }
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (containerWidthPx * placement.xFraction).roundToInt() +
                        dragDelta.x.roundToInt(),
                    y = (containerHeightPx * placement.yFraction).roundToInt() +
                        dragDelta.y.roundToInt()
                )
            }
            .size(itemWidth.coerceAtLeast(1.dp), itemHeight.coerceAtLeast(1.dp))
            .clip(shape)
            .then(if (selected) Modifier.border(2.dp, Color(0xFF0A84FF), shape) else Modifier)
            .pointerInput(widget.key, containerWidthPx, containerHeightPx, placement) {
                var resizing = false
                detectDragGesturesAfterLongPress(
                    onDragStart = { start ->
                        onSelect()
                        resizing = start.x >= size.width - 44.dp.toPx() &&
                            start.y >= size.height - 44.dp.toPx()
                    },
                    onDragEnd = {
                        val next = if (resizing) {
                            placement.copy(
                                widthFraction = placement.widthFraction + resizeDelta.x / containerWidthPx,
                                heightFraction = placement.heightFraction + resizeDelta.y / containerHeightPx
                            )
                        } else {
                            placement.copy(
                                xFraction = placement.xFraction + dragDelta.x / containerWidthPx,
                                yFraction = placement.yFraction + dragDelta.y / containerHeightPx
                            )
                        }
                        if (resizing || dragDelta != Offset.Zero) {
                            onPlacementChange(
                                widget.key,
                                next.constrained(minWidthFraction, minHeightFraction)
                            )
                        }
                        if (resizing && widget.appWidgetId != null) {
                            val resized = next.constrained(minWidthFraction, minHeightFraction)
                            val widthDp = (containerWidth.value * resized.widthFraction).roundToInt()
                            val heightDp = (containerHeight.value * resized.heightFraction).roundToInt()
                            AppWidgetManager.getInstance(context).updateAppWidgetOptions(
                                widget.appWidgetId,
                                Bundle().apply {
                                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp)
                                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp)
                                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp)
                                    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp)
                                }
                            )
                        }
                        dragDelta = Offset.Zero
                        resizeDelta = Offset.Zero
                    },
                    onDragCancel = {
                        dragDelta = Offset.Zero
                        resizeDelta = Offset.Zero
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        if (resizing) {
                            resizeDelta += amount
                        } else {
                            dragDelta += amount
                        }
                    }
                )
            }
    ) {
        WidgetContent(
            widget = widget,
            appWidgetHost = appWidgetHost,
            style = style,
            modifier = Modifier.fillMaxSize()
        )
        if (selected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(Color(0xDD0A84FF))
            ) {
                Icon(
                    imageVector = Icons.Rounded.OpenInFull,
                    contentDescription = "Изменить размер виджета",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun WidgetContent(
    widget: HomeWidgetItem,
    appWidgetHost: AppWidgetHost,
    style: WidgetCardStyle,
    modifier: Modifier
) {
    when (widget.builtInWidget) {
        BuiltInWidget.DATE -> DateWidget(modifier, style)
        BuiltInWidget.POWER -> BatteryWidget(modifier, style)
        BuiltInWidget.NETWORK -> ConnectionWidget(modifier, style)
        null -> {
            val id = widget.appWidgetId
            val info = widget.appWidgetInfo
            if (id == null || info == null) {
                Box(
                    modifier = modifier
                        .background(Color(0x661C1C1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Виджет недоступен",
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                GlassCard(modifier = modifier, style = style) {
                    AndroidView(
                        factory = { appWidgetHost.createView(it, id, info) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
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
