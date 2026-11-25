package com.aibox.launcher.settings

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aibox.launcher.core.model.WallpaperTheme

enum class ClockStyle(val id: String, val title: String) {
    BOLD("bold", "Жирный"),
    LIGHT("light", "Тонкий");

    companion object {
        fun fromId(id: String?): ClockStyle = entries.firstOrNull { it.id == id } ?: BOLD
    }
}

/**
 * Все настройки лаунчера. Значения хранятся в SharedPreferences и сразу применяются на экране
 * (это Compose-состояние).
 */
class LauncherSettings(private val prefs: SharedPreferences) {

    var panelScale by mutableFloatStateOf(prefs.getFloat(K_PANEL, 1f).coerceIn(0.75f, 1.35f))
        private set
    var interfaceScale by mutableFloatStateOf(prefs.getFloat(K_INTERFACE, 0.85f).coerceIn(0.5f, 1.5f))
        private set
    var iconScale by mutableFloatStateOf(prefs.getFloat(K_ICON, 1f).coerceIn(0.7f, 1.5f))
        private set
    var clockScale by mutableFloatStateOf(prefs.getFloat(K_CLOCK, 1f).coerceIn(0.6f, 1.4f))
        private set

    var theme by mutableStateOf(WallpaperTheme.fromId(prefs.getString(K_THEME, null)))
        private set
    var clockStyle by mutableStateOf(ClockStyle.fromId(prefs.getString(K_CLOCK_STYLE, null)))
        private set

    var musicPackage by mutableStateOf(prefs.getString(K_MUSIC, "") ?: "")
        private set
    var navPackage by mutableStateOf(prefs.getString(K_NAV, "") ?: "")
        private set
    var city by mutableStateOf(prefs.getString(K_CITY, "") ?: "")
        private set

    var showDateWidget by mutableStateOf(prefs.getBoolean(K_W_DATE, true))
        private set
    var showPowerWidget by mutableStateOf(prefs.getBoolean(K_W_POWER, true))
        private set
    var showNetworkWidget by mutableStateOf(prefs.getBoolean(K_W_NET, true))
        private set
    var showDateWidgetOnSecondScreen by mutableStateOf(prefs.getBoolean(K_W_DATE_2, false))
        private set
    var showPowerWidgetOnSecondScreen by mutableStateOf(prefs.getBoolean(K_W_POWER_2, false))
        private set
    var showNetworkWidgetOnSecondScreen by mutableStateOf(prefs.getBoolean(K_W_NET_2, false))
        private set

    var homeScreenApps by mutableStateOf(
        prefs.getStringSet(K_HOME_APPS, emptySet())?.toSet() ?: emptySet()
    )
        private set

    var hiddenInDock by mutableStateOf(
        prefs.getStringSet(K_HIDDEN, emptySet())?.toSet() ?: emptySet<String>()
    )
        private set
    var dockAppOrder by mutableStateOf(
        prefs.getString(K_DOCK_ORDER, null)?.split('\n')?.filter(String::isNotBlank).orEmpty()
    )
        private set

    fun updatePanelScale(value: Float) {
        panelScale = value.coerceIn(0.75f, 1.35f)
        prefs.edit().putFloat(K_PANEL, panelScale).apply()
    }

    fun updateInterfaceScale(value: Float) {
        interfaceScale = value.coerceIn(0.5f, 1.5f)
        prefs.edit().putFloat(K_INTERFACE, interfaceScale).apply()
    }

    fun resetInterfaceScale() {
        updateInterfaceScale(0.85f)
    }

    fun updateIconScale(value: Float) {
        iconScale = value.coerceIn(0.7f, 1.5f)
        prefs.edit().putFloat(K_ICON, iconScale).apply()
    }

    fun updateClockScale(value: Float) {
        clockScale = value.coerceIn(0.6f, 1.4f)
        prefs.edit().putFloat(K_CLOCK, clockScale).apply()
    }

    fun updateTheme(value: WallpaperTheme) {
        theme = value
        prefs.edit().putString(K_THEME, value.id).apply()
    }

    fun updateClockStyle(value: ClockStyle) {
        clockStyle = value
        prefs.edit().putString(K_CLOCK_STYLE, value.id).apply()
    }

    fun updateMusicPackage(value: String) {
        musicPackage = value
        prefs.edit().putString(K_MUSIC, value).apply()
    }

    fun updateNavPackage(value: String) {
        navPackage = value
        prefs.edit().putString(K_NAV, value).apply()
    }

    fun updateCity(value: String) {
        city = value.trim()
        prefs.edit().putString(K_CITY, city).apply()
    }

    fun updateWidgets(date: Boolean? = null, power: Boolean? = null, network: Boolean? = null) {
        if (date != null) showDateWidget = date
        if (power != null) showPowerWidget = power
        if (network != null) showNetworkWidget = network
        prefs.edit()
            .putBoolean(K_W_DATE, showDateWidget)
            .putBoolean(K_W_POWER, showPowerWidget)
            .putBoolean(K_W_NET, showNetworkWidget)
            .apply()
    }

    fun updateSecondScreenWidgets(date: Boolean? = null, power: Boolean? = null, network: Boolean? = null) {
        if (date != null) showDateWidgetOnSecondScreen = date
        if (power != null) showPowerWidgetOnSecondScreen = power
        if (network != null) showNetworkWidgetOnSecondScreen = network
        prefs.edit()
            .putBoolean(K_W_DATE_2, showDateWidgetOnSecondScreen)
            .putBoolean(K_W_POWER_2, showPowerWidgetOnSecondScreen)
            .putBoolean(K_W_NET_2, showNetworkWidgetOnSecondScreen)
            .apply()
    }

    fun setShownOnHomeScreen(packageName: String, shown: Boolean) {
        homeScreenApps = if (shown) homeScreenApps + packageName else homeScreenApps - packageName
        prefs.edit().putStringSet(K_HOME_APPS, homeScreenApps.toSet()).apply()
    }

    fun setShownInDock(packageName: String, shown: Boolean) {
        hiddenInDock = if (shown) hiddenInDock - packageName else hiddenInDock + packageName
        prefs.edit().putStringSet(K_HIDDEN, hiddenInDock.toSet()).apply()
    }

    fun updateDockAppOrder(packageNames: List<String>) {
        val ordered = packageNames.distinct() + dockAppOrder.filterNot(packageNames.toSet()::contains)
        dockAppOrder = ordered
        prefs.edit().putString(K_DOCK_ORDER, ordered.joinToString("\n")).apply()
    }

    private companion object {
        // Ключи panel_scale и weather_city совпадают с прежними версиями — настройки не теряются.
        const val K_PANEL = "panel_scale"
        const val K_INTERFACE = "interface_scale"
        const val K_ICON = "icon_scale"
        const val K_CLOCK = "clock_scale"
        const val K_THEME = "wallpaper_theme"
        const val K_CLOCK_STYLE = "clock_style"
        const val K_MUSIC = "music_package"
        const val K_NAV = "nav_package"
        const val K_CITY = "weather_city"
        const val K_W_DATE = "widget_date"
        const val K_W_POWER = "widget_power"
        const val K_W_NET = "widget_network"
        const val K_W_DATE_2 = "widget_date_second_screen"
        const val K_W_POWER_2 = "widget_power_second_screen"
        const val K_W_NET_2 = "widget_network_second_screen"
        const val K_HOME_APPS = "home_screen_apps"
        const val K_HIDDEN = "dock_hidden"
        const val K_DOCK_ORDER = "dock_app_order"
    }
}
