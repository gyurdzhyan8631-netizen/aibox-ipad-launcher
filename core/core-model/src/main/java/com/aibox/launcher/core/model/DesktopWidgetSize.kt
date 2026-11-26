package com.aibox.launcher.core.model

/**
 * Размер виджета на рабочем столе:
 * [weight] — доля ширины в ряду виджетов, [height] — доля высоты рабочей области (0.35…1).
 */
data class DesktopWidgetSize(
    val weight: Float = 1f,
    val height: Float = 1f
)
