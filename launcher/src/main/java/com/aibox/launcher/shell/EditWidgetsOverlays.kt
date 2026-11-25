package com.aibox.launcher.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.aibox.launcher.core.model.DesktopWidget
import com.aibox.launcher.core.ui.glassSurface

/** Кнопка «Готово» — выход из режима правки рабочего стола. */
@Composable
fun EditDonePill(
    visible: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
        modifier = modifier
    ) {
        val shape = RoundedCornerShape(22.dp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .glassSurface(shape, strength = 1.2f)
                .background(Color(0xFF0A84FF).copy(alpha = 0.55f))
                .clickable(onClick = onDone)
                .padding(horizontal = 22.dp, vertical = 8.dp)
        ) {
            Text("Готово", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Окно «Добавить виджет»: встроенные виджеты, которых ещё нет на столе, и виджеты других приложений. */
@Composable
fun AddWidgetSheet(
    visible: Boolean,
    available: List<DesktopWidget>,
    onAdd: (DesktopWidget) -> Unit,
    onOpenAppWidgets: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val shape = RoundedCornerShape(24.dp)
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
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .widthIn(max = 420.dp)
                    .heightIn(max = 300.dp)
                    .clip(shape)
                    .background(Color(0xF0171A24))
                    .glassSurface(shape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(vertical = 14.dp)
            ) {
                Text(
                    text = "Добавить виджет",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    available.forEach { widget ->
                        SheetRow(widget.title) {
                            onAdd(widget)
                            onDismiss()
                        }
                    }
                    SheetRow("Виджеты других приложений…") {
                        onDismiss()
                        onOpenAppWidgets()
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetRow(title: String, onClick: () -> Unit) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 16.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    )
}
