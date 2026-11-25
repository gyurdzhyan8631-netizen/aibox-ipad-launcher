package com.aibox.launcher.weather

import com.aibox.launcher.core.model.WeatherCondition

/** Последняя полученная погода. Хранится на устройстве и показывается даже без интернета. */
data class WeatherSnapshot(
    /** Город так, как его ввёл пользователь. */
    val query: String,
    /** Название города из геокодера. */
    val cityName: String,
    val tempC: Int,
    val condition: WeatherCondition,
    val isDay: Boolean,
    val sunriseTime: String? = null,
    val sunsetTime: String? = null,
    val fetchedAtMillis: Long
)

class CityNotFoundException(city: String) : Exception("Город не найден: $city")
