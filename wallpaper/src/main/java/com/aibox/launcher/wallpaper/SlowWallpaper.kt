package com.aibox.launcher.wallpaper

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import java.time.LocalTime

@Composable
fun SlowWallpaper(modifier: Modifier = Modifier) {
    val hour = LocalTime.now().hour
    val (top, bottom) = getPaletteForHour(hour)
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(top, bottom)))
    )
}

private fun getPaletteForHour(hour: Int): Pair<Color, Color> = when (hour) {
    in 5..8 -> Color(0xFFFDBB2D) to Color(0xFF22C1C3)
    in 9..12 -> Color(0xFF56CCF2) to Color(0xFF2F80ED)
    in 13..16 -> Color(0xFF4A90E2) to Color(0xFF1E3C72)
    in 17..20 -> Color(0xFFF2994A) to Color(0xFFB24592)
    in 21..23 -> Color(0xFF1A1A2E) to Color(0xFF16213E)
    else -> Color(0xFF0F0C29) to Color(0xFF24243E)
}
