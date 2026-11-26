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
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalDensity
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
import com.aibox.launcher.widgets.NavigationWidget
import com.aibox.launcher.widgets.ShortcutInfo
import com.aibox.launcher.widgets.VideoWidget
import com.aibox.launcher.widgets.WeatherWidget
import com.aibox.launcher.widgets.WeatherWidgetInfo

/** Префикс записи о виджете другого приложения: «aw:123». */
const val APP_WIDGET_PREFIX = "aw:"

private val EditAccent = Color(0xFF0A84FF)

/** Начальное положение: новые виджеты сразу квадратные, дальше их можно перемещать и тянуть. */
fun defaultWidgetSize(
    token: String,
    areaWidth: Dp,
    areaHeight: Dp,
    index: Int = 0,
    count: Int = 1
): DesktopWidgetSize {
    val width = areaWidth.value.coerceAtLeast(1f)
    val height = areaHeight.value.coerceAtLeast(1f)
    val side = minOf(width, height) * 0.30f
    val gap = 12f
    val fitColumns = (width / (side + gap)).toInt().coerceAtLeast(1).coerceAtMost(4)
    val columns = if (count <= 1) 1 else minOf(count, fitColumns)
    val col = index % columns
    val row = index / columns
    val weight = (side / width).coerceIn(0.08f, 0.95f)
    val heightFraction = (side / height).coerceIn(0.08f, 0.95f)
    val x = ((col * (side + gap) + gap) / width).coerceIn(0f, (1f - weight).coerceAtLeast(0f))
    val y = ((row * (side + gap) + gap) / height).coerceIn(0f, (1f - heightFraction).coerceAtLeast(0f))
    return DesktopWidgetSize(weight = weight, height = heightFraction, x = x, y = y)
}

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

/** Лёгкое покачивание виджетов в режиме правки (не трогаем). */
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
 * Рабочий стол: виджеты свободно располагаются в любом месте.
 * Долгое нажатие включает правку: покачивание, красный крестик и угловая стрелка размера.
 */
@Composable
fun HomeScreenPage(
    widgets: List<String>,
    sizes: Map<String, DesktopWidgetSize>,
    editMode: Boolean,
    onEnterEdit: () -> Unit,
    onRemove: (String) -> Unit,
    onResize: (String, DesktopWidgetSize) -> Unit,
    onAddWidget: () -> Unit,
    weather: WeatherWidgetInfo,
    apps: List<InstalledApp>,
    onLaunchApp: (InstalledApp) -> Unit,
    onOpenMusic: () -> Unit,
    onOpenWidget: (DesktopWidget) -> Unit,
    navigationAppLabel: String,
    videoAppLabel: String,
    widgetController: AppWidgetController,
    iconScale: Float,
    widgetScale: Float,
    uiScale: Float = 1f,
    topPadding: Dp,
    bottomPadding: Dp,
    endPadding: Dp = 28.dp,
    modifier: Modifier = Modifier
) {
    val area = modifier
        .fillMaxSize()
        .padding(start = 28.dp, end = endPadding, top = topPadding, bottom = bottomPadding)

    // Иконки не зависят от общего масштаба интерфейса.
    val effectiveIconScale = iconScale / uiScale.coerceAtLeast(0.01f)

    var live by remember(sizes) { mutableStateOf(sizes) }

    Box(modifier = area.longPressObserver { if (!editMode) onEnterEdit() }) {
        ScaledContent(widgetScale) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                widgets.forEachIndexed { index, token ->
                    val size = live[token]
                        ?: defaultWidgetSize(token, maxWidth, maxHeight, index, widgets.size)
                    WidgetCell(
                        token = token,
                        index = index,
                        size = size,
                        editMode = editMode,
                        areaWidth = maxWidth,
                        areaHeight = maxHeight,
                        onRemove = { onRemove(token) },
                        onChange = { updated ->
                            live = live + (token to updated)
                            onResize(token, updated)
                        },
                        onTap = { widget -> onOpenWidget(widget) }
                    ) {
                        val fill = Modifier.fillMaxSize()
                        val builtin = DesktopWidget.entries.firstOrNull { it.id == token }
                        val appWidgetId = if (token.startsWith(APP_WIDGET_PREFIX)) {
                            token.removePrefix(APP_WIDGET_PREFIX).toIntOrNull()
                        } else {
                            null
                        }
                        when {
                            builtin == DesktopWidget.DATE -> DateWidget(modifier = fill)
                            builtin == DesktopWidget.POWER -> BatteryWidget(modifier = fill)
                            builtin == DesktopWidget.NETWORK -> ConnectionWidget(modifier = fill)
                            builtin == DesktopWidget.WEATHER -> WeatherWidget(info = weather, modifier = fill)
                            builtin == DesktopWidget.MUSIC -> MusicWidget(onOpenPlayer = onOpenMusic, modifier = fill)
                            builtin == DesktopWidget.NAVIGATION -> NavigationWidget(
                                info = ShortcutInfo(
                                    appLabel = navigationAppLabel,
                                    place = weather.place,
                                    note = "Нажмите для маршрута"
                                ),
                                onClick = { onOpenWidget(DesktopWidget.NAVIGATION) },
                                modifier = fill
                            )
                            builtin == DesktopWidget.VIDEO -> VideoWidget(
                                info = ShortcutInfo(
                                    appLabel = videoAppLabel,
                                    note = "Нажмите, чтобы открыть видео"
                                ),
                                onClick = { onOpenWidget(DesktopWidget.VIDEO) },
                                modifier = fill
                            )
                            builtin == DesktopWidget.APPS -> AppsWidget(
                                apps = apps,
                                onLaunchApp = onLaunchApp,
                                iconScale = effectiveIconScale,
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
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .size(width = 92.dp, height = 44.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetCell(
    token: String,
    index: Int,
    size: DesktopWidgetSize,
    editMode: Boolean,
    areaWidth: Dp,
    areaHeight: Dp,
    onRemove: () -> Unit,
    onChange: (DesktopWidgetSize) -> Unit,
    onTap: (DesktopWidget) -> Unit,
    content: @Composable () -> Unit
) {
    val wiggle = rememberWiggle(editMode, index)
    var drag by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val widthPx = with(density) { areaWidth.toPx().coerceAtLeast(1f) }
    val heightPx = with(density) { areaHeight.toPx().coerceAtLeast(1f) }
    val currentEditMode by rememberUpdatedState(editMode)
    val currentOnChange by rememberUpdatedState(onChange)
    val tapTarget = remember(token) { DesktopWidget.entries.firstOrNull { it.id == token } }

    val cellModifier = Modifier
        .offset(
            x = areaWidth * size.x + with(density) { drag.x.toDp() },
            y = areaHeight * size.y + with(density) { drag.y.toDp() }
        )
        .size(width = areaWidth * size.weight, height = areaHeight * size.height)
        .graphicsLayer { rotationZ = wiggle.value }
        .then(
            if (tapTarget != null && tapTarget != DesktopWidget.APPS) {
                Modifier.clickable { onTap(tapTarget) }
            } else {
                Modifier
            }
        )
        // Прямое перетаскивание пальцем: тап остаётся тапом, движение — переносом виджета.
        .pointerInput(token) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val start = down.position
                val handleZone = with(density) { 48.dp.toPx() }
                val badgeZone = with(density) { 40.dp.toPx() }
                val reserved = currentEditMode && (
                    (start.x >= widthPx - handleZone && start.y >= heightPx - handleZone) ||
                        (start.x <= badgeZone && start.y <= badgeZone)
                    )
                if (reserved) {
                    // Крестик и стрелка размера обрабатываются своими own-жестами.
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                    }
                    return@awaitEachGesture
                }

                var dragging = false
                var moved = Offset.Zero
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    if (!dragging && (change.position - start).getDistance() > viewConfiguration.touchSlop) {
                        dragging = true
                    }
                    if (dragging) {
                        change.consume()
                        moved += change.position - change.previousPosition
                        drag = moved
                    }
                }
                if (dragging) {
                    val nextX = (size.x + moved.x / widthPx)
                        .coerceIn(0f, (1f - size.weight).coerceAtLeast(0f))
                    val nextY = (size.y + moved.y / heightPx)
                        .coerceIn(0f, (1f - size.height).coerceAtLeast(0f))
                    drag = Offset.Zero
                    if (nextX != size.x || nextY != size.y) {
                        currentOnChange(size.copy(x = nextX, y = nextY))
                    }
                }
            }
        }

    Box(modifier = cellModifier) {
        content()

        if (editMode) {
            EditBadge(
                icon = Icons.Rounded.Close,
                background = Color(0xFFFF453A),
                description = "Убрать виджет",
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-6).dp, y = (-6).dp)
            )

            // Только одна стрелка в правом нижнем углу: тянет и вширь, и ввысь.
            ResizeHandle(
                onDrag = { dx, dy ->
                    onChange(
                        size.copy(
                            weight = (size.weight + dx / widthPx).coerceIn(
                                0.15f, (1f - size.x).coerceAtLeast(0.15f)
                            ),
                            height = (size.height + dy / heightPx).coerceIn(
                                0.12f, (1f - size.y).coerceAtLeast(0.12f)
                            )
                        )
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 6.dp, y = 6.dp)
            )
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

/** Угловая стрелка: потяните, чтобы изменить размер в любую сторону. */
@Composable
private fun ResizeHandle(
    onDrag: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDrag by rememberUpdatedState(onDrag)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(EditAccent)
            .pointerInput(Unit) {
                detectDragGestures { change, amount ->
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
