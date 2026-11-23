package com.aibox.launcher.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

/** Сколько приложений помещается на первый рабочий стол под виджетами. */
const val HOME_PAGE_ONE_APP_COUNT = 8

/**
 * Рабочий стол.
 * Страница 0: виджеты + первые приложения. Страница 1: остальные приложения.
 */
@Composable
fun HomeScreenPage(
    pageIndex: Int,
    apps: List<InstalledApp>,
    onLaunchApp: (InstalledApp) -> Unit,
    modifier: Modifier = Modifier
) {
    if (pageIndex == 0) {
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DateWidget(modifier = Modifier.weight(1f).fillMaxSize())
                BatteryWidget(modifier = Modifier.weight(1f).fillMaxSize())
                ConnectionWidget(modifier = Modifier.weight(1f).fillMaxSize())
            }
            AppGrid(
                apps = apps.take(HOME_PAGE_ONE_APP_COUNT),
                onLaunchApp = onLaunchApp,
                modifier = Modifier.weight(1f).fillMaxWidth()
            )
        }
    } else {
        val rest = apps.drop(HOME_PAGE_ONE_APP_COUNT)
        if (rest.isEmpty()) {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Здесь пока пусто.\nВсе приложения — в меню «Приложения» на панели.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            AppGrid(
                apps = rest,
                onLaunchApp = onLaunchApp,
                modifier = modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun AppGrid(
    apps: List<InstalledApp>,
    onLaunchApp: (InstalledApp) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 96.dp),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(apps, key = { it.packageName }) { app ->
            AppIconView(app = app, onClick = { onLaunchApp(app) })
        }
    }
}
