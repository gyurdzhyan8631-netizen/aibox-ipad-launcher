package com.aibox.launcher.core.model

/** Приложение, которое можно запустить с рабочего стола. */
data class InstalledApp(
    val label: String,
    val packageName: String
)
