package com.aibox.launcher.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.ui.theme.LocalLauncherTokens

/** Заглушка для системного виджета, который больше недоступен или не привязан. */
@Composable
fun MissingWidgetPlaceholder() {
    val tokens = LocalLauncherTokens.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().padding(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Виджет недоступен",
                color = tokens.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Удалите его кнопкой «×» и добавьте заново",
                color = tokens.textSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
