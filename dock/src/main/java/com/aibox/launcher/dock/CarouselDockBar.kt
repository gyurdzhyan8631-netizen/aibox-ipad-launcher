package com.aibox.launcher.dock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.model.InstalledApp
import com.aibox.launcher.icons.AppIconView

/**
 * Нижний док.
 * Слева закреплена кнопка «Все приложения» (не прокручивается и открывает табло приложений),
 * затем прокручиваемый ряд всех приложений, справа кнопка размера панелей.
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
            title = "Все",
            accent = Color(0xFF5E5CE6),
            iconSize = iconSize,
            onClick = onOpenDrawer
        )
        DockDivider(iconSize)

        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(apps, key = { it.packageName }) { app ->
                AppIconView(
                    app = app,
                    onClick = { onLaunchApp(app) },
                    iconSize = iconSize
                )
            }
        }

        DockDivider(iconSize)
        DockShortcut(
            icon = Icons.Rounded.Tune,
            title = "Размер",
            accent = Color(0xFF8E8E93),
            iconSize = iconSize,
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
    title: String,
    accent: Color,
    iconSize: Dp,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
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
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(iconSize * 0.55f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(maxOf(iconSize + 16.dp, 52.dp))
        )
    }
}
