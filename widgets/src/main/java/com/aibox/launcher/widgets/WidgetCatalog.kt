package com.aibox.launcher.widgets

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** Сетка рабочего стола: как на iPad — 2×2, 4×2 и 4×4. */
const val WIDGET_GRID_COLUMNS = 6
const val WIDGET_GRID_ROWS = 10

enum class WidgetCategory(val title: String) {
    WEATHER("Погода"),
    MUSIC("Музыка"),
    CALENDAR("Календарь"),
    CLOCK("Часы"),
    BATTERY("Батарея"),
    NOTES("Заметки"),
    PHOTOS("Фото"),
    SMART("Умный стек")
}

enum class WidgetSizeClass(
    val id: String,
    val title: String,
    val columns: Int,
    val rows: Int
) {
    SMALL("small", "2×2", 2, 2),
    MEDIUM("medium", "4×2", 4, 2),
    LARGE("large", "4×4", 4, 4);

    companion object {
        fun fromId(id: String?): WidgetSizeClass = entries.firstOrNull { it.id == id } ?: MEDIUM
    }
}

/** Полностью новая галерея виджетов: вместо прежнего набора «дата/сеть/навигация». */
enum class WidgetType(
    val id: String,
    val title: String,
    val description: String,
    val category: WidgetCategory,
    val icon: ImageVector,
    val accent: Color,
    val defaultSize: WidgetSizeClass
) {
    WEATHER(
        "weather",
        "Погода",
        "Температура, состояние неба, город и время заката",
        WidgetCategory.WEATHER,
        Icons.Rounded.Cloud,
        Color(0xFF64D2FF),
        WidgetSizeClass.MEDIUM
    ),
    MUSIC(
        "music",
        "Музыка",
        "Активный плеер, обложка и управление воспроизведением",
        WidgetCategory.MUSIC,
        Icons.Rounded.MusicNote,
        Color(0xFFFF375F),
        WidgetSizeClass.MEDIUM
    ),
    CALENDAR(
        "calendar",
        "Календарь",
        "Месяц, сегодняшний день и ближайшие даты",
        WidgetCategory.CALENDAR,
        Icons.Rounded.DateRange,
        Color(0xFFFF9F0A),
        WidgetSizeClass.LARGE
    ),
    CLOCK(
        "clock",
        "Часы",
        "Аналоговые часы, время и дата",
        WidgetCategory.CLOCK,
        Icons.Rounded.AccessTime,
        Color(0xFFBF5AF2),
        WidgetSizeClass.SMALL
    ),
    BATTERY(
        "battery",
        "Батарея",
        "Заряд устройства и состояние питания",
        WidgetCategory.BATTERY,
        Icons.Rounded.BatteryFull,
        Color(0xFF30D158),
        WidgetSizeClass.SMALL
    ),
    NOTES(
        "notes",
        "Заметки",
        "Быстрая заметка и напоминание о дне",
        WidgetCategory.NOTES,
        Icons.Rounded.Description,
        Color(0xFFFFD60A),
        WidgetSizeClass.SMALL
    ),
    PHOTOS(
        "photos",
        "Фото",
        "Подборка воспоминаний по текущей дате",
        WidgetCategory.PHOTOS,
        Icons.Rounded.Image,
        Color(0xFFFF9F0A),
        WidgetSizeClass.LARGE
    ),
    SMART_STACK(
        "smart_stack",
        "Умный стек",
        "Несколько виджетов в одном месте — листайте касанием",
        WidgetCategory.SMART,
        Icons.Rounded.Widgets,
        Color(0xFF5E5CE6),
        WidgetSizeClass.MEDIUM
    );

    companion object {
        fun fromId(id: String?): WidgetType = entries.firstOrNull { it.id == id } ?: WEATHER
    }
}

/** Одна запись на рабочем столе. Позиция задана в ячейках сетки. */
data class WidgetItem(
    val id: String,
    val type: WidgetType,
    val size: WidgetSizeClass,
    val column: Int,
    val row: Int
)

/** Данные погоды для всех погодных виджетов. */
data class WidgetWeatherInfo(
    val temp: String = "",
    val condition: String = "Погода",
    val place: String = "",
    val note: String = "",
    val isDay: Boolean = true,
    val sunrise: String = "06:30",
    val sunset: String = "18:30"
)

private fun item(
    id: String,
    type: WidgetType,
    column: Int,
    row: Int,
    size: WidgetSizeClass = type.defaultSize
) = WidgetItem(id, type, size, column, row)

/** Стартовые рабочие столы — сразу с настоящими iPad-размерами. */
fun defaultWidgetBoard(page: Int): List<WidgetItem> = when (page) {
    1 -> listOf(
        item("p1-clock", WidgetType.CLOCK, 0, 0, WidgetSizeClass.SMALL),
        item("p1-weather", WidgetType.WEATHER, 2, 0, WidgetSizeClass.MEDIUM),
        item("p1-music", WidgetType.MUSIC, 0, 2, WidgetSizeClass.MEDIUM),
        item("p1-calendar", WidgetType.CALENDAR, 2, 2, WidgetSizeClass.LARGE)
    )
    else -> listOf(
        item("p2-stack", WidgetType.SMART_STACK, 0, 0, WidgetSizeClass.MEDIUM),
        item("p2-notes", WidgetType.NOTES, 4, 0, WidgetSizeClass.SMALL),
        item("p2-battery", WidgetType.BATTERY, 4, 2, WidgetSizeClass.SMALL),
        item("p2-photos", WidgetType.PHOTOS, 0, 4, WidgetSizeClass.LARGE)
    )
}

fun nextWidgetSize(size: WidgetSizeClass): WidgetSizeClass = when (size) {
    WidgetSizeClass.SMALL -> WidgetSizeClass.MEDIUM
    WidgetSizeClass.MEDIUM -> WidgetSizeClass.LARGE
    WidgetSizeClass.LARGE -> WidgetSizeClass.SMALL
}

private fun rectsOverlap(
    left: Int,
    top: Int,
    size: WidgetSizeClass,
    other: WidgetItem
): Boolean =
    left < other.column + other.size.columns &&
        left + size.columns > other.column &&
        top < other.row + other.size.rows &&
        top + size.rows > other.row

/** Ближайшее свободное место: новые виджеты не накладываются друг на друга. */
fun findFreeWidgetSpot(
    items: List<WidgetItem>,
    excludeId: String?,
    size: WidgetSizeClass,
    preferredColumn: Int,
    preferredRow: Int
): Pair<Int, Int> {
    val maxColumn = (WIDGET_GRID_COLUMNS - size.columns).coerceAtLeast(0)
    val maxRow = (WIDGET_GRID_ROWS - size.rows).coerceAtLeast(0)
    fun isFree(column: Int, row: Int): Boolean = items.none { other ->
        other.id != excludeId && rectsOverlap(column, row, size, other)
    }

    val startColumn = preferredColumn.coerceIn(0, maxColumn)
    val startRow = preferredRow.coerceIn(0, maxRow)
    if (isFree(startColumn, startRow)) return startColumn to startRow

    for (radius in 1..(maxColumn + maxRow + 2)) {
        for (deltaRow in -radius..radius) {
            for (deltaColumn in -radius..radius) {
                val column = (startColumn + deltaColumn).coerceIn(0, maxColumn)
                val row = (startRow + deltaRow).coerceIn(0, maxRow)
                if (isFree(column, row)) return column to row
            }
        }
    }
    return 0 to 0
}
