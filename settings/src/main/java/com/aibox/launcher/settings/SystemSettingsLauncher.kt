package com.aibox.launcher.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

/** Открывает разделы системных настроек Android. Если раздела нет на устройстве — сообщает об этом. */
object SystemSettingsLauncher {

    fun open(context: Context, action: String) {
        val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure {
                Toast.makeText(context, "Этот раздел недоступен на устройстве", Toast.LENGTH_SHORT).show()
            }
    }

    fun openWifi(context: Context) {
        val panel = Intent(Settings.Panel.ACTION_WIFI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(panel) }
            .onFailure { open(context, Settings.ACTION_WIFI_SETTINGS) }
    }

    fun openAppDetails(context: Context, packageName: String) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
