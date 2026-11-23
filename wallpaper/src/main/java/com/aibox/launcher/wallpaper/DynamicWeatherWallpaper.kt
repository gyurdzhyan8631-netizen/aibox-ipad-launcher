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
import com.aibox.launcher.core.model.DayPeriod
import com.aibox.launcher.core.model.WeatherCondition

@Composable
fun DynamicWeatherWallpaper(
    dayPeriod: DayPeriod,
    weather: WeatherCondition,
    modifier: Modifier = Modifier
) {
    val topColorTarget = when (dayPeriod) {
        DayPeriod.MORNING -> Color(0xFF2B5876)
        DayPeriod.DAY -> Color(0xFF1E62B5)
        DayPeriod.EVENING -> Color(0xFF3B1D5E)
        DayPeriod.NIGHT -> Color(0xFF070B14)
    }
    val bottomColorTarget = when (dayPeriod) {
        DayPeriod.MORNING -> Color(0xFFE89B72)
        DayPeriod.DAY -> Color(0xFF63A4FF)
        DayPeriod.EVENING -> Color(0xFFD96B43)
        DayPeriod.NIGHT -> Color(0xFF131D31)
    }

    val topColor by animateColorAsState(topColorTarget, tween(1200), label = "skyTop")
    val bottomColor by animateColorAsState(bottomColorTarget, tween(1200), label = "skyBottom")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
    )
}
