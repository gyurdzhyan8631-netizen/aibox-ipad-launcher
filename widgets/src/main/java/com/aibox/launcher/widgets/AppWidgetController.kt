package com.aibox.launcher.widgets

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/** Размещение виджетов других приложений (часы, погода, плееры и т.д.) на рабочих столах. */
class AppWidgetController(context: Context) {

    private val appContext = context.applicationContext
    private val manager: AppWidgetManager = AppWidgetManager.getInstance(appContext)
    private val host = AppWidgetHost(appContext, HOST_ID)

    fun startListening() {
        runCatching { host.startListening() }
    }

    fun stopListening() {
        runCatching { host.stopListening() }
    }

    fun providers(): List<AppWidgetProviderInfo> =
        runCatching { manager.installedProviders }.getOrDefault(emptyList())

    /** Название вида «Приложение — Виджет». */
    fun title(info: AppWidgetProviderInfo): String {
        val pm = appContext.packageManager
        val widget = runCatching { info.loadLabel(pm) }.getOrNull() ?: ""
        val app = runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
        }.getOrNull() ?: info.provider.packageName
        return if (widget.isBlank() || widget == app) app else "$app — $widget"
    }

    fun info(appWidgetId: Int): AppWidgetProviderInfo? =
        runCatching { manager.getAppWidgetInfo(appWidgetId) }.getOrNull()

    fun allocateId(): Int = host.allocateAppWidgetId()

    fun bindIfAllowed(appWidgetId: Int, provider: ComponentName): Boolean =
        runCatching { manager.bindAppWidgetIdIfAllowed(appWidgetId, provider) }.getOrDefault(false)

    fun delete(appWidgetId: Int) {
        runCatching { host.deleteAppWidgetId(appWidgetId) }
    }

    internal fun createView(context: Context, appWidgetId: Int, info: AppWidgetProviderInfo): AppWidgetHostView =
        host.createView(context, appWidgetId, info)

    private companion object {
        const val HOST_ID = 4242
    }
}

/** Виджет другого приложения внутри рабочего стола. */
@Composable
fun HostedAppWidget(
    controller: AppWidgetController,
    appWidgetId: Int,
    modifier: Modifier = Modifier
) {
    val info = remember(appWidgetId) { controller.info(appWidgetId) }
    if (info == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Виджет недоступен", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        }
        return
    }
    BoxWithConstraints(modifier = modifier.clip(RoundedCornerShape(24.dp))) {
        val widthDp = maxWidth.value.toInt()
        val heightDp = maxHeight.value.toInt()
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context -> controller.createView(context, appWidgetId, info) },
            update = { view -> view.updateAppWidgetSize(null, widthDp, heightDp, widthDp, heightDp) }
        )
    }
}
