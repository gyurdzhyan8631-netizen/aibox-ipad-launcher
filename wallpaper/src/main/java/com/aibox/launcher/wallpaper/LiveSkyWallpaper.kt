package com.aibox.launcher.wallpaper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.aibox.launcher.core.model.WallpaperTheme
import com.aibox.launcher.core.model.WeatherCondition
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

private fun frac(x: Float): Float = x - floor(x)

/** Цвета неба (верх, низ) по времени суток. Для фиксированной темы берутся цвета темы. */
private fun skyColors(minutes: Int, sunrise: Int, sunset: Int, theme: WallpaperTheme): Pair<Color, Color> {
    wallpaperThemeColors(theme)?.let { return it }

    val nightTop = Color(0xFF050814)
    val nightBottom = Color(0xFF111A30)
    val twilightTop = Color(0xFF3A3F7A)
    val twilightBottom = Color(0xFFFF9F68)
    val dayTop = Color(0xFF1E62B5)
    val dayBottom = Color(0xFF78B8FF)

    val m = minutes.toFloat()
    val w = 45f
    // 0 — ночь, 0.5 — сумерки, 1 — день
    val x = when {
        m < sunrise - w -> 0f
        m < sunrise + w -> (m - (sunrise - w)) / (2 * w)
        m < sunset - w -> 1f
        m < sunset + w -> 1f - (m - (sunset - w)) / (2 * w)
        else -> 0f
    }
    return if (x < 0.5f) {
        val f = x * 2f
        lerp(nightTop, twilightTop, f) to lerp(nightBottom, twilightBottom, f)
    } else {
        val f = (x - 0.5f) * 2f
        lerp(twilightTop, dayTop, f) to lerp(twilightBottom, dayBottom, f)
    }
}

private fun cloudiness(weather: WeatherCondition): Float = when (weather) {
    WeatherCondition.CLEAR -> 0f
    WeatherCondition.WIND -> 0.2f
    WeatherCondition.CLOUDY -> 0.5f
    WeatherCondition.FOG -> 0.8f
    WeatherCondition.OVERCAST -> 0.9f
    WeatherCondition.RAIN -> 0.85f
    WeatherCondition.SNOW -> 0.85f
    WeatherCondition.HEAVY_RAIN -> 1f
    WeatherCondition.THUNDERSTORM -> 1f
}

private fun weatherTint(weather: WeatherCondition): Pair<Color, Float> = when (weather) {
    WeatherCondition.CLEAR -> Color.Black to 0f
    WeatherCondition.CLOUDY -> Color(0xFF8E9AAF) to 0.15f
    WeatherCondition.OVERCAST -> Color(0xFF6B7280) to 0.30f
    WeatherCondition.RAIN -> Color(0xFF475569) to 0.40f
    WeatherCondition.HEAVY_RAIN -> Color(0xFF334155) to 0.50f
    WeatherCondition.SNOW -> Color(0xFFCBD5E1) to 0.25f
    WeatherCondition.THUNDERSTORM -> Color(0xFF1E1B2E) to 0.55f
    WeatherCondition.FOG -> Color(0xFF94A3B8) to 0.35f
    WeatherCondition.WIND -> Color(0xFF7C8DA6) to 0.15f
}

/**
 * Живое небо: солнце и луна идут по дуге в реальном времени суток (по восходу и закату города),
 * облака плывут, а при дожде или снеге по экрану идут капли и снежинки.
 * Для экономии ресурсов: без осадков и облаков ничего не перерисовывается каждый кадр,
 * облака двигаются раз в четверть секунды, осадки — примерно 30 раз в секунду.
 */
@Composable
fun LiveSkyWallpaper(
    nowMinutes: Int,
    sunriseMin: Int,
    sunsetMin: Int,
    weather: WeatherCondition,
    theme: WallpaperTheme,
    precipitation: Boolean,
    modifier: Modifier = Modifier
) {
    val rain = weather == WeatherCondition.RAIN ||
        weather == WeatherCondition.HEAVY_RAIN ||
        weather == WeatherCondition.THUNDERSTORM
    val snow = weather == WeatherCondition.SNOW
    val precipActive = precipitation && (rain || snow)
    val clouds = cloudiness(weather)
    val tickMs = when {
        precipActive -> 33L
        clouds > 0.1f -> 250L
        else -> 0L
    }

    var seconds by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(tickMs) {
        if (tickMs > 0L) {
            while (true) {
                val nanos = withFrameNanos { it }
                seconds = (nanos % 3_600_000_000_000L) / 1_000_000_000f
                delay(tickMs)
            }
        }
    }

    val rise = if (sunsetMin > sunriseMin) sunriseMin else 390
    val set = if (sunsetMin > sunriseMin) sunsetMin else 1110
    val (baseTop, baseBottom) = skyColors(nowMinutes, rise, set, theme)
    val (tint, tintAmount) = weatherTint(weather)
    val top = lerp(baseTop, tint, tintAmount)
    val bottom = lerp(baseBottom, tint, tintAmount)

    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(listOf(top, bottom)))
        drawSkyBodies(nowMinutes, rise, set, clouds, seconds)
        if (clouds > 0.1f) drawClouds(clouds, nowMinutes, rise, set, seconds)
        if (precipActive) {
            if (snow) {
                drawSnow(seconds)
            } else {
                drawRain(seconds, heavy = weather != WeatherCondition.RAIN)
                if (weather == WeatherCondition.THUNDERSTORM && (seconds % 9f) < 0.12f) {
                    drawRect(Color.White.copy(alpha = 0.22f))
                }
            }
        }
        // лёгкое затемнение низа, чтобы док и подписи читались
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0x55000000))))
    }
}

private fun DrawScope.drawSkyBodies(minutes: Int, rise: Int, set: Int, clouds: Float, seconds: Float) {
    val w = size.width
    val h = size.height
    val horizon = h * 0.82f
    val arc = h * 0.70f
    val radius = min(w, h) * 0.055f
    val m = minutes.toFloat()
    val isDay = m in rise.toFloat()..set.toFloat()

    // --- звёзды ---
    val dist = when {
        m < rise -> rise - m
        m > set -> m - set
        else -> 0f
    }
    val nightFactor = (dist / 60f).coerceIn(0f, 1f)
    if (nightFactor > 0.05f && clouds < 0.8f) {
        val starAlphaBase = nightFactor * (1f - clouds)
        for (i in 0 until 50) {
            val x = frac(i * 0.618034f) * w
            val y = frac(i * 0.7548776f + 0.13f) * horizon * 0.85f
            val twinkle = 0.6f + 0.4f * sin(seconds * 0.8f + i)
            drawCircle(
                color = Color.White.copy(alpha = (starAlphaBase * (0.35f + frac(i * 0.31f) * 0.65f) * twinkle).coerceIn(0f, 1f)),
                radius = (1f + frac(i * 0.17f) * 1.4f).dp.toPx(),
                center = Offset(x, y)
            )
        }
    }

    // --- солнце или луна ---
    val progress: Float
    if (isDay) {
        progress = (m - rise) / (set - rise).toFloat()
    } else {
        val nightLength = (1440 - set + rise).toFloat()
        progress = if (m > set) (m - set) / nightLength else (m + 1440 - set) / nightLength
    }
    val cx = w * (0.10f + 0.80f * progress.coerceIn(0f, 1f))
    val cy = horizon - sin(PI.toFloat() * progress.coerceIn(0f, 1f)) * arc
    val visibility = (sin(PI.toFloat() * progress.coerceIn(0f, 1f)) * 1.6f).coerceIn(0f, 1f)
    val center = Offset(cx, cy)

    if (isDay) {
        val strength = visibility * (1f - clouds * 0.8f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFFFD36B).copy(alpha = 0.55f * strength), Color.Transparent),
                center = center,
                radius = radius * 4.5f
            ),
            radius = radius * 4.5f,
            center = center
        )
        drawCircle(Color(0xFFFFE9A6).copy(alpha = strength), radius, center)
    } else {
        val strength = visibility * (1f - clouds * 0.7f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFBFD0FF).copy(alpha = 0.35f * strength), Color.Transparent),
                center = center,
                radius = radius * 4f
            ),
            radius = radius * 4f,
            center = center
        )
        drawCircle(Color(0xFFEDEFF7).copy(alpha = strength), radius, center)
        // мягкие «моря» на диске
        drawCircle(Color(0xFFB9BED0).copy(alpha = 0.35f * strength), radius * 0.28f, center + Offset(-radius * 0.3f, -radius * 0.1f))
        drawCircle(Color(0xFFB9BED0).copy(alpha = 0.28f * strength), radius * 0.2f, center + Offset(radius * 0.35f, radius * 0.25f))
    }
}

private fun DrawScope.drawClouds(clouds: Float, minutes: Int, rise: Int, set: Int, seconds: Float) {
    val w = size.width
    val h = size.height
    val isDay = minutes in rise..set
    val color = lerp(if (isDay) Color.White else Color(0xFF9AA6C0), Color(0xFF8E9AAF), clouds * 0.5f)
    val count = 3 + (clouds * 5f).toInt()
    for (j in 0 until count) {
        val speed = 0.004f + frac(j * 0.29f) * 0.004f
        val cx = ((frac(j * 0.37f) * 1.4f + seconds * speed) % 1.4f - 0.2f) * w
        val cy = h * (0.10f + frac(j * 0.53f) * 0.38f)
        val size = w * (0.09f + frac(j * 0.71f) * 0.08f)
        val alpha = 0.10f + clouds * 0.20f
        drawCircle(color.copy(alpha = alpha), size, Offset(cx, cy))
        drawCircle(color.copy(alpha = alpha), size * 0.8f, Offset(cx + size * 0.9f, cy + size * 0.1f))
        drawCircle(color.copy(alpha = alpha), size * 0.7f, Offset(cx - size * 0.85f, cy + size * 0.15f))
        drawCircle(color.copy(alpha = alpha), size * 0.6f, Offset(cx + size * 0.2f, cy - size * 0.35f))
    }
}

private fun DrawScope.drawRain(seconds: Float, heavy: Boolean) {
    val w = size.width
    val h = size.height
    val count = if (heavy) 140 else 80
    val length = h * 0.04f
    val stroke = 1.5.dp.toPx()
    for (i in 0 until count) {
        val speed = 0.9f + frac(i * 0.337f) * 0.7f
        val y = frac(frac(i * 0.7548776f) + seconds * speed) * (h + length) - length
        val x = frac(i * 0.618034f) * w - y * 0.15f
        drawLine(
            color = Color(0xFFCFE3FF).copy(alpha = 0.45f),
            start = Offset(x, y),
            end = Offset(x - length * 0.15f, y + length),
            strokeWidth = stroke
        )
    }
}

private fun DrawScope.drawSnow(seconds: Float) {
    val w = size.width
    val h = size.height
    val sway = 14.dp.toPx()
    for (i in 0 until 70) {
        val speed = 0.06f + frac(i * 0.337f) * 0.08f
        val y = frac(frac(i * 0.7548776f) + seconds * speed) * h
        val x = frac(i * 0.618034f) * w + sin(seconds * 0.8f + i) * sway
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = (1.5f + frac(i * 0.173f) * 2.2f).dp.toPx(),
            center = Offset(x, y)
        )
    }
}
