package com.aibox.launcher.settings

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aibox.launcher.core.model.DesktopWidget
import com.aibox.launcher.core.model.WallpaperTheme
import com.aibox.launcher.core.model.WallpaperType
import com.aibox.launcher.core.model.DesktopWidgetSize
import java.io.File

enum class ClockStyle(val id: String, val title: String) {
    BOLD("bold", "Жирный"),
    LIGHT("light", "Тонкий");

    companion object {
        fun fromId(id: String?): ClockStyle = entries.firstOrNull { it.id == id } ?: LIGHT
    }
}

/**
 * Все настройки лаунчера. Значения хранятся в SharedPreferences и сразу применяются на экране
 * (это Compose-состояние).
 */
class LauncherSettings(private val prefs: SharedPreferences) {

    var panelScale by mutableFloatStateOf(prefs.getFloat(K_PANEL, 1f).coerceIn(0.75f, 1.35f))
        private set
    var iconScale by mutableFloatStateOf(prefs.getFloat(K_ICON, 1f).coerceIn(0.7f, 1.5f))
        private set
    var clockScale by mutableFloatStateOf(prefs.getFloat(K_CLOCK, 1f).coerceIn(0.6f, 1.4f))
        private set

    /** Масштаб всего интерфейса (по умолчанию чуть меньше, чем «как есть»). */
    var uiScale by mutableFloatStateOf(prefs.getFloat(K_UI, DEFAULT_UI_SCALE).coerceIn(0.6f, 1.3f))
        private set
    var statusScale by mutableFloatStateOf(prefs.getFloat(K_STATUS, 1f).coerceIn(0.6f, 1.3f))
        private set
    var widgetScale by mutableFloatStateOf(prefs.getFloat(K_WIDGET, 1f).coerceIn(0.6f, 1f))
        private set
    var quickScale by mutableFloatStateOf(prefs.getFloat(K_QUICK, 1f).coerceIn(0.6f, 1.3f))
        private set

    /** Ширина дока относительно доступного места: меняется растягиванием долгим нажатием. */
    var dockWidth by mutableFloatStateOf(prefs.getFloat(K_DOCK_WIDTH, 1f).coerceIn(0.45f, 1f))
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

    /** Записи рабочего стола: id встроенного виджета («date») или виджет другого приложения («aw:123»). */
    var page1Widgets by mutableStateOf(parseTokens(prefs.getString(K_P1, "date,power,network")))
        private set
    var page2Widgets by mutableStateOf(parseTokens(prefs.getString(K_P2, "weather,music")))
        private set

    /** Дождь и снег в «Живом небе» (самая «тяжёлая» часть анимации). */
    var skyPrecipitation by mutableStateOf(prefs.getBoolean(K_SKY_PRECIP, true))
        private set

    /** Порядок иконок в доке (пакеты); новые приложения встают в конец. */
    var dockOrder by mutableStateOf(parseTokens(prefs.getString(K_DOCK_ORDER, "")))
        private set

    /** Размеры виджетов: ключ «страница:запись», например «1:date». */
    var widgetSizes by mutableStateOf(parseSizes(prefs.getString(K_SIZES, "")))
        private set

    var wallpaperType by mutableStateOf(WallpaperType.fromId(prefs.getString(K_WP_TYPE, null)))
        private set
    var wallpaperPath by mutableStateOf(prefs.getString(K_WP_PATH, "") ?: "")
        private set

    var hiddenInDock by mutableStateOf(
        prefs.getStringSet(K_HIDDEN, emptySet())?.toSet() ?: emptySet<String>()
    )
        private set

    fun updatePanelScale(value: Float) {
        panelScale = value.coerceIn(0.75f, 1.35f)
        prefs.edit().putFloat(K_PANEL, panelScale).apply()
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

    fun widgetsOf(page: Int): List<String> = if (page == 1) page1Widgets else page2Widgets

    private fun storeWidgets(page: Int, tokens: List<String>) {
        val csv = tokens.joinToString(",")
        if (page == 1) {
            page1Widgets = tokens
            prefs.edit().putString(K_P1, csv).apply()
        } else {
            page2Widgets = tokens
            prefs.edit().putString(K_P2, csv).apply()
        }
    }

    /** Включает или выключает встроенный виджет. Новый добавляется в конец ряда. */
    fun setBuiltinWidget(page: Int, widget: DesktopWidget, enabled: Boolean) {
        val current = widgetsOf(page)
        val updated = when {
            enabled && widget.id !in current -> current + widget.id
            !enabled -> current.filter { it != widget.id }
            else -> current
        }
        storeWidgets(page, updated)
    }

    fun addAppWidget(page: Int, appWidgetId: Int) {
        storeWidgets(page, widgetsOf(page) + "aw:$appWidgetId")
    }

    fun removeWidgetToken(page: Int, token: String) {
        storeWidgets(page, widgetsOf(page).filter { it != token })
    }

    fun sizesFor(page: Int): Map<String, DesktopWidgetSize> =
        widgetSizes
            .filterKeys { it.startsWith("$page:") }
            .mapKeys { it.key.substringAfter(':') }

    fun setWidgetSize(page: Int, token: String, size: DesktopWidgetSize) {
        widgetSizes = widgetSizes + ("$page:$token" to size)
        prefs.edit()
            .putString(
                K_SIZES,
                widgetSizes.entries.joinToString(";") {
                    "${it.key}=${it.value.weight},${it.value.height},${it.value.x},${it.value.y}"
                }
            )
            .apply()
    }

    /** Сдвигает виджет на [delta] позиций (−1 — влево, +1 — вправо). */
    fun moveWidget(page: Int, token: String, delta: Int) {
        val list = widgetsOf(page).toMutableList()
        val from = list.indexOf(token)
        val to = from + delta
        if (from < 0 || to < 0 || to >= list.size) return
        list.add(to, list.removeAt(from))
        storeWidgets(page, list)
    }

    fun updateUiScale(value: Float) {
        uiScale = value.coerceIn(0.6f, 1.3f)
        prefs.edit().putFloat(K_UI, uiScale).apply()
    }

    fun updateStatusScale(value: Float) {
        statusScale = value.coerceIn(0.6f, 1.3f)
        prefs.edit().putFloat(K_STATUS, statusScale).apply()
    }

    fun updateWidgetScale(value: Float) {
        widgetScale = value.coerceIn(0.6f, 1f)
        prefs.edit().putFloat(K_WIDGET, widgetScale).apply()
    }

    fun updateQuickScale(value: Float) {
        quickScale = value.coerceIn(0.6f, 1.3f)
        prefs.edit().putFloat(K_QUICK, quickScale).apply()
    }

    fun updateDockWidth(value: Float) {
        dockWidth = value.coerceIn(0.45f, 1f)
        prefs.edit().putFloat(K_DOCK_WIDTH, dockWidth).apply()
    }

    /** Возвращает все размеры к значениям по умолчанию. */
    fun resetSizes() {
        updateUiScale(DEFAULT_UI_SCALE)
        updateStatusScale(1f)
        updateWidgetScale(1f)
        updateQuickScale(1f)
        updatePanelScale(1f)
        updateIconScale(1f)
        updateClockScale(1f)
        updateDockWidth(1f)
    }

    fun updateSkyPrecipitation(value: Boolean) {
        skyPrecipitation = value
        prefs.edit().putBoolean(K_SKY_PRECIP, value).apply()
    }

    fun updateDockOrder(packages: List<String>) {
        dockOrder = packages
        prefs.edit().putString(K_DOCK_ORDER, packages.joinToString(",")).apply()
    }

    fun selectWallpaper(type: WallpaperType, path: String = "") {
        wallpaperType = type
        wallpaperPath = path
        prefs.edit().putString(K_WP_TYPE, type.id).putString(K_WP_PATH, path).apply()
    }

    /** Файл обоев удалён с устройства — вернуться к теме оформления. */
    fun wallpaperFileDeleted(path: String) {
        if (wallpaperPath == path) selectWallpaper(WallpaperType.THEME)
    }

    fun setShownInDock(packageName: String, shown: Boolean) {
        hiddenInDock = if (shown) hiddenInDock - packageName else hiddenInDock + packageName
        prefs.edit().putStringSet(K_HIDDEN, hiddenInDock.toSet()).apply()
    }

    private fun parseSizes(raw: String?): Map<String, DesktopWidgetSize> =
        (raw ?: "").split(';').mapNotNull { entry ->
            val parts = entry.split('=')
            if (parts.size != 2) return@mapNotNull null
            val values = parts[1].split(',')
            val weight = values.getOrNull(0)?.toFloatOrNull()
            val height = values.getOrNull(1)?.toFloatOrNull()
            val x = values.getOrNull(2)?.toFloatOrNull() ?: 0f
            val y = values.getOrNull(3)?.toFloatOrNull() ?: 0f
            if (weight == null || height == null) null else parts[0] to DesktopWidgetSize(weight, height, x, y)
        }.toMap()

    private fun parseTokens(csv: String?): List<String> =
        (csv ?: "").split(',').map { it.trim() }.filter { it.isNotEmpty() }

    private companion object {
        const val DEFAULT_UI_SCALE = 0.85f
        const val K_SIZES = "widget_sizes"
        const val K_SKY_PRECIP = "sky_precip"
        const val K_DOCK_ORDER = "dock_order"
        const val K_UI = "ui_scale"
        const val K_STATUS = "status_scale"
        const val K_WIDGET = "widget_scale"
        const val K_QUICK = "quick_scale"
        const val K_DOCK_WIDTH = "dock_width"
        // Ключи panel_scale и weather_city совпадают с прежними версиями — настройки не теряются.
        const val K_PANEL = "panel_scale"
        const val K_ICON = "icon_scale"
        const val K_CLOCK = "clock_scale"
        const val K_THEME = "wallpaper_theme"
        const val K_CLOCK_STYLE = "clock_style"
        const val K_MUSIC = "music_package"
        const val K_NAV = "nav_package"
        const val K_CITY = "weather_city"
        const val K_P1 = "widgets_page1"
        const val K_P2 = "widgets_page2"
        const val K_WP_TYPE = "wallpaper_type"
        const val K_WP_PATH = "wallpaper_path"
        const val K_HIDDEN = "dock_hidden"
    }
}
