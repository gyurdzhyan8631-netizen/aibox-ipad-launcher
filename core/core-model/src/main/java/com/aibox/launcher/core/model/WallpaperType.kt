package com.aibox.launcher.core.model

/** Откуда берутся обои. */
enum class WallpaperType(val id: String, val title: String) {
    THEME("theme", "Тема оформления"),
    IMAGE("image", "Фото"),
    VIDEO("video", "Живые обои (видео)"),
    SYSTEM("system", "Системные обои Android");

    companion object {
        fun fromId(id: String?): WallpaperType = entries.firstOrNull { it.id == id } ?: THEME
    }
}
