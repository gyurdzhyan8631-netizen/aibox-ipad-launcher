package com.aibox.launcher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.aibox.launcher.core.ui.theme.AiBoxLauncherTheme
import com.aibox.launcher.shell.LauncherRootScreen

class MainActivity : ComponentActivity() {
    private lateinit var widgetHost: AppWidgetHost
    private lateinit var widgetPickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var widgetConfigureLauncher: ActivityResultLauncher<Intent>
    private lateinit var dockIconPickerLauncher: ActivityResultLauncher<Array<String>>
    private var desktopWidgetIds by mutableStateOf<Map<Int, List<Int>>>(emptyMap())
    private var customDockIcons by mutableStateOf<Map<String, String>>(emptyMap())
    private var pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var pendingWidgetScreen = 0
    private var pendingDockIconPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()

        widgetHost = AppWidgetHost(this, APP_WIDGET_HOST_ID)
        desktopWidgetIds = mapOf(0 to readWidgetIds(0), 1 to readWidgetIds(1))
        customDockIcons = readCustomDockIcons()
        widgetPickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::onWidgetPicked
        )
        widgetConfigureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::onWidgetConfigured
        )
        dockIconPickerLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocument(),
            ::onDockIconPicked
        )

        setContent {
            AiBoxLauncherTheme {
                LauncherRootScreen(
                    appWidgetHost = widgetHost,
                    widgetIdsByScreen = desktopWidgetIds,
                    onAddWidget = ::startWidgetPicker,
                    onRemoveWidget = ::removeWidget,
                    customDockIcons = customDockIcons,
                    onChooseDockIcon = ::chooseDockIcon,
                    onClearDockIcon = ::clearDockIcon
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        widgetHost.startListening()
    }

    override fun onStop() {
        widgetHost.stopListening()
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun startWidgetPicker(screenIndex: Int) {
        if (screenIndex !in 0..1) {
            showWidgetMessage("Не удалось определить рабочий стол.")
            return
        }
        pendingWidgetScreen = screenIndex
        pendingWidgetId = widgetHost.allocateAppWidgetId()
        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingWidgetId)
        }
        try {
            widgetPickerLauncher.launch(intent)
        } catch (error: ActivityNotFoundException) {
            discardPendingWidget()
            showWidgetMessage("На устройстве нет системного выбора виджетов.")
        } catch (error: SecurityException) {
            discardPendingWidget()
            showWidgetMessage("Нет разрешения открыть выбор виджетов.")
        }
    }

    private fun onWidgetPicked(result: ActivityResult) {
        val selectedId = result.data?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            pendingWidgetId
        ) ?: pendingWidgetId
        if (result.resultCode != RESULT_OK || selectedId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            discardPendingWidget(selectedId)
            discardPendingWidget()
            showWidgetMessage("Добавление виджета отменено.")
            return
        }
        if (selectedId != pendingWidgetId) widgetHost.deleteAppWidgetId(pendingWidgetId)
        pendingWidgetId = selectedId

        val info = AppWidgetManager.getInstance(this).getAppWidgetInfo(selectedId)
        if (info == null) {
            discardPendingWidget()
            showWidgetMessage("Не удалось получить выбранный виджет.")
            return
        }
        val configureActivity = info.configure
        if (configureActivity == null) {
            savePendingWidget()
        } else {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                .setComponent(configureActivity)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, selectedId)
            try {
                widgetConfigureLauncher.launch(intent)
            } catch (error: ActivityNotFoundException) {
                discardPendingWidget()
                showWidgetMessage("Не удалось открыть настройку виджета.")
            } catch (error: SecurityException) {
                discardPendingWidget()
                showWidgetMessage("Нет разрешения настроить виджет.")
            }
        }
    }

    private fun onWidgetConfigured(result: ActivityResult) {
        if (result.resultCode == RESULT_OK) {
            savePendingWidget()
        } else {
            discardPendingWidget()
            showWidgetMessage("Настройка виджета отменена.")
        }
    }

    private fun savePendingWidget() {
        val id = pendingWidgetId
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID ||
            AppWidgetManager.getInstance(this).getAppWidgetInfo(id) == null
        ) {
            discardPendingWidget()
            showWidgetMessage("Виджет не удалось сохранить.")
            return
        }
        val ids = (desktopWidgetIds[pendingWidgetScreen].orEmpty() + id).distinct()
        desktopWidgetIds = desktopWidgetIds + (pendingWidgetScreen to ids)
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            .edit()
            .putStringSet(widgetPreferenceKey(pendingWidgetScreen), ids.map { it.toString() }.toSet())
            .apply()
        pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        showWidgetMessage("Виджет добавлен.")
    }

    private fun removeWidget(id: Int) {
        val screen = desktopWidgetIds.entries.firstOrNull { id in it.value }?.key ?: return
        val updated = desktopWidgetIds[screen].orEmpty() - id
        desktopWidgetIds = desktopWidgetIds + (screen to updated)
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            .edit()
            .putStringSet(widgetPreferenceKey(screen), updated.map { it.toString() }.toSet())
            .apply()
        widgetHost.deleteAppWidgetId(id)
    }

    private fun readWidgetIds(screenIndex: Int): List<Int> =
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            .getStringSet(widgetPreferenceKey(screenIndex), emptySet())
            .orEmpty()
            .mapNotNull(String::toIntOrNull)

    private fun chooseDockIcon(packageName: String) {
        pendingDockIconPackage = packageName
        try {
            dockIconPickerLauncher.launch(arrayOf("image/*"))
        } catch (error: ActivityNotFoundException) {
            pendingDockIconPackage = null
            showWidgetMessage("Не удалось открыть выбор изображения.")
        } catch (error: SecurityException) {
            pendingDockIconPackage = null
            showWidgetMessage("Нет разрешения выбрать изображение.")
        }
    }

    private fun onDockIconPicked(uri: Uri?) {
        val packageName = pendingDockIconPackage
        pendingDockIconPackage = null
        if (uri == null || packageName == null) return
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (error: SecurityException) {
            showWidgetMessage("Не удалось сохранить доступ к изображению.")
            return
        }
        customDockIcons = customDockIcons + (packageName to uri.toString())
        saveCustomDockIcons()
        showWidgetMessage("Значок изменён.")
    }

    private fun clearDockIcon(packageName: String) {
        val uri = customDockIcons[packageName] ?: return
        customDockIcons = customDockIcons - packageName
        saveCustomDockIcons()
        try {
            contentResolver.releasePersistableUriPermission(
                Uri.parse(uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (error: SecurityException) {
            showWidgetMessage("Значок удалён, но доступ к файлу не удалось закрыть.")
        } catch (error: IllegalArgumentException) {
            showWidgetMessage("Значок удалён; сохранённое разрешение уже отсутствует.")
        }
    }

    private fun readCustomDockIcons(): Map<String, String> =
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            .getStringSet(DOCK_ICONS_PREFERENCE, emptySet())
            .orEmpty()
            .mapNotNull { entry ->
                val separator = entry.indexOf('|')
                if (separator <= 0 || separator == entry.lastIndex) null
                else entry.substring(0, separator) to entry.substring(separator + 1)
            }
            .toMap()

    private fun saveCustomDockIcons() {
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            .edit()
            .putStringSet(
                DOCK_ICONS_PREFERENCE,
                customDockIcons.map { (packageName, uri) -> "$packageName|$uri" }.toSet()
            )
            .apply()
    }

    private fun discardPendingWidget(id: Int = pendingWidgetId) {
        if (id != AppWidgetManager.INVALID_APPWIDGET_ID) widgetHost.deleteAppWidgetId(id)
        if (pendingWidgetId == id) pendingWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    }

    private fun showWidgetMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private companion object {
        const val APP_WIDGET_HOST_ID = 0xA1B0
        const val PREFERENCES_NAME = "launcher_prefs"
        const val DOCK_ICONS_PREFERENCE = "custom_dock_icons"

        fun widgetPreferenceKey(screenIndex: Int) = "hosted_widgets_screen_$screenIndex"
    }
}
