package com.aibox.launcher.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.dock.DockDefaults
import kotlin.math.roundToInt

/** Окно «Размер панелей»: один ползунок меняет и док, и капсулу кнопок (они остаются одной высоты). */
@Composable
fun PanelSettingsOverlay(
    visible: Boolean,
    scale: Float,
    onScaleChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            val shape = RoundedCornerShape(28.dp)
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .width(440.dp)
                    .clip(shape)
                    .background(Color(0xF21C1C1E))
                    .border(1.dp, Color(0x33FFFFFF), shape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(22.dp)
            ) {
                Text(
                    text = "Размер панелей",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Док и кнопки навигации меняются вместе — всегда одной высоты.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
                Text(
                    text = "${(scale * 100).roundToInt()}%",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = scale,
                    onValueChange = onScaleChange,
                    valueRange = DockDefaults.MIN_SCALE..DockDefaults.MAX_SCALE
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PanelButton(
                        text = "Сбросить",
                        background = Color.White.copy(alpha = 0.14f),
                        onClick = { onScaleChange(DockDefaults.DEFAULT_SCALE) }
                    )
                    PanelButton(
                        text = "Готово",
                        background = Color(0xFF0A84FF),
                        onClick = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun PanelButton(text: String, background: Color, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
