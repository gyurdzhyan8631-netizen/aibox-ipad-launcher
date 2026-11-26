package com.aibox.launcher.dock

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Общие размеры бокового дока и капсулы горячих клавиш. */
object DockDefaults {
    const val MIN_SCALE = 0.75f
    const val MAX_SCALE = 1.35f
    const val DEFAULT_SCALE = 1.0f

    /** Капсула горячих клавиш. */
    fun panelHeight(scale: Float): Dp = 84.dp * scale.coerceIn(MIN_SCALE, MAX_SCALE)

    /** Иконка дока не зависит от общего масштаба интерфейса. */
    fun iconSize(scale: Float, uiScale: Float): Dp =
        (54.dp * scale.coerceIn(MIN_SCALE, MAX_SCALE)) / uiScale.coerceAtLeast(0.01f)

    /** Ширина вертикальной капсулы дока вместе с внутренними отступами. */
    fun dockWidth(scale: Float, uiScale: Float): Dp =
        iconSize(scale, uiScale) + (16.dp / uiScale.coerceAtLeast(0.01f))
}
