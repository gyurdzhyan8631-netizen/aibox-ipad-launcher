package com.aibox.launcher.widgets

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.ui.theme.LocalLauncherTokens
import kotlinx.coroutines.delay

private data class ConnectionInfo(val title: String, val subtitle: String)

private fun readConnection(context: Context): ConnectionInfo {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val caps = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
    val vpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    val hasInternet = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    val title = when {
        caps == null -> "Нет сети"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Мобильная"
        else -> "Подключено"
    }
    val subtitle = when {
        caps == null -> "Подключение отсутствует"
        vpn -> "VPN активен"
        hasInternet -> "Интернет доступен"
        else -> "Без доступа в интернет"
    }
    return ConnectionInfo(title, subtitle)
}

/** Виджет «Сеть»: тип подключения, интернет и VPN. */
@Composable
fun ConnectionWidget(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val tokens = LocalLauncherTokens.current
    var info by remember { mutableStateOf(readConnection(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            info = readConnection(context)
            delay(5_000L)
        }
    }

    GlassCard(modifier = modifier) {
        Column {
            Text(
                text = "Сеть",
                color = tokens.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = info.title,
                color = tokens.textPrimary,
                fontSize = widgetNumberSize() * 0.7f,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = info.subtitle,
                color = tokens.textSecondary,
                fontSize = 13.sp
            )
        }
    }
}
