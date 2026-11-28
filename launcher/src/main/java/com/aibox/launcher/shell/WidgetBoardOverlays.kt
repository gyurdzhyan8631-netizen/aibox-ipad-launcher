package com.aibox.launcher.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.ui.glassSurface
import com.aibox.launcher.core.ui.theme.LocalLauncherTokens

/** Кнопка «Готово» — выход из режима правки рабочего стола. */
@Composable
fun EditDonePill(
    visible: Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalLauncherTokens.current.accent
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
                .background(accent.copy(alpha = 0.55f))
                .clickable(onClick = onDone)
                .padding(horizontal = 22.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Готово",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
