package com.aibox.launcher.controlcenter

import android.app.Activity
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.provider.Settings
import androidx.core.content.getSystemService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ControlTileType {
    WIFI,
    BLUETOOTH,
    INTERNET,
    VPN,
    SOUND_MODE,
    BRIGHTNESS,
    VOLUME,
    MUSIC,
    SYSTEM_SETTINGS,
    CONNECTION_INFO,
    TIME_AND_BATTERY
}

enum class SoundModeLabel(val title: String) {
    NORMAL("Обычный"),
    VIBRATE("Вибро"),
    SILENT("Без звука")
}

data class ControlCenterUiState(
    val isOpen: Boolean = false,
    val swipeProgress: Float = 0f,
    val isWifiConnected: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val isInternetAvailable: Boolean = false,
    val isVpnActive: Boolean = false,
    val connectionSummary: String = "Нет сети",
    val soundMode: SoundModeLabel = SoundModeLabel.NORMAL,
    val brightnessPercent: Int = 70,
    val volumePercent: Int = 50,
    val currentTimeText: String = "",
    val batteryPercent: Int? = null,
    val isCharging: Boolean = false,
    val visibleTiles: List<ControlTileType> = listOf(
        ControlTileType.WIFI,
        ControlTileType.BLUETOOTH,
        ControlTileType.INTERNET,
        ControlTileType.VPN,
        ControlTileType.SOUND_MODE,
        ControlTileType.BRIGHTNESS,
        ControlTileType.VOLUME,
        ControlTileType.MUSIC,
        ControlTileType.SYSTEM_SETTINGS,
        ControlTileType.CONNECTION_INFO,
        ControlTileType.TIME_AND_BATTERY
    )
)

class ControlCenterSystemController(
    private val context: Context
) {
    private val connectivityManager = context.getSystemService<ConnectivityManager>()
    private val audioManager = context.getSystemService<AudioManager>()
    private val bluetoothManager = context.getSystemService<BluetoothManager>()
    private val batteryManager = context.getSystemService<BatteryManager>()

    fun readSnapshot(currentBrightnessPercent: Int): ControlCenterUiState {
        val activeNetwork = connectivityManager?.activeNetwork
        val capabilities = activeNetwork?.let { connectivityManager?.getNetworkCapabilities(it) }

        val hasWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val hasCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val hasEthernet = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
        val hasVpn = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val connectionText = when {
            hasWifi -> "Wi-Fi"
            hasEthernet -> "Ethernet"
            hasCellular -> "Мобильная сеть"
            hasInternet -> "Подключено"
            else -> "Нет подключения"
        }

        val btEnabled = bluetoothManager?.adapter?.isEnabled == true

        val soundMode = when (audioManager?.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> SoundModeLabel.SILENT
            AudioManager.RINGER_MODE_VIBRATE -> SoundModeLabel.VIBRATE
            else -> SoundModeLabel.NORMAL
        }

        val maxVol = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.coerceAtLeast(1) ?: 15
        val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        val volPercent = ((curVol * 100f) / maxVol).toInt().coerceIn(0, 100)

        val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            ?.takeIf { it in 0..100 }
        val charging = batteryManager?.isCharging == true

        return ControlCenterUiState(
            isWifiConnected = hasWifi,
            isBluetoothEnabled = btEnabled,
            isInternetAvailable = hasInternet,
            isVpnActive = hasVpn,
            connectionSummary = connectionText,
            soundMode = soundMode,
            brightnessPercent = currentBrightnessPercent.coerceIn(5, 100),
            volumePercent = volPercent,
            currentTimeText = timeText,
            batteryPercent = batteryLevel,
            isCharging = charging
        )
    }

    fun openWifiSettings() {
        openSettingsPanelOrIntent(
            panelAction = Settings.Panel.ACTION_WIFI,
            fallbackAction = Settings.ACTION_WIFI_SETTINGS
        )
    }

    fun openBluetoothSettings() {
    openAction(Settings.ACTION_BLUETOOTH_SETTINGS)
}


    fun openInternetSettings() {
        openSettingsPanelOrIntent(
            panelAction = Settings.Panel.ACTION_INTERNET_CONNECTIVITY,
            fallbackAction = Settings.ACTION_WIRELESS_SETTINGS
        )
    }

    fun openVpnSettings() {
        openAction(Settings.ACTION_VPN_SETTINGS)
    }

    fun openSystemSettings() {
        openAction(Settings.ACTION_SETTINGS)
    }

    fun openMusicApp() {
        val intent = Intent.makeMainSelectorActivity(
            Intent.ACTION_MAIN,
            Intent.CATEGORY_APP_MUSIC
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { openAction(Settings.ACTION_SOUND_SETTINGS) }
    }

    fun cycleSoundMode() {
        val am = audioManager ?: return
        val nextMode = when (am.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
            AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
            else -> AudioManager.RINGER_MODE_NORMAL
        }
        runCatching { am.ringerMode = nextMode }
            .onFailure { openAction(Settings.ACTION_SOUND_SETTINGS) }
    }

    fun setMediaVolumePercent(percent: Int) {
        val am = audioManager ?: return
        val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val target = ((percent.coerceIn(0, 100) / 100f) * maxVol).toInt()
        runCatching {
            am.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        }
    }

    fun applyWindowBrightness(activity: Activity?, percent: Int) {
        val window = activity?.window ?: return
        val params = window.attributes
        params.screenBrightness = (percent.coerceIn(5, 100) / 100f)
        window.attributes = params
    }

    private fun openSettingsPanelOrIntent(panelAction: String, fallbackAction: String) {
        val panelIntent = Intent(panelAction).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(panelIntent) }
            .onFailure { openAction(fallbackAction) }
    }

    private fun openAction(action: String) {
        val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
