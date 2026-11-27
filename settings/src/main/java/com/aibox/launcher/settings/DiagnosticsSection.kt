package com.aibox.launcher.settings

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.view.Choreographer
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.roundToInt

/** Сведения об устройстве, доступные без root и без дополнительных разрешений. */
internal data class DeviceDiagnostics(
    val device: String,
    val android: String,
    val soc: String,
    val board: String,
    val abis: String,
    val gpu: String,
    val glEs: String,
    val ramTotal: String,
    val ramAvailable: String,
    val lowMemory: Boolean,
    val screen: String,
    val density: String
)

internal fun collectDiagnostics(context: Context): DeviceDiagnostics {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val glInfo = activityManager.deviceConfigurationInfo
    val memory = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
    val metrics = context.resources.displayMetrics
    val refreshRate = runCatching {
        @Suppress("DEPRECATION")
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.refreshRate
    }.getOrDefault(0f)

    return DeviceDiagnostics(
        device = listOf(Build.MANUFACTURER, Build.MODEL).filter { it.isNotBlank() }.joinToString(" "),
        android = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        soc = Build.SOC_MODEL?.takeIf { it.isNotBlank() } ?: Build.HARDWARE,
        board = Build.BOARD,
        abis = Build.SUPPORTED_ABIS.joinToString(", "),
        gpu = glInfo.glRenderer?.takeIf { it.isNotBlank() } ?: "не сообщается системой",
        glEs = glInfo.glEsVersion.ifBlank { "req ${glInfo.reqGlEsVersion}" },
        ramTotal = formatBytes(memory.totalMem),
        ramAvailable = formatBytes(memory.availMem),
        lowMemory = activityManager.isLowRamDevice || memory.lowMemory,
        screen = "${metrics.widthPixels}×${metrics.heightPixels} px · ${refreshRate.roundToInt()} Гц",
        density = "${(metrics.density * 160f).roundToInt()} dpi (x${"%.2f".format(Locale.US, metrics.density)})"
    )
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 ->
        String.format(Locale.US, "%.1f ГБ", bytes / (1024.0 * 1024.0 * 1024.0))
    bytes >= 1024L * 1024 -> String.format(Locale.US, "%d МБ", bytes / (1024L * 1024L))
    else -> "$bytes Б"
}

/** Считает число кадров интерфейса за секунду. Работает только пока экран открыт. */
private class FpsMeter(private val onFps: (Int) -> Unit) {
    private val choreographer = Choreographer.getInstance()
    private var running = false
    private var frames = 0
    private var windowStartNanos = 0L

    private val callback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return
            if (windowStartNanos == 0L) windowStartNanos = frameTimeNanos
            frames++
            val elapsed = frameTimeNanos - windowStartNanos
            if (elapsed >= 1_000_000_000L) {
                onFps(((frames - 1) * 1_000_000_000L / elapsed).toInt().coerceIn(0, 240))
                frames = 0
                windowStartNanos = frameTimeNanos
            }
            choreographer.postFrameCallback(this)
        }
    }

    fun start() {
        if (running) return
        running = true
        frames = 0
        windowStartNanos = 0L
        choreographer.postFrameCallback(callback)
    }

    fun stop() {
        running = false
        choreographer.removeFrameCallback(callback)
    }
}

/** Диагностика железа: что реально установлено и насколько плавен интерфейс лаунчера. */
@Composable
internal fun DiagnosticsSection() {
    val context = LocalContext.current
    val info = remember { collectDiagnostics(context) }
    var fps by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        val meter = FpsMeter { fps = it }
        meter.start()
        onDispose { meter.stop() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle("Устройство")
        SettingsGroup {
            SettingsRow(Icons.Rounded.Info, Color(0xFF8E8E93), "Модель",
                value = info.device, showChevron = false)
            RowDivider()
            SettingsRow(Icons.Rounded.Info, Color(0xFF8E8E93), "Android",
                value = info.android, showChevron = false)
            RowDivider()
            SettingsRow(Icons.Rounded.Storage, Color(0xFF64D2FF), "SoC / платформа",
                value = info.soc, subtitle = info.board, showChevron = false)
            RowDivider()
            SettingsRow(Icons.Rounded.Code, Color(0xFFBF5AF2), "Архитектуры",
                value = info.abis, showChevron = false)

            RowDivider()
            SettingsRow(Icons.Rounded.Storage, Color(0xFF5E5CE6), "Графика (GPU)",
                value = info.gpu, showChevron = false)
            RowDivider()
            SettingsRow(Icons.Rounded.Code, Color(0xFF0A84FF), "OpenGL ES",
                value = info.glEs, showChevron = false)

            RowDivider()
            SettingsRow(Icons.Rounded.BatteryFull, Color(0xFF30D158), "Память",
                value = info.ramTotal,
                subtitle = "Свободно сейчас: ${info.ramAvailable}" +
                    (if (info.lowMemory) " · устройство с малым объёмом памяти" else ""),
                showChevron = false)

            RowDivider()
            SettingsRow(Icons.Rounded.WbSunny, Color(0xFFFF9F0A), "Экран",
                value = info.screen, subtitle = info.density, showChevron = false)
        }

        SectionTitle("Производительность")
        SettingsGroup {
            SettingsRow(
                icon = Icons.Rounded.Refresh,
                iconColor = Color(0xFF30D158),
                title = "Кадры интерфейса",
                value = if (fps == 0) "измерение…" else "$fps FPS",
                subtitle = "За последнюю секунду; измеряется только пока открыта диагностика",
                showChevron = false
            )
        }

        val advice = mutableListOf<String>()
        if (info.lowMemory) {
            advice += "Малый объём памяти: имеет смысл отключить динамические обои и осадки в настройках."
        }
        if (fps in 1..44) {
            advice += "FPS ниже 45: проверьте обои и виджеты на первом столе, сначала отключите анимации обоев."
        }
        if (advice.isEmpty()) {
            advice += "Явных признаков перегрузки нет. Тяжёлые эффекты можно оставить включёнными."
        }
        advice += "FPS показывает плавность Compose-интерфейса, а не нагрузку GPU живых обоев."
        advice.forEach { HintText(it) }
    }
}
