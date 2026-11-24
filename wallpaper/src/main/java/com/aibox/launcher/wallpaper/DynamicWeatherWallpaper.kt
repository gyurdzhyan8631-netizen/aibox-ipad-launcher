package com.aibox.launcher.wallpaper

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.aibox.launcher.core.model.DayPeriod
import com.aibox.launcher.core.model.WallpaperTheme
import com.aibox.launcher.core.model.WeatherCondition

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
 * Обои: один градиент. Никакой покадровой анимации — цвет плавно меняется только
 * при смене темы, времени суток или погоды, поэтому нагрузка на GPU минимальна.
 */
@Composable
fun DynamicWeatherWallpaper(
    dayPeriod: DayPeriod,
    weather: WeatherCondition,
    modifier: Modifier = Modifier,
    theme: WallpaperTheme = WallpaperTheme.AUTO
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

    val topTarget = fixed?.first ?: lerp(topBase, tint, amount)
    val bottomTarget = fixed?.second ?: lerp(bottomBase, tint, amount)

    val topColor by animateColorAsState(topTarget, tween(3000), label = "skyTop")
    val bottomColor by animateColorAsState(bottomTarget, tween(3000), label = "skyBottom")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
    )
}
