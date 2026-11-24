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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.dock.DockDefaults
import kotlin.math.roundToInt

/**
 * Настройки лаунчера: размер нижних панелей и город для погоды.
 * Город вводится один раз на стоянке; погода дальше обновляется сама и хранится офлайн.
 */
@Composable
fun PanelSettingsOverlay(
    visible: Boolean,
    scale: Float,
    onScaleChange: (Float) -> Unit,
    city: String,
    onCityConfirm: (String) -> Unit,
    weatherStatus: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        var draft by remember { mutableStateOf(city) }
        val finish = {
            onCityConfirm(draft.trim())
            onDismiss()
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .imePadding()
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
                    .fillMaxWidth(0.9f)
                    .widthIn(max = 460.dp)
                    .clip(shape)
                    .background(Color(0xF21C1C1E))
                    .border(1.dp, Color(0x33FFFFFF), shape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                Text(
                    text = "Настройки",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // --- Размер панелей ---
                Text(
                    text = "Размер панелей: ${(scale * 100).roundToInt()}%",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Slider(
                    value = scale,
                    onValueChange = onScaleChange,
                    valueRange = DockDefaults.MIN_SCALE..DockDefaults.MAX_SCALE
                )

                // --- Город для погоды ---
                Text(
                    text = "Город для погоды",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                CityField(
                    value = draft,
                    onValueChange = { draft = it },
                    onDone = { finish() }
                )
                Text(
                    text = weatherStatus,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PanelButton(
                        text = "Сбросить размер",
                        background = Color.White.copy(alpha = 0.14f),
                        onClick = { onScaleChange(DockDefaults.DEFAULT_SCALE) }
                    )
                    PanelButton(
                        text = "Готово",
                        background = Color(0xFF0A84FF),
                        onClick = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
private fun CityField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
        cursorBrush = SolidColor(Color.White),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), shape)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = "Например: Москва",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 17.sp
                    )
                }
                inner()
            }
        }
    )
}

@Composable
private fun PanelButton(text: String, background: Color, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(text = text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
