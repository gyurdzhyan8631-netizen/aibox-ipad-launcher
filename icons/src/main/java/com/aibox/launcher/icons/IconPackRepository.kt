package com.aibox.launcher.icons

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.xmlpull.v1.XmlPullParser

data class IconPackInfo(
    val packageName: String,
    val label: String
)

class IconPackRepository(private val context: Context) {
    private val mappingCache = LruCache<String, Map<String, String>>(16)
    private val iconCache = LruCache<String, ImageBitmap>(512)

    fun installedIconPacks(): List<IconPackInfo> {
        val pm = context.packageManager
        val themes = listOf(
            "org.adw.launcher.THEMES",
            "com.gau.go.launcherex.theme",
            "com.novalauncher.THEME",
            "com.teslacoilsw.launcher.THEME"
        )
        val packageNames = linkedSetOf<String>()

        themes.forEach { action ->
            pm.queryIntentActivities(Intent(action), PackageManager.MATCH_ALL)
                .forEach { resolved: ResolveInfo -> packageNames += resolved.activityInfo.packageName }
        }

        pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES).forEach { appInfo ->
            runCatching {
                val resources = pm.getResourcesForApplication(appInfo)
                if (resources.getIdentifier("appfilter", "xml", appInfo.packageName) != 0) {
                    packageNames += appInfo.packageName
                }
            }
        }

        return packageNames.mapNotNull { packageName ->
            runCatching {
                val appInfo = pm.getApplicationInfo(packageName, PackageManager.MATCH_UNINSTALLED_PACKAGES)
                IconPackInfo(packageName, pm.getApplicationLabel(appInfo).toString())
            }.getOrNull()
        }.sortedBy { it.label.lowercase() }
    }

    fun loadIcon(iconPackPackage: String, appPackageName: String): ImageBitmap? {
        if (iconPackPackage.isBlank()) return null
        val cacheKey = "$iconPackPackage/$appPackageName"
        iconCache.get(cacheKey)?.let { return it }

        val pm = context.packageManager
        val resources = runCatching { pm.getResourcesForApplication(iconPackPackage) }.getOrNull()
            ?: return null
        val mapping = mappingCache.get(iconPackPackage) ?: readAppFilter(resources, iconPackPackage).also {
            mappingCache.put(iconPackPackage, it)
        }
        val drawableName = mapping[appPackageName] ?: return null
        val drawableId = resources.getIdentifier(drawableName, "drawable", iconPackPackage)
        if (drawableId == 0) return null

        val drawable: Drawable = runCatching {
            resources.getDrawable(drawableId, context.theme)
        }.getOrNull() ?: return null
        val bitmap = drawable.toBitmap(width = 160, height = 160).asImageBitmap()
        iconCache.put(cacheKey, bitmap)
        return bitmap
    }

    private fun readAppFilter(resources: android.content.res.Resources, packageName: String): Map<String, String> {
        val mapping = mutableMapOf<String, String>()
        val id = resources.getIdentifier("appfilter", "xml", packageName)
        if (id == 0) return mapping

        runCatching {
            val parser = resources.getXml(id)
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "item") {
                    val component = parser.getAttributeValue(null, "component")
                    val drawable = parser.getAttributeValue(null, "drawable")
                    val appPackage = component
                        ?.substringAfter("{", "")
                        ?.substringBefore("/", "")
                        ?.substringBefore("}", "")
                        ?.trim()
                    if (!appPackage.isNullOrBlank() && !drawable.isNullOrBlank()) {
                        mapping[appPackage] = drawable
                    }
                }
                event = parser.next()
            }
        }
        return mapping
    }
}
