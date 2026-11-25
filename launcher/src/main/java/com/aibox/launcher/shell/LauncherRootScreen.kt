package com.aibox.launcher.shell

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.appdrawer.AppDrawerOverlay
import com.aibox.launcher.controlcenter.ControlCenterSystemController
import com.aibox.launcher.core.model.DayPeriod
import com.aibox.launcher.core.model.InstalledApp
import com.aibox.launcher.core.model.WeatherCondition
import com.aibox.launcher.desktop.HomeScreenPage
import com.aibox.launcher.dock.CarouselDockBar
import com.aibox.launcher.dock.SystemNavigationCapsule
import com.aibox.launcher.icons.InstalledAppsRepository
import com.aibox.launcher.settings.ClockStyle
import com.aibox.launcher.settings.LauncherSettings
import com.aibox.launcher.settings.SettingsScreen
import com.aibox.launcher.system.SystemActionsHandler
import com.aibox.launcher.wallpaper.DynamicWeatherWallpaper
import com.aibox.launcher.weather.CityNotFoundException
import com.aibox.launcher.weather.WeatherRepository
import com.aibox.launcher.weather.ageText
import com.aibox.launcher.weather.formatTemp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LauncherRootScreen(
    appWidgetHost: AppWidgetHost,
    widgetIdsByScreen: Map<Int, List<Int>>,
    onAddWidget: (Int) -> Unit,
    onRemoveWidget: (Int) -> Unit,
    customDockIcons: Map<String, String>,
    onChooseDockIcon: (String) -> Unit,
    onClearDockIcon: (String) -> Unit
) {
    val context = LocalContext.current
    val systemActions = remember { SystemActionsHandler(context) }
    val controller = remember(context) { ControlCenterSystemController(context) }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val compact = LocalConfiguration.current.screenHeightDp < 480

    // --- Настройки (хранятся на устройстве и применяются сразу) ---
    val prefs = remember { context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE) }
    val settings = remember { LauncherSettings(prefs) }
    val density = LocalDensity.current
    val widgetLabels = remember(widgetIdsByScreen, context) {
        val manager = AppWidgetManager.getInstance(context)
        widgetIdsByScreen.values.flatten().distinct().associateWith { id ->
            manager.getAppWidgetInfo(id)
                ?.loadLabel(context.packageManager)
                ?.toString()
                ?: "Недоступный виджет"
        }
    }

    // --- Погода: офлайн-приоритет (кэш на устройстве + фоновое обновление) ---
    val weatherRepo = remember { WeatherRepository(context) }
    var weather by remember { mutableStateOf(weatherRepo.cached()) }
    var weatherError by remember { mutableStateOf<String?>(null) }
    val city = settings.city

    LaunchedEffect(city) {
        while (city.isNotBlank()) {
            val result = weatherRepo.refresh(city)
            result.onSuccess {
                weather = it
                weatherError = null
            }
            result.onFailure {
                weatherError = if (it is CityNotFoundException) {
                    "город не найден"
                } else {
                    "нет связи с сервером погоды"
                }
            }
            // успех — раз в 30 минут, ошибка — повтор через 5 минут
            delay(if (result.isSuccess) 30 * 60_000L else 5 * 60_000L)
        }
    }

    val shownWeather = weather?.takeIf { it.query.equals(city.trim(), ignoreCase = true) }
    val nowMillis = System.currentTimeMillis()
    val errorText = weatherError
    val weatherStatus = when {
        city.isBlank() -> "Город не указан — погода не обновляется."
        shownWeather != null -> {
            val base = "${shownWeather.cityName} · обновлено ${ageText(shownWeather.fetchedAtMillis, nowMillis)}"
            if (errorText != null) "$base · $errorText" else base
        }
        errorText != null -> "Ошибка: $errorText"
        else -> "Загрузка…"
    }

    // --- Приложения ---
    val apps by produceState(initialValue = emptyList<InstalledApp>()) {
        value = InstalledAppsRepository(context).loadLaunchableApps()
    }
    val dockApps = apps
        .filter { it.packageName !in settings.hiddenInDock }
        .sortedBy { app ->
            val index = settings.dockAppOrder.indexOf(app.packageName)
            if (index < 0) Int.MAX_VALUE else index
        }

    val launchPackage: (String) -> Boolean = { packageName ->
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }.isSuccess
        } else {
            false
        }
    }
    val launchApp: (InstalledApp) -> Unit = { app -> launchPackage(app.packageName) }

    val openMusic: () -> Unit = {
        val chosen = settings.musicPackage
        if (chosen.isBlank() || !launchPackage(chosen)) controller.openMusicApp()
    }
    val openNavigation: () -> Unit = {
        val chosen = settings.navPackage
        if (chosen.isBlank() || !launchPackage(chosen)) {
            val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(geo) }
        }
    }

    // --- Состояние экрана ---
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var showQuickPanel by remember { mutableStateOf(false) }
    var showAppDrawer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var settingsSection by remember { mutableStateOf("personalization") }

    val openSettings: (String) -> Unit = { sectionId ->
        settingsSection = sectionId
        showQuickPanel = false
        showAppDrawer = false
        showSettings = true
    }

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

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density * settings.interfaceScale,
            fontScale = density.fontScale
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Свайп вниз от верхнего края открывает шторку быстрого доступа.
                .pointerInput(Unit) {
                    var startedAtTop = false
                    var accumulated = 0f
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            startedAtTop = offset.y <= 96f
                            accumulated = 0f
                        },
                        onDragEnd = {
                            startedAtTop = false
                            accumulated = 0f
                        },
                        onDragCancel = {
                            startedAtTop = false
                            accumulated = 0f
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (startedAtTop && dragAmount > 0f &&
                                !showQuickPanel && !showSettings && !showAppDrawer
                            ) {
                                accumulated += dragAmount
                                if (accumulated >= 140f) {
                                    change.consume()
                                    showQuickPanel = true
                                    startedAtTop = false
                                    accumulated = 0f
                                }
                            }
                        }
                    )
                }
        ) {
        // 1. Обои: тема + время суток + погода
        DynamicWeatherWallpaper(
            dayPeriod = dayPeriod,
            weather = shownWeather?.condition ?: WeatherCondition.CLEAR,
            theme = settings.theme,
            currentTime = now.toLocalTime()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = if (compact) 10.dp else 18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 2. Шапка: часы, дата, значки состояния, шторка
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = now.format(DateTimeFormatter.ofPattern("HH:mm")),
                        fontSize = (if (compact) 44f else 56f).sp * settings.clockScale,
                        fontWeight = if (settings.clockStyle == ClockStyle.BOLD) FontWeight.Bold else FontWeight.Light,
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
                    StatusIconsCapsule(
                        status = rememberStatusState(),
                        onClick = { showQuickPanel = true }
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0x401C1C1E))
                            .border(1.dp, Color(0x33FFFFFF), CircleShape)
                            .clickable { showQuickPanel = true }
                    ) {
                        Icon(Icons.Rounded.Tune, "Шторка быстрого доступа", tint = Color.White)
                    }
                }
            }

            // 3. Рабочие столы 1 и 2
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    HomeScreenPage(
                        pageIndex = page,
                        apps = apps,
                        onLaunchApp = launchApp,
                        appWidgetHost = appWidgetHost,
                        nativeWidgetIds = widgetIdsByScreen[page].orEmpty(),
                        iconScale = settings.iconScale,
                        showDateWidget = settings.showDateWidget,
                        showPowerWidget = settings.showPowerWidget,
                        showNetworkWidget = settings.showNetworkWidget,
                        showDateWidgetOnSecondScreen = settings.showDateWidgetOnSecondScreen,
                        showPowerWidgetOnSecondScreen = settings.showPowerWidgetOnSecondScreen,
                        showNetworkWidgetOnSecondScreen = settings.showNetworkWidgetOnSecondScreen,
                        homeScreenApps = settings.homeScreenApps
                    )
                }

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

            // 4. Низ: капсула кнопок + док (одной высоты)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SystemNavigationCapsule(
                    scale = settings.panelScale,
                    onBack = {
                        when {
                            showSettings -> showSettings = false
                            showAppDrawer -> showAppDrawer = false
                            showQuickPanel -> showQuickPanel = false
                            else -> systemActions.triggerBack()
                        }
                    },
                    onHome = {
                        showSettings = false
                        showAppDrawer = false
                        showQuickPanel = false
                        scope.launch { pagerState.animateScrollToPage(0) }
                        systemActions.triggerHome {}
                    },
                    onRecents = { systemActions.triggerRecents() }
                )

                Spacer(Modifier.width(14.dp))

                CarouselDockBar(
                    apps = dockApps,
                    onLaunchApp = launchApp,
                    onOpenDrawer = { showAppDrawer = !showAppDrawer },
                    onOpenSettings = { openSettings("personalization") },
                    modifier = Modifier.weight(1f),
                    scale = settings.panelScale,
                    customIcons = customDockIcons,
                    onReorderApps = settings::updateDockAppOrder
                )
            }
        }

        // 5. Меню всех приложений
        AppDrawerOverlay(
            visible = showAppDrawer,
            apps = apps,
            onLaunchApp = { app ->
                showAppDrawer = false
                launchApp(app)
            },
            onDismiss = { showAppDrawer = false },
            iconScale = settings.iconScale
        )

        // 6. Шторка быстрого доступа
        QuickPanelOverlay(
            visible = showQuickPanel,
            controller = controller,
            weatherText = shownWeather?.let { formatTemp(it.tempC) } ?: "Погода",
            onDismiss = { showQuickPanel = false },
            onOpenApps = {
                showQuickPanel = false
                showAppDrawer = true
            },
            onOpenSettings = openSettings,
            onOpenMusic = openMusic,
            onOpenNavigation = openNavigation
        )

        // 7. Настройки (как в iPad)
            SettingsScreen(
                visible = showSettings,
                initialSectionId = settingsSection,
                settings = settings,
                apps = apps,
                weatherStatus = weatherStatus,
                widgetIdsByScreen = widgetIdsByScreen,
                widgetLabels = widgetLabels,
                onAddWidget = onAddWidget,
                onRemoveWidget = onRemoveWidget,
                customDockIcons = customDockIcons,
                onChooseDockIcon = onChooseDockIcon,
                onClearDockIcon = onClearDockIcon,
                onClose = { showSettings = false }
            )
        }
    }
}
