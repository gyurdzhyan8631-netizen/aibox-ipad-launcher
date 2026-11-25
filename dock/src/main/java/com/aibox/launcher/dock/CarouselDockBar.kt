package com.aibox.launcher.dock

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aibox.launcher.core.model.InstalledApp
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
    modifier: Modifier = Modifier,
    scale: Float = DockDefaults.DEFAULT_SCALE,
    customIcons: Map<String, String> = emptyMap(),
    iconPackPackage: String = "",
    onReorderApps: (List<String>) -> Unit = {},
    iconCornerRadiusFraction: Float = 0.22f
) {
    val panelHeight = DockDefaults.panelHeight(scale)
    val iconSize = maxOf(panelHeight - 36.dp, 28.dp)
    val shape = RoundedCornerShape(panelHeight * 0.38f)

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var viewportWidthPx by remember { mutableIntStateOf(0) }
    val cellDp = iconSize + 16.dp
    // Отступы по краям, чтобы первую и последнюю иконки тоже можно было поставить в центр.
    val sidePadding = with(density) {
        ((viewportWidthPx / 2f) - (cellDp.toPx() / 2f)).coerceAtLeast(0f).toDp()
    }

    // Когда прокрутка остановилась — мягко (с пружиной) докручиваем ближайшую иконку в центр.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { inProgress ->
            if (!inProgress) {
                val info = listState.layoutInfo
                val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                val nearest = info.visibleItemsInfo.minByOrNull {
                    abs(it.offset + it.size / 2f - center)
                }
                if (nearest != null) {
                    val delta = nearest.offset + nearest.size / 2f - center
                    if (abs(delta) > 1f) {
                        listState.animateScrollBy(
                            delta,
                            spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessLow)
                        )
                    }
                }
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(panelHeight)
            .clip(shape)
            .background(Color(0x661C1C1E))
            .border(1.dp, Color(0x33FFFFFF), shape)
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
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                items(apps, key = { it.packageName }) { app ->
                    val appIndex = apps.indexOf(app)
                    DockAppItem(
                        app = app,
                        index = appIndex,
                        listState = listState,
                        iconSize = iconSize,
                        customIconUri = customIcons[app.packageName],
                        iconPackPackage = iconPackPackage,
                        iconCornerRadiusFraction = iconCornerRadiusFraction,
                        onDrop = { draggedBy ->
                            val info = listState.layoutInfo
                            val dragged = info.visibleItemsInfo
                                .firstOrNull { it.key == app.packageName }
                            if (dragged != null) {
                                val dropCenter = dragged.offset + dragged.size / 2f + draggedBy
                                val target = info.visibleItemsInfo.minByOrNull {
                                    abs(it.offset + it.size / 2f - dropCenter)
                                }
                                val targetIndex = target?.let { item ->
                                    apps.indexOfFirst { it.packageName == item.key }
                                } ?: appIndex
                                if (targetIndex >= 0 && targetIndex != appIndex) {
                                    val reordered = apps.map { it.packageName }.toMutableList()
                                    val moved = reordered.removeAt(appIndex)
                                    reordered.add(targetIndex.coerceIn(0, reordered.size), moved)
                                    onReorderApps(reordered)
                                }
                            }
                        },
                        onClick = { onLaunchApp(app) }
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
}

@Composable
private fun DockAppItem(
    app: InstalledApp,
    index: Int,
    listState: LazyListState,
    iconSize: Dp,
    customIconUri: String?,
    iconPackPackage: String,
    iconCornerRadiusFraction: Float,
    onDrop: (Float) -> Unit,
    onClick: () -> Unit
) {
    var dragOffset by remember(app.packageName) { mutableFloatStateOf(0f) }
    // 0 — иконка в центре дока, 1 — далеко от центра.
    val distance by remember(app.packageName) {
        derivedStateOf {
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.key == app.packageName }
            if (item == null) {
                1f
            } else {
                val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                (abs(item.offset + item.size / 2f - center) / (item.size * 1.6f)).coerceIn(0f, 1f)
            }
        }
    }
    val itemScale by animateFloatAsState(
        targetValue = 1.22f - 0.22f * distance,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow),
        label = "dockItemScale"
    )

    AppIconView(
        app = app,
        onClick = onClick,
        iconSize = iconSize,
        showLabel = false,
        customIconUri = customIconUri,
        iconPackPackage = iconPackPackage,
        cornerRadiusFraction = iconCornerRadiusFraction,
        modifier = Modifier
            .pointerInput(app.packageName, index) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { dragOffset = 0f },
                    onDragEnd = {
                        onDrop(dragOffset)
                        dragOffset = 0f
                    },
                    onDragCancel = { dragOffset = 0f },
                    onDrag = { change, amount ->
                        change.consume()
                        dragOffset += amount.x
                    }
                )
            }
            .graphicsLayer {
                scaleX = itemScale
                scaleY = itemScale
                alpha = 1f - 0.55f * distance
                translationX = dragOffset
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
