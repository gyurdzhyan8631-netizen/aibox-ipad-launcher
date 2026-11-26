package com.aibox.launcher.dock

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aibox.launcher.core.model.InstalledApp
import com.aibox.launcher.core.ui.glassSurface
import com.aibox.launcher.icons.AppIconView
import kotlin.math.abs

/**
 * Нижний док без подписей.
 * Слева закреплена кнопка «Все приложения», справа — настройки.
 * Между ними карусель: иконка в центре чуть крупнее и «пружинит»,
 * соседние слева и справа затемнены. После прокрутки ближайшая иконка сама встаёт в центр.
 */
@Composable
fun CarouselDockBar(
    apps: List<InstalledApp>,
    onLaunchApp: (InstalledApp) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onReorder: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = DockDefaults.DEFAULT_SCALE,
    uiScale: Float = 1f,
    dockWidth: Float = 1f,
    onDockWidthChange: (Float) -> Unit = {},
    onPanelScaleChange: (Float) -> Unit = {}
) {
    val panelHeight = DockDefaults.panelHeight(scale)
    // Размер иконки задаётся только panelScale, а не общим uiScale.
    val iconSize = maxOf(panelHeight - 36.dp, 28.dp) / uiScale.coerceAtLeast(0.01f)
    val shape = RoundedCornerShape(panelHeight * 0.38f)

    // Растягивание дока: ширина меняет только ширину, высота — panelsScale,
    // поэтому иконки остаются квадратными и не «плывут».
    var dockWidthPx by remember { mutableIntStateOf(1) }
    var resizing by remember { mutableStateOf(false) }
    val currentWidth by rememberUpdatedState(dockWidth)
    val currentScale by rememberUpdatedState(scale)
    val setWidth by rememberUpdatedState(onDockWidthChange)
    val setPanelScale by rememberUpdatedState(onPanelScaleChange)

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var viewportWidthPx by remember { mutableIntStateOf(0) }
    val cellDp = iconSize + 16.dp
    val spacingDp = 2.dp
    val viewportWidthDp = with(density) { viewportWidthPx.toDp() }
    val contentWidth = cellDp * apps.size + spacingDp * (apps.size - 1).coerceAtLeast(0)
    // Если иконок мало, не раздвигаем их на пол экрана ради «центрирования» первой.
    val sidePadding = if (contentWidth >= viewportWidthDp) {
        maxOf(0.dp, (viewportWidthDp - cellDp) / 2f)
    } else {
        8.dp
    }

    // Порядок иконок, который меняется во время перетаскивания.
    var working by remember(apps) { mutableStateOf(apps) }
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var suppressClickUntil by remember { mutableLongStateOf(0L) }

    // Когда прокрутка остановилась — мягко (с пружиной) докручиваем ближайшую иконку в центр.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { inProgress ->
            if (!inProgress) snapToCenter(listState)
        }
    }
    // То же после того, как иконку отпустили.
    LaunchedEffect(draggedKey) {
        if (draggedKey == null) snapToCenter(listState)
    }

    Box(
        modifier = modifier
            .onSizeChanged { dockWidthPx = it.width.coerceAtLeast(1) }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { resizing = true },
                    onDrag = { change, amount ->
                        change.consume()
                        // Док растягивается от центра: дельта с обеих сторон => x2.
                        setWidth(
                            (currentWidth + amount.x * 2f / dockWidthPx).coerceIn(0.45f, 1f)
                        )
                        setPanelScale(
                            (currentScale - amount.y / 420f).coerceIn(0.75f, 1.35f)
                        )
                    },
                    onDragEnd = { resizing = false },
                    onDragCancel = { resizing = false }
                )
            }
    ) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight)
            .clip(shape)
            .background(Color(0x40080A10))
            .glassSurface(shape)
            .padding(horizontal = 10.dp)
    ) {
        DockShortcut(
            icon = Icons.Rounded.Apps,
            description = "Все приложения",
            accent = Color(0xFF5E5CE6),
            iconSize = iconSize,
            onClick = onOpenDrawer
        )
        DockDivider(iconSize)

        Box(
            modifier = Modifier
                .weight(1f)
                .height(panelHeight)
                .onSizeChanged { viewportWidthPx = it.width }
        ) {
            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = sidePadding),
                horizontalArrangement = Arrangement.spacedBy(spacingDp),
                verticalAlignment = Alignment.CenterVertically,
                userScrollEnabled = draggedKey == null,
                modifier = Modifier.fillMaxSize()
            ) {
                items(working, key = { it.packageName }) { app ->
                    val isDragged = draggedKey == app.packageName
                    DockAppItem(
                        app = app,
                        listState = listState,
                        iconSize = iconSize,
                        isDragged = isDragged,
                        dragOffset = if (isDragged) dragOffset else 0f,
                        onClick = {
                            val now = System.currentTimeMillis()
                            if (draggedKey == null && now > suppressClickUntil) onLaunchApp(app)
                        },
                        onDragStart = {
                            draggedKey = app.packageName
                            dragOffset = 0f
                        },
                        onDrag = { dx ->
                            dragOffset += dx
                            val info = listState.layoutInfo
                            val dragged = info.visibleItemsInfo.firstOrNull { it.key == app.packageName }
                            if (dragged != null) {
                                val center = dragged.offset + dragged.size / 2f + dragOffset
                                val target = info.visibleItemsInfo.firstOrNull {
                                    it.key != app.packageName &&
                                        center >= it.offset && center <= it.offset + it.size
                                }
                                if (target != null) {
                                    val from = working.indexOfFirst { it.packageName == app.packageName }
                                    val to = working.indexOfFirst { it.packageName == target.key }
                                    if (from >= 0 && to >= 0 && from != to) {
                                        val list = working.toMutableList()
                                        list.add(to, list.removeAt(from))
                                        working = list
                                        dragOffset -= (target.offset - dragged.offset)
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            draggedKey = null
                            dragOffset = 0f
                            suppressClickUntil = System.currentTimeMillis() + 400L
                            onReorder(working.map { it.packageName })
                        },
                        modifier = if (isDragged) Modifier else Modifier.animateItem()
                    )
                }
            }
        }

        DockDivider(iconSize)
        DockShortcut(
            icon = Icons.Rounded.Tune,
            description = "Настройки",
            accent = Color(0xFF8E8E93),
            iconSize = iconSize,
            onClick = onOpenSettings
        )
    }

        // Полоса-ручка: зажмите док и тяните влево/вправо (ширина) или вверх/вниз (высота).
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
                .size(width = 36.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (resizing) Color(0xFF0A84FF) else Color.White.copy(alpha = 0.22f))
        )
    }
}

/** Докручивает ближайшую к центру иконку точно в центр дока. */
private suspend fun snapToCenter(listState: LazyListState) {
    val info = listState.layoutInfo
    val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
    val nearest = info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2f - center) } ?: return
    val delta = nearest.offset + nearest.size / 2f - center
    if (abs(delta) > 1f) {
        listState.animateScrollBy(
            delta,
            spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow)
        )
    }
}

@Composable
private fun DockAppItem(
    app: InstalledApp,
    listState: LazyListState,
    iconSize: Dp,
    isDragged: Boolean,
    dragOffset: Float,
    onClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    // -1 … 1: насколько иконка смещена от центра дока (знак — влево или вправо).
    val signed by remember(app.packageName) {
        derivedStateOf {
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.key == app.packageName }
            if (item == null) {
                1f
            } else {
                val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                ((item.offset + item.size / 2f - center) / (item.size * 1.6f)).coerceIn(-1f, 1f)
            }
        }
    }
    val distance = abs(signed)
    val itemScale by animateFloatAsState(
        targetValue = 1.22f - 0.22f * distance,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "dockItemScale"
    )

    val currentStart by rememberUpdatedState(onDragStart)
    val currentDrag by rememberUpdatedState(onDrag)
    val currentEnd by rememberUpdatedState(onDragEnd)

    AppIconView(
        app = app,
        onClick = onClick,
        iconSize = iconSize,
        showLabel = false,
        modifier = modifier
            .zIndex(if (isDragged) 1f else 0f)
            .pointerInput(app.packageName) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { currentStart() },
                    onDrag = { change, amount ->
                        change.consume()
                        currentDrag(amount.x)
                    },
                    onDragEnd = { currentEnd() },
                    onDragCancel = { currentEnd() }
                )
            }
            .graphicsLayer {
                val lift = if (isDragged) 1.12f else 1f
                scaleX = itemScale * lift
                scaleY = itemScale * lift
                alpha = if (isDragged) 1f else 1f - 0.55f * distance
                // пространственный эффект: соседние иконки чуть «развёрнуты» к центру
                rotationY = -signed * 28f
                cameraDistance = 14f * density
                translationX = if (isDragged) dragOffset else 0f
            }
    )
}

@Composable
private fun DockDivider(height: Dp) {
    Box(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .width(1.dp)
            .height(height)
            .background(Color(0x33FFFFFF))
    )
}

@Composable
private fun DockShortcut(
    icon: ImageVector,
    description: String,
    accent: Color,
    iconSize: Dp,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(iconSize * 0.22f))
                .background(accent)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = Color.White,
                modifier = Modifier.size(iconSize * 0.55f)
            )
        }
    }
}
