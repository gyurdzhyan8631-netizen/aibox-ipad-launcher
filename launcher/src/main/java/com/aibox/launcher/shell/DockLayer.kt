package com.aibox.launcher.shell

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aibox.launcher.core.model.DesktopWidgetSize
import com.aibox.launcher.core.model.InstalledApp
import com.aibox.launcher.dock.CarouselDockBar
import com.aibox.launcher.dock.DockDefaults

private val DockEditAccent = Color(0xFF0A84FF)

/** Та же лёгкая покачивание, что и у виджетов. */
@Composable
private fun rememberDockWiggle(enabled: Boolean, phase: Int): State<Float> {
    val still = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    if (!enabled) return still
    val transition = rememberInfiniteTransition(label = "dockWiggle")
    return transition.animateFloat(
        initialValue = -1.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 140, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset((phase * 37) % 140)
        ),
        label = "dockWiggleAngle"
    )
}

/**
 * Док как обычный виджет: долгое нажатие включает правку, пальцем можно перенести
 * док в любую точку экрана, стрелка в правом нижнем углу меняет размер,
 * красный крестик убирает док (вернуть можно в «Размерах»).
 */
@Composable
fun DockLayer(
    visible: Boolean,
    layout: DesktopWidgetSize,
    editMode: Boolean,
    onHide: () -> Unit,
    onChange: (DesktopWidgetSize) -> Unit,
    apps: List<InstalledApp>,
    onLaunchApp: (InstalledApp) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    onReorder: (List<String>) -> Unit,
    panelScale: Float,
    uiScale: Float
) {
    if (!visible && !editMode) return

    val density = LocalDensity.current
    val change by rememberUpdatedState(onChange)
    var drag by remember { mutableStateOf(Offset.Zero) }
    val wiggle = rememberDockWiggle(editMode, 3)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val widthPx = with(density) { maxWidth.toPx().coerceAtLeast(1f) }
        val heightPx = with(density) { maxHeight.toPx().coerceAtLeast(1f) }

        val wantedHeight = maxHeight * layout.height.coerceIn(0.06f, 0.22f)
        val scale = (wantedHeight / 84.dp).coerceIn(DockDefaults.MIN_SCALE, DockDefaults.MAX_SCALE)
        val actualHeight = DockDefaults.panelHeight(scale)
        val width = maxWidth * layout.weight.coerceIn(0.3f, 1f)

        Box(
            modifier = Modifier
                .offset(
                    x = maxWidth * layout.x + with(density) { drag.x.toDp() },
                    y = maxHeight * layout.y + with(density) { drag.y.toDp() }
                )
                .size(width = width, height = actualHeight)
                .graphicsLayer { rotationZ = wiggle.value }
        ) {
            CarouselDockBar(
                apps = apps,
                onLaunchApp = onLaunchApp,
                onOpenDrawer = onOpenDrawer,
                onOpenSettings = onOpenSettings,
                onReorder = onReorder,
                modifier = Modifier.fillMaxSize(),
                scale = scale,
                uiScale = uiScale
            )

            if (editMode) {
                // Перетаскивание дока пальцем в любую точку рабочего стола.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { drag = Offset.Zero },
                                onDrag = { pointer, amount ->
                                    pointer.consume()
                                    drag += amount
                                },
                                onDragEnd = {
                                    val nextX = (layout.x + drag.x / widthPx)
                                        .coerceIn(0f, (1f - layout.weight).coerceAtLeast(0f))
                                    val nextY = (layout.y + drag.y / heightPx)
                                        .coerceIn(0f, (1f - layout.height).coerceAtLeast(0f))
                                    drag = Offset.Zero
                                    if (nextX != layout.x || nextY != layout.y) {
                                        change(layout.copy(x = nextX, y = nextY))
                                    }
                                },
                                onDragCancel = { drag = Offset.Zero }
                            )
                        }
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-6).dp, y = (-6).dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF453A))
                        .clickable(onClick = onHide)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Убрать док",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 6.dp, y = 6.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(DockEditAccent)
                        .pointerInput(Unit) {
                            detectDragGestures { pointer, amount ->
                                pointer.consume()
                                change(
                                    layout.copy(
                                        weight = (layout.weight + amount.x / widthPx).coerceIn(
                                            0.3f, (1f - layout.x).coerceAtLeast(0.3f)
                                        ),
                                        height = (layout.height + amount.y / heightPx)
                                            .coerceIn(0.06f, 0.22f)
                                    )
                                )
                            }
                        }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = "Изменить размер дока",
                        tint = Color.White,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { rotationZ = 45f }
                    )
                }
            }
        }
    }
}
