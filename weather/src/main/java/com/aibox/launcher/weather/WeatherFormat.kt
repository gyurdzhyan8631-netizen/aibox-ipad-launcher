package com.aibox.launcher.weather

import com.aibox.launcher.core.model.WeatherCondition

/** Данные старше этого срока считаются устаревшими, и интерфейс прямо об этом говорит. */
const val WEATHER_STALE_AFTER_MILLIS = 3 * 60 * 60 * 1000L

fun WeatherCondition.titleRu(): String = when (this) {
    WeatherCondition.CLEAR -> "Ясно"
    WeatherCondition.CLOUDY -> "Облачно"
    WeatherCondition.OVERCAST -> "Пасмурно"
    WeatherCondition.RAIN -> "Дождь"
    WeatherCondition.HEAVY_RAIN -> "Ливень"
    WeatherCondition.SNOW -> "Снег"
    WeatherCondition.THUNDERSTORM -> "Гроза"
    WeatherCondition.FOG -> "Туман"
    WeatherCondition.WIND -> "Ветер"
}

fun formatTemp(tempC: Int): String = if (tempC > 0) "+$tempC°" else "$tempC°"

fun ageText(fetchedAtMillis: Long, nowMillis: Long): String {
    val minutes = ((nowMillis - fetchedAtMillis) / 60_000L).coerceAtLeast(0L)
    return when {
        minutes < 1L -> "только что"
        minutes < 60L -> "$minutes мин назад"
        minutes < 24L * 60L -> "${minutes / 60L} ч назад"
        else -> "${minutes / (24L * 60L)} дн назад"
    }
}

fun isStale(fetchedAtMillis: Long, nowMillis: Long): Boolean =
    nowMillis - fetchedAtMillis > WEATHER_STALE_AFTER_MILLIS

/** Код погоды WMO (его отдаёт Open-Meteo) → наша модель. */
fun conditionFromWmoCode(code: Int): WeatherCondition = when (code) {
    0 -> WeatherCondition.CLEAR
    1, 2 -> WeatherCondition.CLOUDY
    3 -> WeatherCondition.OVERCAST
    45, 48 -> WeatherCondition.FOG
    51, 53, 55, 56, 57, 61, 63, 66, 67, 80, 81 -> WeatherCondition.RAIN
    65, 82 -> WeatherCondition.HEAVY_RAIN
    71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOW
    95, 96, 99 -> WeatherCondition.THUNDERSTORM
    else -> WeatherCondition.CLOUDY
}
