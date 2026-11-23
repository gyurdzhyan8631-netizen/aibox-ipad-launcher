package com.aibox.launcher.icons

import android.content.Context
import android.content.Intent
import com.aibox.launcher.core.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** Читает список приложений, у которых есть иконка запуска. */
class InstalledAppsRepository(private val context: Context) {

    @Suppress("DEPRECATION")
    suspend fun loadLaunchableApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .map { info ->
                InstalledApp(
                    label = info.loadLabel(pm).toString(),
                    packageName = info.activityInfo.packageName
                )
            }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.getDefault()) }
    }
}
