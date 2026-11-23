package com.aibox.launcher.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.model.InstalledApp
import com.aibox.launcher.icons.AppIconView
import com.aibox.launcher.widgets.BatteryWidget
import com.aibox.launcher.widgets.ConnectionWidget
import com.aibox.launcher.widgets.DateWidget

/**
 * Рабочий стол.
 * Страница 0: виджеты на всю высоту. Страница 1: все приложения (прокручивается).
 * Снизу оставлено место под индикатор страниц.
 */
@Composable
fun HomeScreenPage(
    pageIndex: Int,
    apps: List<InstalledApp>,
    onLaunchApp: (InstalledApp) -> Unit,
    modifier: Modifier = Modifier
) {
    val content = modifier
        .fillMaxSize()
        .padding(bottom = 22.dp)

    if (pageIndex == 0) {
        Row(
            modifier = content,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DateWidget(modifier = Modifier.weight(1f).fillMaxHeight())
            BatteryWidget(modifier = Modifier.weight(1f).fillMaxHeight())
            ConnectionWidget(modifier = Modifier.weight(1f).fillMaxHeight())
        }
    } else if (apps.isEmpty()) {
        Box(modifier = content, contentAlignment = Alignment.Center) {
            Text(
                text = "Приложения загружаются…",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 88.dp),
            modifier = content,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(apps, key = { it.packageName }) { app ->
                AppIconView(
                    app = app,
                    iconSize = 56.dp,
                    onClick = { onLaunchApp(app) }
                )
            }
        }
    }
}
