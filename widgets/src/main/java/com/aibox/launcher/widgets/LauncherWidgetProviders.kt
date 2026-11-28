package com.aibox.launcher.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.view.KeyEvent
import android.widget.RemoteViews
import org.json.JSONObject
import java.util.Locale

/** Компоненты встроенных настоящих AppWidget лаунчера. */
object LauncherWidgetProviders {
    private const val PKG = "com.aibox.launcher"

    val WEATHER = ComponentName(PKG, "$PKG.widgets.WeatherWidgetProvider")
    val NAVIGATOR = ComponentName(PKG, "$PKG.widgets.NavigatorWidgetProvider")
    val YOUTUBE = ComponentName(PKG, "$PKG.widgets.YouTubeWidgetProvider")
    val MUSIC = ComponentName(PKG, "$PKG.widgets.MusicWidgetProvider")
    val MEMORY = ComponentName(PKG, "$PKG.widgets.MemoryWidgetProvider")
}

/** Лаунчер шлёт его после обновления погоды, чтобы виджет сразу показал новые данные. */
const val ACTION_REFRESH_WEATHER_WIDGET = "com.aibox.launcher.widget.REFRESH_WEATHER"

private const val ACTION_PREV = "com.aibox.launcher.widget.MUSIC_PREV"
private const val ACTION_PLAY = "com.aibox.launcher.widget.MUSIC_PLAY"
private const val ACTION_NEXT = "com.aibox.launcher.widget.MUSIC_NEXT"

private fun activityIntent(
    context: Context,
    intent: Intent,
    requestCode: Int
): PendingIntent = PendingIntent.getActivity(
    context,
    requestCode,
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)

private fun broadcastIntent(
    context: Context,
    component: ComponentName,
    action: String,
    requestCode: Int
): PendingIntent = PendingIntent.getBroadcast(
    context,
    requestCode,
    Intent(action).setComponent(component),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
)

private fun preferredApp(
    context: Context,
    prefKey: String,
    fallback: () -> Intent
): Intent {
    val prefs = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
    val packageName = prefs.getString(prefKey, "") ?: ""
    if (packageName.isNotBlank()) {
        context.packageManager.getLaunchIntentForPackage(packageName)?.let { return it }
    }
    return fallback()
}

private fun updateAll(
    context: Context,
    manager: AppWidgetManager,
    ids: IntArray,
    layoutRes: Int,
    fill: (RemoteViews) -> Unit
) {
    val views = RemoteViews(context.packageName, layoutRes)
    fill(views)
    ids.forEach { manager.updateAppWidget(it, views) }
}

/** Погода: читает тот же офлайн-кэш, что и лаунчер. */
class WeatherWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val snapshot = runCatching {
            val raw = context.getSharedPreferences("weather_cache", Context.MODE_PRIVATE)
                .getString("snapshot", null)
            if (raw.isNullOrBlank()) null else JSONObject(raw)
        }.getOrNull()

        val temp = snapshot?.optInt("tempC") ?: 0
        val condition = snapshot?.optString("condition", "") ?: ""
        val city = snapshot?.optString("cityName", "") ?: ""
        updateAll(context, appWidgetManager, appWidgetIds, R.layout.widget_weather) { views ->
            views.setTextViewText(R.id.weather_icon, weatherIcon(condition))
            views.setTextViewText(
                R.id.weather_temp,
                if (snapshot == null) "—" else "${if (temp > 0) "+" else ""}$temp°"
            )
            views.setTextViewText(R.id.weather_condition, weatherText(condition))
            views.setTextViewText(
                R.id.weather_place,
                city.ifBlank { "Укажите город в настройках" }
            )
            views.setOnClickPendingIntent(
                R.id.weather_open,
                activityIntent(
                    context,
                    preferredApp(context, "weather_package") {
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://weather.google.com"))
                    },
                    11
                )
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WEATHER_WIDGET) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WeatherWidgetProvider::class.java))
            if (ids.isNotEmpty()) onUpdate(context, manager, ids)
        }
    }

    private fun weatherIcon(condition: String) = when (condition) {
        "CLEAR" -> "☀"
        "CLOUDY", "OVERCAST", "FOG", "WIND" -> "☁"
        "RAIN", "HEAVY_RAIN", "THUNDERSTORM" -> "☂"
        "SNOW" -> "❄"
        else -> "◌"
    }

    private fun weatherText(condition: String) = when (condition) {
        "CLEAR" -> "Ясно"
        "CLOUDY" -> "Облачно"
        "OVERCAST" -> "Пасмурно"
        "RAIN" -> "Дождь"
        "HEAVY_RAIN" -> "Сильный дождь"
        "SNOW" -> "Снег"
        "THUNDERSTORM" -> "Гроза"
        "FOG" -> "Туман"
        "WIND" -> "Ветрено"
        else -> "Погода"
    }
}

/** Навигатор: крупная подсказка и запуск выбранного приложения. */
class NavigatorWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateAll(context, appWidgetManager, appWidgetIds, R.layout.widget_navigator) { views ->
            views.setTextViewText(R.id.nav_title, "Навигация")
            views.setTextViewText(R.id.nav_hint, "Куда едем?")
            views.setTextViewText(R.id.nav_app, "Yandex Navi / Карты")
            views.setOnClickPendingIntent(
                R.id.nav_open,
                activityIntent(
                    context,
                    preferredApp(context, "nav_package") {
                        Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))
                    },
                    12
                )
            )
        }
    }
}

/** YouTube: быстрый вход в приложение. */
class YouTubeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateAll(context, appWidgetManager, appWidgetIds, R.layout.widget_youtube) { views ->
            views.setTextViewText(R.id.youtube_title, "YouTube")
            views.setTextViewText(R.id.youtube_hint, "Смотреть видео")
            val youtube = context.packageManager
                .getLaunchIntentForPackage("com.google.android.youtube")
                ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com"))
            views.setOnClickPendingIntent(R.id.youtube_open, activityIntent(context, youtube, 13))
        }
    }
}

/** Музыка: быстрый вход и управление воспроизведением. */
class MusicWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateAll(context, appWidgetManager, appWidgetIds, R.layout.widget_music) { views ->
            views.setTextViewText(R.id.music_title, "Музыка")
            views.setTextViewText(R.id.music_hint, "Управление воспроизведением")
            views.setOnClickPendingIntent(
                R.id.music_open,
                activityIntent(
                    context,
                    preferredApp(context, "music_package") {
                        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC)
                    },
                    14
                )
            )
            val component = ComponentName(context, MusicWidgetProvider::class.java)
            views.setOnClickPendingIntent(
                R.id.music_prev,
                broadcastIntent(context, component, ACTION_PREV, 15)
            )
            views.setOnClickPendingIntent(
                R.id.music_play,
                broadcastIntent(context, component, ACTION_PLAY, 16)
            )
            views.setOnClickPendingIntent(
                R.id.music_next,
                broadcastIntent(context, component, ACTION_NEXT, 17)
            )
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val code = when (intent.action) {
            ACTION_PREV -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            ACTION_PLAY -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
            ACTION_NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
            else -> return
        }
        val audio = context.getSystemService(AudioManager::class.java) ?: return
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
    }
}

/** Память: свободное место на внутреннем накопителе. */
class MemoryWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = (stat.blockCountLong * stat.blockSizeLong).coerceAtLeast(1L)
        val free = stat.availableBlocksLong * stat.blockSizeLong
        val usedPercent = (((total - free) * 100) / total).toInt().coerceIn(0, 100)
        updateAll(context, appWidgetManager, appWidgetIds, R.layout.widget_memory) { views ->
            views.setTextViewText(R.id.memory_title, "Память")
            views.setTextViewText(R.id.memory_text, "${gb(free)} свободно")
            views.setTextViewText(R.id.memory_detail, "из ${gb(total)}")
            views.setProgressBar(R.id.memory_bar, 100, usedPercent, false)
            views.setOnClickPendingIntent(
                R.id.memory_open,
                activityIntent(context, Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS), 18)
            )
        }
    }

    private fun gb(bytes: Long): String =
        String.format(Locale("ru"), "%.1f ГБ", bytes / (1024f * 1024f * 1024f))
}
