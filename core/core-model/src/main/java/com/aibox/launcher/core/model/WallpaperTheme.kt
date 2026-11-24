package com.aibox.launcher.core.model

/** Темы оформления (обои). AUTO — по времени суток и погоде. */
enum class WallpaperTheme(val id: String, val title: String) {
    STYLISH("stylish", "Стильная"),
    DARK("dark", "Тёмная"),
    BRIGHT("bright", "Яркая"),
    NATURE("nature", "Природа"),
    DAY("day", "День"),
    DAWN("dawn", "Рассвет"),
    NIGHT("night", "Ночь"),
    AUTO("auto", "Авто (по солнцу)");

    companion object {
        fun fromId(id: String?): WallpaperTheme = entries.firstOrNull { it.id == id } ?: AUTO
    }
}
