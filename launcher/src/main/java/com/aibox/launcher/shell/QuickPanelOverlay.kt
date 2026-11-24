package com.aibox.launcher.shell

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.controlcenter.ControlCenterSystemController
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PanelBlue = Color(0xFF0A84FF)

private fun Modifier.glass(radius: Dp = 28.dp): Modifier {
    val shape = RoundedCornerShape(radius)
    return this
        .clip(shape)
        .background(Color(0x33FFFFFF))
        .border(1.dp, Color(0x40FFFFFF), shape)
}

private fun sendMediaKey(audio: AudioManager?, keyCode: Int) {
    if (audio == null) return
    audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
    audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
}

/**
 * Шторка быстрого доступа: связь, музыка, яркость, громкость и ряд быстрых кнопок.
 */
@Composable
fun QuickPanelOverlay(
    visible: Boolean,
    controller: ControlCenterSystemController,
    weatherText: String,
    onDismiss: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenSettings: (String) -> Unit,
    onOpenMusic: () -> Unit,
    onOpenNavigation: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.96f),
        exit = fadeOut() + scaleOut(targetScale = 0.96f),
        modifier = modifier
    ) {
        val context = LocalContext.current
        val activity = context as? Activity
        val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

        var brightness by remember {
            mutableIntStateOf(
                runCatching {
                    val raw = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                    (raw * 100 / 255).coerceIn(5, 100)
                }.getOrDefault(70)
            )
        }
        var state by remember { mutableStateOf(controller.readSnapshot(brightness)) }
        var volume by remember { mutableIntStateOf(state.volumePercent) }
        var musicActive by remember { mutableStateOf(audio?.isMusicActive == true) }
        var now by remember { mutableStateOf(LocalDateTime.now()) }

        LaunchedEffect(Unit) {
            while (true) {
                state = controller.readSnapshot(brightness)
                musicActive = audio?.isMusicActive == true
                now = LocalDateTime.now()
                delay(1_500L)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xEE070B16))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.82f)
                    .widthIn(max = 640.dp)
            ) {
                // --- шапка: время, дата, «Готово» ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = now.format(DateTimeFormatter.ofPattern("HH:mm")),
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("ru")))
                            .replaceFirstChar { it.titlecase(Locale("ru")) },
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 14.dp)
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0x99FFFFFF).copy(alpha = 0.35f))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 22.dp, vertical = 8.dp)
                    ) {
                        Text("Готово", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // --- главный ряд ---
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .height(150.dp)
                        .fillMaxWidth()
                ) {
                    ConnectivityCard(
                        vpn = state.isVpnActive,
                        wifi = state.isWifiConnected,
                        bluetooth = state.isBluetoothEnabled,
                        mobile = state.isInternetAvailable && state.connectionSummary == "Мобильная сеть",
                        onVpn = { controller.openVpnSettings() },
                        onWifi = { controller.openWifiSettings() },
                        onBluetooth = { controller.openBluetoothSettings() },
                        onMobile = { controller.openInternetSettings() },
                        modifier = Modifier
                            .weight(1.25f)
                            .fillMaxHeight()
                    )
                    MusicCard(
                        playing = musicActive,
                        onOpen = onOpenMusic,
                        onPrevious = { sendMediaKey(audio, KeyEvent.KEYCODE_MEDIA_PREVIOUS) },
                        onPlayPause = { sendMediaKey(audio, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) },
                        onNext = { sendMediaKey(audio, KeyEvent.KEYCODE_MEDIA_NEXT) },
                        modifier = Modifier
                            .weight(1.5f)
                            .fillMaxHeight()
                    )
                    LevelSlider(
                        value = brightness,
                        icon = Icons.Rounded.WbSunny,
                        onChange = { v ->
                            brightness = v.coerceIn(5, 100)
                            controller.applyWindowBrightness(activity, brightness)
                        },
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                    )
                    LevelSlider(
                        value = volume,
                        icon = Icons.Rounded.VolumeUp,
                        onChange = { v ->
                            volume = v.coerceIn(0, 100)
                            controller.setMediaVolumePercent(volume)
                        },
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                    )
                }

                // --- нижний ряд быстрых кнопок ---
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(66.dp)
                ) {
                    QuickTile(Icons.Rounded.Palette, "Авто", { onOpenSettings("personalization") }, Modifier.weight(1f))
                    QuickTile(Icons.Rounded.DesktopWindows, "Обои", { onOpenSettings("personalization") }, Modifier.weight(1f))
                    QuickTile(Icons.Rounded.Navigation, "Навигация", onOpenNavigation, Modifier.weight(1f))
                    QuickTile(Icons.Rounded.Cloud, weatherText, { onOpenSettings("home") }, Modifier.weight(1f))
                    QuickTile(Icons.Rounded.Apps, "Приложения", onOpenApps, Modifier.weight(1f))
                    QuickTile(Icons.Rounded.Settings, "Настройки", { onOpenSettings("home") }, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ConnectivityCard(
    vpn: Boolean,
    wifi: Boolean,
    bluetooth: Boolean,
    mobile: Boolean,
    onVpn: () -> Unit,
    onWifi: () -> Unit,
    onBluetooth: () -> Unit,
    onMobile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .glass()
            .padding(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            RoundToggle(active = vpn, onClick = onVpn, modifier = Modifier.weight(1f).fillMaxHeight()) {
                Text("VPN", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            RoundToggle(active = wifi, onClick = onWifi, modifier = Modifier.weight(1f).fillMaxHeight()) {
                Icon(Icons.Rounded.Wifi, contentDescription = "Wi-Fi", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            RoundToggle(active = bluetooth, onClick = onBluetooth, modifier = Modifier.weight(1f).fillMaxHeight()) {
                Icon(Icons.Rounded.Bluetooth, contentDescription = "Bluetooth", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            RoundToggle(active = mobile, onClick = onMobile, modifier = Modifier.weight(1f).fillMaxHeight()) {
                Icon(Icons.Rounded.SignalCellularAlt, contentDescription = "Мобильная сеть", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun RoundToggle(
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val diameter = minOf(maxWidth, maxHeight)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(diameter)
                .clip(CircleShape)
                .background(if (active) PanelBlue else Color(0x59FFFFFF))
                .clickable(onClick = onClick)
        ) {
            content()
        }
    }
}

@Composable
private fun MusicCard(
    playing: Boolean,
    onOpen: () -> Unit,
    onPrevious: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .glass()
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onOpen)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF7B3FF2), Color(0xFFE040C8))))
            ) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    text = if (playing) "Воспроизводится" else "Не исполняется",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = "Музыка", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
            }
        }
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            MediaButton(Icons.Rounded.SkipPrevious, "Предыдущий", onPrevious)
            MediaButton(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Пауза / играть", onPlayPause)
            MediaButton(Icons.Rounded.SkipNext, "Следующий", onNext)
        }
    }
}

@Composable
private fun MediaButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(26.dp))
    }
}

/** Вертикальный ползунок (яркость, громкость): тянуть или нажать в нужном месте. */
@Composable
private fun LevelSlider(
    value: Int,
    icon: ImageVector,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var heightPx by remember { mutableIntStateOf(1) }
    Box(
        modifier = modifier
            .glass()
            .onSizeChanged { heightPx = it.height.coerceAtLeast(1) }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onChange(((1f - offset.y / heightPx) * 100f).toInt().coerceIn(0, 100))
                }
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    onChange(((1f - change.position.y / heightPx) * 100f).toInt().coerceIn(0, 100))
                }
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight((value / 100f).coerceIn(0.02f, 1f))
                .background(Color.White.copy(alpha = 0.92f))
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF3A3A3C),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .size(20.dp)
        )
    }
}

@Composable
private fun QuickTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .glass(22.dp)
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
