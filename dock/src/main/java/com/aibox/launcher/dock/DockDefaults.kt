package com.aibox.launcher.dock

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Общие размеры нижних панелей: док и капсула кнопок всегда одной высоты. */
object DockDefaults {
    const val MIN_SCALE = 0.75f
    const val MAX_SCALE = 1.35f
    const val DEFAULT_SCALE = 1.0f

    fun panelHeight(scale: Float): Dp = 84.dp * scale.coerceIn(MIN_SCALE, MAX_SCALE)
}
