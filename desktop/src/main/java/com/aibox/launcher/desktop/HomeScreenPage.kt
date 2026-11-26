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
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import com.aibox.launcher.widgets.WeatherWidget
import com.aibox.launcher.widgets.WeatherWidgetInfo

/** Префикс записи о виджете другого приложения: «aw:123». */
const val APP_WIDGET_PREFIX = "aw:"

private val EditAccent = Color(0xFF0A84FF)

/** Начальное положение: аккуратная сетка, дальше виджет можно поставить куда угодно. */
fun defaultWidgetSize(token: String, index: Int = 0, count: Int = 1): DesktopWidgetSize {
    val columns = if (count <= 1) 1 else 2
    val slot = 1f / columns
    val extra = when {
        token == DesktopWidget.APPS.id -> 1.15f
        token.startsWith(APP_WIDGET_PREFIX) -> 1.05f
        else -> 1f
    }
    return DesktopWidgetSize(
        weight = (slot * 0.9f * extra).coerceAtMost(0.95f),
        height = 0.34f,
        x = (index % columns) * slot + slot * 0.05f,
        y = (index / columns) * 0.36f + 0.02f
    )
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
                    val size = live[token] ?: defaultWidgetSize(token, index, widgets.size)
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
                        }
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
    content: @Composable () -> Unit
) {
    val wiggle = rememberWiggle(editMode, index)
    var drag by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val widthPx = with(density) { areaWidth.toPx().coerceAtLeast(1f) }
    val heightPx = with(density) { areaHeight.toPx().coerceAtLeast(1f) }

    Box(
        modifier = Modifier
            .offset(
                x = areaWidth * size.x + with(density) { drag.x.toDp() },
                y = areaHeight * size.y + with(density) { drag.y.toDp() }
            )
            .size(width = areaWidth * size.weight, height = areaHeight * size.height)
            .graphicsLayer { rotationZ = wiggle.value }
    ) {
        content()

        if (editMode) {
            // Табы не перехватываются: кнопки и виджеты приложений остаются доступными.
            Box(
                modifier = Modifier.matchParentSize().pointerInput(token) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { drag = Offset.Zero },
                        onDrag = { change, amount ->
                            change.consume()
                            drag += amount
                        },
                        onDragEnd = {
                            val nextX = (size.x + drag.x / widthPx)
                                .coerceIn(0f, (1f - size.weight).coerceAtLeast(0f))
                            val nextY = (size.y + drag.y / heightPx)
                                .coerceIn(0f, (1f - size.height).coerceAtLeast(0f))
                            drag = Offset.Zero
                            if (nextX != size.x || nextY != size.y) {
                                onChange(size.copy(x = nextX, y = nextY))
                            }
                        },
                        onDragCancel = { drag = Offset.Zero }
                    )
                }
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
