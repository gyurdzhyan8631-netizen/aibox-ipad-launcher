package com.aibox.launcher.core.model

/** Виджеты рабочих столов. Для каждого стола пользователь выбирает свой набор. */
enum class DesktopWidget(val id: String, val title: String) {
    DATE("date", "Дата"),
    POWER("power", "Питание"),
    NETWORK("network", "Сеть"),
    WEATHER("weather", "Погода"),
    MUSIC("music", "Музыка"),
    NAVIGATION("navigation", "Навигация"),
    VIDEO("video", "YouTube"),
    APPS("apps", "Все приложения");

    companion object {
        fun parse(csv: String): List<DesktopWidget> =
            csv.split(',').mapNotNull { id -> entries.firstOrNull { it.id == id.trim() } }
    }
}
