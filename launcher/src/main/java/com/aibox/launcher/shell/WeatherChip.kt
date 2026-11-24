package com.aibox.launcher.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.model.WeatherCondition
import com.aibox.launcher.weather.WeatherSnapshot
import com.aibox.launcher.weather.ageText
import com.aibox.launcher.weather.formatTemp
import com.aibox.launcher.weather.isStale
import com.aibox.launcher.weather.titleRu

private fun WeatherCondition.icon(): ImageVector = when (this) {
    WeatherCondition.CLEAR -> Icons.Rounded.WbSunny
    WeatherCondition.CLOUDY,
    WeatherCondition.OVERCAST,
    WeatherCondition.FOG,
    WeatherCondition.WIND -> Icons.Rounded.Cloud
    WeatherCondition.RAIN,
    WeatherCondition.HEAVY_RAIN -> Icons.Rounded.Opacity
    WeatherCondition.SNOW -> Icons.Rounded.AcUnit
    WeatherCondition.THUNDERSTORM -> Icons.Rounded.Bolt
}

private fun WeatherCondition.iconTint(): Color = when (this) {
    WeatherCondition.CLEAR -> Color(0xFFFFD60A)
    WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN -> Color(0xFF64D2FF)
    WeatherCondition.SNOW -> Color(0xFFE5F0FF)
    WeatherCondition.THUNDERSTORM -> Color(0xFFFFD60A)
    else -> Color(0xFFD1D5DB)
}

/**
 * Плашка погоды в шапке. Всегда честно показывает возраст данных:
 * если им больше 3 часов — пишет «Нет связи» и время последнего обновления.
 */
@Composable
fun WeatherChip(
    city: String,
    snapshot: WeatherSnapshot?,
    nowMillis: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(shape)
            .background(Color(0x401C1C1E))
            .border(1.dp, Color(0x33FFFFFF), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val icon = snapshot?.condition?.icon() ?: Icons.Rounded.Cloud
        val tint = snapshot?.condition?.iconTint() ?: Color(0xFF8E8E93)
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(8.dp))
        Column {
            val title = when {
                city.isBlank() -> "Укажите город"
                snapshot == null -> "Погода…"
                else -> "${formatTemp(snapshot.tempC)} ${snapshot.condition.titleRu()}"
            }
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            if (snapshot != null) {
                val stale = isStale(snapshot.fetchedAtMillis, nowMillis)
                Text(
                    text = if (stale) {
                        "Нет связи · ${ageText(snapshot.fetchedAtMillis, nowMillis)}"
                    } else {
                        "обновлено ${ageText(snapshot.fetchedAtMillis, nowMillis)}"
                    },
                    color = if (stale) Color(0xFFFF9F0A) else Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
