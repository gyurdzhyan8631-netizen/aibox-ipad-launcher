package com.aibox.launcher.shell

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private const val HEX_PACKAGE = "ly.anoma.hex"
private const val HEX_DICTATION_ACTION = "ly.anoma.hex.action.START_DICTATION"

/**
 * Плавающая кнопка диктовки. Работает поверх всех экранов лаунчера: главных столов,
 * шторки, меню приложений и настроек. Запускает отдельное приложение Hex.
 *
 * Если установлен патченый Hex с экспортированной точкой входа, открывается сразу
 * лист диктовки. Если Hex есть, но без патча — открывается его главный экран.
 * Если Hex не установлен — показывается подсказка.
 */
@Composable
fun DictationButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(Color(0x661C1C1E))
            .border(1.dp, Color(0x55FFFFFF), CircleShape)
            .clickable {
                val dictation = Intent(HEX_DICTATION_ACTION)
                    .setPackage(HEX_PACKAGE)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (context.packageManager.resolveActivity(dictation, 0) != null) {
                    runCatching { context.startActivity(dictation) }
                        .onFailure {
                            Toast.makeText(context, "Не удалось открыть Hex", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    val app = context.packageManager.getLaunchIntentForPackage(HEX_PACKAGE)
                    if (app == null) {
                        Toast.makeText(context, "Установите Hex для диктовки", Toast.LENGTH_SHORT).show()
                    } else {
                        app.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        runCatching { context.startActivity(app) }
                            .onFailure {
                                Toast.makeText(context, "Не удалось открыть Hex", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Mic,
            contentDescription = "Диктовка Hex",
            tint = Color.White
        )
    }
}
