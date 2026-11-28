package com.aibox.launcher.widgets

/** Сетка рабочего стола: 6 колонок × 10 строк. */
const val WIDGET_GRID_COLUMNS = 6
const val WIDGET_GRID_ROWS = 10

/** Размеры системного AppWidget в ячейках сетки. */
enum class WidgetSizeClass(val id: String, val title: String, val columns: Int, val rows: Int) {
    SMALL("small", "2×2", 2, 2),
    MEDIUM("medium", "4×2", 4, 2),
    LARGE("large", "4×4", 4, 4),
    EXTRA_LARGE("xl", "6×4", 6, 4);

    companion object {
        fun fromId(id: String?): WidgetSizeClass = entries.firstOrNull { it.id == id } ?: MEDIUM
    }
}

/** Одна запись системного AppWidget на рабочем столе. */
data class WidgetItem(
    val id: String,
    val appWidgetId: Int,
    val size: WidgetSizeClass,
    val column: Int,
    val row: Int
)

/** Столы начинаются пустыми: виджеты добавляются системным пикером Android. */
fun defaultWidgetBoard(page: Int): List<WidgetItem> = emptyList()

/** Формат одной записи: appWidgetId,sizeId,column,row. Старые записи с typeId не читаются. */
fun encodeBoard(items: List<WidgetItem>): String = items.joinToString(";") { item ->
    listOf(
        item.appWidgetId.toString(),
        item.size.id,
        item.column.toString(),
        item.row.toString()
    ).joinToString(",")
}

fun parseBoard(raw: String?, fallback: List<WidgetItem>): List<WidgetItem> {
    if (raw.isNullOrBlank()) return fallback
    val parsed = raw.split(';').mapNotNull { entry ->
        val parts = entry.split(',')
        if (parts.size != 4) return@mapNotNull null
        val appWidgetId = parts[0].toIntOrNull() ?: return@mapNotNull null
        val column = parts[2].toIntOrNull() ?: return@mapNotNull null
        val row = parts[3].toIntOrNull() ?: return@mapNotNull null
        if (appWidgetId <= 0) return@mapNotNull null
        WidgetItem(
            id = appWidgetId.toString(),
            appWidgetId = appWidgetId,
            size = WidgetSizeClass.fromId(parts[1]),
            column = column.coerceIn(0, WIDGET_GRID_COLUMNS - 1),
            row = row.coerceIn(0, WIDGET_GRID_ROWS - 1)
        )
    }
    return parsed.ifEmpty { fallback }
}

/** Первое свободное место для нового виджета; пользователь затем переносит его куда угодно. */
fun firstFreeWidgetSpot(items: List<WidgetItem>, size: WidgetSizeClass): Pair<Int, Int> {
    val maxColumn = (WIDGET_GRID_COLUMNS - size.columns).coerceAtLeast(0)
    val maxRow = (WIDGET_GRID_ROWS - size.rows).coerceAtLeast(0)
    for (row in 0..maxRow) {
        for (column in 0..maxColumn) {
            val occupied = items.any { other ->
                column < other.column + other.size.columns &&
                    column + size.columns > other.column &&
                    row < other.row + other.size.rows &&
                    row + size.rows > other.row
            }
            if (!occupied) return column to row
        }
    }
    return 0 to 0
}
