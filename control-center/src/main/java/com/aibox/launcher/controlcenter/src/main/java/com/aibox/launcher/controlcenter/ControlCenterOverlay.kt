package com.aibox.launcher.controlcenter

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TopSwipeControlCenterHost(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val controller = remember(context) { ControlCenterSystemController(context) }

    var uiState by remember {
        mutableStateOf(controller.readSnapshot(currentBrightnessPercent = 70))
    }
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    fun refreshState(
        isOpen: Boolean = uiState.isOpen,
        brightness: Int = uiState.brightnessPercent
    ) {
        val snapshot = controller.readSnapshot(currentBrightnessPercent = brightness)
        uiState = snapshot.copy(
            isOpen = isOpen,
            brightnessPercent = brightness.coerceIn(5, 100)
        )
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        content()

        // Зона захвата свайпа сверху вниз
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.statusBars)
                .pointerInput(uiState.isOpen) {
                    detectVerticalDragGestures(
                        onDragStart = { dragAccumulator = 0f },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            dragAccumulator += dragAmount
                            if (!uiState.isOpen && dragAccumulator > 80f) {
                                refreshState(isOpen = true)
                                dragAccumulator = 0f
                            }
                        },
                        onDragEnd = { dragAccumulator = 0f },
                        onDragCancel = { dragAccumulator = 0f }
                    )
                }
        )

        AnimatedVisibility(
            visible = uiState.isOpen,
            enter = fadeIn() + slideInVertically(
                initialOffsetY = { -it / 2 },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ),
            exit = fadeOut() + slideOutVertically(
                targetOffsetY = { -it / 2 },
                animationSpec = spring(stiffness = Spring.StiffnessMedium)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable {
                        uiState = uiState.copy(isOpen = false)
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            if (dragAmount < -60f) {
                                uiState = uiState.copy(isOpen = false)
                            }
                        }
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                ControlCenterPanel(
                    uiState = uiState,
                    onDismiss = { uiState = uiState.copy(isOpen = false) },
                    onTileClick = { tile ->
                        when (tile) {
                            ControlTileType.WIFI -> controller.openWifiSettings()
                            ControlTileType.BLUETOOTH -> controller.openBluetoothSettings()
                            ControlTileType.INTERNET -> controller.openInternetSettings()
                            ControlTileType.VPN -> controller.openVpnSettings()
                            ControlTileType.SOUND_MODE -> {
                                controller.cycleSoundMode()
                                refreshState(isOpen = true)
                            }
                            ControlTileType.MUSIC -> controller.openMusicApp()
                            ControlTileType.SYSTEM_SETTINGS,
                            ControlTileType.CONNECTION_INFO,
                            ControlTileType.TIME_AND_BATTERY -> controller.openSystemSettings()
                            ControlTileType.BRIGHTNESS,
                            ControlTileType.VOLUME -> Unit
                        }
                    },
                    onBrightnessChange = { value ->
                        val nextBrightness = value.toInt().coerceIn(5, 100)
                        controller.applyWindowBrightness(activity, nextBrightness)
                        uiState = uiState.copy(brightnessPercent = nextBrightness)
                    },
                    onVolumeChange = { value ->
                        val nextVolume = value.toInt().coerceIn(0, 100)
                        controller.setMediaVolumePercent(nextVolume)
                        uiState = uiState.copy(volumePercent = nextVolume)
                    }
                )
            }
        }
    }
}

@Composable
private fun ControlCenterPanel(
    uiState: ControlCenterUiState,
    onDismiss: () -> Unit,
    onTileClick: (ControlTileType) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(16.dp)
            .clickable(enabled = false, onClick = {}),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF1A1D24).copy(alpha = 0.96f),
        contentColor = Color.White,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Control Center",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${uiState.currentTimeText} • ${uiState.connectionSummary} • ${uiState.batteryPercent}%",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
                Button(onClick = onDismiss) {
                    Text("Закрыть")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    title = "Wi-Fi",
                    subtitle = if (uiState.isWifiConnected) "Вкл" else "Настроить",
                    active = uiState.isWifiConnected,
                    onClick = { onTileClick(ControlTileType.WIFI) }
                )
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    title = "Bluetooth",
                    subtitle = if (uiState.isBluetoothEnabled) "Вкл" else "Выкл",
                    active = uiState.isBluetoothEnabled,
                    onClick = { onTileClick(ControlTileType.BLUETOOTH) }
                )
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    title = "Звук",
                    subtitle = uiState.soundMode.title,
                    active = uiState.soundMode == SoundModeLabel.NORMAL,
                    onClick = { onTileClick(ControlTileType.SOUND_MODE) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    title = "Сеть",
                    subtitle = if (uiState.isInternetAvailable) "Онлайн" else "Нет сети",
                    active = uiState.isInternetAvailable,
                    onClick = { onTileClick(ControlTileType.INTERNET) }
                )
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    title = "VPN",
                    subtitle = if (uiState.isVpnActive) "Активен" else "Выкл",
                    active = uiState.isVpnActive,
                    onClick = { onTileClick(ControlTileType.VPN) }
                )
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    title = "Музыка",
                    subtitle = "Открыть",
                    active = false,
                    onClick = { onTileClick(ControlTileType.MUSIC) }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Яркость: ${uiState.brightnessPercent}%",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = uiState.brightnessPercent.toFloat(),
                    onValueChange = onBrightnessChange,
                    valueRange = 5f..100f
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Громкость: ${uiState.volumePercent}%",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = uiState.volumePercent.toFloat(),
                    onValueChange = onVolumeChange,
                    valueRange = 0f..100f
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Настройки системы",
                    color = Color(0xFF7CC4FA),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTileClick(ControlTileType.SYSTEM_SETTINGS) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (active) Color(0xFF2D6CDF) else Color.White.copy(alpha = 0.10f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(2.dp))
    }
}
