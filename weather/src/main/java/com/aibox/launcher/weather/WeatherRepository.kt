package com.aibox.launcher.weather

import android.content.Context
import com.aibox.launcher.core.model.WeatherCondition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

/**
 * Погода по принципу «сначала офлайн»:
 *  - последний результат всегда хранится на устройстве ([cached]);
 *  - [refresh] пробует обновить его через Open-Meteo (без ключа и регистрации);
 *  - при ошибке сети старые данные остаются, а интерфейс показывает их возраст.
 */
class WeatherRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("weather_cache", Context.MODE_PRIVATE)

    fun cached(): WeatherSnapshot? {
        val raw = prefs.getString(KEY_SNAPSHOT, null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            WeatherSnapshot(
                query = o.getString("query"),
                cityName = o.getString("cityName"),
                tempC = o.getInt("tempC"),
                condition = WeatherCondition.valueOf(o.getString("condition")),
                isDay = o.getBoolean("isDay"),
                fetchedAtMillis = o.getLong("fetchedAt")
            )
        }.getOrNull()
    }

    suspend fun refresh(city: String): Result<WeatherSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val query = city.trim()
            val place = resolvePlace(query)

            val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=${place.lat}&longitude=${place.lon}" +
                "&current=temperature_2m,weather_code,is_day&timezone=auto"
            val current = JSONObject(httpGet(url)).getJSONObject("current")

            val snapshot = WeatherSnapshot(
                query = query,
                cityName = place.name,
                tempC = current.getDouble("temperature_2m").roundToInt(),
                condition = conditionFromWmoCode(current.getInt("weather_code")),
                isDay = current.optInt("is_day", 1) == 1,
                fetchedAtMillis = System.currentTimeMillis()
            )
            save(snapshot)
            snapshot
        }
    }

    private data class Place(val name: String, val lat: String, val lon: String)

    private fun resolvePlace(query: String): Place {
        val savedQuery = prefs.getString(KEY_PLACE_QUERY, null)
        val savedName = prefs.getString(KEY_PLACE_NAME, null)
        val savedLat = prefs.getString(KEY_PLACE_LAT, null)
        val savedLon = prefs.getString(KEY_PLACE_LON, null)
        if (savedQuery.equals(query, ignoreCase = true) &&
            savedName != null && savedLat != null && savedLon != null
        ) {
            return Place(savedName, savedLat, savedLon)
        }

        val encoded = URLEncoder.encode(query, "UTF-8")
        val body = httpGet(
            "https://geocoding-api.open-meteo.com/v1/search?name=$encoded&count=1&language=ru&format=json"
        )
        val results = JSONObject(body).optJSONArray("results")
        if (results == null || results.length() == 0) throw CityNotFoundException(query)

        val first = results.getJSONObject(0)
        val place = Place(
            name = first.optString("name", query),
            lat = first.getDouble("latitude").toString(),
            lon = first.getDouble("longitude").toString()
        )
        prefs.edit()
            .putString(KEY_PLACE_QUERY, query)
            .putString(KEY_PLACE_NAME, place.name)
            .putString(KEY_PLACE_LAT, place.lat)
            .putString(KEY_PLACE_LON, place.lon)
            .apply()
        return place
    }

    private fun save(s: WeatherSnapshot) {
        val o = JSONObject()
            .put("query", s.query)
            .put("cityName", s.cityName)
            .put("tempC", s.tempC)
            .put("condition", s.condition.name)
            .put("isDay", s.isDay)
            .put("fetchedAt", s.fetchedAtMillis)
        prefs.edit().putString(KEY_SNAPSHOT, o.toString()).apply()
    }

    private fun httpGet(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/json")
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("HTTP $code")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val KEY_SNAPSHOT = "snapshot"
        const val KEY_PLACE_QUERY = "place_query"
        const val KEY_PLACE_NAME = "place_name"
        const val KEY_PLACE_LAT = "place_lat"
        const val KEY_PLACE_LON = "place_lon"
    }
}
