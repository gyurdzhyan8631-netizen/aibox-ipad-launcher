package com.aibox.launcher.core.model

/**
 * Положение и размер виджета на рабочем столе.
 * [x], [y] — левый верхний угол в долях рабочей области (0…1),
 * [weight] — ширина в долях рабочей области, [height] — высота в долях.
 */
data class DesktopWidgetSize(
    val weight: Float = 1f,
    val height: Float = 1f,
    val x: Float = 0f,
    val y: Float = 0f
)
