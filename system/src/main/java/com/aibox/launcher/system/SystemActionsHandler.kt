package com.aibox.launcher.system

import android.content.Context
import android.content.Intent
import android.provider.Settings

class SystemActionsHandler(private val context: Context) {

    fun triggerBack(fallback: () -> Unit = {}) {
        val handled = NavAccessibilityService.instance?.performBack() ?: false
        if (!handled) fallback()
    }

    fun triggerHome(onGoHome: () -> Unit) {
        onGoHome()
        NavAccessibilityService.instance?.performHome()
    }

    fun triggerRecents(): Boolean {
        val handled = NavAccessibilityService.instance?.performRecents() ?: false
        if (!handled) {
            // Fallback: open Accessibility Settings so user can enable the capsule service
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
        }
        return handled
    }

    fun openSystemSettings() {
        val intent = Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
