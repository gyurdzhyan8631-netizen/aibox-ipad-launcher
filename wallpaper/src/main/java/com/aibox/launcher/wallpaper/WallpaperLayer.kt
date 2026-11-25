package com.aibox.launcher.wallpaper

import android.graphics.BitmapFactory
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import com.aibox.launcher.core.model.DayPeriod
import com.aibox.launcher.core.model.WallpaperTheme
import com.aibox.launcher.core.model.WallpaperType
import com.aibox.launcher.core.model.WeatherCondition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * Слой обоев: тема-градиент, свои фото, живые обои (видео) или системные обои Android.
 * Для SYSTEM лаунчер ничего не рисует — обои видны через прозрачное окно.
 */
@Composable
fun WallpaperLayer(
    type: WallpaperType,
    path: String,
    dayPeriod: DayPeriod,
    weather: WeatherCondition,
    theme: WallpaperTheme,
    modifier: Modifier = Modifier,
    nowMinutes: Int = 720,
    sunriseMin: Int = 390,
    sunsetMin: Int = 1110,
    precipitation: Boolean = true
) {
    val fileOk = path.isNotEmpty() && File(path).exists()
    when {
        type == WallpaperType.IMAGE && fileOk -> ImageWallpaper(path, modifier)
        type == WallpaperType.VIDEO && fileOk -> VideoWallpaper(path, modifier)
        type == WallpaperType.LIVE_SKY -> LiveSkyWallpaper(
            nowMinutes = nowMinutes,
            sunriseMin = sunriseMin,
            sunsetMin = sunsetMin,
            weather = weather,
            theme = theme,
            precipitation = precipitation,
            modifier = modifier
        )
        type == WallpaperType.SYSTEM -> Box(modifier = modifier.fillMaxSize())
        else -> DynamicWeatherWallpaper(dayPeriod, weather, modifier, theme)
    }
}

private fun decodeSampled(path: String, maxSize: Int): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / sample > maxSize) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    BitmapFactory.decodeFile(path, options)?.asImageBitmap()
}.getOrNull()

@Composable
fun ImageWallpaper(path: String, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key1 = path) {
        value = withContext(Dispatchers.IO) { decodeSampled(path, 2560) }
    }
    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        // лёгкое затемнение, чтобы белый текст читался на любых фото
        Box(modifier = Modifier.fillMaxSize().background(Color(0x33000000)))
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
