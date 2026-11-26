package com.aibox.launcher.widgets

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.view.KeyEvent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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

/** Данные активного плеера, которые показывает виджет. */
data class MediaPlayerInfo(
    val title: String = "Музыка",
    val artist: String = "",
    val appLabel: String = "",
    val art: Bitmap? = null,
    val playing: Boolean = false
)

private fun readPlayerInfo(
    context: Context,
    controller: MediaController?,
    audioActive: Boolean
): MediaPlayerInfo {
    val metadata = controller?.metadata
    val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        ?.takeIf { it.isNotBlank() }
        ?: "Музыка"
    val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
        ?.takeIf { it.isNotBlank() }
        ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)
        ?: ""
    val art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
    val appLabel = controller?.packageName?.let { pkg ->
        runCatching {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(pkg, 0)
            ).toString()
        }.getOrDefault(pkg)
    } ?: ""
    val playing = controller?.playbackState?.state == PlaybackState.STATE_PLAYING || audioActive
    return MediaPlayerInfo(title, artist, appLabel, art, playing)
}

/**
 * Виджет музыки: показывает активный плеер (обложка, трек, исполнитель, приложение)
 * и кнопки предыдущий / пауза / следующий. Нажатие на карточку открывает плеер.
 */
@Composable
fun MusicWidget(
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val tokens = LocalLauncherTokens.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    var info by remember { mutableStateOf(MediaPlayerInfo()) }

    LaunchedEffect(Unit) {
        val sessions = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
        while (true) {
            val active = try {
                sessions?.getActiveSessions(null).orEmpty()
            } catch (_: SecurityException) {
                emptyList()
            }
            val controller = active.firstOrNull {
                it.playbackState?.state == PlaybackState.STATE_PLAYING
            } ?: active.firstOrNull()
            info = readPlayerInfo(context, controller, audio?.isMusicActive == true)
            delay(1_500L)
        }
    }

    GlassCard(modifier = modifier.clickable(onClick = onClick ?: onOpenPlayer)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(tokens.accent.copy(alpha = 0.16f))
                ) {
                    val art = info.art
                    if (art != null) {
                        Image(
                            bitmap = art.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = tokens.accent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = info.title,
                        color = tokens.textPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val details = listOf(info.appLabel, info.artist)
                        .filter { it.isNotEmpty() }
                        .joinToString(" · ")
                        .ifEmpty { "Активный плеер" }
                    Text(
                        text = details,
                        color = tokens.textSecondary,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = if (info.playing) "Воспроизводится" else "Не исполняется",
                color = tokens.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                MusicButton(Icons.Rounded.SkipPrevious, "Предыдущий") {
                    sendMediaKey(audio, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                }
                MusicButton(if (info.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Пауза / играть") {
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
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = description, tint = tokens.textPrimary, modifier = Modifier.size(24.dp))
    }
}
