package com.aibox.launcher.dock

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ViewCarousel
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aibox.launcher.core.ui.glassSurface

@Composable
fun SystemNavigationCapsule(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onRecents: () -> Unit,
    modifier: Modifier = Modifier,
    scale: Float = DockDefaults.DEFAULT_SCALE,
    uiScale: Float = 1f
) {
    val panelHeight = DockDefaults.panelHeight(scale)
    // Иконки не зависят от общего масштаба интерфейса.
    val buttonSize = panelHeight * 0.62f / uiScale.coerceAtLeast(0.01f)
    val shape = RoundedCornerShape(panelHeight * 0.38f)
    Row(
        horizontalArrangement = Arrangement.spacedBy(panelHeight * 0.14f),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(panelHeight)
            .clip(shape)
            .background(Color(0x40080A10))
            .glassSurface(shape)
            .padding(horizontal = panelHeight * 0.2f)
    ) {
        CapsuleButton(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", buttonSize, onBack)
        CapsuleButton(Icons.Rounded.Home, "Домой", buttonSize, onHome)
        CapsuleButton(Icons.Rounded.ViewCarousel, "Недавние", buttonSize, onRecents)
    }
}

@Composable
private fun CapsuleButton(
    icon: ImageVector,
    contentDescription: String,
    buttonSize: Dp,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(buttonSize)
            .clip(CircleShape)
            .background(Color(0x26FFFFFF))
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(buttonSize * 0.5f)
        )
    }
}
