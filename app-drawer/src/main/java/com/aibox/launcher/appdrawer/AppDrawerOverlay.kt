package com.aibox.launcher.appdrawer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.model.InstalledApp
import com.aibox.launcher.icons.AppIconView
import java.util.Locale

/** Полноэкранное меню всех приложений с поиском. */
@Composable
fun AppDrawerOverlay(
    visible: Boolean,
    apps: List<InstalledApp>,
    onLaunchApp: (InstalledApp) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    iconScale: Float = 1f,
    customIcons: Map<String, String> = emptyMap(),
    iconCornerRadiusFraction: Float = 0.22f
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.96f),
        exit = fadeOut() + scaleOut(targetScale = 0.96f),
        modifier = modifier
    ) {
        var query by remember { mutableStateOf("") }
        val filtered = remember(apps, query) {
            val q = query.trim().lowercase(Locale.getDefault())
            if (q.isEmpty()) apps
            else apps.filter { it.label.lowercase(Locale.getDefault()).contains(q) }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xB3101218))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val panelSize = minOf(maxHeight * 0.76f, maxWidth * 0.62f, 440.dp)
                val panelShape = RoundedCornerShape(28.dp)
                Column(
                    modifier = Modifier
                        .size(panelSize)
                        .clip(panelShape)
                        .background(Color(0xB51C1F2A))
                        .border(1.dp, Color(0x44FFFFFF), panelShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SearchField(
                        query = query,
                        onQueryChange = { query = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 88.dp * iconScale),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.packageName }) { app ->
                            AppIconView(
                                app = app,
                                iconSize = 56.dp * iconScale,
                                customIconUri = customIcons[app.packageName],
                                cornerRadiusFraction = iconCornerRadiusFraction,
                                onClick = { onLaunchApp(app) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
        cursorBrush = SolidColor(Color.White),
        modifier = modifier,
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), shape)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Поиск приложений",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 18.sp
                    )
                }
                inner()
            }
        }
    )
}
