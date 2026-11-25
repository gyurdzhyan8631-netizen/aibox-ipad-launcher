package com.aibox.launcher.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.model.WidgetCardStyle
import com.aibox.launcher.core.ui.theme.LocalLauncherTokens
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Виджет «Дата»: день недели, число и месяц. */
@Composable
fun DateWidget(
    modifier: Modifier = Modifier,
    cardStyle: WidgetCardStyle = WidgetCardStyle.GLASS
) {
    val tokens = LocalLauncherTokens.current
    var today by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            today = LocalDate.now()
            delay(60_000L)
        }
    }

    val ru = Locale("ru")
    GlassCard(modifier = modifier, style = cardStyle) {
        Column {
            Text(
                text = today.format(DateTimeFormatter.ofPattern("EEEE", ru))
                    .replaceFirstChar { it.titlecase(ru) },
                color = tokens.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = today.dayOfMonth.toString(),
                color = tokens.textPrimary,
                fontSize = widgetNumberSize(),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = today.format(DateTimeFormatter.ofPattern("LLLL yyyy", ru)),
                color = tokens.textSecondary,
                fontSize = 13.sp
            )
        }
    }
}
