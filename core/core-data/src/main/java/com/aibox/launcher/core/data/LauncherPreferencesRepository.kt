package com.aibox.launcher.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.aibox.launcher.core.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "aibox_launcher_prefs"
)

class LauncherPreferencesRepository(private val context: Context) {

    private object Keys {
        val VISUAL_STYLE = stringPreferencesKey("visual_style")
        val AUTOMOTIVE_MODE = booleanPreferencesKey("automotive_mode")
        val ANIMATION_INTENSITY = stringPreferencesKey("animation_intensity")
        val DOCK_OPACITY = floatPreferencesKey("dock_opacity")
        val DOCK_SCALE = floatPreferencesKey("dock_scale")
        val DOCK_SPACING = floatPreferencesKey("dock_spacing")
        val SHOW_CAPSULE = booleanPreferencesKey("show_capsule")
        val IS_24_HOUR = booleanPreferencesKey("is_24_hour")
        val SHOW_SECONDS = booleanPreferencesKey("show_seconds")
        val MANUAL_CITY = stringPreferencesKey("manual_city")
        val ECO_WALLPAPER = booleanPreferencesKey("eco_wallpaper")
        val DESKTOP_ELEMENTS_JSON = stringPreferencesKey("desktop_elements_json")
        val DOCK_APPS_JSON = stringPreferencesKey("dock_apps_json")
    }

    val configFlow: Flow<LauncherConfig> = context.launcherDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            LauncherConfig(
                visualStyle = prefs[Keys.VISUAL_STYLE]?.let {
                    runCatching { VisualStyle.valueOf(it) }.getOrNull()
                } ?: VisualStyle.IPADOS_MINIMAL,
                automotiveMode = prefs[Keys.AUTOMOTIVE_MODE] ?: false,
                animationIntensity = prefs[Keys.ANIMATION_INTENSITY]?.let {
                    runCatching { AnimationIntensity.valueOf(it) }.getOrNull()
                } ?: AnimationIntensity.STANDARD,
                dockOpacity = prefs[Keys.DOCK_OPACITY] ?: 0.72f,
                dockScale = prefs[Keys.DOCK_SCALE] ?: 1.0f,
                dockSpacingDp = prefs[Keys.DOCK_SPACING] ?: 16f,
                is24HourFormat = prefs[Keys.IS_24_HOUR] ?: true,
                showSeconds = prefs[Keys.SHOW_SECONDS] ?: false,
                manualCity = prefs[Keys.MANUAL_CITY] ?: "",
                ecoWallpaperMode = prefs[Keys.ECO_WALLPAPER] ?: false,
                capsuleConfig = CapsuleConfig(
                    enabled = prefs[Keys.SHOW_CAPSULE] ?: true
                )
            )
        }

    val desktopElementsFlow: Flow<List<DesktopElement>> = context.launcherDataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs ->
            val raw = prefs[Keys.DESKTOP_ELEMENTS_JSON]
            if (raw.isNullOrBlank()) defaultDesktopElements() else deserializeElements(raw)
        }

    suspend fun updateConfig(transform: (LauncherConfig) -> LauncherConfig) {
        context.launcherDataStore.edit { prefs ->
            val current = LauncherConfig(
                visualStyle = prefs[Keys.VISUAL_STYLE]?.let {
                    runCatching { VisualStyle.valueOf(it) }.getOrNull()
                } ?: VisualStyle.IPADOS_MINIMAL,
                automotiveMode = prefs[Keys.AUTOMOTIVE_MODE] ?: false,
                animationIntensity = prefs[Keys.ANIMATION_INTENSITY]?.let {
                    runCatching { AnimationIntensity.valueOf(it) }.getOrNull()
                } ?: AnimationIntensity.STANDARD,
                dockOpacity = prefs[Keys.DOCK_OPACITY] ?: 0.72f,
                dockScale = prefs[Keys.DOCK_SCALE] ?: 1.0f,
                dockSpacingDp = prefs[Keys.DOCK_SPACING] ?: 16f,
                is24HourFormat = prefs[Keys.IS_24_HOUR] ?: true,
                showSeconds = prefs[Keys.SHOW_SECONDS] ?: false,
                manualCity = prefs[Keys.MANUAL_CITY] ?: "",
                ecoWallpaperMode = prefs[Keys.ECO_WALLPAPER] ?: false,
                capsuleConfig = CapsuleConfig(enabled = prefs[Keys.SHOW_CAPSULE] ?: true)
            )
            val updated = transform(current)
            prefs[Keys.VISUAL_STYLE] = updated.visualStyle.name
            prefs[Keys.AUTOMOTIVE_MODE] = updated.automotiveMode
            prefs[Keys.ANIMATION_INTENSITY] = updated.animationIntensity.name
            prefs[Keys.DOCK_OPACITY] = updated.dockOpacity
            prefs[Keys.DOCK_SCALE] = updated.dockScale
            prefs[Keys.DOCK_SPACING] = updated.dockSpacingDp
            prefs[Keys.SHOW_CAPSULE] = updated.capsuleConfig.enabled
            prefs[Keys.IS_24_HOUR] = updated.is24HourFormat
            prefs[Keys.SHOW_SECONDS] = updated.showSeconds
            prefs[Keys.MANUAL_CITY] = updated.manualCity
            prefs[Keys.ECO_WALLPAPER] = updated.ecoWallpaperMode
        }
    }

    suspend fun saveDesktopElements(elements: List<DesktopElement>) {
        context.launcherDataStore.edit { prefs ->
            prefs[Keys.DESKTOP_ELEMENTS_JSON] = serializeElements(elements)
        }
    }

    private fun serializeElements(list: List<DesktopElement>): String {
        val arr = JSONArray()
        list.forEach { el ->
            arr.put(JSONObject().apply {
                put("id", el.id)
                put("screenIndex", el.screenIndex)
                put("title", el.title)
                put("type", el.type.name)
                put("xFraction", el.xFraction.toDouble())
                put("yFraction", el.yFraction.toDouble())
                put("widthDp", el.widthDp.toDouble())
                put("heightDp", el.heightDp.toDouble())
                put("alpha", el.alpha.toDouble())
                put("cornerRadiusDp", el.cornerRadiusDp.toDouble())
                put("packageName", el.packageName ?: "")
                put("customImageUri", el.customImageUri ?: "")
            })
        }
        return arr.toString()
    }

    private fun deserializeElements(raw: String): List<DesktopElement> {
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        DesktopElement(
                            id = o.getString("id"),
                            screenIndex = o.getInt("screenIndex"),
                            title = o.getString("title"),
                            type = ElementType.valueOf(o.getString("type")),
                            xFraction = o.getDouble("xFraction").toFloat(),
                            yFraction = o.getDouble("yFraction").toFloat(),
                            widthDp = o.getDouble("widthDp").toFloat(),
                            heightDp = o.getDouble("heightDp").toFloat(),
                            alpha = o.optDouble("alpha", 0.85).toFloat(),
                            cornerRadiusDp = o.optDouble("cornerRadiusDp", 26.0).toFloat(),
                            packageName = o.optString("packageName").ifEmpty { null },
                            customImageUri = o.optString("customImageUri").ifEmpty { null }
                        )
                    )
                }
            }
        }.getOrElse { defaultDesktopElements() }
    }

    companion object {
        fun defaultDesktopElements(): List<DesktopElement> = listOf(
            DesktopElement(
                id = "w_nav_1",
                screenIndex = 0,
                title = "Навигация",
                type = ElementType.WIDGET_NAV,
                xFraction = 0.03f,
                yFraction = 0.05f,
                widthDp = 340f,
                heightDp = 240f
            ),
            DesktopElement(
                id = "w_music_1",
                screenIndex = 0,
                title = "Музыка",
                type = ElementType.WIDGET_MUSIC,
                xFraction = 0.42f,
                yFraction = 0.05f,
                widthDp = 310f,
                heightDp = 240f
            ),
            DesktopElement(
                id = "w_weather_1",
                screenIndex = 0,
                title = "Погода и поездка",
                type = ElementType.WIDGET_WEATHER,
                xFraction = 0.76f,
                yFraction = 0.05f,
                widthDp = 240f,
                heightDp = 240f
            ),
            DesktopElement(
                id = "w_sys_2",
                screenIndex = 1,
                title = "Система AI Box",
                type = ElementType.WIDGET_SYSTEM,
                xFraction = 0.05f,
                yFraction = 0.08f,
                widthDp = 320f,
                heightDp = 220f
            ),
            DesktopElement(
                id = "w_logo_2",
                screenIndex = 1,
                title = "Логотип авто",
                type = ElementType.CUSTOM_LOGO,
                xFraction = 0.45f,
                yFraction = 0.08f,
                widthDp = 280f,
                heightDp = 220f
            )
        )
    }
}
