package com.aibox.launcher.shell

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.SignalCellular4Bar
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.appdrawer.AppDrawerOverlay
import com.aibox.launcher.core.model.DayPeriod
import com.aibox.launcher.core.model.InstalledApp
import com.aibox.launcher.core.model.WeatherCondition
import com.aibox.launcher.desktop.HomeScreenPage
import com.aibox.launcher.dock.CarouselDockBar
import com.aibox.launcher.dock.DockDefaults
import com.aibox.launcher.dock.SystemNavigationCapsule
import com.aibox.launcher.icons.InstalledAppsRepository
import com.aibox.launcher.system.SystemActionsHandler
import com.aibox.launcher.wallpaper.DynamicWeatherWallpaper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class TopStatusCapsuleState(
    val isVpnActive: Boolean = false,
    val isWifiActive: Boolean = false,
    val wifiConnectedCount: Int = 0,
    val isCellularActive: Boolean = false,
    val isBluetoothActive: Boolean = false
)

private fun readTopStatusCapsuleState(context: Context): TopStatusCapsuleState {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val allNetworks = runCatching { cm?.allNetworks }.getOrNull().orEmpty()

    var hasVpn = false
    var hasWifi = false
    var hasCellular = false
    var wifiCount = 0

    allNetworks.forEach { network ->
        val caps = runCatching { cm?.getNetworkCapabilities(network) }.getOrNull() ?: return@forEach
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
            hasVpn = true
        }
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            hasWifi = true
            wifiCount += 1
        }
        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            hasCellular = true
        }
    }

    val activeCaps = runCatching { cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) } }.getOrNull()
    if (activeCaps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true && wifiCount == 0) {
        wifiCount = 1
    }

    val isBtOn = runCatching {
        Settings.Global.getInt(context.contentResolver, Settings.Global.BLUETOOTH_ON, 0) == 1
    }.getOrDefault(false)

    return TopStatusCapsuleState(
        isVpnActive = hasVpn,
        isWifiActive = hasWifi,
        wifiConnectedCount = if (hasWifi) wifiCount.coerceAtLeast(1) else 0,
        isCellularActive = hasCellular,
        isBluetoothActive = isBtOn
    )
}

@Composable
fun LauncherRootScreen(onOpenControlCenter: () -> Unit = {}) {
    val context = LocalContext.current
    val systemActions = remember { SystemActionsHandler(context) }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val compact = LocalConfiguration.current.screenHeightDp < 480

    val prefs = remember { context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE) }
    var panelScale by remember {
        mutableFloatStateOf(
            prefs.getFloat("panel_scale", DockDefaults.DEFAULT_SCALE)
                .coerceIn(DockDefaults.MIN_SCALE, DockDefaults.MAX_SCALE)
        )
    }

    val apps by produceState(initialValue = emptyList<InstalledApp>()) {
        value = InstalledAppsRepository(context).loadLaunchableApps()
    }

    val launchApp: (InstalledApp) -> Unit = { app ->
        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
        }
    }

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var statusState by remember { mutableStateOf(readTopStatusCapsuleState(context)) }
    var showControlCenter by remember { mutableStateOf(false) }
    var showAppDrawer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            statusState = readTopStatusCapsuleState(context)
            delay(1000L)
        }
    }

    val dayPeriod = when (now.hour) {
        in 5..10 -> DayPeriod.MORNING
        in 11..17 -> DayPeriod.DAY
        in 18..21 -> DayPeriod.EVENING
        else -> DayPeriod.NIGHT
    }

    val goHome: () -> Unit = {
        showSettings = false
        showAppDrawer = false
        showControlCenter = false
        scope.launch { pagerState.animateScrollToPage(0) }
        systemActions.triggerHome()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Live Weather & Time Background
        DynamicWeatherWallpaper(
            dayPeriod = dayPeriod,
            weather = WeatherCondition.CLEAR
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = if (compact) 10.dp else 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 2. Top Header: Big iPadOS Clock, Date, Status Capsule & Control Center
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = now.format(DateTimeFormatter.ofPattern("HH:mm")),
                        fontSize = if (compact) 44.sp else 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("ru"))),
                        fontSize = 16.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TopConnectivityCapsule(state = statusState)

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0x401C1C1E))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { onOpenControlCenter() }
                    ) {
                        Icon(Icons.Rounded.Tune, "Control Center", tint = Color.White)
                    }
                }
            }

            // 3. Home Screen 1 & Home Screen 2 Pager
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    HomeScreenPage(
                        pageIndex = page,
                        apps = apps,
                        onLaunchApp = launchApp
                    )
                }

                // Page Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                ) {
                    repeat(2) { idx ->
                        val active = pagerState.currentPage == idx
                        Box(
                            modifier = Modifier
                                .size(if (active) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (active) Color.White else Color.White.copy(alpha = 0.4f))
                        )
                    }
                }
            }

            // 4. Bottom area: system capsule + dock (always the same height)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SystemNavigationCapsule(
                    scale = panelScale,
                    onBack = {
                        when {
                            showSettings -> showSettings = false
                            showAppDrawer -> showAppDrawer = false
                            showControlCenter -> showControlCenter = false
                            else -> systemActions.triggerBack()
                        }
                    },
                    onHome = goHome,
                    onRecents = { systemActions.triggerRecents() },
                    onGoHome = goHome
                )

                Spacer(modifier = Modifier.width(14.dp))

                CarouselDockBar(
                    apps = apps,
                    onLaunchApp = launchApp,
                    onOpenDrawer = { showAppDrawer = !showAppDrawer },
                    onOpenSettings = { showSettings = !showSettings },
                    modifier = Modifier.weight(1f),
                    scale = panelScale
                )
            }
        }

        // 5. App Drawer overlay
        AppDrawerOverlay(
            visible = showAppDrawer,
            apps = apps,
            onLaunchApp = { app ->
                showAppDrawer = false
                launchApp(app)
            },
            onDismiss = { showAppDrawer = false }
        )

        // 6. Panel size settings overlay
        PanelSettingsOverlay(
            visible = showSettings,
            scale = panelScale,
            onScaleChange = { value ->
                val v = value.coerceIn(DockDefaults.MIN_SCALE, DockDefaults.MAX_SCALE)
                panelScale = v
                prefs.edit().putFloat("panel_scale", v).apply()
            },
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun TopConnectivityCapsule(state: TopStatusCapsuleState) {
    val activeMint = Color(0xFF34E5B5)
    val inactiveWhite = Color.White

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0x401C1C1E))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        val vpnColor = if (state.isVpnActive) activeMint else inactiveWhite
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .border(1.5.dp, vpnColor, RoundedCornerShape(6.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "VPN",
                color = vpnColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                imageVector = Icons.Rounded.Wifi,
                contentDescription = "Wi-Fi",
                tint = if (state.isWifiActive) activeMint else inactiveWhite,
                modifier = Modifier
                    .size(22.dp)
                    .padding(top = 2.dp, end = 2.dp)
            )
            if (state.isWifiActive && state.wifiConnectedCount > 0) {
                Text(
                    text = state.wifiConnectedCount.toString(),
                    color = activeMint,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 9.sp
                )
            }
        }

        Icon(
            imageVector = Icons.Rounded.SignalCellular4Bar,
            contentDescription = "Сеть",
            tint = if (state.isCellularActive) activeMint else inactiveWhite,
            modifier = Modifier.size(21.dp)
        )

        Icon(
            imageVector = Icons.Rounded.Bluetooth,
            contentDescription = "Bluetooth",
            tint = if (state.isBluetoothActive) activeMint else inactiveWhite,
            modifier = Modifier.size(21.dp)
        )
    }
}
