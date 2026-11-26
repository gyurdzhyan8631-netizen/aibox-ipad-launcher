package com.aibox.launcher.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.ui.theme.LocalLauncherTokens

/** Виджет-ярлык навигатора: нажатие открывает выбранное приложение. */
@Composable
fun NavigationWidget(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = LocalLauncherTokens.current
    GlassCard(modifier = modifier.clickable(onClick = onClick)) {
        Column {
            Text(
                text = "Навигация",
                color = tokens.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = Icons.Rounded.Navigation,
                contentDescription = null,
                tint = tokens.textPrimary,
                modifier = Modifier.size(48.dp)
            )
            Text("Открыть навигатор", color = tokens.textSecondary, fontSize = 13.sp)
        }
    }
}

/** Виджет-ярлык YouTube: нажатие открывает выбранное видеоприложение. */
@Composable
fun VideoWidget(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = LocalLauncherTokens.current
    GlassCard(modifier = modifier.clickable(onClick = onClick)) {
        Column {
            Text(
                text = "YouTube",
                color = tokens.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = tokens.textPrimary,
                modifier = Modifier.size(48.dp)
            )
            Text("Открыть видео", color = tokens.textSecondary, fontSize = 13.sp)
        }
    }
}
