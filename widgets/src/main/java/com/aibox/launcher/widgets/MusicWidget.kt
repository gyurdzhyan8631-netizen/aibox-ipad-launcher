package com.aibox.launcher.widgets

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.ui.theme.LocalLauncherTokens
import kotlinx.coroutines.delay

private fun sendMediaKey(audio: AudioManager?, keyCode: Int) {
    if (audio == null) return
    audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
    audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
}

/** Виджет музыки: предыдущий / пауза / следующий. Нажатие на заголовок открывает плеер. */
@Composable
fun MusicWidget(
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val tokens = LocalLauncherTokens.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    var playing by remember { mutableStateOf(audio?.isMusicActive == true) }

    LaunchedEffect(Unit) {
        while (true) {
            playing = audio?.isMusicActive == true
            delay(1_500L)
        }
    }

    GlassCard(modifier = modifier.clickable(onClick = onClick ?: onOpenPlayer)) {
        Column(verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(
                    text = "Музыка",
                    color = tokens.accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (playing) "Воспроизводится" else "Не исполняется",
                    color = tokens.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                MusicButton(Icons.Rounded.SkipPrevious, "Предыдущий") {
                    sendMediaKey(audio, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                }
                MusicButton(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Пауза / играть") {
                    sendMediaKey(audio, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
                }
                MusicButton(Icons.Rounded.SkipNext, "Следующий") {
                    sendMediaKey(audio, KeyEvent.KEYCODE_MEDIA_NEXT)
                }
            }
        }
    }
}

@Composable
private fun MusicButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val tokens = LocalLauncherTokens.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = description, tint = tokens.textPrimary, modifier = Modifier.size(28.dp))
    }
}
