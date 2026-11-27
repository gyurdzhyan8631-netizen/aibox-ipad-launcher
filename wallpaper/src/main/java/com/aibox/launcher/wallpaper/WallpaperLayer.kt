package com.aibox.launcher.wallpaper

import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.aibox.launcher.core.model.WallpaperType
import java.io.File

/**
 * Слой обоев. Оставлены только два источника: видео файл и системные обои Android.
 * Для SYSTEM лаунчер рисует прозрачный слой — системные обои видны через окно.
 */
@Composable
fun WallpaperLayer(
    type: WallpaperType,
    path: String,
    modifier: Modifier = Modifier
) {
    val fileOk = path.isNotEmpty() && File(path).exists()
    when {
        type == WallpaperType.VIDEO && fileOk -> VideoWallpaper(path, modifier)
        else -> Box(modifier = modifier.fillMaxSize())
    }
}

@Composable
fun VideoWallpaper(path: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                VideoView(context).apply {
                    setVideoPath(path)
                    setOnPreparedListener { player ->
                        player.isLooping = true
                        player.setVolume(0f, 0f)
                        start()
                    }
                }
            },
            onRelease = { view -> view.stopPlayback() }
        )
        Box(modifier = Modifier.fillMaxSize().background(Color(0x33000000)))
    }
}
