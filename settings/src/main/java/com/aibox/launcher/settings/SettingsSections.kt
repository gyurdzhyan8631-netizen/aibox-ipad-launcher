package com.aibox.launcher.settings

import android.app.Activity
import android.graphics.BitmapFactory
import android.content.Intent
import androidx.compose.material.icons.rounded.Tune
import com.aibox.launcher.core.ui.LauncherFonts
import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image as ImageIcon
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.model.InstalledApp
import com.aibox.launcher.core.model.WallpaperType
import com.aibox.launcher.core.model.WallpaperTheme
import com.aibox.launcher.core.ui.glassSurface
import com.aibox.launcher.icons.AppIconPreview
import com.aibox.launcher.icons.BuiltinIcons
import com.aibox.launcher.icons.CustomIconStore
import com.aibox.launcher.icons.rememberAppIcon
import com.aibox.launcher.wallpaper.DynamicScene
import com.aibox.launcher.wallpaper.wallpaperPreviewColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.withContext
import java.time.LocalTime
import kotlin.math.roundToInt

// ---------------------------------------------------------------- состояние сети

private data class NetSummary(val wifi: Boolean, val bluetooth: Boolean, val vpn: Boolean)

@Suppress("DEPRECATION")
private fun readNetSummary(context: Context): NetSummary {
    var wifi = false
    var vpn = false
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    if (cm != null) {
        for (network in cm.allNetworks) {
            val caps = cm.getNetworkCapabilities(network) ?: continue
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) wifi = true
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) vpn = true
        }
    }
    val bluetooth = runCatching {
        Settings.Global.getInt(context.contentResolver, "bluetooth_on", 0) == 1
    }.getOrDefault(false)
    return NetSummary(wifi, bluetooth, vpn)
}

@Composable
private fun rememberNetSummary(context: Context): State<NetSummary> =
    produceState(initialValue = NetSummary(false, false, false), key1 = context) {
        while (true) {
            value = withContext(Dispatchers.IO) { readNetSummary(context) }
            delay(3_000L)
        }
    }

// ---------------------------------------------------------------- Главная

@Composable
internal fun HomeSection(
    settings: LauncherSettings,
    weatherStatus: String
) {
    SectionTitle("Погода и динамические обои")
    SettingsGroup {
        var draft by remember(settings.city) { mutableStateOf(settings.city) }
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Город", color = Color.White, fontSize = 17.sp)
            val fieldShape = RoundedCornerShape(14.dp)
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { settings.updateCity(draft) }),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(fieldShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(1.dp, Color.White.copy(alpha = 0.18f), fieldShape)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        if (draft.isEmpty()) {
                            Text("Например: Москва", color = Color.White.copy(alpha = 0.4f), fontSize = 17.sp)
                        }
                        inner()
                    }
                }
            )
            Text(weatherStatus, color = SettingsColors.textSecondary, fontSize = 12.sp)
            PrimaryButton("Сохранить город") { settings.updateCity(draft) }
        }
    }

    SettingsGroup {
        SettingsRow(
            icon = Icons.Rounded.Navigation,
            iconColor = Color(0xFF0A84FF),
            title = "Источник погоды",
            value = settings.weatherSource.title,
            subtitle = when (settings.weatherSource) {
                WeatherSource.GPS -> "Город и погода определяются по GPS. Разрешение запросит лаунчер"
                WeatherSource.CITY -> "Используется город, указанный выше"
            },
            onClick = {
                settings.updateWeatherSource(
                    if (settings.weatherSource == WeatherSource.CITY) WeatherSource.GPS else WeatherSource.CITY
                )
            }
        )
        RowDivider()
        SwitchRow(
            icon = Icons.Rounded.WbSunny,
            iconColor = Color(0xFFFF9F0A),
            title = "Динамические обои на 1-м столе",
            subtitle = "6 сцен: рассвет, день, ночь, дождь, снег, ветер — по погоде и времени суток",
            checked = settings.dynamicWallpaper,
            onCheckedChange = { settings.updateDynamicWallpaper(it) }
        )
    }

    SectionTitle("Рабочие столы")
    SettingsGroup {
        SettingsRow(
            icon = Icons.Rounded.Widgets,
            iconColor = SettingsColors.accent,
            title = "Система виджетов",
            value = "iPad",
            subtitle = "Сетка 6×10, размеры 2×2 / 4×2 / 4×4 / 6×4, умный стек",
            showChevron = false
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.Refresh,
            iconColor = Color(0xFFFF9F0A),
            title = "Сбросить рабочие столы",
            subtitle = "Вернуть стандартный набор виджетов",
            onClick = { settings.resetWidgetBoards() }
        )
    }
    HintText("Добавляйте виджеты кнопкой «+» в режиме правки. Перетаскивайте пальцем, а кнопки «+» и «−» на виджете меняют его размер.")
}

// ---------------------------------------------------------------- Wi-Fi / Bluetooth

@Composable
internal fun WifiSection() {
    val context = LocalContext.current
    val net by rememberNetSummary(context)
    SettingsGroup {
        SettingsRow(
            icon = Icons.Rounded.Wifi,
            iconColor = Color(0xFF0A84FF),
            title = "Wi-Fi",
            value = if (net.wifi) "Подключено" else "Не подключено",
            showChevron = false
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.Settings,
            iconColor = Color(0xFF8E8E93),
            title = "Выбрать сеть",
            subtitle = "Откроется системная панель Wi-Fi",
            onClick = { SystemSettingsLauncher.openWifi(context) }
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.VpnKey,
            iconColor = Color(0xFF5E5CE6),
            title = "VPN",
            value = if (net.vpn) "Включён" else "Выключен",
            onClick = { SystemSettingsLauncher.open(context, Settings.ACTION_VPN_SETTINGS) }
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.Language,
            iconColor = Color(0xFF30B0C7),
            title = "Сеть и интернет",
            onClick = { SystemSettingsLauncher.open(context, Settings.ACTION_WIRELESS_SETTINGS) }
        )
    }
    HintText("Android не даёт лаунчеру включать Wi-Fi напрямую, поэтому выбор сети делается в системной панели.")
}

@Composable
internal fun BluetoothSection() {
    val context = LocalContext.current
    val net by rememberNetSummary(context)
    SettingsGroup {
        SettingsRow(
            icon = Icons.Rounded.Bluetooth,
            iconColor = Color(0xFF0A84FF),
            title = "Bluetooth",
            value = if (net.bluetooth) "Включён" else "Выключен",
            showChevron = false
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.Settings,
            iconColor = Color(0xFF8E8E93),
            title = "Устройства Bluetooth",
            subtitle = "Подключение и сопряжение",
            onClick = { SystemSettingsLauncher.open(context, Settings.ACTION_BLUETOOTH_SETTINGS) }
        )
    }
}

// ---------------------------------------------------------------- Экран и яркость

@Composable
internal fun DisplaySection() {
    val context = LocalContext.current
    val activity = context as? Activity
    var brightness by remember {
        mutableIntStateOf(
            runCatching {
                val raw = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                (raw * 100 / 255).coerceIn(5, 100)
            }.getOrDefault(70)
        )
    }
    SettingsGroup {
        SliderRow(
            title = "Яркость",
            valueText = "$brightness%",
            value = brightness.toFloat(),
            valueRange = 5f..100f,
            onValueChange = { v ->
                brightness = v.roundToInt()
                val window = activity?.window
                if (window != null) {
                    val params = window.attributes
                    params.screenBrightness = brightness / 100f
                    window.attributes = params
                }
            }
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.WbSunny,
            iconColor = Color(0xFF0A84FF),
            title = "Системные настройки экрана",
            onClick = { SystemSettingsLauncher.open(context, Settings.ACTION_DISPLAY_SETTINGS) }
        )
    }
    HintText("Яркость меняется для окна лаунчера. Системная яркость регулируется в системных настройках.")
}

// ---------------------------------------------------------------- Звук

@Composable
internal fun SoundSection() {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val maxVol = (audio?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1)
    var volume by remember {
        mutableIntStateOf(((audio?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0) * 100 / maxVol))
    }
    var ringer by remember { mutableIntStateOf(audio?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL) }

    SettingsGroup {
        SliderRow(
            title = "Громкость мультимедиа",
            valueText = "$volume%",
            value = volume.toFloat(),
            valueRange = 0f..100f,
            onValueChange = { v ->
                volume = v.roundToInt()
                runCatching {
                    audio?.setStreamVolume(AudioManager.STREAM_MUSIC, volume * maxVol / 100, 0)
                }
            }
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.VolumeUp,
            iconColor = Color(0xFFFF375F),
            title = "Режим звука",
            value = when (ringer) {
                AudioManager.RINGER_MODE_SILENT -> "Без звука"
                AudioManager.RINGER_MODE_VIBRATE -> "Вибро"
                else -> "Обычный"
            },
            onClick = {
                val next = when (ringer) {
                    AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                    AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                    else -> AudioManager.RINGER_MODE_NORMAL
                }
                runCatching { audio?.ringerMode = next }
                ringer = audio?.ringerMode ?: next
            }
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.Settings,
            iconColor = Color(0xFF8E8E93),
            title = "Системные настройки звука",
            onClick = { SystemSettingsLauncher.open(context, Settings.ACTION_SOUND_SETTINGS) }
        )
    }
}

// ---------------------------------------------------------------- Персонализация

private fun currentPeriodTitle(): String {
    val hour = LocalTime.now().hour
    return when (hour) {
        in 5..10 -> "Утро"
        in 11..17 -> "День"
        in 18..21 -> "Вечер"
        else -> "Ночь"
    }
}

private fun percentOptions(values: List<Int>) =
    values.map { PickerOption(it.toString(), "$it%") }

@Composable
internal fun PersonalizationSection(
    settings: LauncherSettings,
    apps: List<InstalledApp>,
    onPick: (PickerRequest) -> Unit,
    onOpenSection: (SettingsSection) -> Unit
) {
    val appLabels = remember(apps) { apps.associate { it.packageName to it.label } }
    val appOptions = remember(apps) {
        listOf(PickerOption("", "Авто")) + apps.map { PickerOption(it.packageName, it.label) }
    }

    // --- карточка «Обои» ---
    val (previewTop, previewBottom) = wallpaperPreviewColors(settings.theme)
    SettingsGroup {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenSection(SettingsSection.WALLPAPERS) }
                .padding(14.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(width = 150.dp, height = 72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (settings.wallpaperType == WallpaperType.THEME || settings.wallpaperType == WallpaperType.LIVE_SKY) {
                            Brush.verticalGradient(listOf(previewTop, previewBottom))
                        } else {
                            Brush.verticalGradient(listOf(Color(0xFF2A2E3B), Color(0xFF14161E)))
                        }
                    )
            ) {
                if (settings.wallpaperType != WallpaperType.THEME) {
                    Text(settings.wallpaperType.title, color = Color.White, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Обои", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    when (settings.wallpaperType) {
                        WallpaperType.THEME -> "Тема: ${settings.theme.title}"
                        WallpaperType.LIVE_SKY -> "Живое небо · тема ${settings.theme.title}"
                        else -> settings.wallpaperType.title
                    },
                    color = SettingsColors.textSecondary,
                    fontSize = 14.sp
                )
                Text(
                    "Добавить фото, живые обои, системные обои",
                    color = SettingsColors.textSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }

    // --- темы оформления ---
    SectionTitle("Темы оформления")
    WallpaperTheme.entries.chunked(4).forEach { rowThemes ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            rowThemes.forEach { theme ->
                ThemeCard(
                    theme = theme,
                    selected = theme == settings.theme &&
                        (settings.wallpaperType == WallpaperType.THEME || settings.wallpaperType == WallpaperType.LIVE_SKY),
                    onClick = {
                        settings.updateTheme(theme)
                        if (settings.wallpaperType != WallpaperType.LIVE_SKY) {
                            settings.selectWallpaper(WallpaperType.THEME)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
    HintText("Сейчас: ${currentPeriodTitle()}")

    // --- параметры ---
    SettingsGroup {
        SettingsRow(
            icon = Icons.Rounded.Apps,
            iconColor = Color(0xFF5E5CE6),
            title = "Стиль иконок",
            value = "Стеклянный",
            subtitle = "Другие стили появятся позже",
            enabled = false
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.Apps,
            iconColor = Color(0xFF5E5CE6),
            title = "Размер иконок",
            value = "${(settings.iconScale * 100).roundToInt()}%",
            onClick = {
                onPick(
                    PickerRequest(
                        title = "Размер иконок",
                        options = percentOptions(listOf(80, 90, 100, 110, 125, 140)),
                        selectedKey = (settings.iconScale * 100).roundToInt().toString(),
                        onSelect = { settings.updateIconScale(it.toInt() / 100f) }
                    )
                )
            }
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.MusicNote,
            iconColor = Color(0xFFFF375F),
            title = "Музыкальное приложение",
            value = appLabels[settings.musicPackage] ?: "Авто",
            onClick = {
                onPick(
                    PickerRequest(
                        title = "Музыкальное приложение",
                        options = appOptions,
                        selectedKey = settings.musicPackage,
                        onSelect = { settings.updateMusicPackage(it) }
                    )
                )
            }
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.Navigation,
            iconColor = Color(0xFF30D158),
            title = "Навигатор",
            value = appLabels[settings.navPackage] ?: "Авто",
            onClick = {
                onPick(
                    PickerRequest(
                        title = "Навигатор",
                        options = appOptions,
                        selectedKey = settings.navPackage,
                        onSelect = { settings.updateNavPackage(it) }
                    )
                )
            }
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.Cloud,
            iconColor = Color(0xFF64D2FF),
            title = "Погодное приложение",
            value = appLabels[settings.weatherPackage] ?: "Авто",
            onClick = {
                onPick(
                    PickerRequest(
                        title = "Погодное приложение",
                        options = appOptions,
                        selectedKey = settings.weatherPackage,
                        onSelect = { settings.updateWeatherPackage(it) }
                    )
                )
            }
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.Videocam,
            iconColor = Color(0xFFFF453A),
            title = "Видео / YouTube",
            value = appLabels[settings.videoPackage] ?: "Авто",
            onClick = {
                onPick(
                    PickerRequest(
                        title = "Видео / YouTube",
                        options = appOptions,
                        selectedKey = settings.videoPackage,
                        onSelect = { settings.updateVideoPackage(it) }
                    )
                )
            }
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.Palette,
            iconColor = Color(0xFF5E5CE6),
            title = "Пак иконок",
            value = "Выбрать",
            subtitle = "Готовые пакеты иконок с устройства",
            onClick = { onOpenSection(SettingsSection.ICONS) }
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.AccessTime,
            iconColor = Color(0xFFFF9F0A),
            title = "Стиль часов",
            value = settings.clockStyle.title,
            onClick = {
                onPick(
                    PickerRequest(
                        title = "Стиль часов",
                        options = ClockStyle.entries.map { PickerOption(it.id, it.title) },
                        selectedKey = settings.clockStyle.id,
                        onSelect = { settings.updateClockStyle(ClockStyle.fromId(it)) }
                    )
                )
            }
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.AccessTime,
            iconColor = Color(0xFFFF9F0A),
            title = "Размер часов",
            value = "${(settings.clockScale * 100).roundToInt()}%",
            onClick = {
                onPick(
                    PickerRequest(
                        title = "Размер часов",
                        options = percentOptions(listOf(70, 85, 100, 115, 130)),
                        selectedKey = (settings.clockScale * 100).roundToInt().toString(),
                        onSelect = { settings.updateClockScale(it.toInt() / 100f) }
                    )
                )
            }
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.Widgets,
            iconColor = Color(0xFF0A84FF),
            title = "Виджеты главного экрана",
            value = "Изменить",
            onClick = { onOpenSection(SettingsSection.HOME) }
        )
    }
}

/** Отдельный раздел шрифтов: весь интерфейс и часы настраиваются независимо. */
@Composable
internal fun FontSection(settings: LauncherSettings, onPick: (PickerRequest) -> Unit) {
    val fonts = remember { LauncherFonts.all.map { PickerOption(it.id, it.title) } }
    SectionTitle("Шрифты")
    SettingsGroup {
        SettingsRow(
            icon = Icons.Rounded.TextFields,
            iconColor = SettingsColors.accent,
            title = "Шрифт интерфейса",
            value = LauncherFonts.title(settings.interfaceFontId),
            subtitle = "Применяется ко всему лаунчеру и настройкам",
            onClick = {
                onPick(
                    PickerRequest(
                        title = "Шрифт интерфейса",
                        options = fonts,
                        selectedKey = settings.interfaceFontId,
                        onSelect = { settings.updateInterfaceFont(it) }
                    )
                )
            }
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.AccessTime,
            iconColor = Color(0xFFFF9F0A),
            title = "Шрифт часов",
            value = LauncherFonts.title(settings.clockFontId),
            subtitle = "Дата и приветствие не меняют размер",
            onClick = {
                onPick(
                    PickerRequest(
                        title = "Шрифт часов",
                        options = fonts,
                        selectedKey = settings.clockFontId,
                        onSelect = { settings.updateClockFont(it) }
                    )
                )
            }
        )
    }
    HintText("Системный шрифт повторяет шрифт Android. Остальные встроены в лаунчер и не требуют загрузки.")
}

@Composable
private fun ThemeCard(
    theme: WallpaperTheme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (top, bottom) = wallpaperPreviewColors(theme)
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) SettingsColors.accent else Color(0x33FFFFFF),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Text(
            text = theme.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.BottomStart)
        )
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
            )
        }
    }
}

// ---------------------------------------------------------------- Сторонние иконки

/**
 * Раздел «Сторонние иконки»: можно добавить скачанные картинки в библиотеку
 * и поставить любую из них на любое приложение.
 */
@Composable
internal fun CustomIconsSection(
    settings: LauncherSettings,
    apps: List<InstalledApp>,
    onPick: (PickerRequest) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var library by remember { mutableStateOf(CustomIconStore.library(context)) }
    var packs by remember { mutableStateOf<List<IconPackInfo>>(emptyList()) }
    var pendingApp by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        packs = withContext(Dispatchers.IO) { IconPackRepository.installed(context) }
    }

    fun refresh() {
        library = CustomIconStore.library(context)
    }

    val pickForLibrary = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val added = withContext(Dispatchers.IO) { CustomIconStore.importToLibrary(context, uri) != null }
            if (added) refresh() else Toast.makeText(context, "Не удалось добавить иконку", Toast.LENGTH_SHORT).show()
        }
    }

    val pickForApp = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val packageName = pendingApp
        pendingApp = null
        if (uri == null || packageName == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) { CustomIconStore.set(context, packageName, uri) }
            if (!ok) Toast.makeText(context, "Не удалось поставить иконку", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchFilePicker(picker: androidx.activity.result.ActivityResultLauncher<String>, type: String = "image/*") {
        runCatching { picker.launch(type) }.onFailure {
            Toast.makeText(context, "На устройстве нет приложения для выбора файлов", Toast.LENGTH_LONG).show()
        }
    }
    var targetApp by remember { mutableStateOf<String?>(null) }
    var packPicker by remember { mutableStateOf<IconPackInfo?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    SectionTitle("Пакеты иконок на устройстве")
    if (packs.isEmpty()) {
        HintText("Готовые пакеты иконок не найдены. Установите пакет иконок — он появится здесь.")
    } else {
        SettingsGroup {
            packs.forEachIndexed { index, pack ->
                if (index > 0) RowDivider()
                SettingsRow(
                    icon = Icons.Rounded.Apps,
                    iconColor = Color(0xFFBF5AF2),
                    title = pack.label,
                    subtitle = "Открыть набор иконок и выбрать нужную вручную",
                    onClick = { packPicker = pack }
                )
            }
        }
    }

    SectionTitle("Встроенные иконки лаунчера")
    SettingsGroup {
        SettingsRow(
            icon = Icons.Rounded.Apps,
            iconColor = Color(0xFF0A84FF),
            title = "Приложение: " + (targetApp?.let { pkg ->
                apps.firstOrNull { it.packageName == pkg }?.label ?: pkg
            } ?: "не выбрано"),
            subtitle = "Нажмите, чтобы выбрать приложение, затем иконку ниже",
            onClick = {
                onPick(
                    PickerRequest(
                        title = "Для какого приложения?",
                        options = listOf(PickerOption("", "Отмена")) + apps.map {
                            PickerOption(it.packageName, it.label)
                        },
                        selectedKey = targetApp.orEmpty(),
                        onSelect = { key -> if (key.isNotEmpty()) targetApp = key }
                    )
                )
            }
        )
        RowDivider()
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            BuiltinIcons.list.forEach { item ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x33FFFFFF))
                        .clickable {
                            val pkg = targetApp
                            if (pkg == null) {
                                Toast.makeText(context, "Сначала выберите приложение", Toast.LENGTH_SHORT).show()
                            } else {
                                CustomIconStore.setBuiltin(context, pkg, item.key)
                                Toast.makeText(context, "Иконка применена", Toast.LENGTH_SHORT).show()
                            }
                        }
                ) {
                    Icon(
                        imageVector = item.vector,
                        contentDescription = item.label,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }

    SectionTitle("Свои иконки из файла")
    HintText("Добавьте скачанную картинку в библиотеку и назначьте её приложению.")
    SectionTitle("Библиотека иконок")
    SettingsGroup {
        SettingsRow(
            icon = Icons.Rounded.Add,
            iconColor = Color(0xFF30D158),
            title = "Добавить скачанную иконку",
            subtitle = "Выбрать файл с устройства",
            onClick = { launchFilePicker(pickForLibrary) }
        )
        library.forEachIndexed { index, file ->
            RowDivider()
            val bitmap = remember(file.path, CustomIconStore.version) {
                runCatching { BitmapFactory.decodeFile(file.path)?.asImageBitmap() }.getOrNull()
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onPick(
                            PickerRequest(
                                title = "Иконка ${index + 1} — поставить на приложение",
                                options = listOf(PickerOption("", "Отмена")) + apps.map {
                                    PickerOption(it.packageName, it.label)
                                },
                                selectedKey = "",
                                onSelect = { key ->
                                    if (key.isEmpty()) return@PickerRequest
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            CustomIconStore.setFromLibrary(context, key, file)
                                        }
                                    }
                                }
                            )
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "Иконка ${index + 1}",
                    color = Color.White,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Удалить иконку",
                    tint = Color(0xFFFF453A),
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            CustomIconStore.deleteFromLibrary(context, file)
                            refresh()
                        }
                        .padding(6.dp)
                )
            }
        }
        if (library.isEmpty()) {
            RowDivider()
            Text(
                text = "Библиотека пуста",
                color = SettingsColors.textSecondary,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }

    SectionTitle("Иконки приложений")
    SettingsGroup {
        apps.forEachIndexed { index, app ->
            if (index > 0) RowDivider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val options = mutableListOf(
                            PickerOption("file", "Выбрать файл…"),
                            PickerOption("reset", "Вернуть стандартную иконку")
                        )
                        library.forEachIndexed { i, _ ->
                            options += PickerOption("lib:$i", "Из библиотеки · иконка ${i + 1}")
                        }
                        onPick(
                            PickerRequest(
                                title = app.label,
                                options = options,
                                selectedKey = "",
                                onSelect = { key ->
                                    when {
                                        key == "file" -> {
                                            pendingApp = app.packageName
                                            launchFilePicker(pickForApp)
                                        }
                                        key == "reset" -> CustomIconStore.clear(context, app.packageName)
                                        key.startsWith("lib:") -> {
                                            val file = library.getOrNull(key.removePrefix("lib:").toIntOrNull() ?: -1)
                                            if (file != null) {
                                                scope.launch {
                                                    withContext(Dispatchers.IO) {
                                                        CustomIconStore.setFromLibrary(context, app.packageName, file)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                AppIconPreview(app.packageName, app.label, 40.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = app.label, color = Color.White, fontSize = 17.sp)
                    if (CustomIconStore.has(context, app.packageName)) {
                        Text("Своя иконка", color = SettingsColors.accent, fontSize = 12.sp)
                    }
                }
            }
        }
    }
    }
    packPicker?.let { picked ->
        PackIconPickerDialog(
            pack = picked,
            apps = apps,
            onDismiss = { packPicker = null },
            onApplied = { refresh() }
        )
    }
    }
}

/** Окно выбора конкретной иконки из установленного пакета иконок. */
@Composable
private fun PackIconPickerDialog(
    pack: IconPackInfo,
    apps: List<InstalledApp>,
    onDismiss: () -> Unit,
    onApplied: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var app by remember { mutableStateOf<InstalledApp?>(null) }
    var icons by remember { mutableStateOf<List<PackIconInfo>>(emptyList()) }
    LaunchedEffect(pack.packageName) {
        icons = withContext(Dispatchers.IO) { IconPackRepository.icons(context, pack) }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xDD000000))
            .clickable(onClick = onDismiss)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .heightIn(max = 560.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xF5171A24))
                .glassSurface(RoundedCornerShape(24.dp), strength = 0.7f)
                .clickable(enabled = true, onClick = {})
                .padding(16.dp)
        ) {
            Text(text = "Иконки: ${pack.label}", color = Color.White, fontSize = 20.sp)
            Text(
                text = "Приложение: ${app?.label ?: "не выбрано"}",
                color = SettingsColors.textSecondary,
                fontSize = 14.sp
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                apps.forEach { item ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (item.packageName == app?.packageName) Color(0x550A84FF)
                                else Color(0x22FFFFFF)
                            )
                            .clickable { app = item }
                    ) {
                        AppIconPreview(item.packageName, item.label, 38.dp)
                    }
                }
            }
            Text(
                text = if (icons.isEmpty()) "Иконки в пакете не найдены" else "Выберите иконку",
                color = SettingsColors.textSecondary,
                fontSize = 14.sp
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                icons.forEach { info ->
                    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, info.name, pack.packageName) {
                        value = withContext(Dispatchers.IO) {
                            IconPackRepository.iconBitmap(context, pack, info.name)
                                ?.asImageBitmap()
                        }
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x22FFFFFF))
                            .clickable {
                                val chosen = app
                                if (chosen == null) {
                                    Toast.makeText(context, "Сначала выберите приложение", Toast.LENGTH_SHORT).show()
                                } else {
                                    scope.launch {
                                        val ok = withContext(Dispatchers.IO) {
                                            IconPackRepository.setIcon(context, pack, info.name, chosen.packageName)
                                        }
                                        Toast.makeText(
                                            context,
                                            if (ok) "Иконка применена" else "Не удалось применить иконку",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        onApplied()
                                    }
                                    onDismiss()
                                }
                            }
                    ) {
                        val iconBitmap = bitmap
                        if (iconBitmap != null) {
                            Image(
                                bitmap = iconBitmap,
                                contentDescription = info.label,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Приложения

@Composable
internal fun AppsSection(
    settings: LauncherSettings,
    apps: List<InstalledApp>,
    onPick: (PickerRequest) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingPackage by remember { mutableStateOf<String?>(null) }

    val pickIcon = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val packageName = pendingPackage
        pendingPackage = null
        if (uri != null && packageName != null) {
            scope.launch {
                val ok = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    CustomIconStore.set(context, packageName, uri)
                }
                if (!ok) Toast.makeText(context, "Не удалось поставить иконку", Toast.LENGTH_SHORT).show()
            }
        }
    }

    HintText("Нажмите на приложение, чтобы поставить свою иконку. Все приложения автоматически есть в боковом доке.")
    SettingsGroup {
        apps.forEachIndexed { index, app ->
            if (index > 0) RowDivider()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onPick(
                            PickerRequest(
                                title = app.label,
                                options = listOf(
                                    PickerOption("icon", "Выбрать свою иконку…"),
                                    PickerOption("reset", "Вернуть стандартную иконку"),
                                    PickerOption("info", "Системные настройки приложения")
                                ),
                                selectedKey = "",
                                onSelect = { key ->
                                    when (key) {
                                        "icon" -> {
                                            pendingPackage = app.packageName
                                            runCatching { pickIcon.launch("image/*") }.onFailure {
                                                Toast.makeText(
                                                    context,
                                                    "На устройстве нет приложения для выбора файлов",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                        "reset" -> CustomIconStore.clear(context, app.packageName)
                                        else -> SystemSettingsLauncher.openAppDetails(context, app.packageName)
                                    }
                                }
                            )
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                AppIconPreview(app.packageName, app.label, 40.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = app.label, color = Color.White, fontSize = 17.sp)
                    if (CustomIconStore.has(context, app.packageName)) {
                        Text("Своя иконка", color = SettingsColors.accent, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Система

@Composable
internal fun SystemSection() {
    val context = LocalContext.current
    fun open(action: String) = SystemSettingsLauncher.open(context, action)

    SettingsGroup {
        SettingsRow(Icons.Rounded.Wifi, Color(0xFF0A84FF), "Сеть и интернет",
            onClick = { open(Settings.ACTION_WIRELESS_SETTINGS) })
        RowDivider()
        SettingsRow(Icons.Rounded.VpnKey, Color(0xFF5E5CE6), "VPN",
            onClick = { open(Settings.ACTION_VPN_SETTINGS) })
        RowDivider()
        SettingsRow(Icons.Rounded.WbSunny, Color(0xFF0A84FF), "Экран",
            onClick = { open(Settings.ACTION_DISPLAY_SETTINGS) })
        RowDivider()
        SettingsRow(Icons.Rounded.VolumeUp, Color(0xFFFF375F), "Звук",
            onClick = { open(Settings.ACTION_SOUND_SETTINGS) })
        RowDivider()
        SettingsRow(Icons.Rounded.Apps, Color(0xFFBF5AF2), "Приложения",
            onClick = { open(Settings.ACTION_APPLICATION_SETTINGS) })
        RowDivider()
        SettingsRow(Icons.Rounded.Storage, Color(0xFF8E8E93), "Хранилище",
            onClick = { open(Settings.ACTION_INTERNAL_STORAGE_SETTINGS) })
        RowDivider()
        SettingsRow(Icons.Rounded.DateRange, Color(0xFFFF9F0A), "Дата и время",
            onClick = { open(Settings.ACTION_DATE_SETTINGS) })
        RowDivider()
        SettingsRow(Icons.Rounded.Language, Color(0xFF30B0C7), "Язык",
            onClick = { open(Settings.ACTION_LOCALE_SETTINGS) })
        RowDivider()
        SettingsRow(Icons.Rounded.Keyboard, Color(0xFF8E8E93), "Клавиатура",
            onClick = { open(Settings.ACTION_INPUT_METHOD_SETTINGS) })
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.Accessibility,
            iconColor = Color(0xFF0A84FF),
            title = "Специальные возможности",
            subtitle = "Включите службу лаунчера — тогда работают кнопки Назад, Домой и Недавние",
            onClick = { open(Settings.ACTION_ACCESSIBILITY_SETTINGS) }
        )
        RowDivider()
        SettingsRow(Icons.Rounded.Notifications, Color(0xFFFF453A), "Доступ к уведомлениям",
            onClick = { open(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) })
        RowDivider()
        SettingsRow(Icons.Rounded.Code, Color(0xFF636366), "Для разработчиков",
            onClick = { open(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS) })
        RowDivider()
        SettingsRow(Icons.Rounded.Info, Color(0xFF8E8E93), "Об устройстве",
            onClick = { open(Settings.ACTION_DEVICE_INFO_SETTINGS) })
        RowDivider()
        SettingsRow(Icons.Rounded.Settings, Color(0xFF8E8E93), "Все настройки Android",
            onClick = { open(Settings.ACTION_SETTINGS) })
    }
}

// ---------------------------------------------------------------- О лаунчере

@Composable
internal fun AboutSection() {
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "—"
    }
    SettingsGroup {
        SettingsRow(Icons.Rounded.Star, Color(0xFF8E8E93), "AI Box Car Launcher",
            value = versionName, showChevron = false)
        RowDivider()
        SettingsRow(Icons.Rounded.Info, Color(0xFF8E8E93), "Устройство",
            value = Build.MODEL, showChevron = false)
        RowDivider()
        SettingsRow(Icons.Rounded.Info, Color(0xFF8E8E93), "Android",
            value = Build.VERSION.RELEASE, showChevron = false)
        RowDivider()
        SettingsRow(Icons.Rounded.Info, Color(0xFF8E8E93), "Экран",
            value = "${config.screenWidthDp}×${config.screenHeightDp} dp", showChevron = false)
    }
    HintText("github.com/gyurdzhyan8631-netizen/aibox-ipad-launcher")
}


// ---------------------------------------------------------------- Обои

@Composable
internal fun WallpaperSection(settings: LauncherSettings, onOpenSection: (SettingsSection) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf(WallpaperStore.list(context)) }
    var busy by remember { mutableStateOf(false) }

    fun importPicked(uri: android.net.Uri?, video: Boolean) {
        if (uri == null) return
        busy = true
        scope.launch {
            val file = kotlinx.coroutines.withContext(Dispatchers.IO) {
                WallpaperStore.import(context, uri, video)
            }
            busy = false
            items = WallpaperStore.list(context)
            if (file != null) {
                settings.selectWallpaper(if (video) WallpaperType.VIDEO else WallpaperType.IMAGE, file.absolutePath)
            } else {
                Toast.makeText(context, "Не удалось добавить файл", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        importPicked(uri, video = false)
    }
    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        importPicked(uri, video = true)
    }
    // Системный выбор обоев: тип меняется только если пользователь действительно выбрал обои.
    val pickSystemWallpaper = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) settings.selectWallpaper(WallpaperType.SYSTEM)
    }
    fun launchPicker(video: Boolean) {
        runCatching { if (video) pickVideo.launch("video/*") else pickImage.launch("image/*") }
            .onFailure {
                Toast.makeText(context, "На устройстве нет приложения для выбора файлов", Toast.LENGTH_LONG).show()
            }
    }

    SettingsGroup {
        SettingsRow(
            icon = Icons.Rounded.Palette,
            iconColor = Color(0xFF5E5CE6),
            title = "Темы оформления",
            value = if (settings.wallpaperType == WallpaperType.THEME) "Выбрано" else "",
            subtitle = "Градиенты, включая «Авто (по солнцу)». Для живого неба выберите тему в «Персонализации»",
            onClick = {
                settings.selectWallpaper(WallpaperType.THEME)
                onOpenSection(SettingsSection.PERSONALIZATION)
            }
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.WbSunny,
            iconColor = Color(0xFFFF9F0A),
            title = "Живое небо",
            value = if (settings.wallpaperType == WallpaperType.LIVE_SKY) "Выбрано" else "",
            subtitle = "Солнце и луна идут по реальному времени суток, дождь и снег — по погоде. " +
                "Хорошо сочетается с темами День, Рассвет и Ночь",
            onClick = { settings.selectWallpaper(WallpaperType.LIVE_SKY) }
        )
        RowDivider()
        SwitchRow(
            icon = Icons.Rounded.Cloud,
            iconColor = Color(0xFF64D2FF),
            title = "Осадки в живом небе",
            subtitle = "Дождь и снег. Выключите, если слабое устройство",
            checked = settings.skyPrecipitation,
            onCheckedChange = { settings.updateSkyPrecipitation(it) }
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.ImageIcon,
            iconColor = Color(0xFF30D158),
            title = "Добавить фото",
            subtitle = if (busy) "Копирование…" else "Свой снимок как обои главного экрана",
            onClick = { launchPicker(video = false) }
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.Videocam,
            iconColor = Color(0xFFFF375F),
            title = "Добавить живые обои (видео)",
            subtitle = "Видео без звука играет по кругу. Расходует больше ресурсов",
            onClick = { launchPicker(video = true) }
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.Apps,
            iconColor = Color(0xFF0A84FF),
            title = "Системные и живые обои Android",
            value = if (settings.wallpaperType == WallpaperType.SYSTEM) "Выбрано" else "",
            subtitle = "Открыть выбор системных и живых обоев Android",
            onClick = {
                val chooser = android.content.Intent.createChooser(
                    android.content.Intent(android.content.Intent.ACTION_SET_WALLPAPER),
                    "Выбрать системные обои"
                )
                runCatching { pickSystemWallpaper.launch(chooser) }.onFailure {
                    Toast.makeText(context, "Выбор обоев недоступен на этом устройстве", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    SectionTitle("Динамические сцены 1-го стола")
    HintText(
        "Сменяются автоматически: " +
            DynamicScene.entries.joinToString(" · ") { it.title } +
            ". Состояние выбирается по погоде и восходу/закату для вашего местоположения."
    )

    SectionTitle("Мои обои")
    if (items.isEmpty()) {
        HintText("Пока ничего не добавлено.")
    } else {
        SettingsGroup {
            items.forEachIndexed { index, file ->
                if (index > 0) RowDivider()
                val video = WallpaperStore.isVideo(file)
                val selected = settings.wallpaperPath == file.absolutePath &&
                    (settings.wallpaperType == WallpaperType.IMAGE || settings.wallpaperType == WallpaperType.VIDEO)
                val stamp = remember(file) {
                    SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(Date(file.lastModified()))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            settings.selectWallpaper(
                                if (video) WallpaperType.VIDEO else WallpaperType.IMAGE,
                                file.absolutePath
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    IconBadge(
                        icon = if (video) Icons.Rounded.Videocam else Icons.Rounded.ImageIcon,
                        color = if (video) Color(0xFFFF375F) else Color(0xFF30D158)
                    )
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (video) "Живые обои · $stamp" else "Фото · $stamp",
                            color = Color.White,
                            fontSize = 17.sp
                        )
                        if (selected) {
                            Text("Используются сейчас", color = SettingsColors.accent, fontSize = 12.sp)
                        }
                    }
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Удалить",
                        tint = Color(0xFFFF453A),
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                file.delete()
                                settings.wallpaperFileDeleted(file.absolutePath)
                                items = WallpaperStore.list(context)
                            }
                            .padding(6.dp)
                    )
                }
            }
        }
    }
    HintText("Системные живые обои показываются через прозрачное окно лаунчера и зависят от прошивки устройства.")
}

// ---------------------------------------------------------------- Обновление

@Composable
internal fun UpdateSection() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val info = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
    }
    val currentBuild = remember { info?.longVersionCode?.toInt() ?: 0 }
    val versionName = info?.versionName ?: "—"

    var status by remember { mutableStateOf("Нажмите, чтобы проверить") }
    var checking by remember { mutableStateOf(false) }
    var latest by remember { mutableStateOf<UpdateInfo?>(null) }

    SettingsGroup {
        SettingsRow(
            icon = Icons.Rounded.Info,
            iconColor = Color(0xFF8E8E93),
            title = "Установленная версия",
            value = "$versionName (сборка $currentBuild)",
            showChevron = false
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.Refresh,
            iconColor = Color(0xFF30D158),
            title = "Проверить обновления",
            subtitle = status,
            onClick = {
                if (!checking) {
                    checking = true
                    status = "Проверяем…"
                    scope.launch {
                        val result = UpdateChecker.fetchLatest()
                        checking = false
                        result.onSuccess { found ->
                            latest = found
                            status = if (found.build > currentBuild) {
                                "Доступна новая сборка ${found.build}"
                            } else {
                                "Установлена последняя версия"
                            }
                        }
                        result.onFailure { error ->
                            status = if (error is NoReleasesException) {
                                "Релизов пока нет"
                            } else {
                                "Не удалось проверить: нет связи с GitHub"
                            }
                        }
                    }
                }
            }
        )
    }

    val available = latest
    if (available != null && available.build > currentBuild) {
        SectionTitle("Доступно обновление")
        SettingsGroup {
            SettingsRow(
                icon = Icons.Rounded.Refresh,
                iconColor = SettingsColors.accent,
                title = "Скачать и установить",
                subtitle = "Сборка ${available.build}" +
                    if (available.notes.isNotBlank()) " · ${available.notes.take(80)}" else "",
                onClick = { UpdateChecker.downloadAndInstall(context, available) }
            )
        }
    }
    HintText("Обновление скачивается с GitHub и устанавливается поверх текущей версии. Настройки сохраняются.")
}


// ---------------------------------------------------------------- Размеры

private fun percent(value: Float) = "${(value * 100).roundToInt()}%"

@Composable
internal fun SizesSection(settings: LauncherSettings) {
    SettingsGroup {
        SliderRow(
            title = "Весь интерфейс",
            valueText = percent(settings.uiScale),
            value = settings.uiScale,
            valueRange = 0.6f..1.3f,
            onValueChange = { settings.updateUiScale(it) }
        )
        RowDivider()
        SliderRow(
            title = "Часы и дата",
            valueText = percent(settings.clockScale),
            value = settings.clockScale,
            valueRange = 0.6f..1.4f,
            onValueChange = { settings.updateClockScale(it) }
        )
        RowDivider()
        SliderRow(
            title = "Значки состояния",
            valueText = percent(settings.statusScale),
            value = settings.statusScale,
            valueRange = 0.6f..1.3f,
            onValueChange = { settings.updateStatusScale(it) }
        )
        RowDivider()
        SliderRow(
            title = "Иконки приложений",
            valueText = percent(settings.iconScale),
            value = settings.iconScale,
            valueRange = 0.7f..1.5f,
            onValueChange = { settings.updateIconScale(it) }
        )
        RowDivider()
        SliderRow(
            title = "Шторка быстрого доступа",
            valueText = percent(settings.quickScale),
            value = settings.quickScale,
            valueRange = 0.6f..1.3f,
            onValueChange = { settings.updateQuickScale(it) }
        )
    }
    PrimaryButton("Сбросить все размеры") { settings.resetSizes() }
    HintText("«Весь интерфейс» меняет всё сразу. Остальные ползунки меняют только свой элемент.")
}
