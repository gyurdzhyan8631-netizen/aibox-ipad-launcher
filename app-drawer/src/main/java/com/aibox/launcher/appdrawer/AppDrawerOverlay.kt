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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.model.InstalledApp
import com.aibox.launcher.core.ui.glassSurface
import com.aibox.launcher.icons.AppIconView
import java.util.Locale

/**
 * Табло приложений: тёмная стеклянная панель по центру экрана (≈55% ширины и ≈86% высоты),
 * восемь колонок иконок с белыми подписями и поиск-таблетка внизу панели.
 * Размеры иконок и подписей считаются от ширины панели, поэтому вид одинаков на любом экране.
 */
@Composable
fun AppDrawerOverlay(
    visible: Boolean,
    apps: List<InstalledApp>,
    onLaunchApp: (InstalledApp) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    iconScale: Float = 1f,
    bottomInset: Dp = 0.dp
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
        val panelShape = RoundedCornerShape(26.dp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC050A18))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            // --- панель ---
            BoxWithConstraints(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.56f)
                    .fillMaxHeight(0.86f)
                    .clip(panelShape)
                    .background(
                        Brush.verticalGradient(listOf(Color(0xF0222A42), Color(0xF0131A2E)))
                    )
                    .glassSurface(panelShape, strength = 0.8f)
                    // касания внутри панели не закрывают табло
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                val sidePadding = maxWidth * 0.028f
                val cellWidth = (maxWidth - sidePadding * 2) / 8
                val iconSize = cellWidth * 0.5f * iconScale
                val labelSize = (cellWidth.value * 0.2f).coerceIn(8f, 12f).sp

                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = sidePadding),
                    contentPadding = PaddingValues(top = maxHeight * 0.04f, bottom = maxHeight * 0.14f),
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalArrangement = Arrangement.spacedBy(maxHeight * 0.012f)
                ) {
                    items(filtered, key = { it.packageName }) { app ->
                        AppIconView(
                            app = app,
                            iconSize = iconSize,
                            labelColor = Color.White,
                            labelSize = labelSize,
                            onClick = { onLaunchApp(app) }
                        )
                    }
                }

                SearchPill(
                    query = query,
                    onQueryChange = { query = it },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = maxHeight * 0.025f)
                        .fillMaxWidth(0.52f)
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
    val shape = RoundedCornerShape(28.dp)
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
        cursorBrush = SolidColor(Color.White),
        modifier = modifier,
        decorationBox = { inner ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(Color(0xE61A2036))
                    .glassSurface(shape, strength = 1.1f)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp)
                )
                Box(modifier = Modifier.width(10.dp))
                Box {
                    if (query.isEmpty()) {
                        Text("Поиск", color = Color.White.copy(alpha = 0.6f), fontSize = 14.sp)
                    }
                    inner()
                }
            }
        }
    )
}
