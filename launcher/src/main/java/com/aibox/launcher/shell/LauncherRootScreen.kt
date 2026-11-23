package com.aibox.launcher.shell

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.appdrawer.AppDrawerOverlay
import com.aibox.launcher.core.model.DayPeriod
import com.aibox.launcher.core.model.InstalledApp
import com.aibox.launcher.core.model.WeatherCondition
import com.aibox.launcher.dock.CarouselDockBar
import com.aibox.launcher.desktop.HomeScreenPage
import com.aibox.launcher.dock.SystemNavigationCapsule
import com.aibox.launcher.icons.InstalledAppsRepository
import com.aibox.launcher.system.SystemActionsHandler
import com.aibox.launcher.wallpaper.DynamicWeatherWallpaper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LauncherRootScreen(onOpenControlCenter: () -> Unit = {}) {
    val context = LocalContext.current
    val systemActions = remember { SystemActionsHandler(context) }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

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
    var showControlCenter by remember { mutableStateOf(false) }
    var showAppDrawer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000L)
        }
    }

    val dayPeriod = when (now.hour) {
        in 5..10 -> DayPeriod.MORNING
        in 11..17 -> DayPeriod.DAY
        in 18..21 -> DayPeriod.EVENING
        else -> DayPeriod.NIGHT
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
                .padding(horizontal = 28.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 2. Top Header: Big iPadOS Clock, Date, Weather & Control Center Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = now.format(DateTimeFormatter.ofPattern("HH:mm")),
                        fontSize = 56.sp,
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0x401C1C1E))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(22.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Rounded.WbSunny, null, tint = Color(0xFFFFD60A))
                        Spacer(Modifier.width(8.dp))
                        Text("+22° Ясно", color = Color.White, fontWeight = FontWeight.Medium)
                    }

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
                                .height(6.dp)
                                .width(if (active) 22.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (active) Color.White else Color.White.copy(alpha = 0.4f))
                        )
                    }
                }
            }

            // 4. Bottom Area: System Capsule + Carousel Dock Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SystemNavigationCapsule(
                    onBack = {
                        when {
                            showSettings -> showSettings = false
                            showAppDrawer -> showAppDrawer = false
                            showControlCenter -> showControlCenter = false
                            else -> systemActions.triggerBack()
                        }
                    },
                    onHome = {
                        showSettings = false
                        showAppDrawer = false
                        showControlCenter = false
                        scope.launch { pagerState.animateScrollToPage(0) }
                        systemActions.triggerHome {}
                    },
                    onRecents = { systemActions.triggerRecents() }
                )

                Spacer(Modifier.width(20.dp))

                CarouselDockBar(
                    onOpenDrawer = { showAppDrawer = !showAppDrawer },
                    onOpenSettings = { showSettings = !showSettings }
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
    }
}
