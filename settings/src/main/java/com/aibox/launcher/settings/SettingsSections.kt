package com.aibox.launcher.settings

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aibox.launcher.core.model.InstalledApp
import com.aibox.launcher.core.model.WallpaperTheme
import com.aibox.launcher.icons.rememberAppIcon
import com.aibox.launcher.wallpaper.wallpaperPreviewColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    weatherStatus: String,
    apps: List<InstalledApp>,
    widgetIdsByScreen: Map<Int, List<Int>>,
    widgetLabels: Map<Int, String>,
    onAddWidget: (Int) -> Unit,
    onRemoveWidget: (Int) -> Unit
) {
    SectionTitle("Погода для обоев")
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

    SectionTitle("Виджеты рабочего стола 1")
    SettingsGroup {
        SwitchRow(
            icon = Icons.Rounded.DateRange,
            iconColor = Color(0xFFFF9F0A),
            title = "Дата",
            checked = settings.showDateWidget,
            onCheckedChange = { settings.updateWidgets(date = it) }
        )
        RowDivider()
        SwitchRow(
            icon = Icons.Rounded.BatteryFull,
            iconColor = Color(0xFF30D158),
            title = "Питание",
            checked = settings.showPowerWidget,
            onCheckedChange = { settings.updateWidgets(power = it) }
        )
        RowDivider()
        SwitchRow(
            icon = Icons.Rounded.Wifi,
            iconColor = Color(0xFF0A84FF),
            title = "Сеть",
            checked = settings.showNetworkWidget,
            onCheckedChange = { settings.updateWidgets(network = it) }
        )
    }

    SectionTitle("Виджеты рабочего стола 2")
    SettingsGroup {
        SwitchRow(
            icon = Icons.Rounded.DateRange,
            iconColor = Color(0xFFFF9F0A),
            title = "Дата",
            checked = settings.showDateWidgetOnSecondScreen,
            onCheckedChange = { settings.updateSecondScreenWidgets(date = it) }
        )
        RowDivider()
        SwitchRow(
            icon = Icons.Rounded.BatteryFull,
            iconColor = Color(0xFF30D158),
            title = "Питание",
            checked = settings.showPowerWidgetOnSecondScreen,
            onCheckedChange = { settings.updateSecondScreenWidgets(power = it) }
        )
        RowDivider()
        SwitchRow(
            icon = Icons.Rounded.Wifi,
            iconColor = Color(0xFF0A84FF),
            title = "Сеть",
            checked = settings.showNetworkWidgetOnSecondScreen,
            onCheckedChange = { settings.updateSecondScreenWidgets(network = it) }
        )
    }

    SectionTitle("Виджеты приложений — рабочий стол 1")
    ExternalWidgetsSettings(
        screenIndex = 0,
        widgetIds = widgetIdsByScreen[0].orEmpty(),
        widgetLabels = widgetLabels,
        onAddWidget = onAddWidget,
        onRemoveWidget = onRemoveWidget
    )

    SectionTitle("Виджеты приложений — рабочий стол 2")
    ExternalWidgetsSettings(
        screenIndex = 1,
        widgetIds = widgetIdsByScreen[1].orEmpty(),
        widgetLabels = widgetLabels,
        onAddWidget = onAddWidget,
        onRemoveWidget = onRemoveWidget
    )

    SectionTitle("Приложения на рабочем столе 1")
    HintText("Добавьте ярлыки приложений на первый стол. Все приложения также доступны на втором столе.")
    SettingsGroup {
        apps.forEachIndexed { index, app ->
            if (index > 0) RowDivider()
            SwitchRow(
                icon = Icons.Rounded.Apps,
                iconColor = Color(0xFF5E5CE6),
                title = app.label,
                checked = app.packageName in settings.homeScreenApps,
                onCheckedChange = { settings.setShownOnHomeScreen(app.packageName, it) }
            )
        }
    }
}

@Composable
private fun ExternalWidgetsSettings(
    screenIndex: Int,
    widgetIds: List<Int>,
    widgetLabels: Map<Int, String>,
    onAddWidget: (Int) -> Unit,
    onRemoveWidget: (Int) -> Unit
) {
    SettingsGroup {
        Box(modifier = Modifier.padding(16.dp)) {
            PrimaryButton("Добавить виджет") { onAddWidget(screenIndex) }
        }
        widgetIds.forEach { id ->
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Widgets,
                iconColor = Color(0xFF0A84FF),
                title = widgetLabels[id] ?: "Виджет недоступен",
                value = "Удалить",
                subtitle = "Нажмите, чтобы удалить",
                showChevron = false,
                onClick = { onRemoveWidget(id) }
            )
        }
    }
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
internal fun DisplaySection(settings: LauncherSettings) {
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
            title = "Масштаб интерфейса",
            valueText = "${(settings.interfaceScale * 100).roundToInt()}%",
            value = settings.interfaceScale,
            valueRange = 0.5f..1.5f,
            steps = 19,
            onValueChange = settings::updateInterfaceScale
        )
        RowDivider()
        Box(modifier = Modifier.padding(16.dp)) {
            PrimaryButton("Сбросить масштаб") { settings.resetInterfaceScale() }
        }
    }
    HintText("Масштаб всех элементов лаунчера: от 50% до 150%. По умолчанию — 85%.")

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
            modifier = Modifier.padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(width = 150.dp, height = 72.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.verticalGradient(listOf(previewTop, previewBottom)))
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Обои", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Тема: ${settings.theme.title}",
                    color = SettingsColors.textSecondary,
                    fontSize = 14.sp
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
                    selected = theme == settings.theme,
                    onClick = { settings.updateTheme(theme) },
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
            icon = Icons.Rounded.Apps,
            iconColor = Color(0xFF8E8E93),
            title = "Размер дока",
            value = "${(settings.panelScale * 100).roundToInt()}%",
            subtitle = "Док и кнопки навигации — всегда одной высоты",
            onClick = {
                onPick(
                    PickerRequest(
                        title = "Размер дока",
                        options = percentOptions(listOf(75, 90, 100, 115, 135)),
                        selectedKey = (settings.panelScale * 100).roundToInt().toString(),
                        onSelect = { settings.updatePanelScale(it.toInt() / 100f) }
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
            icon = Icons.Rounded.Palette,
            iconColor = Color(0xFF5E5CE6),
            title = "Пак иконок",
            value = "Системные",
            subtitle = "Свои паки иконок появятся позже",
            enabled = false
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

// ---------------------------------------------------------------- Приложения

@Composable
internal fun AppsSection(
    settings: LauncherSettings,
    apps: List<InstalledApp>,
    customDockIcons: Map<String, String>,
    onChooseDockIcon: (String) -> Unit,
    onClearDockIcon: (String) -> Unit
) {
    val context = LocalContext.current
    HintText("Переключатель показывает приложение в доке. Можно выбрать свой значок; перетаскивайте иконки дока долгим нажатием. Нажатие на название открывает системные настройки.")
    SettingsGroup {
        apps.forEachIndexed { index, app ->
            if (index > 0) RowDivider()
            val icon = rememberAppIcon(app.packageName)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { SystemSettingsLauncher.openAppDetails(context, app.packageName) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val customIcon = customDockIcons[app.packageName]
                if (customIcon != null) {
                    AsyncImage(
                        model = customIcon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else if (icon != null) {
                    Image(
                        bitmap = icon,
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
                    text = app.label,
                    color = Color.White,
                    fontSize = 17.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (customIcon == null) "Иконка" else "Изменить",
                    color = SettingsColors.accent,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable { onChooseDockIcon(app.packageName) }
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                )
                if (customIcon != null) {
                    Text(
                        text = "Сброс",
                        color = SettingsColors.textSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable { onClearDockIcon(app.packageName) }
                            .padding(end = 8.dp, top = 10.dp, bottom = 10.dp)
                    )
                }
                androidx.compose.material3.Switch(
                    checked = app.packageName !in settings.hiddenInDock,
                    onCheckedChange = { settings.setShownInDock(app.packageName, it) }
                )
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
