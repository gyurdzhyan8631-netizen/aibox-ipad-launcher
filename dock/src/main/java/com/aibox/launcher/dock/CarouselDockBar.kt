package com.aibox.launcher.dock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aibox.launcher.core.model.InstalledApp
import com.aibox.launcher.core.ui.glassSurface
import com.aibox.launcher.icons.AppIconPreview
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Горизонтальный док-карусель в одном ряду с капсулой горячих клавиш.
 *
 * Все приложения крутятся по замкнутому кругу. Центральная иконка чуть больше и «выезжает»
 * вперёд пружиной, соседние тускнеют. Подписей нет. Слева — отдельная кнопка «Все приложения».
 */
@Composable
fun CarouselDockBar(
    apps: List<InstalledApp>,
    onLaunchApp: (InstalledApp) -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = DockDefaults.DEFAULT_SCALE,
    uiScale: Float = 1f
) {
    val density = LocalDensity.current
    val safeUiScale = uiScale.coerceAtLeast(0.01f)
    // Высота дока равна высоте капсулы горячих клавиш.
    val panelHeight = DockDefaults.panelHeight(scale)
    val iconSize = DockDefaults.iconSize(scale, uiScale)
    val shape = RoundedCornerShape(panelHeight * 0.38f)

    val stepPx = with(density) { (iconSize + (6.dp / safeUiScale)).toPx() }
    val forwardPx = with(density) { (8.dp / safeUiScale).toPx() }
    val count = apps.size.coerceAtLeast(1)
    val cyclePx = stepPx * count
    val snapSpec = spring<Float>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)

    val scroll = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val scrollState by remember { derivedStateOf { scroll.value } }
    var viewportPx by remember { mutableIntStateOf(1) }
    var lastDragAt by remember { mutableLongStateOf(0L) }

    // Бесконечная прокрутка приводится к одному циклу, чтобы иконки не уезжали за экран.
    val normalized = scrollState - floor(scrollState / cyclePx) * cyclePx

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { viewportPx = it.width.coerceAtLeast(1) }
            .clip(shape)
            .background(Color(0x40080A10))
            .glassSurface(shape)
            .pointerInput(apps.size) {
                detectDragGestures(
                    onDragEnd = {
                        // Пружина доводит ближайшую иконку до центра карусели.
                        lastDragAt = System.currentTimeMillis()
                        scope.launch {
                            val target = (scroll.value / stepPx).roundToInt() * stepPx
                            scroll.animateTo(target, snapSpec)
                            scroll.snapTo(((target % cyclePx) + cyclePx) % cyclePx)
                        }
                    },
                    onDragCancel = {
                        lastDragAt = System.currentTimeMillis()
                        scope.launch {
                            val target = (scroll.value / stepPx).roundToInt() * stepPx
                            scroll.animateTo(target, snapSpec)
                            scroll.snapTo(((target % cyclePx) + cyclePx) % cyclePx)
                        }
                    }
                ) { change, amount ->
                    change.consume()
                    scope.launch { scroll.snapTo(scroll.value + amount.x) }
                }
            }
    ) {
        // Отдельная кнопка всех приложений не участвует в карусели.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 10.dp / safeUiScale)
                .size(iconSize)
                .clip(CircleShape)
                .background(Color(0x33FFFFFF))
                .clickable(onClick = onOpenDrawer)
        ) {
            Icon(
                imageVector = Icons.Rounded.Apps,
                contentDescription = "Все приложения",
                tint = Color.White,
                modifier = Modifier.size(iconSize * 0.5f)
            )
        }

        apps.forEachIndexed { index, app ->
            // Несколько копий замыкают круг: соседи появляются заранее слева и справа.
            (-2..2).forEach { repeat ->
                val relativePx = index * stepPx + normalized + repeat * cyclePx
                if (abs(relativePx) <= viewportPx / 2f + stepPx) {
                    val distance = (abs(relativePx) / stepPx).coerceIn(0f, 1f)
                    val scaleByDistance by animateFloatAsState(
                        targetValue = 1.22f - 0.22f * distance,
                        animationSpec = spring(
                            dampingRatio = 0.58f,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "dockIconScale"
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(x = with(density) { relativePx.toDp() })
                            .size(iconSize)
                            .graphicsLayer {
                                scaleX = scaleByDistance
                                scaleY = scaleByDistance
                                // Центральная иконка чуть выдвигается из капсулы вверх.
                                translationY = -(1f - distance) * forwardPx
                                alpha = 1f - 0.5f * distance
                            }
                            .clickable {
                                if (System.currentTimeMillis() - lastDragAt > 250L) onLaunchApp(app)
                            }
                    ) {
                        AppIconPreview(app.packageName, app.label, iconSize)
                    }
                }
            }
        }
    }
}
