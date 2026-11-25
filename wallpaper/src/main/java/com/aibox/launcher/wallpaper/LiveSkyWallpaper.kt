package com.aibox.launcher.wallpaper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.aibox.launcher.core.model.WallpaperTheme
import com.aibox.launcher.core.model.WeatherCondition
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

private fun frac(x: Float): Float = x - floor(x)

/** Цвета неба (верх, горизонт) по времени суток; для фиксированной темы берутся цвета темы. */
private fun skyColors(minutes: Int, sunrise: Int, sunset: Int, theme: WallpaperTheme): Pair<Color, Color> {
    wallpaperThemeColors(theme)?.let { (top, bottom) -> return top to lerp(bottom, Color.White, 0.18f) }

    val nightTop = Color(0xFF070B1C)
    val nightHorizon = Color(0xFF1A2745)
    val twilightTop = Color(0xFF3C4A86)
    val twilightHorizon = Color(0xFFF2A27A)
    val dayTop = Color(0xFF4A78B6)
    val dayHorizon = Color(0xFFB7CFE8)

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
        lerp(nightTop, twilightTop, f) to lerp(nightHorizon, twilightHorizon, f)
    } else {
        val f = (x - 0.5f) * 2f
        lerp(twilightTop, dayTop, f) to lerp(twilightHorizon, dayHorizon, f)
    }
}

private fun cloudiness(weather: WeatherCondition): Float = when (weather) {
    WeatherCondition.CLEAR -> 0.12f
    WeatherCondition.WIND -> 0.25f
    WeatherCondition.CLOUDY -> 0.55f
    WeatherCondition.FOG -> 0.8f
    WeatherCondition.OVERCAST -> 0.9f
    WeatherCondition.RAIN -> 0.85f
    WeatherCondition.SNOW -> 0.85f
    WeatherCondition.HEAVY_RAIN -> 1f
    WeatherCondition.THUNDERSTORM -> 1f
}

private fun weatherTint(weather: WeatherCondition): Pair<Color, Float> = when (weather) {
    WeatherCondition.CLEAR -> Color.Black to 0f
    WeatherCondition.CLOUDY -> Color(0xFF8E9AAF) to 0.12f
    WeatherCondition.OVERCAST -> Color(0xFF6B7280) to 0.28f
    WeatherCondition.RAIN -> Color(0xFF475569) to 0.38f
    WeatherCondition.HEAVY_RAIN -> Color(0xFF334155) to 0.48f
    WeatherCondition.SNOW -> Color(0xFFCBD5E1) to 0.22f
    WeatherCondition.THUNDERSTORM -> Color(0xFF1E1B2E) to 0.55f
    WeatherCondition.FOG -> Color(0xFF94A3B8) to 0.32f
    WeatherCondition.WIND -> Color(0xFF7C8DA6) to 0.12f
}

/** Три слоя гор: дальний, средний, ближний. Строятся один раз для размера экрана. */
private fun buildMountains(w: Float, h: Float): List<Path> {
    if (w <= 0f || h <= 0f) return emptyList()
    val layers = listOf(
        Triple(0.74f, 0.20f, 7),   // базовая линия, высота пиков, число вершин
        Triple(0.82f, 0.17f, 9),
        Triple(0.90f, 0.12f, 6)
    )
    return layers.mapIndexed { layerIndex, (baseline, peak, count) ->
        Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * baseline)
            val step = w / count
            for (i in 0..count) {
                val x = i * step
                // чередуем вершины и впадины; «шум» одинаков при каждой отрисовке
                val noise = 0.45f + 0.55f * frac(sin((i + 1) * 12.9898f + layerIndex * 78.233f) * 43758.547f)
                val y = h * baseline - h * peak * noise
                lineTo(x - step * 0.5f, h * baseline + h * 0.015f)
                lineTo(x, y)
            }
            lineTo(w, h * baseline)
            lineTo(w, h)
            close()
        }
    }
}

/**
 * Живое небо в мягком стиле: небо с дымкой у горизонта, матовый диск солнца или луны,
 * размытые облака и три слоя гор. Солнце и луна идут по дуге в реальном времени суток
 * (по восходу и закату города), при дожде и снеге идут капли и снежинки.
 * Без облаков и осадков ничего не перерисовывается каждый кадр: облака двигаются раз в четверть секунды,
 * осадки — примерно 30 раз в секунду.
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
    val tickMs = if (precipActive) 33L else 250L

    var seconds by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(tickMs) {
        while (true) {
            val nanos = withFrameNanos { it }
            seconds = (nanos % 3_600_000_000_000L) / 1_000_000_000f
            delay(tickMs)
        }
    }

    val rise = if (sunsetMin > sunriseMin) sunriseMin else 390
    val set = if (sunsetMin > sunriseMin) sunsetMin else 1110
    val (baseTop, baseHorizon) = skyColors(nowMinutes, rise, set, theme)
    val (tint, tintAmount) = weatherTint(weather)
    val top = lerp(baseTop, tint, tintAmount)
    val horizon = lerp(baseHorizon, tint, tintAmount)

    var sizePx by remember { mutableStateOf(IntSize.Zero) }
    val mountains = remember(sizePx) { buildMountains(sizePx.width.toFloat(), sizePx.height.toFloat()) }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { sizePx = it }
    ) {
        drawRect(Brush.verticalGradient(listOf(top, horizon), startY = 0f, endY = size.height * 0.85f))
        drawSkyBodies(nowMinutes, rise, set, clouds, seconds, horizon)
        drawSoftClouds(clouds, nowMinutes in rise..set, seconds)
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
        // горы: дальний слой светлее и «в дымке», ближний — самый тёмный
        mountains.forEachIndexed { index, path ->
            val depth = (index + 1) / mountains.size.toFloat()
            val color = lerp(horizon, Color(0xFF1C2A4A), 0.30f + 0.45f * depth)
            drawPath(path, Brush.verticalGradient(listOf(lerp(color, Color.White, 0.18f), color)))
        }
        // затемнение низа, чтобы док и подписи читались
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0x66000A18))))
    }
}

private fun DrawScope.drawSkyBodies(
    minutes: Int,
    rise: Int,
    set: Int,
    clouds: Float,
    seconds: Float,
    horizonColor: Color
) {
    val w = size.width
    val h = size.height
    val baseline = h * 0.70f
    val arc = h * 0.55f
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
    if (nightFactor > 0.05f && clouds < 0.85f) {
        val base = nightFactor * (1f - clouds)
        for (i in 0 until 50) {
            val x = frac(i * 0.618034f) * w
            val y = frac(i * 0.7548776f + 0.13f) * baseline * 0.8f
            val twinkle = 0.6f + 0.4f * sin(seconds * 0.8f + i)
            drawCircle(
                color = Color.White.copy(alpha = (base * (0.35f + frac(i * 0.31f) * 0.65f) * twinkle).coerceIn(0f, 1f)),
                radius = (1f + frac(i * 0.17f) * 1.4f).dp.toPx(),
                center = Offset(x, y)
            )
        }
    }

    // --- диск солнца или луны ---
    val rawProgress: Float
    if (isDay) {
        rawProgress = (m - rise) / (set - rise).toFloat()
    } else {
        val nightLength = (1440 - set + rise).toFloat()
        rawProgress = if (m > set) (m - set) / nightLength else (m + 1440 - set) / nightLength
    }
    val progress = rawProgress.coerceIn(0f, 1f)
    val cx = w * (0.10f + 0.80f * progress)
    val cy = baseline - sin(PI.toFloat() * progress) * arc
    val visibility = (sin(PI.toFloat() * progress) * 1.8f).coerceIn(0f, 1f)
    val center = Offset(cx, cy)

    if (isDay) {
        // чем больше облаков, тем «матовее» и холоднее диск
        val color = lerp(Color(0xFFFFF3C9), Color(0xFFAEBDC2), clouds)
        val strength = visibility * (1f - clouds * 0.25f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFFFF1BF).copy(alpha = 0.40f * (1f - clouds) * visibility), Color.Transparent),
                center = center,
                radius = radius * 4.5f
            ),
            radius = radius * 4.5f,
            center = center
        )
        drawCircle(color.copy(alpha = strength), radius, center)
    } else {
        val strength = visibility * (1f - clouds * 0.5f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0xFFCAD7FF).copy(alpha = 0.28f * strength), Color.Transparent),
                center = center,
                radius = radius * 4f
            ),
            radius = radius * 4f,
            center = center
        )
        drawCircle(lerp(Color(0xFFE9EDF7), horizonColor, 0.12f).copy(alpha = strength), radius, center)
    }
}

/** Крупные мягкие облака без чётких границ. */
private fun DrawScope.drawSoftClouds(clouds: Float, isDay: Boolean, seconds: Float) {
    val w = size.width
    val h = size.height
    val tintColor = if (isDay) Color.White else Color(0xFF9AA6C0)
    val count = 2 + (clouds * 5f).toInt()
    for (j in 0 until count) {
        val speed = 0.0025f + frac(j * 0.29f) * 0.003f
        val cx = ((frac(j * 0.37f + 0.2f) * 1.5f + seconds * speed) % 1.5f - 0.25f) * w
        val cy = h * (0.12f + frac(j * 0.53f) * 0.42f)
        val radius = w * (0.10f + frac(j * 0.71f) * 0.10f)
        val alpha = (0.10f + clouds * 0.20f).coerceAtMost(0.34f)
        for (k in 0 until 3) {
            val c = Offset(cx + (k - 1) * radius * 0.8f, cy + (if (k == 1) -radius * 0.15f else 0f))
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(tintColor.copy(alpha = alpha), Color.Transparent),
                    center = c,
                    radius = radius
                ),
                radius = radius,
                center = c
            )
        }
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
