package com.aibox.launcher.shell

import android.content.Context
import android.content.res.Configuration
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.appdrawer.AppDrawerOverlay
import com.aibox.launcher.controlcenter.ControlCenterSystemController
import com.aibox.launcher.core.model.DayPeriod
import com.aibox.launcher.core.model.DesktopWidget
import com.aibox.launcher.core.model.InstalledApp
import com.aibox.launcher.core.model.WeatherCondition
import com.aibox.launcher.core.ui.LauncherFonts
import com.aibox.launcher.core.ui.ScaledContent
import com.aibox.launcher.widgets.AppWidgetController
import androidx.compose.ui.unit.Density
import com.aibox.launcher.desktop.HomeScreenPage
import com.aibox.launcher.dock.CarouselDockBar
import com.aibox.launcher.dock.DockDefaults
import com.aibox.launcher.dock.SystemNavigationCapsule
import com.aibox.launcher.icons.InstalledAppsRepository
import com.aibox.launcher.settings.ClockStyle
import com.aibox.launcher.settings.LauncherSettings
import com.aibox.launcher.settings.SettingsScreen
import com.aibox.launcher.system.SystemActionsHandler
import com.aibox.launcher.wallpaper.WallpaperLayer
import com.aibox.launcher.widgets.WeatherWidgetInfo
import com.aibox.launcher.weather.CityNotFoundException
import com.aibox.launcher.weather.WeatherRepository
import com.aibox.launcher.weather.SunCalc
import com.aibox.launcher.weather.ageText
import com.aibox.launcher.weather.formatTemp
import com.aibox.launcher.weather.isStale
import com.aibox.launcher.weather.titleRu
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LauncherRootScreen() {
    val context = LocalContext.current
    // Настройки хранятся на устройстве и применяются сразу
    val prefs = remember { context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE) }
    val settings = remember { LauncherSettings(prefs) }

    // Общий масштаб интерфейса: меняет и размеры (dp), и текст (sp), и «экранные» dp для расчётов.
    val baseDensity = LocalDensity.current
    val baseConfig = LocalConfiguration.current
    val uiScale = settings.uiScale
    val scaledConfig = remember(baseConfig, uiScale) {
        Configuration(baseConfig).apply {
            screenWidthDp = (baseConfig.screenWidthDp / uiScale).toInt()
            screenHeightDp = (baseConfig.screenHeightDp / uiScale).toInt()
        }
    }
    CompositionLocalProvider(
        LocalDensity provides Density(baseDensity.density * uiScale, baseDensity.fontScale),
        LocalConfiguration provides scaledConfig
    ) {
        // Шрифт интерфейса применяется ко всему лаунчеру, включая настройки.
        ProvideTextStyle(TextStyle(fontFamily = LauncherFonts.family(settings.interfaceFontId))) {
            LauncherRootContent(settings, uiScale)
        }
    }
}

@Composable
private fun LauncherRootContent(settings: LauncherSettings, uiScale: Float) {
    val context = LocalContext.current
    val systemActions = remember { SystemActionsHandler(context) }
    val controller = remember(context) { ControlCenterSystemController(context) }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val compact = LocalConfiguration.current.screenHeightDp < 480

    // Виджеты других приложений
    val widgetController = remember { AppWidgetController(context) }
    DisposableEffect(widgetController) {
        widgetController.startListening()
        onDispose { widgetController.stopListening() }
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
    // В боковом доке участвуют все приложения — порядок по алфавиту и замкнутый круг.
    val carouselApps = remember(apps) { apps.sortedBy { it.label } }

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
    val openWeather: () -> Unit = {
        val chosen = settings.weatherPackage
        if (chosen.isBlank() || !launchPackage(chosen)) {
            val url = Intent(Intent.ACTION_VIEW, Uri.parse("https://weather.google.com"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(url) }
        }
    }
    val openVideo: () -> Unit = {
        val chosen = settings.videoPackage
        if (chosen.isBlank() || !launchPackage(chosen)) {
            val url = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(url) }
        }
    }
    val openPowerSettings: () -> Unit = {
        val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
    val openNetworkSettings: () -> Unit = {
        val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    // --- Состояние экрана ---
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var showQuickPanel by remember { mutableStateOf(false) }
    var showAppDrawer by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var editMode by remember { mutableStateOf(false) }
    var showAddWidget by remember { mutableStateOf(false) }
    var settingsSection by remember { mutableStateOf("personalization") }

    val openSettings: (String) -> Unit = { sectionId ->
        settingsSection = sectionId
        showQuickPanel = false
        showAppDrawer = false
        showSettings = true
    }
    val openWidget: (DesktopWidget) -> Unit = { widget ->
        when (widget) {
            DesktopWidget.DATE -> openSettings("personalization")
            DesktopWidget.POWER -> openPowerSettings()
            DesktopWidget.NETWORK -> openNetworkSettings()
            DesktopWidget.WEATHER -> openWeather()
            DesktopWidget.MUSIC -> openMusic()
            DesktopWidget.NAVIGATION -> openNavigation()
            DesktopWidget.VIDEO -> openVideo()
            DesktopWidget.APPS -> showAppDrawer = true
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1000L)
        }
    }

    // Восход и закат по координатам города (без интернета); если города нет — 6:30 и 18:30.
    val today = now.toLocalDate()
    val sunTimes = remember(shownWeather?.cityName, today) {
        weatherRepo.coordinates()?.let { (lat, lon) -> SunCalc.sunriseSunset(lat, lon, today) }
    }

    val dayPeriod = when (now.hour) {
        in 5..10 -> DayPeriod.MORNING
        in 11..17 -> DayPeriod.DAY
        in 18..21 -> DayPeriod.EVENING
        else -> DayPeriod.NIGHT
    }

    val greeting = when (dayPeriod) {
        DayPeriod.MORNING -> "ДОБРОЕ УТРО"
        DayPeriod.DAY -> "ДОБРЫЙ ДЕНЬ"
        DayPeriod.EVENING -> "ДОБРЫЙ ВЕЧЕР"
        DayPeriod.NIGHT -> "ДОБРОЙ НОЧИ"
    }

    val weatherInfo = when {
        city.isBlank() -> WeatherWidgetInfo(condition = "Укажите город", place = "Настройки → Главная")
        shownWeather != null -> WeatherWidgetInfo(
            temp = formatTemp(shownWeather.tempC),
            condition = shownWeather.condition.titleRu(),
            place = shownWeather.cityName,
            note = if (isStale(shownWeather.fetchedAtMillis, nowMillis)) "нет связи" else ""
        )
        else -> WeatherWidgetInfo(condition = "Загрузка…", place = city)
    }

    // Высоты шапки и нижней панели: рабочие столы занимают весь экран, а виджеты не залезают под панели.
    val density = LocalDensity.current
    var headerPx by remember { mutableIntStateOf(0) }
    var bottomPx by remember { mutableIntStateOf(0) }
    val verticalPad = if (compact) 10.dp else 18.dp
    val pageTop = with(density) { headerPx.toDp() } + verticalPad + 8.dp
    val pageBottom = with(density) { bottomPx.toDp() } + verticalPad + 24.dp

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
        WallpaperLayer(
            type = settings.wallpaperType,
            path = settings.wallpaperPath,
            dayPeriod = dayPeriod,
            weather = shownWeather?.condition ?: WeatherCondition.CLEAR,
            theme = settings.theme,
            nowMinutes = now.hour * 60 + now.minute,
            sunriseMin = sunTimes?.first ?: 390,
            sunsetMin = sunTimes?.second ?: 1110,
            precipitation = settings.skyPrecipitation
        )

        // 2. Рабочие столы 1 и 2 — на весь экран, ничего не обрезается жёсткой границей
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageNumber = page + 1
            HomeScreenPage(
                widgets = if (page == 0) settings.page1Widgets else settings.page2Widgets,
                sizes = settings.sizesFor(pageNumber),
                editMode = editMode,
                onEnterEdit = {
                    showQuickPanel = false
                    showAppDrawer = false
                    editMode = true
                },
                onRemove = { token ->
                    if (token.startsWith("aw:")) {
                        token.removePrefix("aw:").toIntOrNull()?.let { widgetController.delete(it) }
                    }
                    settings.removeWidgetToken(pageNumber, token)
                },
                onResize = { token, size -> settings.setWidgetSize(pageNumber, token, size) },
                onAddWidget = { showAddWidget = true },
                weather = weatherInfo,
                apps = apps,
                onLaunchApp = launchApp,
                onOpenMusic = openMusic,
                onOpenWidget = openWidget,
                widgetController = widgetController,
                iconScale = settings.iconScale,
                widgetScale = settings.widgetScale,
                uiScale = uiScale,
                topPadding = pageTop,
                bottomPadding = pageBottom
            )
        }

        // 4. Шапка и нижние панели лежат поверх рабочих столов
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = verticalPad)
        ) {
            // Шапка: приветствие + часы + дата, значки состояния, шторка
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { headerPx = it.height },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClockHeader(
                    greeting = greeting,
                    now = now,
                    compact = compact,
                    scale = settings.clockScale,
                    style = settings.clockStyle,
                    fontFamily = LauncherFonts.family(settings.clockFontId)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val status = rememberStatusState()
                    ScaledContent(settings.statusScale) {
                        StatusIconsCapsule(
                            status = status,
                            onClick = { showQuickPanel = true }
                        )
                    }

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

            // Пустое место между шапкой и низом: касания проходят к рабочим столам
            Spacer(modifier = Modifier.weight(1f))

            // Индикатор страниц
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
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

            // Низ: горизонтальный док рядом с капсулой горячих клавиш, одной высоты.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { bottomPx = it.height },
                verticalAlignment = Alignment.CenterVertically
            ) {
                CarouselDockBar(
                    apps = carouselApps,
                    onLaunchApp = launchApp,
                    onOpenDrawer = { showAppDrawer = true },
                    scale = settings.panelScale,
                    uiScale = uiScale,
                    modifier = Modifier
                        .weight(1f)
                        .height(DockDefaults.panelHeight(settings.panelScale))
                )
                Spacer(Modifier.width(12.dp))
                SystemNavigationCapsule(
                    scale = settings.panelScale,
                    uiScale = uiScale,
                    onBack = {
                        when {
                            showAddWidget -> showAddWidget = false
                            editMode -> editMode = false
                            showSettings -> showSettings = false
                            showAppDrawer -> showAppDrawer = false
                            showQuickPanel -> showQuickPanel = false
                            else -> systemActions.triggerBack()
                        }
                    },
                    onHome = {
                        editMode = false
                        showAddWidget = false
                        showSettings = false
                        showAppDrawer = false
                        showQuickPanel = false
                        scope.launch { pagerState.animateScrollToPage(0) }
                        systemActions.triggerHome {}
                    },
                    onRecents = { systemActions.triggerRecents() }
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
        ScaledContent(settings.quickScale) {
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
        }

        // 6.1 Режим правки рабочего стола
        EditDonePill(
            visible = editMode,
            onDone = { editMode = false },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = verticalPad + 6.dp)
        )
        val currentPageTokens = if (pagerState.currentPage == 0) settings.page1Widgets else settings.page2Widgets
        AddWidgetSheet(
            visible = showAddWidget,
            available = DesktopWidget.entries.filter { it.id !in currentPageTokens },
            onAdd = { widget -> settings.setBuiltinWidget(pagerState.currentPage + 1, widget, true) },
            onOpenAppWidgets = {
                editMode = false
                openSettings("home")
            },
            onDismiss = { showAddWidget = false }
        )

        // 7. Настройки (как в iPad)
        SettingsScreen(
            visible = showSettings,
            initialSectionId = settingsSection,
            settings = settings,
            apps = apps,
            weatherStatus = weatherStatus,
            widgetController = widgetController,
            onClose = { showSettings = false }
        )
    }
}


/** Приветствие, крупные часы и дата. Размер даты и приветствия не зависит от стиля часов. */
@Composable
private fun ClockHeader(
    greeting: String,
    now: LocalDateTime,
    compact: Boolean,
    scale: Float,
    style: ClockStyle,
    fontFamily: FontFamily
) {
    val ru = Locale("ru")
    val smallSize = (if (compact) 14f else 16f).sp
    val timeSize = (if (compact) 48f else 64f).sp * scale * when (style) {
        ClockStyle.SAMSUNG -> 0.94f
        ClockStyle.IPHONE -> 1.06f
        ClockStyle.PIXEL -> 0.98f
        else -> 1f
    }
    val timeWeight = when (style) {
        ClockStyle.BOLD -> FontWeight.SemiBold
        ClockStyle.SAMSUNG -> FontWeight.Bold
        ClockStyle.IPHONE -> FontWeight.Thin
        ClockStyle.PIXEL -> FontWeight.Medium
        ClockStyle.LIGHT -> FontWeight.Light
    }
    val timeColor = if (style == ClockStyle.PIXEL) Color(0xFFE4EEFF) else Color.White
    val tracking = when (style) {
        ClockStyle.SAMSUNG -> (-2).sp
        ClockStyle.IPHONE -> (-1).sp
        ClockStyle.PIXEL -> (-1).sp
        else -> 0.sp
    }
    val greetingStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = smallSize * 0.85f,
        fontWeight = FontWeight.Medium,
        letterSpacing = 3.sp,
        color = Color.White.copy(alpha = 0.9f)
    )
    val dateStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = smallSize,
        fontWeight = FontWeight.Medium,
        letterSpacing = 2.sp,
        color = Color.White
    )
    val timeStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = timeSize,
        fontWeight = timeWeight,
        letterSpacing = tracking,
        color = timeColor
    )
    val weekday = now.format(DateTimeFormatter.ofPattern("EEEE", ru)).uppercase(ru)
    val date = now.format(DateTimeFormatter.ofPattern("d MMMM", ru)).uppercase(ru)
    val clock = now.format(DateTimeFormatter.ofPattern("HH:mm"))

    Column {
        Text(
            text = greeting,
            style = greetingStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        when (style) {
            ClockStyle.LIGHT, ClockStyle.BOLD -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = clock, style = timeStyle)
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .width(1.dp)
                            .height(timeSize.value.dp * 0.7f)
                            .background(Color.White.copy(alpha = 0.35f))
                    )
                    Column {
                        Text(text = weekday, style = dateStyle)
                        Text(text = date, style = dateStyle)
                    }
                }
            }
            ClockStyle.SAMSUNG -> {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(text = clock, style = timeStyle)
                    Text(
                        text = now.format(DateTimeFormatter.ofPattern("ss")),
                        style = dateStyle.copy(fontSize = smallSize * 1.15f, letterSpacing = 1.sp),
                        modifier = Modifier.padding(
                            start = 4.dp,
                            bottom = timeSize.value.dp * 0.12f
                        )
                    )
                }
                Text(
                    text = "$weekday · $date",
                    style = dateStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            ClockStyle.IPHONE -> {
                Text(text = clock, style = timeStyle.copy(fontSize = timeSize * 1.04f))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = weekday, style = dateStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = date, style = dateStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            ClockStyle.PIXEL -> {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = now.format(DateTimeFormatter.ofPattern("HH")),
                        style = timeStyle.copy(color = Color(0xFFFF9F0A))
                    )
                    Text(
                        text = ":",
                        style = timeStyle.copy(color = Color.White.copy(alpha = 0.55f))
                    )
                    Text(text = now.format(DateTimeFormatter.ofPattern("mm")), style = timeStyle)
                }
                Text(
                    text = "$weekday · $date",
                    style = dateStyle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
