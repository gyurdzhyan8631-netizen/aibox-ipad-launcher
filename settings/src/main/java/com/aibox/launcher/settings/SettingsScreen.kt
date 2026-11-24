package com.aibox.launcher.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.model.InstalledApp
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Разделы настроек (левое меню, как в настройках iPad). */
enum class SettingsSection(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val color: Color
) {
    HOME("home", "Главная", Icons.Rounded.Home, Color(0xFF30D158)),
    WIFI("wifi", "Wi-Fi", Icons.Rounded.Wifi, Color(0xFF0A84FF)),
    BLUETOOTH("bluetooth", "Bluetooth", Icons.Rounded.Bluetooth, Color(0xFF0A84FF)),
    DISPLAY("display", "Экран и яркость", Icons.Rounded.WbSunny, Color(0xFF0A84FF)),
    SOUND("sound", "Звук", Icons.Rounded.VolumeUp, Color(0xFFFF375F)),
    PERSONALIZATION("personalization", "Персонализация", Icons.Rounded.Palette, Color(0xFF5E5CE6)),
    APPS("apps", "Приложения", Icons.Rounded.Apps, Color(0xFFBF5AF2)),
    SYSTEM("system", "Система", Icons.Rounded.Info, Color(0xFF8E8E93)),
    ABOUT("about", "О лаунчере", Icons.Rounded.Star, Color(0xFF8E8E93));

    companion object {
        fun fromId(id: String?): SettingsSection =
            entries.firstOrNull { it.id == id } ?: PERSONALIZATION
    }
}

/** Полноэкранные настройки: слева разделы, справа содержимое выбранного раздела. */
@Composable
fun SettingsScreen(
    visible: Boolean,
    initialSectionId: String,
    settings: LauncherSettings,
    apps: List<InstalledApp>,
    weatherStatus: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 8 }),
        exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 8 }),
        modifier = modifier
    ) {
        var section by remember { mutableStateOf(SettingsSection.fromId(initialSectionId)) }
        var picker by remember { mutableStateOf<PickerRequest?>(null) }
        val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
        var time by remember { mutableStateOf(LocalTime.now().format(formatter)) }
        LaunchedEffect(Unit) {
            while (true) {
                time = LocalTime.now().format(formatter)
                delay(20_000L)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SettingsColors.background)
                // поглощаем касания, чтобы они не доходили до экрана под настройками
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // --- верхняя панель ---
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(width = 64.dp, height = 44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF2A2E3B))
                            .clickable(onClick = onClose)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = section.title,
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = time, color = Color.White, fontSize = 20.sp)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Sidebar(
                        selected = section,
                        onSelect = { section = it },
                        modifier = Modifier
                            .weight(0.34f)
                            .fillMaxHeight()
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .weight(0.66f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 12.dp)
                    ) {
                        when (section) {
                            SettingsSection.HOME -> HomeSection(settings, weatherStatus)
                            SettingsSection.WIFI -> WifiSection()
                            SettingsSection.BLUETOOTH -> BluetoothSection()
                            SettingsSection.DISPLAY -> DisplaySection()
                            SettingsSection.SOUND -> SoundSection()
                            SettingsSection.PERSONALIZATION -> PersonalizationSection(
                                settings = settings,
                                apps = apps,
                                onPick = { picker = it },
                                onOpenSection = { section = it }
                            )
                            SettingsSection.APPS -> AppsSection(settings, apps)
                            SettingsSection.SYSTEM -> SystemSection()
                            SettingsSection.ABOUT -> AboutSection()
                        }
                    }
                }
            }

            val request = picker
            if (request != null) {
                OptionPicker(request = request, onDismiss = { picker = null })
            }
        }
    }
}

@Composable
private fun Sidebar(
    selected: SettingsSection,
    onSelect: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .clip(shape)
            .background(SettingsColors.card)
            .border(1.dp, SettingsColors.cardBorder, shape)
            .verticalScroll(rememberScrollState())
            .padding(10.dp)
    ) {
        SettingsSection.entries.forEach { item ->
            val isSelected = item == selected
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) SettingsColors.accent else Color.Transparent)
                    .clickable { onSelect(item) }
                    .padding(horizontal = 10.dp, vertical = 9.dp)
            ) {
                IconBadge(item.icon, item.color)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}
