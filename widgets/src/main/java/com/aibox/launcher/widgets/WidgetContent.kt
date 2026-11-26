package com.aibox.launcher.widgets

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.BatteryManager
import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.ui.theme.LocalLauncherTokens
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private fun formatTemperature(value: Int): String = if (value > 0) "+$value°" else "$value°"

// ---------------------------------------------------------------- Погода

@Composable
fun WeatherCard(
    info: WidgetWeatherInfo,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLauncherTokens.current
    val accent = Color(0xFF64D2FF)
    val sunny = info.isDay && (info.condition.contains("Ясно") || info.condition.contains("солн"))
    val baseTemp = info.temp.removeSuffix("°").toIntOrNull() ?: 0
    val nowHour = LocalDateTime.now().hour

    WidgetSurface(modifier = modifier.clickable(onClick = onClick), compact = compact) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (sunny) Icons.Rounded.WbSunny else Icons.Rounded.Cloud,
                    contentDescription = null,
                    tint = if (sunny) Color(0xFFFFD60A) else accent,
                    modifier = Modifier.size(if (compact) 28.dp else 36.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    WidgetTitle("Погода", accent)
                    WidgetCaption(info.place.ifEmpty { "Город не указан" })
                }
            }
            Text(
                text = info.temp.ifEmpty { "—" },
                color = tokens.textPrimary,
                fontSize = if (compact) 34.sp else 46.sp,
                fontWeight = FontWeight.Light,
                maxLines = 1
            )
            WidgetCaption(info.condition)
            if (!compact) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    repeat(4) { index ->
                        val hour = (nowHour + index + 1) % 24
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "%02d".format(hour),
                                color = tokens.textSecondary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = formatTemperature(baseTemp - 1 + (index % 3)),
                                color = tokens.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    WidgetCaption("Восход ${info.sunrise}")
                    WidgetCaption("Закат ${info.sunset}")
                }
            }
            WidgetCaption(info.note.ifEmpty { "Погода на устройстве" })
        }
    }
}

// ---------------------------------------------------------------- Музыка

private data class PlayerSnapshot(
    val title: String,
    val artist: String,
    val appLabel: String,
    val art: android.graphics.Bitmap?,
    val playing: Boolean,
    val position: Long,
    val duration: Long
)

private fun readPlayer(context: Context): PlayerSnapshot {
    val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    val sessions = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
    val active = try {
        sessions?.getActiveSessions(null).orEmpty()
    } catch (_: SecurityException) {
        emptyList()
    }
    val controller: MediaController? = active.firstOrNull {
        it.playbackState?.state == PlaybackState.STATE_PLAYING
    } ?: active.firstOrNull()
    val metadata = controller?.metadata
    val state = controller?.playbackState
    val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() } ?: "Музыка"
    val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
        ?.takeIf { it.isNotBlank() }
        ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)
        ?: ""
    val duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
    val label = controller?.packageName?.let { pkg ->
        runCatching {
            context.packageManager.getApplicationLabel(
                context.packageManager.getApplicationInfo(pkg, 0)
            ).toString()
        }.getOrDefault(pkg)
    } ?: ""
    val art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
    return PlayerSnapshot(
        title = title,
        artist = artist,
        appLabel = label,
        art = art,
        playing = state?.state == PlaybackState.STATE_PLAYING || audio?.isMusicActive == true,
        position = state?.position ?: 0L,
        duration = duration
    )
}

private fun mediaKey(context: Context, code: Int) {
    val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
    audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
    audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
}

@Composable
fun MusicCard(
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tokens = LocalLauncherTokens.current
    val player by produceState(initialValue = PlayerSnapshot("", "", "", null, false, 0L, 0L), key1 = context) {
        while (true) {
            value = readPlayer(context)
            delay(1_500L)
        }
    }

    WidgetSurface(modifier = modifier.clickable(onClick = onClick), compact = compact) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(if (compact) 44.dp else 58.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2C2C2E))
                ) {
                    val art = player.art
                    if (art != null) {
                        Image(
                            bitmap = art.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = Color(0xFFFF375F),
                            modifier = Modifier.size(if (compact) 24.dp else 30.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    WidgetTitle("Сейчас играет", Color(0xFFFF375F))
                    Text(
                        text = player.title,
                        color = tokens.textPrimary,
                        fontSize = if (compact) 15.sp else 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    WidgetCaption(
                        listOf(player.appLabel, player.artist)
                            .filter { it.isNotEmpty() }
                            .joinToString(" · ")
                            .ifEmpty { "Откройте плеер" }
                    )
                }
            }
            if (!compact && player.duration > 0L) {
                val progress = (player.position.toFloat() / player.duration).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(3.dp)
                            .background(Color(0xFFFF375F))
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlayerButton(Icons.Rounded.SkipPrevious, "Предыдущий") {
                    mediaKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                }
                PlayerButton(
                    if (player.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    "Воспроизведение"
                ) { mediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) }
                PlayerButton(Icons.Rounded.SkipNext, "Следующий") {
                    mediaKey(context, KeyEvent.KEYCODE_MEDIA_NEXT)
                }
            }
        }
    }
}

@Composable
private fun PlayerButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}

// ---------------------------------------------------------------- Календарь

@Composable
fun CalendarCard(
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLauncherTokens.current
    val today = LocalDate.now()
    val monthTitle = today.format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru")))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString() }

    WidgetSurface(modifier = modifier.clickable(onClick = onClick), compact = compact) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.DateRange,
                    contentDescription = null,
                    tint = Color(0xFFFF9F0A),
                    modifier = Modifier.size(if (compact) 24.dp else 30.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    WidgetTitle("Календарь", Color(0xFFFF9F0A))
                    WidgetCaption(monthTitle)
                }
            }
            if (compact) {
                Text(
                    text = today.dayOfMonth.toString(),
                    color = tokens.textPrimary,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Light
                )
                WidgetCaption(
                    today.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale("ru"))
                )
            } else {
                val firstOffset = today.withDayOfMonth(1).dayOfWeek.value - DayOfWeek.MONDAY.value
                val daysInMonth = today.lengthOfMonth()
                val cells = firstOffset + daysInMonth
                val rows = (cells + 6) / 7
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС").forEach { day ->
                            Text(
                                text = day,
                                color = tokens.textSecondary,
                                fontSize = 10.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    for (row in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (column in 0 until 7) {
                                val day = row * 7 + column - firstOffset + 1
                                val inMonth = day in 1..daysInMonth
                                val isToday = day == today.dayOfMonth
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(1.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isToday) Color(0xFFFF9F0A) else Color.Transparent
                                        )
                                ) {
                                    Text(
                                        text = if (inMonth) day.toString() else "",
                                        color = if (isToday) Color.Black else tokens.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
            WidgetCaption("Нажмите, чтобы открыть календарь")
        }
    }
}

// ---------------------------------------------------------------- Часы

@Composable
fun ClockCard(
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalLauncherTokens.current
    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1_000L)
        }
    }
    val time = now.format(DateTimeFormatter.ofPattern("HH:mm"))
    val date = now.format(DateTimeFormatter.ofPattern("d MMMM", Locale("ru")))

    WidgetSurface(modifier = modifier.clickable(onClick = onClick), compact = compact) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            WidgetTitle("Часы", Color(0xFFBF5AF2))
            AnalogClock(now = now, diameter = if (compact) 66.dp else 92.dp)
            Text(
                text = time,
                color = tokens.textPrimary,
                fontSize = if (compact) 20.sp else 26.sp,
                fontWeight = FontWeight.Light
            )
            WidgetCaption(date)
        }
    }
}

@Composable
private fun AnalogClock(now: LocalDateTime, diameter: androidx.compose.ui.unit.Dp) {
    val face = Color.White.copy(alpha = 0.10f)
    val tick = Color.White.copy(alpha = 0.35f)
    val hourHand = Color.White
    val minuteHand = Color(0xFFBF5AF2)
    Canvas(modifier = Modifier.size(diameter)) {
        val radius = minOf(size.width, size.height) / 2f
        val center = Offset(radius, radius)
        drawCircle(color = face, radius = radius, center = center)
        drawCircle(color = tick, radius = radius * 0.92f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        for (index in 0 until 12) {
            val angle = Math.toRadians(index * 30.0 - 90.0).toFloat()
            val inner = radius * 0.78f
            val outer = radius * 0.88f
            drawLine(
                color = tick,
                start = center + Offset(cos(angle) * inner, sin(angle) * inner),
                end = center + Offset(cos(angle) * outer, sin(angle) * outer),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        val hourAngle = Math.toRadians((now.hour % 12) * 30.0 + now.minute * 0.5 - 90.0).toFloat()
        val minuteAngle = Math.toRadians(now.minute * 6.0 - 90.0).toFloat()
        drawLine(
            color = hourHand,
            start = center,
            end = center + Offset(cos(hourAngle) * radius * 0.46f, sin(hourAngle) * radius * 0.46f),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = minuteHand,
            start = center,
            end = center + Offset(cos(minuteAngle) * radius * 0.68f, sin(minuteAngle) * radius * 0.68f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(color = Color.White, radius = 3.dp.toPx(), center = center)
    }
}

// ---------------------------------------------------------------- Батарея

@Composable
fun BatteryCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val tokens = LocalLauncherTokens.current
    val battery by produceState(initialValue = 0 to false, key1 = context) {
        while (true) {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
            val plugged = (intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
            value = if (level >= 0 && scale > 0) (level * 100 / scale).coerceIn(0, 100) to plugged else 0 to false
            delay(10_000L)
        }
    }
    val percent = battery.first
    val charging = battery.second
    val color = if (charging || percent > 50) Color(0xFF30D158) else Color(0xFFFF9F0A)

    WidgetSurface(modifier = modifier.clickable(onClick = onClick), compact = true) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.BatteryFull,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                WidgetTitle("Батарея", color)
            }
            Text(
                text = "$percent%",
                color = tokens.textPrimary,
                fontSize = 38.sp,
                fontWeight = FontWeight.Light
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percent / 100f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
            WidgetCaption(if (charging) "Заряжается" else "От батареи")
        }
    }
}

// ---------------------------------------------------------------- Заметки

@Composable
fun NotesCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = LocalLauncherTokens.current
    val notes = listOf("Идеи на сегодня", "Позвонить маме", "Купить продукты", "Записать мысль")
    WidgetSurface(modifier = modifier.clickable(onClick = onClick), compact = true) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Description,
                    contentDescription = null,
                    tint = Color(0xFFFFD60A),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                WidgetTitle("Заметки", Color(0xFFFFD60A))
            }
            notes.take(3).forEach { line ->
                Text(
                    text = "• $line",
                    color = tokens.textPrimary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            WidgetCaption("Нажмите, чтобы открыть заметки")
        }
    }
}

// ---------------------------------------------------------------- Фото

@Composable
fun PhotosCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tokens = LocalLauncherTokens.current
    val today = LocalDate.now()
    val date = today.format(DateTimeFormatter.ofPattern("d MMMM", Locale("ru")))
    val gradients = listOf(
        Color(0xFF0A84FF) to Color(0xFF5E5CE6),
        Color(0xFFFF9F0A) to Color(0xFFFF375F),
        Color(0xFF30D158) to Color(0xFF64D2FF),
        Color(0xFFBF5AF2) to Color(0xFFFF375F)
    )
    WidgetSurface(modifier = modifier.clickable(onClick = onClick), compact = false) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            WidgetTitle("Воспоминания", Color(0xFFFF9F0A))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                gradients.forEach { (start, end) ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(start, end)))
                    )
                }
            }
            WidgetCaption("Этот день · $date")
        }
    }
}

// ---------------------------------------------------------------- Умный стек

private val SMART_STACK_TYPES = listOf(
    WidgetType.WEATHER,
    WidgetType.CALENDAR,
    WidgetType.BATTERY,
    WidgetType.CLOCK
)

@Composable
fun SmartStackCard(
    size: WidgetSizeClass,
    weather: WidgetWeatherInfo,
    onOpenWidget: (WidgetType) -> Unit,
    modifier: Modifier = Modifier
) {
    var active by remember { mutableIntStateOf(0) }
    val current = SMART_STACK_TYPES[active]
    val compact = size == WidgetSizeClass.SMALL

    Box(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    active = (active + 1) % SMART_STACK_TYPES.size
                },
                onLongPress = { onOpenWidget(current) }
            )
        }
    ) {
        when (current) {
            WidgetType.WEATHER -> WeatherCard(weather, compact, onClick = {}, modifier = Modifier.fillMaxSize())
            WidgetType.CALENDAR -> CalendarCard(compact, onClick = {}, modifier = Modifier.fillMaxSize())
            WidgetType.BATTERY -> BatteryCard(onClick = {}, modifier = Modifier.fillMaxSize())
            WidgetType.CLOCK -> ClockCard(compact, onClick = {}, modifier = Modifier.fillMaxSize())
            else -> Unit
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
        ) {
            repeat(SMART_STACK_TYPES.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == active) 10.dp else 6.dp, 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == active) Color.White else Color.White.copy(alpha = 0.35f)
                        )
                )
            }
        }
    }
}
