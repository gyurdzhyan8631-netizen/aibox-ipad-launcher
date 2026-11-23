package com.aibox.launcher.dock

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.maxOf
import com.aibox.launcher.core.model.InstalledApp
import com.aibox.launcher.icons.AppIconView

/**
 * Нижний док:
 * - Слева закреплена кнопка «Все приложения»
 * - Затем прокручиваемый ряд иконок приложений с пружинным увеличением активной/центральной иконки
 *   и лёгким затемнением соседних при прокрутке
 * - Справа кнопка размера панелей
 * - Названия под иконками полностью убраны
 */
@Composable
fun CarouselDockBar(
    apps: List<InstalledApp>,
    onLaunchApp: (InstalledApp) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = DockDefaults.DEFAULT_SCALE
) {
    val panelHeight = DockDefaults.panelHeight(scale)
    val iconSize = maxOf(panelHeight - 40.dp, 26.dp)
    val shape = RoundedCornerShape(panelHeight * 0.38f)

    val listState = rememberLazyListState()
    var selectedPackageName by remember { mutableStateOf<String?>(null) }

    val centeredIndex by remember(listState, apps.size) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty() || apps.isEmpty()) {
                0
            } else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                layoutInfo.visibleItemsInfo.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2f
                    kotlin.math.abs(itemCenter - viewportCenter)
                }?.index ?: 0
            }
        }
    }

    val isScrolling by remember {
        derivedStateOf { listState.isScrollInProgress }
    }

    androidx.compose.foundation.layout.Row(
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
            contentDescription = "Все",
            accent = Color(0xFF5E5CE6),
            iconSize = iconSize,
            isSelected = false,
            onClick = onOpenDrawer
        )

        DockDivider(iconSize)

        LazyRow(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                val isCentered = index == centeredIndex
                val isSelected = (selectedPackageName == app.packageName) || (!isScrolling && isCentered)
                val neighborDistance = kotlin.math.abs(index - centeredIndex)

                val targetScale = when {
                    isSelected -> 1.18f
                    isCentered -> 1.10f
                    neighborDistance == 1 -> 0.94f
                    else -> 0.88f
                }

                val targetDimAlpha = when {
                    isSelected -> 1f
                    isCentered && !isScrolling -> 1f
                    neighborDistance == 1 -> if (isScrolling) 0.62f else 0.78f
                    else -> if (isScrolling) 0.45f else 0.65f
                }

                val animatedScale by animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "dock_icon_scale"
                )

                val animatedAlpha by animateFloatAsState(
                    targetValue = targetDimAlpha,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "dock_icon_alpha"
                )

                val darkOverlayAlpha = (1f - animatedAlpha).coerceIn(0f, 0.55f) * 0.65f

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(iconSize * 1.22f)
                        .scale(animatedScale)
                        .alpha(animatedAlpha)
                ) {
                    // Обрезаем нижнюю часть на случай, если AppIconView рисует подпись под иконкой
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(iconSize)
                            .clip(RoundedCornerShape(iconSize * 0.22f))
                    ) {
                        AppIconView(
                            app = app,
                            onClick = {
                                selectedPackageName = app.packageName
                                onLaunchApp(app)
                            },
                            iconSize = iconSize
                        )
                    }

                    if (darkOverlayAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .size(iconSize)
                                .clip(RoundedCornerShape(iconSize * 0.22f))
                                .background(Color.Black.copy(alpha = darkOverlayAlpha))
                        )
                    }
                }
            }
        }

        DockDivider(iconSize)

        DockShortcut(
            icon = Icons.Rounded.Tune,
            contentDescription = "Размер",
            accent = Color(0xFF8E8E93),
            iconSize = iconSize,
            isSelected = false,
            onClick = onOpenSettings
        )
    }
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
    contentDescription: String,
    accent: Color,
    iconSize: Dp,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "dock_shortcut_scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(iconSize)
            .scale(scale)
            .clip(RoundedCornerShape(iconSize * 0.22f))
            .background(accent)
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier
                .fillMaxSize()
                .padding(iconSize * 0.22f)
        )
    }
}
