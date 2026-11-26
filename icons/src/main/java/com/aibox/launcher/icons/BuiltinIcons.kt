package com.aibox.launcher.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Camera
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

data class BuiltinIcon(val key: String, val label: String, val vector: ImageVector)

/** Иконки, встроенные в сам лаунчер: их можно поставить на любое приложение. */
object BuiltinIcons {
    val list: List<BuiltinIcon> = listOf(
        BuiltinIcon("apps", "Приложения", Icons.Rounded.Apps),
        BuiltinIcon("home", "Главная", Icons.Rounded.Home),
        BuiltinIcon("settings", "Настройки", Icons.Rounded.Settings),
        BuiltinIcon("widgets", "Виджеты", Icons.Rounded.Widgets),
        BuiltinIcon("phone", "Телефон", Icons.Rounded.Phone),
        BuiltinIcon("message", "Сообщения", Icons.Rounded.Message),
        BuiltinIcon("email", "Почта", Icons.Rounded.Email),
        BuiltinIcon("camera", "Камера", Icons.Rounded.Camera),
        BuiltinIcon("image", "Галерея", Icons.Rounded.Image),
        BuiltinIcon("music", "Музыка", Icons.Rounded.MusicNote),
        BuiltinIcon("video", "Видео", Icons.Rounded.PlayArrow),
        BuiltinIcon("navigation", "Навигация", Icons.Rounded.Navigation),
        BuiltinIcon("map", "Карты", Icons.Rounded.Map),
        BuiltinIcon("cloud", "Облако", Icons.Rounded.Cloud),
        BuiltinIcon("wifi", "Wi-Fi", Icons.Rounded.Wifi),
        BuiltinIcon("bluetooth", "Bluetooth", Icons.Rounded.Bluetooth),
        BuiltinIcon("battery", "Батарея", Icons.Rounded.BatteryFull),
        BuiltinIcon("date", "Дата", Icons.Rounded.DateRange),
        BuiltinIcon("time", "Время", Icons.Rounded.AccessTime),
        BuiltinIcon("star", "Избранное", Icons.Rounded.Star),
        BuiltinIcon("favorite", "Любимое", Icons.Rounded.Favorite)
    )

    fun byKey(key: String): BuiltinIcon? = list.firstOrNull { it.key == key }
}
