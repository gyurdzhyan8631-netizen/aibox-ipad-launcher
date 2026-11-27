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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.ui.glassSurface
import com.aibox.launcher.widgets.WIDGET_GRID_COLUMNS
import com.aibox.launcher.widgets.WIDGET_GRID_ROWS
import com.aibox.launcher.widgets.WidgetItem
import com.aibox.launcher.widgets.WidgetSizeClass
import com.aibox.launcher.widgets.WidgetType
import com.aibox.launcher.widgets.WidgetView
import com.aibox.launcher.widgets.WidgetWeatherInfo
import kotlin.math.roundToInt

private val BoardAccent = Color(0xFF0A84FF)

/**
 * Новая система рабочего стола: сетка 6×10, виджеты 2×2 / 4×2 / 4×4 / 6×4,
 * свободное перетаскивание пальцем и крупные кнопки «+» / «−» для смены размера.
 */
@Composable
fun WidgetBoard(
    items: List<WidgetItem>,
    editMode: Boolean,
    weather: WidgetWeatherInfo,
    onEnterEdit: () -> Unit,
    onMove: (String, Int, Int) -> Unit,
    onResize: (String, WidgetSizeClass) -> Unit,
    onRemove: (String) -> Unit,
    onAdd: () -> Unit,
    onOpenWidget: (WidgetType) -> Unit,
    topPadding: Dp,
    bottomPadding: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = topPadding,
                bottom = bottomPadding
            )
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { if (!editMode) onEnterEdit() })
            }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cellWidth = maxWidth * (1f / WIDGET_GRID_COLUMNS)
            val cellHeight = maxHeight * (1f / WIDGET_GRID_ROWS)
            val cell = minOf(cellWidth, cellHeight)
            val gridWidth = cell * WIDGET_GRID_COLUMNS.toFloat()
            val gridHeight = cell * WIDGET_GRID_ROWS.toFloat()
            val originX = (maxWidth - gridWidth) * 0.5f
            val originY = (maxHeight - gridHeight) * 0.5f
            val gap = 6.dp
            val density = LocalDensity.current

            items.forEachIndexed { index, item ->
                val width = cell * item.size.columns.toFloat() - gap
                val height = cell * item.size.rows.toFloat() - gap
                WidgetBoardCell(
                    item = item,
                    index = index,
                    width = width,
                    height = height,
                    originX = originX + cell * item.column.toFloat() + gap / 2f,
                    originY = originY + cell * item.row.toFloat() + gap / 2f,
                    cellPx = with(density) { cell.toPx().coerceAtLeast(1f) },
                    editMode = editMode,
                    onEnterEdit = onEnterEdit,
                    onMove = onMove,
                    onResize = onResize,
                    onRemove = onRemove,
                    weather = weather,
                    onOpenWidget = onOpenWidget
                )
            }

            if (editMode) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 10.dp)
                        .size(52.dp)
                        .clip(CircleShape)
                        .glassSurface(CircleShape, strength = 1.1f)
                        .background(BoardAccent.copy(alpha = 0.55f))
                        .clickable(onClick = onAdd)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Добавить виджет",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetBoardCell(
    item: WidgetItem,
    index: Int,
    width: Dp,
    height: Dp,
    originX: Dp,
    originY: Dp,
    cellPx: Float,
    editMode: Boolean,
    onEnterEdit: () -> Unit,
    onMove: (String, Int, Int) -> Unit,
    onResize: (String, WidgetSizeClass) -> Unit,
    onRemove: (String) -> Unit,
    weather: WidgetWeatherInfo,
    onOpenWidget: (WidgetType) -> Unit
) {
    val wiggle = rememberBoardWiggle(editMode, index)
    var drag by remember(item.id) { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val currentEditMode by rememberUpdatedState(editMode)
    val currentEnterEdit by rememberUpdatedState(onEnterEdit)
    val currentMove by rememberUpdatedState(onMove)
    val widthPx = with(density) { width.toPx().coerceAtLeast(1f) }
    val heightPx = with(density) { height.toPx().coerceAtLeast(1f) }

    Box(
        modifier = Modifier
            .offset(
                x = originX + with(density) { drag.x.toDp() },
                y = originY + with(density) { drag.y.toDp() }
            )
            .size(width, height)
            .graphicsLayer { rotationZ = wiggle.value }
            .pointerInput(item.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    val startTime = System.currentTimeMillis()
                    val start = down.position
                    val reservedZone = with(density) { 40.dp.toPx() }
                    val resizeZone = with(density) { 96.dp.toPx() }
                    val reserved = currentEditMode && (
                        (start.x <= reservedZone && start.y <= reservedZone) ||
                            (start.x >= widthPx - reservedZone && start.y >= heightPx - resizeZone)
                        )
                    if (reserved) {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                        }
                        return@awaitEachGesture
                    }

                    var dragging = false
                    var longPressed = false
                    var moved = Offset.Zero
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val distance = (change.position - start).getDistance()
                        if (!dragging && distance > viewConfiguration.touchSlop) dragging = true
                        if (dragging) {
                            change.consume()
                            val delta = change.position - change.previousPosition
                            moved += delta
                            drag = moved
                        } else if (!longPressed && distance <= viewConfiguration.touchSlop &&
                            System.currentTimeMillis() - startTime >= viewConfiguration.longPressTimeoutMillis
                        ) {
                            longPressed = true
                            currentEnterEdit()
                        }
                    }
                    if (dragging) {
                        drag = Offset.Zero
                        val deltaColumn = (moved.x / cellPx).roundToInt()
                        val deltaRow = (moved.y / cellPx).roundToInt()
                        if (deltaColumn != 0 || deltaRow != 0) {
                            currentMove(item.id, item.column + deltaColumn, item.row + deltaRow)
                        }
                    }
                }
            }
    ) {
        WidgetView(
            item = item,
            weather = weather,
            onOpenWidget = onOpenWidget,
            modifier = Modifier.fillMaxSize()
        )

        if (editMode) {
            // Поглощаем тапы в режиме правки, чтобы виджет не открывал приложение.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            )
            BoardBadge(
                icon = Icons.Rounded.Close,
                color = Color(0xFFFF453A),
                description = "Удалить виджет",
                onClick = { onRemove(item.id) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-6).dp, y = (-6).dp)
            )
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 6.dp, y = 6.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.size != WidgetSizeClass.EXTRA_LARGE) BoardAccent else Color(0x660A84FF)
                        )
                        .clickable(enabled = item.size != WidgetSizeClass.EXTRA_LARGE) {
                            onResize(item.id, resizeStep(item.size, 1))
                        }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Увеличить виджет",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.size != WidgetSizeClass.SMALL) BoardAccent else Color(0x660A84FF)
                        )
                        .clickable(enabled = item.size != WidgetSizeClass.SMALL) {
                            onResize(item.id, resizeStep(item.size, -1))
                        }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "Уменьшить виджет",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Text(
                text = item.size.title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
                    .background(Color(0xAA000000), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

private fun resizeStep(size: WidgetSizeClass, delta: Int): WidgetSizeClass {
    val values = WidgetSizeClass.entries
    val index = values.indexOf(size).coerceAtLeast(0)
    return values[(index + delta).coerceIn(0, values.lastIndex)]
}

@Composable
private fun BoardBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun rememberBoardWiggle(enabled: Boolean, phase: Int): State<Float> {
    val still = remember { mutableFloatStateOf(0f) }
    if (!enabled) return still
    val transition = rememberInfiniteTransition(label = "widgetWiggle")
    return transition.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset((phase * 31) % 150)
        ),
        label = "widgetWiggleAngle"
    )
}
