package com.aibox.launcher.core.model

/** Откуда берутся обои. Новая система: только видео и системные обои Android. */
enum class WallpaperType(val id: String, val title: String) {
    VIDEO("video", "Живые обои (видео)"),
    SYSTEM("system", "Системные обои Android");

    companion object {
        fun fromId(id: String?): WallpaperType = entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}
