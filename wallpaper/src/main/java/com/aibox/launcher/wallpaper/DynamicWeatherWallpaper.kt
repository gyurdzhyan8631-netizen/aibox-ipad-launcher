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
import com.aibox.launcher.core.model.WeatherCondition

/**
 * Обои: один градиент по времени суток, подкрашенный под погоду.
 * Никакой покадровой анимации — цвет плавно меняется только когда меняется время суток или погода,
 * поэтому нагрузка на GPU автомобильного железа минимальна.
 */
@Composable
fun DynamicWeatherWallpaper(
    dayPeriod: DayPeriod,
    weather: WeatherCondition,
    modifier: Modifier = Modifier
) {
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

    val topColor by animateColorAsState(lerp(topBase, tint, amount), tween(3000), label = "skyTop")
    val bottomColor by animateColorAsState(lerp(bottomBase, tint, amount), tween(3000), label = "skyBottom")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
    )
}
