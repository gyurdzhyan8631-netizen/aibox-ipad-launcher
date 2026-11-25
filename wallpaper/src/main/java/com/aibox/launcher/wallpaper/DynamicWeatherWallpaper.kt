package com.aibox.launcher.wallpaper

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.aibox.launcher.core.model.DayPeriod
import com.aibox.launcher.core.model.WallpaperTheme
import com.aibox.launcher.core.model.WeatherCondition
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.sin

/**
 * Цвета фиксированной темы (верх, низ). Для AUTO возвращает null —
 * тогда цвета считаются по времени суток и погоде.
 */
fun wallpaperThemeColors(theme: WallpaperTheme): Pair<Color, Color>? = when (theme) {
    WallpaperTheme.STYLISH -> Color(0xFF1B4BC4) to Color(0xFF0A1A55)
    WallpaperTheme.DARK -> Color(0xFF2A2C33) to Color(0xFF08090C)
    WallpaperTheme.BRIGHT -> Color(0xFF8A3CF0) to Color(0xFFFF5C9E)
    WallpaperTheme.NATURE -> Color(0xFF2F8C74) to Color(0xFF123C2E)
    WallpaperTheme.DAY -> Color(0xFF5CB6F5) to Color(0xFF2C7BD6)
    WallpaperTheme.DAWN -> Color(0xFFFF9F68) to Color(0xFFB04C7A)
    WallpaperTheme.NIGHT -> Color(0xFF0B1230) to Color(0xFF03040A)
    WallpaperTheme.AUTO -> null
}

/** Цвета для карточки-превью темы (у AUTO — условный закат). */
fun wallpaperPreviewColors(theme: WallpaperTheme): Pair<Color, Color> =
    wallpaperThemeColors(theme) ?: (Color(0xFFE89B72) to Color(0xFF1E62B5))

/**
 * Цвет неба зависит от темы, времени суток и погоды; небесное тело движется по локальному времени.
 */
@Composable
fun DynamicWeatherWallpaper(
    dayPeriod: DayPeriod,
    weather: WeatherCondition,
    modifier: Modifier = Modifier,
    theme: WallpaperTheme = WallpaperTheme.AUTO,
    currentTime: LocalTime = LocalTime.now(),
    showCelestialBodies: Boolean = true
) {
    val fixed = wallpaperThemeColors(theme)

    val topBase = when (dayPeriod) {
        DayPeriod.MORNING -> Color(0xFF2B5876)
        DayPeriod.DAY -> Color(0xFF1E62B5)
        DayPeriod.EVENING -> Color(0xFF3B1D5E)
        DayPeriod.NIGHT -> Color(0xFF070B14)
    }
    val bottomBase = when (dayPeriod) {
        DayPeriod.MORNING -> Color(0xFFE89B72)
        DayPeriod.DAY -> Color(0xFF63A4FF)
        DayPeriod.EVENING -> Color(0xFFD96B43)
        DayPeriod.NIGHT -> Color(0xFF131D31)
    }

    val (tint, amount) = when (weather) {
        WeatherCondition.CLEAR -> Color.Black to 0f
        WeatherCondition.CLOUDY -> Color(0xFF8E9AAF) to 0.18f
        WeatherCondition.OVERCAST -> Color(0xFF6B7280) to 0.35f
        WeatherCondition.RAIN -> Color(0xFF475569) to 0.45f
        WeatherCondition.HEAVY_RAIN -> Color(0xFF334155) to 0.55f
        WeatherCondition.SNOW -> Color(0xFFCBD5E1) to 0.30f
        WeatherCondition.THUNDERSTORM -> Color(0xFF1E1B2E) to 0.60f
        WeatherCondition.FOG -> Color(0xFF94A3B8) to 0.40f
        WeatherCondition.WIND -> Color(0xFF7C8DA6) to 0.20f
    }

    val topTarget = lerp(fixed?.first ?: topBase, tint, amount)
    val bottomTarget = lerp(fixed?.second ?: bottomBase, tint, amount)

    val topColor by animateColorAsState(topTarget, tween(3000), label = "skyTop")
    val bottomColor by animateColorAsState(bottomTarget, tween(3000), label = "skyBottom")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
    ) {
        val transition = rememberInfiniteTransition(label = "weather")
        val particlePhase = if (
            weather == WeatherCondition.RAIN ||
            weather == WeatherCondition.HEAVY_RAIN ||
            weather == WeatherCondition.THUNDERSTORM ||
            weather == WeatherCondition.SNOW
        ) {
            val phase by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1800),
                    repeatMode = RepeatMode.Restart
                ),
                label = "weatherParticles"
            )
            phase
        } else {
            0f
        }
        val atmospherePhase = if (
            dayPeriod == DayPeriod.MORNING ||
            dayPeriod == DayPeriod.DAY ||
            weather == WeatherCondition.CLOUDY ||
            weather == WeatherCondition.OVERCAST ||
            weather == WeatherCondition.FOG
        ) {
            val phase by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 30_000),
                    repeatMode = RepeatMode.Restart
                ),
                label = "weatherAtmosphere"
            )
            phase
        } else {
            0f
        }
        Canvas(Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val timeFraction = (currentTime.toSecondOfDay() / 86_400f)
            val isDay = currentTime.hour in 6..17
            val pathFraction = if (isDay) {
                ((timeFraction - 6f / 24f) / 0.5f).coerceIn(0f, 1f)
            } else {
                val nightFraction = if (timeFraction >= 18f / 24f) {
                    timeFraction - 18f / 24f
                } else {
                    timeFraction + 6f / 24f
                }
                (nightFraction / 0.5f).coerceIn(0f, 1f)
            }
            val bodyX = width * pathFraction
            val bodyY = height * (0.67f - 0.42f * sin(PI.toFloat() * pathFraction))
            val bodyColor = if (isDay) Color(0xFFFFD166) else Color(0xFFE9F1FF)
            val bodyRadius = minOf(width, height) * 0.022f

            if (dayPeriod == DayPeriod.NIGHT) {
                repeat(28) { index ->
                    val x = width * ((index * 0.618f + 0.17f) % 1f)
                    val y = height * ((index * 0.371f + 0.08f) % 0.48f)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.38f),
                        radius = 1.2.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }

            if (showCelestialBodies) {
                drawCircle(
                    color = bodyColor.copy(alpha = 0.08f),
                    radius = bodyRadius * 1.65f,
                    center = Offset(bodyX, bodyY)
                )
                drawCircle(
                    color = bodyColor.copy(alpha = 0.88f),
                    radius = bodyRadius,
                    center = Offset(bodyX, bodyY)
                )
            }

            if (dayPeriod == DayPeriod.DAY ||
                weather == WeatherCondition.CLOUDY ||
                weather == WeatherCondition.OVERCAST
            ) {
                val cloudAlpha = when (weather) {
                    WeatherCondition.CLOUDY -> 0.24f
                    WeatherCondition.OVERCAST -> 0.34f
                    else -> 0.16f
                }
                repeat(3) { index ->
                    val cloudX = width * ((index * 0.43f + atmospherePhase) % 1f)
                    val cloudY = height * (0.25f + index * 0.09f)
                    val cloudRadius = minOf(width, height) * (0.035f + index * 0.004f)
                    val cloudColor = Color.White.copy(alpha = cloudAlpha)
                    drawCircle(cloudColor, cloudRadius, Offset(cloudX, cloudY))
                    drawCircle(cloudColor, cloudRadius * 0.78f, Offset(cloudX - cloudRadius, cloudY + cloudRadius * 0.3f))
                    drawCircle(cloudColor, cloudRadius * 0.88f, Offset(cloudX + cloudRadius, cloudY + cloudRadius * 0.25f))
                    drawRect(
                        color = cloudColor,
                        topLeft = Offset(cloudX - cloudRadius * 1.6f, cloudY + cloudRadius * 0.25f),
                        size = Size(cloudRadius * 3.2f, cloudRadius * 0.65f)
                    )
                }
            }

            if (dayPeriod == DayPeriod.MORNING || weather == WeatherCondition.FOG) {
                val fogAlpha = if (weather == WeatherCondition.FOG) 0.22f else 0.08f
                repeat(3) { index ->
                    val fogY = height * (0.43f + index * 0.12f)
                    val fogX = width * ((atmospherePhase + index * 0.37f) % 1f) - width * 0.35f
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0f),
                                Color(0xFFE5EAF0).copy(alpha = fogAlpha),
                                Color.White.copy(alpha = 0f)
                            )
                        ),
                        topLeft = Offset(fogX, fogY),
                        size = Size(width * 0.7f, height * 0.075f)
                    )
                }
            }

            when (weather) {
                WeatherCondition.RAIN,
                WeatherCondition.HEAVY_RAIN,
                WeatherCondition.THUNDERSTORM -> {
                    val dropCount = if (weather == WeatherCondition.HEAVY_RAIN ||
                        weather == WeatherCondition.THUNDERSTORM
                    ) 42 else 24
                    repeat(dropCount) { index ->
                        val x = width * ((index * 0.618f) % 1f)
                        val y = height * ((index * 0.371f + particlePhase) % 1f)
                        drawLine(
                            color = Color(0xFFB7D7FF).copy(alpha = 0.38f),
                            start = Offset(x, y),
                            end = Offset(x - 5.dp.toPx(), y + 14.dp.toPx()),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                    drawRect(
                        color = Color(0xFF1E293B).copy(alpha = 0.18f),
                        topLeft = Offset(0f, height * 0.88f),
                        size = Size(width, height * 0.12f)
                    )
                    repeat(4) { index ->
                        val puddleX = width * (0.12f + index * 0.23f)
                        drawOval(
                            color = Color(0xFFB7D7FF).copy(alpha = 0.2f),
                            topLeft = Offset(puddleX, height * (0.91f + (index % 2) * 0.035f)),
                            size = Size(width * 0.09f, height * 0.012f)
                        )
                    }
                }

                WeatherCondition.SNOW -> repeat(30) { index ->
                    val x = width * ((index * 0.618f + particlePhase * 0.08f) % 1f)
                    val y = height * ((index * 0.371f + particlePhase) % 1f)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.75f),
                        radius = 2.dp.toPx(),
                        center = Offset(x, y)
                    )
                }

                else -> Unit
            }
        }
    }
}
