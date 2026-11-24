package com.aibox.launcher.settings

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val tag: String,
    val build: Int,
    val title: String,
    val notes: String,
    val apkUrl: String
)

class NoReleasesException : Exception("Релизов пока нет")

/**
 * Проверка и установка обновлений. Каждая сборка на GitHub публикуется как релиз «build-N»
 * с APK-файлом; номер N сравнивается с версией установленного приложения.
 */
object UpdateChecker {
    private const val REPO = "gyurdzhyan8631-netizen/aibox-ipad-launcher"

    suspend fun fetchLatest(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL("https://api.github.com/repos/$REPO/releases/latest")
                .openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 8_000
                connection.readTimeout = 8_000
                connection.setRequestProperty("Accept", "application/vnd.github+json")
                connection.setRequestProperty("User-Agent", "aibox-launcher")
                val code = connection.responseCode
                if (code == 404) throw NoReleasesException()
                if (code !in 200..299) throw IllegalStateException("HTTP $code")
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val tag = json.getString("tag_name")
                val assets = json.getJSONArray("assets")
                var apkUrl = ""
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    if (asset.getString("name").endsWith(".apk")) {
                        apkUrl = asset.getString("browser_download_url")
                        break
                    }
                }
                if (apkUrl.isEmpty()) throw NoReleasesException()
                UpdateInfo(
                    tag = tag,
                    build = tag.substringAfterLast('-').toIntOrNull() ?: 0,
                    title = json.optString("name", tag),
                    notes = json.optString("body", ""),
                    apkUrl = apkUrl
                )
            } finally {
                connection.disconnect()
            }
        }
    }

    /** Скачивает APK через системный загрузчик и запускает установку. */
    fun downloadAndInstall(context: Context, info: UpdateInfo) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(
                context,
                "Разрешите установку приложений из этого источника и повторите",
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
            return
        }

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        if (manager == null) {
            Toast.makeText(context, "Загрузчик недоступен на устройстве", Toast.LENGTH_LONG).show()
            return
        }
        val request = DownloadManager.Request(Uri.parse(info.apkUrl))
            .setTitle("AI Box Launcher ${info.tag}")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "aibox-launcher-update.apk")
        val downloadId = manager.enqueue(request)
        Toast.makeText(context, "Загрузка обновления началась", Toast.LENGTH_SHORT).show()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) != downloadId) return
                runCatching { context.unregisterReceiver(this) }
                val uri = manager.getUriForDownloadedFile(downloadId)
                if (uri == null) {
                    Toast.makeText(context, "Не удалось скачать обновление", Toast.LENGTH_LONG).show()
                    return
                }
                val install = Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                runCatching { context.startActivity(install) }
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }
}
