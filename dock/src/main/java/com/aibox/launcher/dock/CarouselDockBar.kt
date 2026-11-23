package com.aibox.launcher.dock

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

data class DockSlot(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val accent: Color,
    val onClick: () -> Unit
)

@Composable
fun CarouselDockBar(
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var focusedIndex by remember { mutableIntStateOf(2) }
    val listState = rememberLazyListState()

    val slots = remember {
        listOf(
            DockSlot("nav", "Навигация", Icons.Rounded.Navigation, Color(0xFF0A84FF)) {},
            DockSlot("music", "Музыка", Icons.Rounded.MusicNote, Color(0xFFFF375F)) {},
            DockSlot("drawer", "Приложения", Icons.Rounded.Apps, Color(0xFF5E5CE6), onOpenDrawer),
            DockSlot("phone", "Связь", Icons.Rounded.Phone, Color(0xFF30D158)) {},
            DockSlot("settings", "Настройки", Icons.Rounded.Settings, Color(0xFF8E8E93), onOpenSettings)
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0x661C1C1E))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(32.dp))
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(slots) { index, slot ->
                val distance = abs(index - focusedIndex)
                val targetScale = when (distance) {
                    0 -> 1.18f
                    1 -> 0.96f
                    else -> 0.84f
                }
                val targetAlpha = when (distance) {
                    0 -> 1.0f
                    1 -> 0.82f
                    else -> 0.62f
                }

                val scale by animateFloatAsState(
                    targetValue = targetScale,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "dockScale"
                )
                val alpha by animateFloatAsState(
                    targetValue = targetAlpha,
                    label = "dockAlpha"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            this.alpha = alpha
                        }
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            focusedIndex = index
                            slot.onClick()
                        }
                        .padding(6.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(slot.accent)
                    ) {
                        Icon(
                            imageVector = slot.icon,
                            contentDescription = slot.title,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = slot.title,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
