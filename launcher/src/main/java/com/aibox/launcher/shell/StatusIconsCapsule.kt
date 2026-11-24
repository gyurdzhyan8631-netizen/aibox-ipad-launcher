package com.aibox.launcher.shell

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

/** Бирюзовый цвет активных значков (VPN, Bluetooth). */
private val ActiveColor = Color(0xFF6BD3BF)

data class StatusState(
    val vpn: Boolean = false,
    val bluetooth: Boolean = false,
    val wifiConnected: Boolean = false,
    val cellular: Boolean = false,
    /** Уровень сигнала сотовой сети 0..4. */
    val signalLevel: Int = 3,
    /** Сколько устройств подключено к точке доступа (0 — не показываем цифру). */
    val hotspotClients: Int = 0
)

@Suppress("DEPRECATION")
private fun readStatus(context: Context): StatusState {
    var vpn = false
    var wifi = false
    var cellular = false

    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    if (cm != null) {
        for (network in cm.allNetworks) {
            val caps = cm.getNetworkCapabilities(network) ?: continue
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) vpn = true
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) wifi = true
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) cellular = true
        }
    }

    val bluetooth = runCatching {
        Settings.Global.getInt(context.contentResolver, "bluetooth_on", 0) == 1
    }.getOrDefault(false)

    val level = runCatching {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        tm?.signalStrength?.level
    }.getOrNull() ?: 3

    return StatusState(
        vpn = vpn,
        bluetooth = bluetooth,
        wifiConnected = wifi,
        cellular = cellular,
        signalLevel = level.coerceIn(0, 4),
        hotspotClients = readHotspotClients()
    )
}

/**
 * Количество устройств, подключённых к точке доступа этого устройства.
 * Читаем таблицу соседей; на части прошивок Android это запрещает, тогда цифры не будет.
 */
private fun readHotspotClients(): Int = runCatching {
    val hotspotPrefixes = listOf("ap", "swlan", "softap", "wlan1", "wlan2")
    File("/proc/net/arp").readLines().drop(1).count { line ->
        val parts = line.trim().split(Regex("\\s+"))
        parts.size >= 6 &&
            parts[2] == "0x2" &&
            hotspotPrefixes.any { parts[5].startsWith(it) }
    }
}.getOrDefault(0)

@Composable
fun rememberStatusState(): StatusState {
    val context = LocalContext.current
    val state = produceState(initialValue = StatusState(), key1 = context) {
        while (true) {
            value = withContext(Dispatchers.IO) { readStatus(context) }
            delay(3_000L)
        }
    }
    return state.value
}

/**
 * Капсула значков в шапке: VPN, Wi-Fi, сотовая сеть, Bluetooth.
 * Неактивные значки полностью белые. Активные VPN и Bluetooth — бирюзовые.
 * На значке Wi-Fi вверху маленькая цифра — сколько устройств подключено.
 */
@Composable
fun StatusIconsCapsule(
    status: StatusState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(shape)
            .background(Color(0x401C1C1E))
            .border(1.dp, Color(0x33FFFFFF), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        VpnBadge(active = status.vpn)
        WifiIcon(clients = status.hotspotClients)
        SignalBars(
            litBars = if (status.cellular) maxOf(status.signalLevel, 1) else 4
        )
        Icon(
            imageVector = Icons.Rounded.Bluetooth,
            contentDescription = "Bluetooth",
            tint = if (status.bluetooth) ActiveColor else Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun VpnBadge(active: Boolean) {
    val color = if (active) ActiveColor else Color.White
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .border(2.dp, color, RoundedCornerShape(8.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(text = "VPN", color = color, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun WifiIcon(clients: Int) {
    Box {
        Icon(
            imageVector = Icons.Rounded.Wifi,
            contentDescription = "Wi-Fi",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
        if (clients > 0) {
            Text(
                text = clients.toString(),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
            )
        }
    }
}

/** Четыре столбика уровня сети; [litBars] первых — белые, остальные приглушены. */
@Composable
private fun SignalBars(litBars: Int) {
    Canvas(modifier = Modifier.size(width = 28.dp, height = 24.dp)) {
        val barWidth = size.width / (4 + 3 * 0.55f)
        val gap = barWidth * 0.55f
        val fractions = listOf(0.30f, 0.55f, 0.78f, 1.0f)
        fractions.forEachIndexed { index, fraction ->
            val barHeight = size.height * fraction
            drawRoundRect(
                color = if (index < litBars) Color.White else Color.White.copy(alpha = 0.35f),
                topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}
