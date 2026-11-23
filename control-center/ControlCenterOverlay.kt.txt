package com.aibox.launcher.controlcenter

import android.app.Activity
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun TopSwipeControlCenterHost(
    modifier: Modifier = Modifier,
    topEdgeTriggerPx: Float = 96f,
    dragThresholdPx: Float = 140f,
    visibleTiles: List<ControlTileType> = ControlCenterUiState().visibleTiles,
    onOpenSettingsRequested: () -> Unit = {},
    content: @Composable (onOpenControlCenter: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val controller = remember(context) { ControlCenterSystemController(context) }

    var brightnessPercent by remember { mutableIntStateOf(75) }
    var uiState by remember {
        mutableStateOf(controller.readSnapshot(brightnessPercent).copy(visibleTiles = visibleTiles))
    }

    LaunchedEffect(uiState.isOpen, visibleTiles) {
        while (true) {
            val fresh = controller.readSnapshot(brightnessPercent)
            uiState = fresh.copy(
                isOpen = uiState.isOpen,
                visibleTiles = visibleTiles
            )
            delay(if (uiState.isOpen) 1500L else 5000L)
        }
    }

    fun setOpen(open: Boolean) {
        val fresh = controller.readSnapshot(brightnessPercent)
        uiState = fresh.copy(isOpen = open, visibleTiles = visibleTiles)
    }

    BackHandler(enabled = uiState.isOpen) {
        setOpen(false)
    }

    var accumulatedDrag by remember { mutableStateOf(0f) }
    var gestureStartedAtTop by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(uiState.isOpen) {
                if (uiState.isOpen) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = { startOffset ->
                        gestureStartedAtTop = startOffset.y <= topEdgeTriggerPx
                        accumulatedDrag = 0f
                    },
                    onDragEnd = {
                        if (gestureStartedAtTop && accumulatedDrag >= dragThresholdPx) {
                            setOpen(true)
                        }
                        gestureStartedAtTop = false
                        accumulatedDrag = 0f
                    },
                    onDragCancel = {
                        gestureStartedAtTop = false
                        accumulatedDrag = 0f
                    },
                    onVerticalDrag = { change, dragAmount ->
                        if (gestureStartedAtTop && dragAmount > 0f) {
                            accumulatedDrag += dragAmount
                            if (accumulatedDrag >= dragThresholdPx) {
                                change.consume()
                                setOpen(true)
                                gestureStartedAtTop = false
                                accumulatedDrag = 0f
                            }
                        }
                    }
                )
            }
    ) {
        content { setOpen(true) }

        AnimatedVisibility(
            visible = uiState.isOpen,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                slideInVertically(
                    initialOffsetY = { -it / 4 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it / 4 })
        ) {
            ControlCenterPanel(
                state = uiState,
                onDismiss = { setOpen(false) },
                onWifiClick = { controller.openWifiSettings() },
                onBluetoothClick = { controller.openBluetoothSettings() },
                onInternetClick = { controller.openInternetSettings() },
                onVpnClick = { controller.openVpnSettings() },
                onSoundModeClick = {
                    controller.cycleSoundMode()
                    uiState = controller.readSnapshot(brightnessPercent)
                        .copy(isOpen = true, visibleTiles = visibleTiles)
                },
                onBrightnessChange = { newVal ->
                    brightnessPercent = newVal
                    controller.applyWindowBrightness(activity, newVal)
                    uiState = uiState.copy(brightnessPercent = newVal)
                },
                onVolumeChange = { newVal ->
                    controller.setMediaVolumePercent(newVal)
                    uiState = uiState.copy(volumePercent = newVal)
                },
                onMusicClick = { controller.openMusicApp() },
                onSystemSettingsClick = {
                    setOpen(false)
                    onOpenSettingsRequested()
                },
                onAndroidSettingsClick = { controller.openSystemSettings() }
            )
        }
    }
}

@Composable
private fun ControlCenterPanel(
    state: ControlCenterUiState,
    onDismiss: () -> Unit,
    onWifiClick: () -> Unit,
    onBluetoothClick: () -> Unit,
    onInternetClick: () -> Unit,
    onVpnClick: () -> Unit,
    onSoundModeClick: () -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onMusicClick: () -> Unit,
    onSystemSettingsClick: () -> Unit,
    onAndroidSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.48f))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f)
                .clickable(enabled = false, onClick = {}),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1A1D24).copy(alpha = 0.92f),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Control Center",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "${state.currentTimeText} • ${state.connectionSummary}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.72f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onSystemSettingsClick) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Настройки лаунчера",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Закрыть",
                                tint = Color.White
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .weight(1.35f)
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.visibleTiles, key = { it.name }) { tile ->
                            when (tile) {
                                ControlTileType.WIFI -> ControlToggleCard(
                                    title = "Wi-Fi",
                                    subtitle = if (state.isWifiConnected) "Активен" else "Настроить",
                                    active = state.isWifiConnected,
                                    icon = Icons.Rounded.Wifi,
                                    onClick = onWifiClick
                                )

                                ControlTileType.BLUETOOTH -> ControlToggleCard(
                                    title = "Bluetooth",
                                    subtitle = if (state.isBluetoothEnabled) "Включён" else "Настроить",
                                    active = state.isBluetoothEnabled,
                                    icon = Icons.Rounded.Bluetooth,
                                    onClick = onBluetoothClick
                                )

                                ControlTileType.INTERNET -> ControlToggleCard(
                                    title = "Интернет",
                                    subtitle = if (state.isInternetAvailable) "Доступен" else "Сеть",
                                    active = state.isInternetAvailable,
                                    icon = Icons.Rounded.Language,
                                    onClick = onInternetClick
                                )

                                ControlTileType.VPN -> ControlToggleCard(
                                    title = "VPN",
                                    subtitle = if (state.isVpnActive) "Подключён" else "Выкл / Настр.",
                                    active = state.isVpnActive,
                                    icon = Icons.Rounded.Security,
                                    onClick = onVpnClick
                                )

                                ControlTileType.SOUND_MODE -> ControlToggleCard(
                                    title = "Звук",
                                    subtitle = state.soundMode.title,
                                    active = state.soundMode == SoundModeLabel.NORMAL,
                                    icon = if (state.soundMode == SoundModeLabel.SILENT) {
                                        Icons.Rounded.VolumeOff
                                    } else {
                                        Icons.Rounded.VolumeUp
                                    },
                                    onClick = onSoundModeClick
                                )

                                ControlTileType.MUSIC -> ControlToggleCard(
                                    title = "Музыка",
                                    subtitle = "Открыть плеер",
                                    active = false,
                                    icon = Icons.Rounded.MusicNote,
                                    onClick = onMusicClick
                                )

                                ControlTileType.CONNECTION_INFO -> ControlToggleCard(
                                    title = "Подключение",
                                    subtitle = state.connectionSummary,
                                    active = state.isInternetAvailable,
                                    icon = Icons.Rounded.NetworkCheck,
                                    onClick = onInternetClick
                                )

                                ControlTileType.TIME_AND_BATTERY -> ControlToggleCard(
                                    title = state.currentTimeText,
                                    subtitle = state.batteryPercent?.let { "Батарея: $it%" }
                                        ?: "Питание AI Box",
                                    active = state.isCharging,
                                    icon = if (state.isCharging) {
                                        Icons.Rounded.BatteryChargingFull
                                    } else {
                                        Icons.Rounded.BatteryFull
                                    },
                                    onClick = onAndroidSettingsClick
                                )

                                ControlTileType.SYSTEM_SETTINGS -> ControlToggleCard(
                                    title = "Система",
                                    subtitle = "Настройки Android",
                                    active = false,
                                    icon = Icons.Rounded.Settings,
                                    onClick = onAndroidSettingsClick
                                )

                                ControlTileType.BRIGHTNESS,
                                ControlTileType.VOLUME -> Unit
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (ControlTileType.BRIGHTNESS in state.visibleTiles) {
                            SliderCard(
                                title = "Яркость",
                                valuePercent = state.brightnessPercent,
                                icon = Icons.Rounded.Brightness6,
                                onValueChange = onBrightnessChange,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (ControlTileType.VOLUME in state.visibleTiles) {
                            SliderCard(
                                title = "Громкость медиа",
                                valuePercent = state.volumePercent,
                                icon = Icons.Rounded.VolumeUp,
                                onValueChange = onVolumeChange,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlToggleCard(
    title: String,
    subtitle: String,
    active: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val bg = if (active) Color(0xFF0A84FF).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.10f)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = bg
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.78f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SliderCard(
    title: String,
    valuePercent: Int,
    icon: ImageVector,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.10f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "$valuePercent%",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Slider(
                value = valuePercent.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = 5f..100f
            )
        }
    }
}
