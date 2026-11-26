package com.aibox.launcher.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.ui.theme.LocalLauncherTokens

/** Информация, которая показывается на виджетах-ярлыках. */
data class ShortcutInfo(
    val appLabel: String = "",
    val place: String = "",
    val note: String = ""
)

/** Виджет навигатора: нажатие открывает выбранное приложение. */
@Composable
fun NavigationWidget(
    info: ShortcutInfo = ShortcutInfo(),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLauncherTokens.current
    GlassCard(modifier = modifier.clickable(onClick = onClick)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Navigation,
                    contentDescription = null,
                    tint = tokens.accent,
                    modifier = Modifier.size(34.dp)
                )
                Column {
                    Text(
                        text = "Навигация",
                        color = tokens.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = info.appLabel.ifEmpty { "Навигатор" },
                        color = tokens.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column {
                Text(
                    text = if (info.place.isEmpty()) "Город не указан" else "Город: ${info.place}",
                    color = tokens.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = info.note.ifEmpty { "Нажмите для маршрута" },
                    color = tokens.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Виджет YouTube: нажатие открывает выбранное видеоприложение. */
@Composable
fun VideoWidget(
    info: ShortcutInfo = ShortcutInfo(),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLauncherTokens.current
    GlassCard(modifier = modifier.clickable(onClick = onClick)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = tokens.accent,
                    modifier = Modifier.size(34.dp)
                )
                Column {
                    Text(
                        text = "YouTube",
                        color = tokens.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = info.appLabel.ifEmpty { "Видео" },
                        color = tokens.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = info.note.ifEmpty { "Открыть видео" },
                color = tokens.textSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
