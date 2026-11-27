package com.aibox.launcher.settings

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aibox.launcher.core.model.VisualStyle
import com.aibox.launcher.core.model.WallpaperType
import com.aibox.launcher.core.ui.LauncherFonts
import com.aibox.launcher.widgets.WidgetItem
import com.aibox.launcher.widgets.WidgetSizeClass
import com.aibox.launcher.widgets.WidgetType
import com.aibox.launcher.widgets.defaultWidgetBoard
import com.aibox.launcher.widgets.enlargeExistingWidget
import com.aibox.launcher.widgets.findFreeWidgetSpot
import java.util.UUID

enum class ClockStyle(val id: String, val title: String) {
    BOLD("bold", "Жирный"),
    LIGHT("light", "Тонкий"),
    SAMSUNG("samsung", "Samsung"),
    IPHONE("iphone", "iPhone"),
    PIXEL("pixel", "Google Pixel");

    companion object {
        fun fromId(id: String?): ClockStyle = entries.firstOrNull { it.id == id } ?: LIGHT
    }
}

/** Откуда лаунчер берёт погоду для виджетов. */
enum class WeatherSource(val id: String, val title: String) {
    CITY("city", "Город вручную"),
    GPS("gps", "По GPS");

    companion object {
        fun fromId(id: String?): WeatherSource = entries.firstOrNull { it.id == id } ?: CITY
    }
}

/** Шрифты и часы, которые задаёт выбранная обложка стиля OS. */
private data class OsPreset(
    val clock: ClockStyle,
    val interfaceFont: String,
    val clockFont: String,
    val dateFont: String,
    val greetingFont: String
)

/**
 * Все настройки лаунчера. Значения хранятся в SharedPreferences и сразу применяются на экране
 * (это Compose-состояние).
 */
class LauncherSettings(private val prefs: SharedPreferences) {

    init {
        // Старая система виджетов больше не используется — чистим её ключи при первом запуске.
        if (prefs.contains(K_P1) || prefs.contains(K_P2) || prefs.contains(K_SIZES)) {
            prefs.edit()
                .remove(K_P1)
                .remove(K_P2)
                .remove(K_SIZES)
                .remove(K_WIDGET)
                .apply()
        }

        // Старая система обоев (темы, фото, живое небо, динамические сцены) больше не используется.
        val legacyWallpaperKeys = listOf(
            "wallpaper_theme", "sky_precip", "dynamic_scene_wallpaper",
            "dynamic_scene_pack", "dynamic_scene_id", "scene_rotation",
            "scene_seconds", "scene_glass_clock"
        )
        if (legacyWallpaperKeys.any { prefs.contains(it) }) {
            val editor = prefs.edit()
            legacyWallpaperKeys.forEach { editor.remove(it) }
            editor.apply()
        }
        if (prefs.getString(K_WP_TYPE, null) !in setOf("video", "system")) {
            prefs.edit().putString(K_WP_TYPE, "system").putString(K_WP_PATH, "").apply()
        }

        // Виджеты стали крупнее: один раз увеличиваем сохранённые 2×2 → 4×2 и 4×2 → 4×4.
        if (!prefs.getBoolean(K_BOARD_SIZE_V2, false)) {
            listOf(K_BOARD1, K_BOARD2).forEach { key ->
                val raw = prefs.getString(key, null)
                if (!raw.isNullOrBlank()) {
                    val upgraded = mutableListOf<WidgetItem>()
                    parseBoard(raw, emptyList()).forEach { item ->
                        val size = enlargeExistingWidget(item.size)
                        val (column, row) = findFreeWidgetSpot(
                            upgraded, null, size, item.column, item.row
                        )
                        upgraded += item.copy(size = size, column = column, row = row)
                    }
                    prefs.edit().putString(key, encodeBoard(upgraded)).apply()
                }
            }
            prefs.edit().putBoolean(K_BOARD_SIZE_V2, true).apply()
        }
    }

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
    var quickScale by mutableFloatStateOf(prefs.getFloat(K_QUICK, 1f).coerceIn(0.6f, 1.3f))
        private set

    /** Масштаб виджетов на рабочих столах: 60–160%, независимо от общего масштаба. */
    var widgetScale by mutableFloatStateOf(
        prefs.getFloat(K_WIDGET_SCALE, 1f).coerceIn(0.6f, 1.6f)
    )
        private set

    /** Док и капсула горячих клавиш теперь настраиваются отдельно друг от друга. */
    var dockScale by mutableFloatStateOf(
        prefs.getFloat(K_DOCK, prefs.getFloat(K_PANEL, 1f)).coerceIn(0.75f, 1.35f)
    )
        private set
    var hotkeyScale by mutableFloatStateOf(
        prefs.getFloat(K_HOTKEY, prefs.getFloat(K_PANEL, 1f)).coerceIn(0.75f, 1.35f)
    )
        private set

    var clockStyle by mutableStateOf(ClockStyle.fromId(prefs.getString(K_CLOCK_STYLE, null)))
        private set
    var interfaceFontId by mutableStateOf(
        prefs.getString(K_INTERFACE_FONT, LauncherFonts.DEFAULT_ID) ?: LauncherFonts.DEFAULT_ID
    )
        private set
    var clockFontId by mutableStateOf(
        prefs.getString(K_CLOCK_FONT, LauncherFonts.DEFAULT_ID) ?: LauncherFonts.DEFAULT_ID
    )
        private set
    var dateFontId by mutableStateOf(
        prefs.getString(K_DATE_FONT, LauncherFonts.DEFAULT_ID) ?: LauncherFonts.DEFAULT_ID
    )
        private set
    var greetingFontId by mutableStateOf(
        prefs.getString(K_GREETING_FONT, LauncherFonts.DEFAULT_ID) ?: LauncherFonts.DEFAULT_ID
    )
        private set

    /** Брендовый стиль из раздела «Стили OS». Управляет цветами интерфейса. */
    var visualStyle by mutableStateOf(VisualStyle.fromId(prefs.getString(K_VISUAL_STYLE, null)))
        private set

    var musicPackage by mutableStateOf(prefs.getString(K_MUSIC, "") ?: "")
        private set
    var navPackage by mutableStateOf(prefs.getString(K_NAV, "") ?: "")
        private set
    var weatherPackage by mutableStateOf(prefs.getString(K_WEATHER, "") ?: "")
        private set
    var videoPackage by mutableStateOf(prefs.getString(K_VIDEO, "") ?: "")
        private set
    var city by mutableStateOf(prefs.getString(K_CITY, "") ?: "")
        private set
    var weatherSource by mutableStateOf(WeatherSource.fromId(prefs.getString(K_WEATHER_SOURCE, null)))
        private set

    /** Новая iPad-система: позиции виджетов хранятся в ячейках сетки 6×10. */
    var widgetBoard1 by mutableStateOf(
        parseBoard(prefs.getString(K_BOARD1, null), defaultWidgetBoard(1))
    )
        private set
    var widgetBoard2 by mutableStateOf(
        parseBoard(prefs.getString(K_BOARD2, null), defaultWidgetBoard(2))
    )
        private set

    var wallpaperType by mutableStateOf(WallpaperType.fromId(prefs.getString(K_WP_TYPE, null)))
        private set
    var wallpaperPath by mutableStateOf(prefs.getString(K_WP_PATH, "") ?: "")
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

    fun updateClockStyle(value: ClockStyle) {
        clockStyle = value
        prefs.edit().putString(K_CLOCK_STYLE, value.id).apply()
    }

    fun updateInterfaceFont(value: String) {
        interfaceFontId = value
        prefs.edit().putString(K_INTERFACE_FONT, value).apply()
    }

    fun updateClockFont(value: String) {
        clockFontId = value
        prefs.edit().putString(K_CLOCK_FONT, value).apply()
    }

    fun updateDateFont(value: String) {
        dateFontId = value
        prefs.edit().putString(K_DATE_FONT, value).apply()
    }

    fun updateGreetingFont(value: String) {
        greetingFontId = value
        prefs.edit().putString(K_GREETING_FONT, value).apply()
    }

    /**
     * Обложка стиля OS задаёт не только цвета, но и часы, и шрифты даты/приветствия.
     * Остальные настройки (масштабы, обои) не сбрасываются.
     */
    fun applyOsStyle(value: VisualStyle) {
        visualStyle = value
        val preset = when (value) {
            VisualStyle.IPAD -> OsPreset(
                ClockStyle.IPHONE, "sans", "sans", "sans", "sans"
            )
            VisualStyle.SAMSUNG_TAB -> OsPreset(
                ClockStyle.SAMSUNG, "system", "system", "system", "system"
            )
            VisualStyle.APPLE_DUO -> OsPreset(
                ClockStyle.IPHONE, "sans", "sans", "serif", "sans"
            )
            VisualStyle.PIXEL_FOLD -> OsPreset(
                ClockStyle.PIXEL, "system", "system", "sans", "sans"
            )
            else -> null
        }
        if (preset != null) {
            clockStyle = preset.clock
            interfaceFontId = preset.interfaceFont
            clockFontId = preset.clockFont
            dateFontId = preset.dateFont
            greetingFontId = preset.greetingFont
        }
        prefs.edit()
            .putString(K_VISUAL_STYLE, value.name)
            .putString(K_CLOCK_STYLE, clockStyle.id)
            .putString(K_INTERFACE_FONT, interfaceFontId)
            .putString(K_CLOCK_FONT, clockFontId)
            .putString(K_DATE_FONT, dateFontId)
            .putString(K_GREETING_FONT, greetingFontId)
            .apply()
    }

    fun updateMusicPackage(value: String) {
        musicPackage = value
        prefs.edit().putString(K_MUSIC, value).apply()
    }

    fun updateNavPackage(value: String) {
        navPackage = value
        prefs.edit().putString(K_NAV, value).apply()
    }

    fun updateWeatherPackage(value: String) {
        weatherPackage = value
        prefs.edit().putString(K_WEATHER, value).apply()
    }

    fun updateVideoPackage(value: String) {
        videoPackage = value
        prefs.edit().putString(K_VIDEO, value).apply()
    }

    fun updateCity(value: String) {
        city = value.trim()
        prefs.edit().putString(K_CITY, city).apply()
    }

    fun updateWeatherSource(value: WeatherSource) {
        weatherSource = value
        prefs.edit().putString(K_WEATHER_SOURCE, value.id).apply()
    }

    fun widgetBoard(page: Int): List<WidgetItem> = if (page == 1) widgetBoard1 else widgetBoard2

    private fun storeBoard(page: Int, items: List<WidgetItem>) {
        val encoded = encodeBoard(items)
        if (page == 1) {
            widgetBoard1 = items
            prefs.edit().putString(K_BOARD1, encoded).apply()
        } else {
            widgetBoard2 = items
            prefs.edit().putString(K_BOARD2, encoded).apply()
        }
    }

    fun addWidget(page: Int, type: WidgetType, size: WidgetSizeClass = type.defaultSize): WidgetItem {
        val current = widgetBoard(page)
        val (column, row) = findFreeWidgetSpot(current, null, size, 0, 0)
        val item = WidgetItem(
            id = "w-${UUID.randomUUID()}",
            type = type,
            size = size,
            column = column,
            row = row
        )
        storeBoard(page, current + item)
        return item
    }

    fun removeWidget(page: Int, id: String) {
        storeBoard(page, widgetBoard(page).filter { it.id != id })
    }

    fun moveWidget(page: Int, id: String, column: Int, row: Int) {
        val current = widgetBoard(page)
        val item = current.firstOrNull { it.id == id } ?: return
        val (freeColumn, freeRow) = findFreeWidgetSpot(current, id, item.size, column, row)
        storeBoard(
            page,
            current.map {
                if (it.id == id) it.copy(column = freeColumn, row = freeRow) else it
            }
        )
    }

    fun resizeWidget(page: Int, id: String, size: WidgetSizeClass) {
        val current = widgetBoard(page)
        val item = current.firstOrNull { it.id == id } ?: return
        val (freeColumn, freeRow) = findFreeWidgetSpot(current, id, size, item.column, item.row)
        storeBoard(
            page,
            current.map {
                if (it.id == id) {
                    it.copy(size = size, column = freeColumn, row = freeRow)
                } else {
                    it
                }
            }
        )
    }

    fun resetWidgetBoards() {
        storeBoard(1, defaultWidgetBoard(1))
        storeBoard(2, defaultWidgetBoard(2))
    }

    fun updateUiScale(value: Float) {
        uiScale = value.coerceIn(0.6f, 1.3f)
        prefs.edit().putFloat(K_UI, uiScale).apply()
    }

    fun updateStatusScale(value: Float) {
        statusScale = value.coerceIn(0.6f, 1.3f)
        prefs.edit().putFloat(K_STATUS, statusScale).apply()
    }

    fun updateQuickScale(value: Float) {
        quickScale = value.coerceIn(0.6f, 1.3f)
        prefs.edit().putFloat(K_QUICK, quickScale).apply()
    }

    fun updateWidgetScale(value: Float) {
        widgetScale = value.coerceIn(0.6f, 1.6f)
        prefs.edit().putFloat(K_WIDGET_SCALE, widgetScale).apply()
    }

    fun updateDockScale(value: Float) {
        dockScale = value.coerceIn(0.75f, 1.35f)
        prefs.edit().putFloat(K_DOCK, dockScale).apply()
    }

    fun updateHotkeyScale(value: Float) {
        hotkeyScale = value.coerceIn(0.75f, 1.35f)
        prefs.edit().putFloat(K_HOTKEY, hotkeyScale).apply()
    }

    /** Возвращает все размеры к значениям по умолчанию. */
    fun resetSizes() {
        updateUiScale(DEFAULT_UI_SCALE)
        updateStatusScale(1f)
        updateQuickScale(1f)
        updateWidgetScale(1f)
        updatePanelScale(1f)
        updateDockScale(1f)
        updateHotkeyScale(1f)
        updateIconScale(1f)
        updateClockScale(1f)
    }

    fun selectWallpaper(type: WallpaperType, path: String = "") {
        wallpaperType = type
        wallpaperPath = path
        prefs.edit().putString(K_WP_TYPE, type.id).putString(K_WP_PATH, path).apply()
    }

    /** Файл обоев удалён с устройства — вернуться к системным обоям. */
    fun wallpaperFileDeleted(path: String) {
        if (wallpaperPath == path) selectWallpaper(WallpaperType.SYSTEM)
    }

    private fun encodeBoard(items: List<WidgetItem>): String =
        items.joinToString(";") { item ->
            listOf(
                item.id,
                item.type.id,
                item.size.id,
                item.column.toString(),
                item.row.toString()
            ).joinToString(",")
        }

    private fun parseBoard(raw: String?, fallback: List<WidgetItem>): List<WidgetItem> {
        if (raw.isNullOrBlank()) return fallback
        val parsed = raw.split(';').mapNotNull { entry ->
            val parts = entry.split(',')
            if (parts.size != 5) return@mapNotNull null
            val column = parts[3].toIntOrNull() ?: return@mapNotNull null
            val row = parts[4].toIntOrNull() ?: return@mapNotNull null
            WidgetItem(
                id = parts[0],
                type = WidgetType.fromId(parts[1]),
                size = WidgetSizeClass.fromId(parts[2]),
                column = column,
                row = row
            )
        }
        return parsed.ifEmpty { fallback }
    }

    private companion object {
        const val DEFAULT_UI_SCALE = 0.85f
        const val K_UI = "ui_scale"
        const val K_STATUS = "status_scale"
        const val K_QUICK = "quick_scale"
        // Новый ключ: старый "widget_scale" удалялся как legacy и его нельзя переиспользовать.
        const val K_WIDGET_SCALE = "widget_board_scale"
        const val K_DOCK = "dock_scale"
        const val K_HOTKEY = "hotkey_scale"
        // Ключи panel_scale и weather_city совпадают с прежними версиями — настройки не теряются.
        const val K_PANEL = "panel_scale"
        const val K_ICON = "icon_scale"
        const val K_CLOCK = "clock_scale"
        const val K_CLOCK_STYLE = "clock_style"
        const val K_INTERFACE_FONT = "interface_font"
        const val K_CLOCK_FONT = "clock_font"
        const val K_DATE_FONT = "date_font"
        const val K_GREETING_FONT = "greeting_font"
        const val K_VISUAL_STYLE = "visual_style"
        const val K_MUSIC = "music_package"
        const val K_NAV = "nav_package"
        const val K_WEATHER = "weather_package"
        const val K_VIDEO = "video_package"
        const val K_CITY = "weather_city"
        const val K_WEATHER_SOURCE = "weather_source"
        const val K_WP_TYPE = "wallpaper_type"
        const val K_WP_PATH = "wallpaper_path"
        const val K_BOARD1 = "widget_board_page1"
        const val K_BOARD2 = "widget_board_page2"
        const val K_BOARD_SIZE_V2 = "widget_board_size_v2"

        // Удаляемые ключи старой системы виджетов.
        const val K_P1 = "widgets_page1"
        const val K_P2 = "widgets_page2"
        const val K_SIZES = "widget_sizes"
        const val K_WIDGET = "widget_scale"
    }
}
