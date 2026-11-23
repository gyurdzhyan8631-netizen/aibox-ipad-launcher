package com.aibox.launcher.wallpaper

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import java.time.LocalTime

@Composable
fun SlowWallpaper(modifier: Modifier = Modifier) {
    val palette = getPaletteForHour(LocalTime.now().hour)
    
    val topColor by animateFloatAsState(
        targetValue = palette.first.value,
        animationSpec = tween(durationMillis = 30000),
        label = "top"
    )
    val bottomColor by animateFloatAsState(
        targetValue = palette.second.value,
        animationSpec = tween(durationMillis = 30000),
        label = "bottom"
    )

    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(topColor), Color(bottomColor))
                )
            )
    )
}

private fun getPaletteForHour(hour: Int): Pair<Color, Color> = when (hour) {
    in 5..8 -> Color(0xFFFDBB2D) to Color(0xFF22C1C3)   // Рассвет
    in 9..12 -> Color(0xFF56CCF2) to Color(0xFF2F80ED)  // Утро
    in 13..16 -> Color(0xFF4A90E2) to Color(0xFF1E3C72) // День
    in 17..20 -> Color(0xFFF2994A) to Color(0xFFB24592) // Закат
    in 21..23 -> Color(0xFF1A1A2E) to Color(0xFF16213E) // Вечер
    else -> Color(0xFF0F0C29) to Color(0xFF24243E)      // Ночь
}
