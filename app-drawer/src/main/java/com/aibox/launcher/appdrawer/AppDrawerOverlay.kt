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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.model.InstalledApp
import com.aibox.launcher.icons.AppIconView
import java.util.Locale

private val PanelColor = Color(0xEBF1F1F4)
private val LabelColor = Color(0xFF2B2B30)

/**
 * Табло приложений в стиле Samsung DeX: светлая матовая панель по центру над нижним доком,
 * сетка небольших иконок с подписями и плавающая строка поиска внизу панели.
 */
@Composable
fun AppDrawerOverlay(
    visible: Boolean,
    apps: List<InstalledApp>,
    onLaunchApp: (InstalledApp) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    iconScale: Float = 1f,
    bottomInset: Dp = 110.dp
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
        val panelShape = RoundedCornerShape(28.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x99000000))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            // --- сама панель ---
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomInset)
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight(0.74f)
                    .clip(panelShape)
                    .background(PanelColor)
                    .border(1.dp, Color(0x66FFFFFF), panelShape)
                    // касания внутри панели не закрывают табло
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 78.dp * iconScale),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 74.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        AppIconView(
                            app = app,
                            iconSize = 40.dp * iconScale,
                            labelColor = LabelColor,
                            labelSize = 11.sp,
                            onClick = { onLaunchApp(app) }
                        )
                    }
                }

                SearchPill(
                    query = query,
                    onQueryChange = { query = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .fillMaxWidth(0.42f)
                )
            }
        }
    }
}

@Composable
private fun SearchPill(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(color = LabelColor, fontSize = 15.sp),
        cursorBrush = SolidColor(LabelColor),
        modifier = modifier,
        decorationBox = { inner ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Color(0xFFF8F8FA))
                    .border(1.dp, Color(0x22000000), shape)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = Color(0xFF55555C),
                    modifier = Modifier.size(18.dp)
                )
                Box(modifier = Modifier.width(10.dp))
                Box {
                    if (query.isEmpty()) {
                        Text("Поиск", color = Color(0xFF6B6B73), fontSize = 15.sp)
                    }
                    inner()
                }
            }
        }
    )
}
