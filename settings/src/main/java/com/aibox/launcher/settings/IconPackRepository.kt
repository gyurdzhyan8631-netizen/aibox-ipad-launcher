package com.aibox.launcher.settings

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.XmlResourceParser
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.aibox.launcher.core.model.InstalledApp
import com.aibox.launcher.icons.CustomIconStore
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class IconPackInfo(val packageName: String, val label: String)

/**
 * Поиск готовых пакетов иконок на устройстве и применение их иконок к приложениям.
 * Пакеты определяются по характерным action'ам и по наличию appfilter.xml в APK.
 */
object IconPackRepository {

    private val themeActions = listOf(
        "org.adw.launcher.THEMES",
        "com.android.launcher.THEME",
        "com.nova.launcher.THEME",
        "com.teslacoilsw.launcher.THEME",
        "org.adw.freedroid.THEME",
        "com.anddoes.launcher.THEME",
        "com.miui.home.launcher.icons.theme",
        "com.oppo.launcher.icons.theme",
        "com.samsung.android.launcher.icons.theme",
        "com.oneplus.launcher.icons.theme"
    )

    fun installed(context: Context): List<IconPackInfo> {
        val pm = context.packageManager
        val candidates = linkedSetOf<String>()

        themeActions.forEach { action ->
            runCatching {
                pm.queryIntentActivities(Intent(action), 0).forEach {
                    candidates += it.activityInfo.packageName
                }
            }
        }

        // Многие паки не объявляют action, но почти всегда несут appfilter.xml в assets.
        runCatching {
            val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(launcher, 0).forEach { info ->
                val pkg = info.activityInfo.packageName
                if (pkg !in candidates && hasFilter(pm, pkg)) candidates += pkg
            }
        }

        return candidates.mapNotNull { pkg ->
            runCatching {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                IconPackInfo(pkg, pm.getApplicationLabel(appInfo).toString())
            }.getOrNull()
        }.sortedBy { it.label.lowercase() }
    }

    private fun hasFilter(pm: PackageManager, pkg: String): Boolean = runCatching {
        val res = pm.getResourcesForApplication(pkg)
        res.getIdentifier("appfilter", "xml", pkg) != 0 ||
            res.getIdentifier("appfilter", "raw", pkg) != 0 ||
            res.assets.list("")?.any { it.equals("appfilter.xml", true) } == true
    }.getOrDefault(false)

    /** Применять можно только к тем приложениям, которые реально установлены. */
    fun apply(context: Context, pack: IconPackInfo, apps: List<InstalledApp>): Int {
        val res = runCatching {
            context.packageManager.getResourcesForApplication(pack.packageName)
        }.getOrNull() ?: return 0

        val known = apps.map { it.packageName }.toSet()
        var applied = 0
        mapping(context, pack).forEach { (packageName, drawableName) ->
            if (packageName !in known) return@forEach
            val id = res.getIdentifier(drawableName, "drawable", pack.packageName)
            if (id == 0) return@forEach
            val bitmap = runCatching { res.getDrawable(id, null) }.getOrNull()?.let(::toBitmap) ?: return@forEach
            if (CustomIconStore.setBitmap(context, packageName, bitmap)) applied++
        }
        return applied
    }

    private fun mapping(context: Context, pack: IconPackInfo): Map<String, String> {
        val res = runCatching {
            context.packageManager.getResourcesForApplication(pack.packageName)
        }.getOrNull() ?: return emptyMap()

        val parser: XmlPullParser = runCatching {
            val xmlId = res.getIdentifier("appfilter", "xml", pack.packageName)
            val rawId = res.getIdentifier("appfilter", "raw", pack.packageName)
            when {
                xmlId != 0 -> res.getXml(xmlId)
                rawId != 0 -> XmlPullParserFactory.newInstance().newPullParser().apply {
                    setInput(res.openRawResource(rawId), null)
                }
                else -> XmlPullParserFactory.newInstance().newPullParser().apply {
                    setInput(res.assets.open("appfilter.xml"), null)
                }
            }
        }.getOrNull() ?: return emptyMap()

        val result = mutableMapOf<String, String>()
        runCatching {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name.equals("item", true)) {
                    val component = parser.getAttributeValue(null, "component").orEmpty()
                    val drawable = parser.getAttributeValue(null, "drawable").orEmpty()
                    val packageName = componentPackage(component)
                    if (packageName.isNotEmpty() && drawable.isNotEmpty()) {
                        result[packageName] = drawable
                    }
                }
                event = parser.next()
            }
        }
        runCatching { (parser as? XmlResourceParser)?.close() }
        return result
    }

    private fun componentPackage(component: String): String {
        val inner = if (component.startsWith("ComponentInfo{") && component.endsWith("}")) {
            component.removePrefix("ComponentInfo{").removeSuffix("}")
        } else {
            component
        }
        val slash = inner.indexOf('/')
        return (if (slash > 0) inner.substring(0, slash) else inner).removePrefix("@")
    }

    private fun toBitmap(drawable: Drawable): Bitmap? = runCatching {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            drawable.bitmap
        } else {
            val width = drawable.intrinsicWidth.coerceIn(1, 512)
            val height = drawable.intrinsicHeight.coerceIn(1, 512)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(Canvas(bitmap))
            bitmap
        }
    }.getOrNull()
}
