package com.aibox.launcher.widgets

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
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
import com.aibox.launcher.core.model.WidgetCardStyle
import com.aibox.launcher.core.ui.theme.LocalLauncherTokens
import kotlinx.coroutines.delay

private data class PowerInfo(val percent: Int?, val charging: Boolean)

private fun readPower(context: Context): PowerInfo {
    val intent: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    if (intent == null || !intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, false)) {
        return PowerInfo(percent = null, charging = true)
    }
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
    val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
        status == BatteryManager.BATTERY_STATUS_FULL
    val percent = if (level >= 0) (level * 100 / scale).coerceIn(0, 100) else null
    return PowerInfo(percent, charging)
}

/** Виджет «Питание»: заряд батареи или питание от автомобиля. */
@Composable
fun BatteryWidget(
    modifier: Modifier = Modifier,
    cardStyle: WidgetCardStyle = WidgetCardStyle.GLASS
) {
    val context = LocalContext.current
    val tokens = LocalLauncherTokens.current
    var info by remember { mutableStateOf(readPower(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            info = readPower(context)
            delay(30_000L)
        }
    }

    GlassCard(modifier = modifier, style = cardStyle) {
        Column {
            Text(
                text = "Питание",
                color = tokens.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = info.percent?.let { "$it%" } ?: "Авто",
                color = tokens.textPrimary,
                fontSize = widgetNumberSize(),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = when {
                    info.percent == null -> "Питание от автомобиля"
                    info.charging -> "Заряжается"
                    else -> "От батареи"
                },
                color = tokens.textSecondary,
                fontSize = 13.sp
            )
        }
    }
}
