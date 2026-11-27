package com.aibox.launcher.dock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aibox.launcher.core.model.InstalledApp
import com.aibox.launcher.core.ui.glassSurface
import com.aibox.launcher.icons.AppIconPreview
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.round

/**
 * Горизонтальный док в стеклянной капсуле, в одном ряду с капсулой горячих клавиш.
 *
 * Карусель занимает всю доступную ширину слева, а справа стоят две кнопки:
 * «Все приложения» и «Настройки». Они не перекрывают иконки карусели.
 * Прокрутка свободная, с инерцией; центральная иконка пружинит «вперёд» внутри стеклянной капсулы.
 */
@Composable
fun CarouselDockBar(
    apps: List<InstalledApp>,
    onLaunchApp: (InstalledApp) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = DockDefaults.DEFAULT_SCALE,
    uiScale: Float = 1f
) {
    val density = LocalDensity.current
    val safeUiScale = uiScale.coerceAtLeast(0.01f)
    val iconSize = DockDefaults.iconSize(scale, uiScale)
    val buttonSize = iconSize * 0.86f

    // Увеличили зазор: иконки больше не слипаются.
    val iconGap = 16.dp / safeUiScale
    val stepPx = with(density) { (iconSize + iconGap).toPx() }
    // Глубина пружины «вперёд»: тень под центральной иконкой, а не сдвиг вверх.
    val depthPx = with(density) { (12.dp / safeUiScale).toPx() }
    val iconHalfPx = with(density) { iconSize.toPx() } / 2f
    val count = apps.size.coerceAtLeast(1)
    val cyclePx = stepPx * count

    val scroll = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val scrollState by remember { derivedStateOf { scroll.value } }
    var viewportPx by remember { mutableIntStateOf(1) }
    var lastDragAt by remember { mutableLongStateOf(0L) }
    var lastDragTime by remember { mutableLongStateOf(0L) }
    var dragVelocity by remember { mutableFloatStateOf(0f) }

    // Бесконечный круг приводится к одному циклу только для расчёта положений.
    val normalized = scrollState - floor(scrollState / cyclePx) * cyclePx
    // Стеклянная оболочка едет вместе с иконкой, ближайшей к центру карусели.
    val shellShiftPx = round(-normalized / stepPx) * stepPx + normalized

    fun wrap(value: Float): Float {
        val wrapped = value % cyclePx
        return if (wrapped < 0f) wrapped + cyclePx else wrapped
    }

    fun finishDrag() {
        val velocity = dragVelocity
        lastDragAt = System.currentTimeMillis()
        scope.launch {
            // Полноценный разгон: прокрутка не ограничена одним шагом.
            if (abs(velocity) > 25f) {
                scroll.animateDecay(velocity, exponentialDecay())
            }
            scroll.snapTo(wrap(scroll.value))
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Стеклянная капсула рисуется отдельным слоем и не обрезает увеличенную иконку.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .glassSurface(RoundedCornerShape(percent = 50))
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp / safeUiScale),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    // Карусель обрезается по краям дока: иконки не заходят под кнопки справа.
                    .clip(RoundedCornerShape(percent = 50))
                    .onSizeChanged { viewportPx = it.width.coerceAtLeast(1) }
                    .pointerInput(apps.size) {
                        detectDragGestures(
                            onDragStart = {
                                dragVelocity = 0f
                                lastDragTime = 0L
                            },
                            onDragEnd = { finishDrag() },
                            onDragCancel = { finishDrag() }
                        ) { change, amount ->
                            change.consume()
                            val now = System.currentTimeMillis()
                            if (lastDragTime != 0L) {
                                val dt = (now - lastDragTime).coerceAtLeast(1L)
                                val instant = amount.x / dt * 1000f
                                dragVelocity = dragVelocity * 0.65f + instant * 0.35f
                            }
                            lastDragTime = now
                            scope.launch { scroll.snapTo(scroll.value + amount.x) }
                        }
                    }
            ) {
                // Стеклянная капсула сопровождает выбранную иконку во время прокрутки.
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = with(density) { shellShiftPx.toDp() })
                        .size(iconSize * 1.18f)
                        .glassSurface(CircleShape, strength = 1.15f)
                )

                apps.forEachIndexed { index, app ->
                    (-2..2).forEach { repeat ->
                        val relativePx = index * stepPx + normalized + repeat * cyclePx
                        if (abs(relativePx) <= viewportPx / 2f + iconHalfPx * 1.5f) {
                            val distance = (abs(relativePx) / stepPx).coerceIn(0f, 1f)
                            // Пружина центральной иконки «вперёд»: рост и тень глубины вместо сдвига вверх.
                            val scaleByDistance by animateFloatAsState(
                                targetValue = 1.30f - 0.30f * distance,
                                animationSpec = spring(
                                    dampingRatio = 0.58f,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "dockIconScale"
                            )
                            // У самого края иконка плавно исчезает, а не обрезается на границе дока.
                            val edgeAlpha = ((viewportPx / 2f - abs(relativePx)) / (iconHalfPx * 1.2f))
                                .coerceIn(0f, 1f)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .offset(x = with(density) { relativePx.toDp() })
                                    .size(iconSize)
                                    .graphicsLayer {
                                        scaleX = scaleByDistance
                                        scaleY = scaleByDistance
                                        shape = CircleShape
                                        shadowElevation = (1f - distance) * depthPx
                                        alpha = (1f - 0.5f * distance) * edgeAlpha
                                    }
                                    .clickable {
                                        if (System.currentTimeMillis() - lastDragAt > 250L) {
                                            onLaunchApp(app)
                                        }
                                    }
                            ) {
                                AppIconPreview(app.packageName, app.label, iconSize)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.width(10.dp / safeUiScale))
            DockActionButton(
                icon = Icons.Rounded.Apps,
                description = "Все приложения",
                size = buttonSize,
                onClick = onOpenDrawer
            )
            Spacer(Modifier.width(8.dp / safeUiScale))
            DockActionButton(
                icon = Icons.Rounded.Settings,
                description = "Настройки",
                size = buttonSize,
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun DockActionButton(
    icon: ImageVector,
    description: String,
    size: Dp,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(DockDefaults.HotkeyButtonBackground)
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = DockDefaults.HotkeyIconTint,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}
