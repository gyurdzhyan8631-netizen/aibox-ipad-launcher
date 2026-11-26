package com.aibox.launcher.desktop

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.model.DesktopWidget
import com.aibox.launcher.core.model.InstalledApp
import com.aibox.launcher.core.model.DesktopWidgetSize
import com.aibox.launcher.core.ui.ScaledContent
import com.aibox.launcher.core.ui.glassSurface
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

private val EditAccent = Color(0xFF0A84FF)

/** Размер виджета по умолчанию. */
fun defaultWidgetSize(token: String): DesktopWidgetSize = DesktopWidgetSize(
    weight = when {
        token == DesktopWidget.APPS.id -> 2f
        token.startsWith(APP_WIDGET_PREFIX) -> 1.5f
        else -> 1f
    },
    height = 1f
)

/**
 * Длинное нажатие, которое только «подслушивается»: касание не перехватывается,
 * поэтому кнопки и прокрутка внутри виджетов продолжают работать.
 */
private fun Modifier.longPressObserver(onLongPress: () -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val slop = viewConfiguration.touchSlop
        val result = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull Unit
                if (!change.pressed) return@withTimeoutOrNull Unit
                if ((change.position - down.position).getDistance() > slop) return@withTimeoutOrNull Unit
            }
        }
        if (result == null) onLongPress()
    }
}

/** Лёгкое покачивание виджетов в режиме правки. */
@Composable
private fun rememberWiggle(enabled: Boolean, phase: Int): State<Float> {
    val still = remember { mutableFloatStateOf(0f) }
    if (!enabled) return still
    val transition = rememberInfiniteTransition(label = "wiggle")
    return transition.animateFloat(
        initialValue = -1.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 140, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset((phase * 37) % 140)
        ),
        label = "wiggleAngle"
    )
}

/**
 * Рабочий стол (первый и второй устроены одинаково): ряд виджетов — встроенные и виджеты других приложений.
 * Долгое нажатие включает режим правки: виджеты покачиваются, на них появляются стрелки (сдвинуть влево/вправо),
 * угловая стрелка (потянуть — изменить размер), кнопка «убрать» и плитка «добавить».
 */
@Composable
fun HomeScreenPage(
    widgets: List<String>,
    sizes: Map<String, DesktopWidgetSize>,
    editMode: Boolean,
    onEnterEdit: () -> Unit,
    onRemove: (String) -> Unit,
    onMove: (String, Int) -> Unit,
    onResize: (String, DesktopWidgetSize) -> Unit,
    onAddWidget: () -> Unit,
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

    // Размеры, которые меняются на глазах во время растягивания.
    var live by remember(sizes) { mutableStateOf(sizes) }
    var rowWidthPx by remember { mutableIntStateOf(1) }
    var rowHeightPx by remember { mutableIntStateOf(1) }

    Box(modifier = area.longPressObserver { if (!editMode) onEnterEdit() }) {
        ScaledContent(widgetScale) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged {
                        rowWidthPx = it.width.coerceAtLeast(1)
                        rowHeightPx = it.height.coerceAtLeast(1)
                    },
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                widgets.forEachIndexed { index, token ->
                    val size = live[token] ?: defaultWidgetSize(token)
                    WidgetCell(
                        token = token,
                        index = index,
                        last = widgets.size - 1,
                        size = size,
                        editMode = editMode,
                        onRemove = { onRemove(token) },
                        onMove = { delta -> onMove(token, delta) },
                        onResizeDrag = { dx, dy ->
                            val total = widgets.sumOf { (live[it] ?: defaultWidgetSize(it)).weight.toDouble() }
                                .toFloat() + 0.5f
                            val unitPx = rowWidthPx / total
                            val current = live[token] ?: defaultWidgetSize(token)
                            live = live + (token to DesktopWidgetSize(
                                weight = (current.weight + dx / unitPx).coerceIn(0.6f, 4f),
                                height = (current.height + dy / rowHeightPx).coerceIn(0.35f, 1f)
                            ))
                        },
                        onResizeEnd = { live[token]?.let { onResize(token, it) } }
                    ) {
                        val builtin = DesktopWidget.entries.firstOrNull { it.id == token }
                        val appWidgetId = if (token.startsWith(APP_WIDGET_PREFIX)) {
                            token.removePrefix(APP_WIDGET_PREFIX).toIntOrNull()
                        } else {
                            null
                        }
                        val fill = Modifier.fillMaxSize()
                        when {
                            builtin == DesktopWidget.DATE -> DateWidget(modifier = fill)
                            builtin == DesktopWidget.POWER -> BatteryWidget(modifier = fill)
                            builtin == DesktopWidget.NETWORK -> ConnectionWidget(modifier = fill)
                            builtin == DesktopWidget.WEATHER -> WeatherWidget(info = weather, modifier = fill)
                            builtin == DesktopWidget.MUSIC -> MusicWidget(onOpenPlayer = onOpenMusic, modifier = fill)
                            builtin == DesktopWidget.APPS -> AppsWidget(
                                apps = apps,
                                onLaunchApp = onLaunchApp,
                                iconScale = iconScale,
                                modifier = fill
                            )
                            appWidgetId != null -> HostedAppWidget(
                                controller = widgetController,
                                appWidgetId = appWidgetId,
                                modifier = fill
                            )
                        }
                    }
                }

                if (editMode) {
                    AddCell(
                        onClick = onAddWidget,
                        modifier = Modifier
                            .weight(if (widgets.isEmpty()) 1f else 0.5f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.WidgetCell(
    token: String,
    index: Int,
    last: Int,
    size: DesktopWidgetSize,
    editMode: Boolean,
    onRemove: () -> Unit,
    onMove: (Int) -> Unit,
    onResizeDrag: (Float, Float) -> Unit,
    onResizeEnd: () -> Unit,
    content: @Composable () -> Unit
) {
    val wiggle = rememberWiggle(editMode, index)
    Box(
        modifier = Modifier
            .weight(size.weight)
            .fillMaxHeight()
            .graphicsLayer { rotationZ = wiggle.value }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(size.height)
        ) {
            content()

            if (editMode) {
                // перехватываем касания: пока идёт правка, сами виджеты не реагируют
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(token) { detectTapGestures { } }
                )

                EditBadge(
                    icon = Icons.Rounded.Close,
                    background = Color(0xFFFF453A),
                    description = "Убрать виджет",
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-6).dp, y = (-6).dp)
                )
                if (index > 0) {
                    EditBadge(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        background = EditAccent,
                        description = "Сдвинуть влево",
                        onClick = { onMove(-1) },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = (-8).dp)
                    )
                }
                if (index < last) {
                    EditBadge(
                        icon = Icons.AutoMirrored.Rounded.ArrowForward,
                        background = EditAccent,
                        description = "Сдвинуть вправо",
                        onClick = { onMove(1) },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .offset(x = 8.dp)
                    )
                }
                ResizeHandle(
                    onDrag = onResizeDrag,
                    onEnd = onResizeEnd,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 6.dp, y = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun EditBadge(
    icon: ImageVector,
    background: Color,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

/** Угловая стрелка: потяните, чтобы изменить ширину и высоту виджета. */
@Composable
private fun ResizeHandle(
    onDrag: (Float, Float) -> Unit,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDrag by rememberUpdatedState(onDrag)
    val currentEnd by rememberUpdatedState(onEnd)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(EditAccent)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { currentEnd() },
                    onDragCancel = { currentEnd() }
                ) { change, amount ->
                    change.consume()
                    currentDrag(amount.x, amount.y)
                }
            }
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = "Изменить размер",
            tint = Color.White,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { rotationZ = 45f }
        )
    }
}

@Composable
private fun AddCell(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .glassSurface(androidx.compose.foundation.shape.RoundedCornerShape(24.dp), strength = 0.7f)
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "Добавить виджет",
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(36.dp)
        )
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
