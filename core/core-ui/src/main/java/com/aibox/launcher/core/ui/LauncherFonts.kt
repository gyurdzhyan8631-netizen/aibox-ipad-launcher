package com.aibox.launcher.core.ui

import androidx.compose.ui.text.font.FontFamily

data class LauncherFont(val id: String, val title: String, val family: FontFamily)

/** Встроенные шрифты лаунчера — без ExternalFont: работают на любом устройстве. */
object LauncherFonts {
    const val DEFAULT_ID = "system"

    val all: List<LauncherFont> = listOf(
        LauncherFont("system", "Системный", FontFamily.Default),
        LauncherFont("sans", "Sans Serif", FontFamily.SansSerif),
        LauncherFont("serif", "Serif", FontFamily.Serif),
        LauncherFont("mono", "Моноширинный", FontFamily.Monospace),
        LauncherFont("cursive", "Рукописный", FontFamily.Cursive)
    )

    fun family(id: String?): FontFamily =
        all.firstOrNull { it.id == id }?.family ?: FontFamily.Default

    fun title(id: String?): String =
        all.firstOrNull { it.id == id }?.title ?: all.first().title
}
