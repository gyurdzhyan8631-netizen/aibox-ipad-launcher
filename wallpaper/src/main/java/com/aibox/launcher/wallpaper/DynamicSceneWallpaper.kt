package com.aibox.launcher.wallpaper

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import com.aibox.launcher.core.model.WeatherCondition
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Три набора реалистичных обоев первого рабочего стола. */
enum class DynamicScenePack(val id: String, val title: String, val subtitle: String) {
    MOUNTAINS(
        "mountains",
        "Горы и храм",
        "Горный хребет, туман в долине, река, храм и человек на скале"
    ),
    CITY(
        "city",
        "Город",
        "Городская панорама, башня с часами, улица и огни окон"
    ),
    FOREST(
        "forest",
        "Лес",
        "Лесистые холмы, озеро, изба, олени и свет в кронах"
    );

    companion object {
        fun fromId(id: String?): DynamicScenePack = entries.firstOrNull { it.id == id } ?: MOUNTAINS
    }
}

/** Шесть состояний, одинаковых для каждого набора. */
enum class DynamicScene(val id: String, val title: String, val subtitle: String) {
    DAWN("dawn", "Рассвет", "Тёплое небо, длинные тени, туман"),
    DAY("day", "День", "Ясное небо, мягкий свет, птицы"),
    NIGHT("night", "Ночь", "Луна, звёзды, огни окон и фонарей"),
    RAIN("rain", "Дождь", "Тяжёлые облака, мокрые поверхности, блики"),
    SNOW("snow", "Снег", "Снегопад, белые склоны, замёрзшая вода"),
    WIND("wind", "Ветер", "Гнущиеся деревья, листья, быстрые облака");

    companion object {
        fun fromId(id: String?): DynamicScene = entries.firstOrNull { it.id == id } ?: DAY
    }
}

/** Состояние по погоде Open-Meteo и реальному времени восхода/заката. */
fun dynamicSceneFor(
    weather: WeatherCondition,
    nowMinutes: Int,
    sunriseMin: Int,
    sunsetMin: Int
): DynamicScene = when (weather) {
    WeatherCondition.RAIN,
    WeatherCondition.HEAVY_RAIN,
    WeatherCondition.THUNDERSTORM -> DynamicScene.RAIN

    WeatherCondition.SNOW -> DynamicScene.SNOW

    WeatherCondition.WIND,
    WeatherCondition.FOG,
    WeatherCondition.OVERCAST,
    WeatherCondition.CLOUDY -> DynamicScene.WIND

    WeatherCondition.CLEAR -> {
        val rise = if (sunsetMin > sunriseMin) sunriseMin else 390
        val set = if (sunsetMin > sunriseMin) sunsetMin else 1110
        when {
            abs(nowMinutes - rise) <= 50 || abs(nowMinutes - set) <= 50 -> DynamicScene.DAWN
            nowMinutes in rise..set -> DynamicScene.DAY
            else -> DynamicScene.NIGHT
        }
    }
}

private fun frac(x: Float): Float = x - floor(x)
private fun tint(color: Color, target: Color, amount: Float): Color = lerp(color, target, amount.coerceIn(0f, 1f))

private data class ScenePalette(
    val sky: List<Color>,
    val sun: Color,
    val glow: Color,
    val far: Color,
    val mid: Color,
    val near: Color,
    val tree: Color,
    val water: Color
)

private fun skyFor(scene: DynamicScene): List<Color> = when (scene) {
    DynamicScene.DAY -> listOf(Color(0xFF9FC6DC), Color(0xFFD5E4D4), Color(0xFFF6E7AE))
    DynamicScene.DAWN -> listOf(Color(0xFF5D517E), Color(0xFFBE7589), Color(0xFFF2AF80), Color(0xFFFFDFAC))
    DynamicScene.NIGHT -> listOf(Color(0xFF04081B), Color(0xFF0D1735), Color(0xFF1B2A56), Color(0xFF35406A))
    DynamicScene.RAIN -> listOf(Color(0xFF39474F), Color(0xFF566970), Color(0xFF849795))
    DynamicScene.SNOW -> listOf(Color(0xFF87AACD), Color(0xFFBBD4E7), Color(0xFFE8F2F9))
    DynamicScene.WIND -> listOf(Color(0xFF86958B), Color(0xFFB4B9A5), Color(0xFFDAC9A6))
}

private fun lightsFor(scene: DynamicScene): Pair<Color, Color> = when (scene) {
    DynamicScene.DAY -> Color(0xFFFFF2C4) to Color(0xFFFFE9A8)
    DynamicScene.DAWN -> Color(0xFFFFD39A) to Color(0xFFFFB472)
    DynamicScene.NIGHT -> Color(0xFFEDE7C8) to Color(0xFFFFC978)
    DynamicScene.RAIN -> Color(0xFFBCCBC8) to Color(0x9AA9A6)
    DynamicScene.SNOW -> Color(0xFFFFF8E6) to Color(0xFFC9E2F2)
    DynamicScene.WIND -> Color(0xFFF0DFB0) to Color(0xFFD9C38F)
}

private fun landFor(pack: DynamicScenePack, scene: DynamicScene): ScenePalette {
    val base = when (pack) {
        DynamicScenePack.MOUNTAINS -> ScenePalette(
            skyFor(scene), Color(0xFFFFF2C4), Color(0xFFFFE9A8),
            Color(0xFF8FA68F), Color(0xFF5C8163), Color(0xFF2B4B3A),
            Color(0xFF244535), Color(0xFF6BB8C4)
        )
        DynamicScenePack.CITY -> ScenePalette(
            skyFor(scene), Color(0xFFFFF2C4), Color(0xFFFFE9A8),
            Color(0xFF8D9EAC), Color(0xFF546E84), Color(0xFF24384B),
            Color(0xFF1B2E40), Color(0xFF4A7187)
        )
        DynamicScenePack.FOREST -> ScenePalette(
            skyFor(scene), Color(0xFFFFF2C4), Color(0xFFFFE9A8),
            Color(0xFF6D9875), Color(0xFF3C6A4E), Color(0xFF1B3D2B),
            Color(0xFF1D4A32), Color(0xFF5A9E93)
        )
    }
    val light = lightsFor(scene)
    val (heat, cool) = when (scene) {
        DynamicScene.DAWN -> Color(0xFF5B4A65) to Color(0xFFFFC08A)
        DynamicScene.NIGHT -> Color(0xFF0B1430) to Color(0xFF33406E)
        DynamicScene.RAIN -> Color(0xFF3C5156) to Color(0xFF7E9A99)
        DynamicScene.SNOW -> Color(0xFFD9EAF5) to Color(0xFFFFFFFF)
        DynamicScene.WIND -> Color(0xFF737E6C) to Color(0xFFC9B98F)
        DynamicScene.DAY -> Color(0xFF7C957B) to Color(0xFFF0D9A0)
    }
    val strength = when (scene) {
        DynamicScene.NIGHT -> 0.72f
        DynamicScene.SNOW -> 0.74f
        DynamicScene.RAIN -> 0.52f
        DynamicScene.DAWN -> 0.40f
        DynamicScene.WIND -> 0.34f
        DynamicScene.DAY -> 0f
    }
    if (strength == 0f) return base.copy(sun = light.first, glow = light.second)
    return base.copy(
        sun = light.first,
        glow = light.second,
        far = tint(base.far, if (scene == DynamicScene.NIGHT) heat else cool, strength),
        mid = tint(base.mid, heat, strength * 0.9f),
        near = tint(base.near, heat, strength * 0.85f),
        tree = tint(base.tree, heat, strength * 0.9f),
        water = tint(base.water, cool, strength * 0.7f)
    )
}

/**
 * Реалистичная векторная сцена: мягкие градиенты неба, атмосферная дымка, объёмные горы,
 * отражения в воде, текстура и виньетка. Рисуется Compose Canvas — без bitmap-ассетов,
 * поэтому работает офлайн и масштабируется под любой экран.
 */
@Composable
fun DynamicSceneWallpaper(
    scene: DynamicScene,
    pack: DynamicScenePack = DynamicScenePack.MOUNTAINS,
    nowMinutes: Int,
    sunriseMin: Int,
    sunsetMin: Int,
    modifier: Modifier = Modifier
) {
    var seconds by remember { mutableFloatStateOf(0f) }
    val tickMs = when (scene) {
        DynamicScene.RAIN, DynamicScene.SNOW -> 40L
        DynamicScene.WIND -> 100L
        else -> 260L
    }

    LaunchedEffect(pack, scene, tickMs) {
        while (true) {
            val nanos = withFrameNanos { it }
            seconds = (nanos % 60_000_000_000L) / 1_000_000_000f
            delay(tickMs)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val pal = landFor(pack, scene)
        drawRealisticScene(pack, scene, pal, seconds, nowMinutes, sunriseMin, sunsetMin)
    }
}

private fun DrawScope.drawRealisticScene(
    pack: DynamicScenePack,
    scene: DynamicScene,
    pal: ScenePalette,
    t: Float,
    nowMinutes: Int,
    sunriseMin: Int,
    sunsetMin: Int
) {
    val w = size.width
    val h = size.height
    if (w <= 0f || h <= 0f) return

    val rise = if (sunsetMin > sunriseMin) sunriseMin else 390
    val set = if (sunsetMin > sunriseMin) sunsetMin else 1110
    val progress = ((nowMinutes - rise).toFloat() / (set - rise).toFloat()).coerceIn(0f, 1f)

    drawRect(
        brush = Brush.verticalGradient(pal.sky, startY = 0f, endY = h * 0.92f),
        size = Size(w, h)
    )
    drawSunAndMoon(scene, pal, t, progress, w, h)
    drawClouds(scene, pal, t, w, h)

    when (pack) {
        DynamicScenePack.MOUNTAINS -> drawMountainScene(w, h, pal, scene, t)
        DynamicScenePack.CITY -> drawCityScene(w, h, pal, scene, t)
        DynamicScenePack.FOREST -> drawForestScene(w, h, pal, scene, t)
    }

    drawWeather(scene, pal, t, w, h)
    drawGrainAndVignette(scene, w, h)
}

private fun DrawScope.drawSunAndMoon(
    scene: DynamicScene,
    pal: ScenePalette,
    t: Float,
    progress: Float,
    w: Float,
    h: Float
) {
    val unit = min(w, h)
    if (scene == DynamicScene.NIGHT) {
        for (i in 0 until 110) {
            val sx = frac(i * 0.618034f + 0.11f) * w
            val sy = frac(i * 0.754878f + 0.07f) * h * 0.52f
            val twinkle = 0.55f + 0.45f * sin(t * 1.1f + i * 1.7f)
            drawCircle(
                Color.White.copy(alpha = (0.25f + 0.75f * frac(i * 0.371f)) * twinkle),
                (0.7f + frac(i * 0.219f) * 1.4f),
                Offset(sx, sy)
            )
        }
        val moon = Offset(w * 0.76f, h * 0.19f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color(0x55C9D8FF), Color(0x00C9D8FF)),
                center = moon,
                radius = unit * 0.18f
            ),
            radius = unit * 0.18f,
            center = moon
        )
        drawCircle(Color(0xFFF2F0DA), unit * 0.035f, moon)
        drawCircle(Color(0xFF101B3B), unit * 0.030f, Offset(moon.x + unit * 0.014f, moon.y - unit * 0.008f))
        return
    }

    val sunX = when (scene) {
        DynamicScene.DAWN -> w * (0.10f + 0.16f * (1f - progress))
        DynamicScene.DAY -> w * (0.16f + 0.68f * progress)
        else -> w * 0.56f
    }
    val sunY = when (scene) {
        DynamicScene.DAWN -> h * 0.50f
        DynamicScene.DAY -> h * (0.36f - 0.20f * sin(progress * 3.14159f))
        else -> h * 0.30f
    }
    val r = unit * if (scene == DynamicScene.DAWN) 0.046f else 0.036f
    val sun = Offset(sunX, sunY)

    drawCircle(
        brush = Brush.radialGradient(
            listOf(pal.glow.copy(alpha = 0.42f), pal.glow.copy(alpha = 0.12f), Color(0x00000000)),
            center = sun,
            radius = r * 8f
        ),
        radius = r * 8f,
        center = sun
    )
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFFFFFF), pal.sun),
            center = sun,
            radius = r
        ),
        radius = r,
        center = sun
    )

    // Тёплый свет у горизонта — то, что делает изображение похожим на фотографию.
    drawRect(
        brush = Brush.radialGradient(
            listOf(pal.glow.copy(alpha = 0.24f), Color(0x00000000)),
            center = Offset(w * 0.5f, h * 0.72f),
            radius = w * 0.75f
        ),
        size = Size(w, h)
    )
}

private fun DrawScope.drawClouds(scene: DynamicScene, pal: ScenePalette, t: Float, w: Float, h: Float) {
    val speed = if (scene == DynamicScene.WIND) 0.045f else 0.008f
    val alpha = when (scene) {
        DynamicScene.NIGHT -> 0.10f
        DynamicScene.RAIN -> 0.26f
        DynamicScene.DAY -> 0.34f
        DynamicScene.DAWN -> 0.30f
        DynamicScene.SNOW -> 0.34f
        DynamicScene.WIND -> 0.22f
    }
    val cloudColor = if (scene == DynamicScene.RAIN || scene == DynamicScene.NIGHT) {
        Color(0xFFC6D0D6)
    } else {
        Color(0xFFFFFFFF)
    }
    repeat(9) { i ->
        val cx = frac(i * 0.2731f + t * speed) * (w * 1.5f) - w * 0.25f
        val cy = h * (0.08f + frac(i * 0.417f) * 0.34f)
        val cw = w * (0.10f + frac(i * 0.711f) * 0.16f)
        val ch = h * (0.025f + frac(i * 0.193f) * 0.035f)
        drawOval(
            brush = Brush.radialGradient(
                listOf(cloudColor.copy(alpha = alpha), Color(0x00FFFFFF)),
                center = Offset(cx, cy),
                radius = cw
            ),
            topLeft = Offset(cx - cw, cy - ch),
            size = Size(cw * 2f, ch * 2f)
        )
    }
}

// ---------------------------------------------------------------- Горы

private fun DrawScope.drawMountainScene(w: Float, h: Float, pal: ScenePalette, scene: DynamicScene, t: Float) {
    val far = ridge(w, h, listOf(0f to .58f, .11f to .39f, .24f to .54f, .43f to .25f, .60f to .48f, .76f to .33f, 1f to .51f))
    val mid = ridge(w, h, listOf(0f to .70f, .16f to .47f, .33f to .64f, .51f to .39f, .69f to .60f, .86f to .44f, 1f to .61f))
    val near = ridge(w, h, listOf(0f to .84f, .18f to .68f, .38f to .82f, .58f to .65f, .78f to .79f, 1f to .72f))

    shadedRidge(far, w, h, pal.far, pal, scene, 0.31f)
    haze(h, w, 0.48f, 0.66f, pal.far)
    pineLine(w, h, .61f, 15, h * .075f, tint(pal.tree, pal.far, 0.45f), 1.7f, t, scene)
    shadedRidge(mid, w, h, pal.mid, pal, scene, 0.26f)
    haze(h, w, 0.60f, 0.74f, pal.mid)

    water(w, h, pal.water, scene, pal)
    pineLine(w, h, .78f, 19, h * .105f, pal.tree, 4.3f, t, scene)
    temple(w, h, pal, scene)
    manOnCliff(w, h, pal.tree)
    shadedRidge(near, w, h, pal.near, pal, scene, 0.20f)
    pineLine(w, h, .93f, 17, h * .125f, pal.tree, 8.1f, t, scene)
}

private fun ridge(w: Float, h: Float, points: List<Pair<Float, Float>>): Path = Path().apply {
    moveTo(0f, h)
    lineTo(0f, h * points.first().second)
    points.forEach { (x, y) -> lineTo(w * x, h * y) }
    lineTo(w, h)
    close()
}

private fun DrawScope.shadedRidge(
    path: Path,
    w: Float,
    h: Float,
    color: Color,
    pal: ScenePalette,
    scene: DynamicScene,
    hazeAmount: Float
) {
    drawPath(
        path,
        Brush.verticalGradient(
            listOf(tint(color, pal.glow, hazeAmount), color, tint(color, Color(0xFF0A1520), 0.18f)),
            startY = h * 0.20f,
            endY = h
        )
    )
    if (scene == DynamicScene.SNOW) {
        drawPath(path, Color(0xB8FFFFFF).copy(alpha = 0.20f))
    }
}

private fun DrawScope.haze(h: Float, w: Float, from: Float, to: Float, color: Color) {
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color(0x00000000), color.copy(alpha = 0.22f), Color(0x00000000)),
            startY = h * from,
            endY = h * to
        ),
        topLeft = Offset(0f, h * from),
        size = Size(w, h * (to - from))
    )
}

private fun DrawScope.water(w: Float, h: Float, color: Color, scene: DynamicScene, pal: ScenePalette) {
    val river = Path().apply {
        moveTo(w * .43f, h * .56f)
        cubicTo(w * .58f, h * .68f, w * .26f, h * .79f, w * .44f, h)
        lineTo(w * .75f, h)
        cubicTo(w * .62f, h * .83f, w * .80f, h * .67f, w * .54f, h * .56f)
        close()
    }
    drawPath(
        river,
        Brush.verticalGradient(
            listOf(tint(color, pal.sky.last(), 0.32f), color, tint(color, Color(0xFF0B222C), 0.30f)),
            startY = h * .56f,
            endY = h
        )
    )
    clipPath(river) {
        for (i in 0..18) {
            val y = h * (0.60f + i * 0.022f)
            val half = w * (0.04f + i * 0.012f)
            drawLine(
                Color.White.copy(alpha = if (scene == DynamicScene.SNOW) 0.13f else 0.10f),
                Offset(w * 0.50f - half, y),
                Offset(w * 0.50f + half * (0.5f + frac(i * 0.37f)), y),
                strokeWidth = 1.1f
            )
        }
        if (scene == DynamicScene.NIGHT || scene == DynamicScene.DAWN || scene == DynamicScene.RAIN) {
            for (i in 0..7) {
                val y = h * (0.74f + i * 0.03f)
                drawLine(
                    pal.glow.copy(alpha = 0.18f),
                    Offset(w * 0.49f - w * 0.012f, y),
                    Offset(w * 0.49f + w * 0.012f, y),
                    strokeWidth = 2.2f
                )
            }
        }
    }
    if (scene == DynamicScene.SNOW) drawPath(river, Color.White.copy(alpha = 0.26f))
}

private fun DrawScope.temple(w: Float, h: Float, pal: ScenePalette, scene: DynamicScene) {
    val u = min(w, h) * 0.052f
    val x = w * 0.79f
    val y = h * 0.50f
    val glow = scene == DynamicScene.NIGHT || scene == DynamicScene.RAIN || scene == DynamicScene.DAWN

    val stairs = Path().apply {
        moveTo(x - u * 1.5f, y + u * .9f)
        lineTo(x + u * 1.5f, y + u * .9f)
        lineTo(x + u * 1.15f, y + u * .45f)
        lineTo(x - u * 1.15f, y + u * .45f)
        close()
    }
    drawPath(stairs, pal.near)
    drawRect(
        brush = Brush.verticalGradient(listOf(tint(pal.mid, pal.glow, .25f), pal.mid)),
        topLeft = Offset(x - u, y - u * .75f),
        size = Size(u * 2f, u * 1.2f)
    )
    val roof = Path().apply {
        moveTo(x - u * 1.35f, y - u * .75f)
        lineTo(x, y - u * 1.45f)
        lineTo(x + u * 1.35f, y - u * .75f)
        close()
    }
    drawPath(roof, pal.near)
    for (i in -1..1) {
        drawLine(pal.near, Offset(x + i * u * .65f, y - u * .68f), Offset(x + i * u * .65f, y + u * .40f), u * .12f)
        if (glow) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(pal.glow.copy(alpha = .95f), Color(0x00FFFFFF)),
                    center = Offset(x + i * u * .65f, y - u * .12f),
                    radius = u * .28f
                ),
                radius = u * .28f,
                center = Offset(x + i * u * .65f, y - u * .12f)
            )
        }
    }
}

private fun DrawScope.manOnCliff(w: Float, h: Float, color: Color) {
    val s = min(w, h) * 0.010f
    val x = w * 0.36f
    val y = h * 0.55f
    drawPath(
        Path().apply {
            moveTo(x - s * 2.6f, y + s * 1.2f)
            lineTo(x + s * 2.2f, y + s * 1.2f)
            lineTo(x + s * .7f, y - s * .2f)
            lineTo(x - s * 1.2f, y - s * .5f)
            close()
        },
        color
    )
    drawCircle(color, s * .42f, Offset(x, y - s * 1.5f))
    drawLine(color, Offset(x, y - s), Offset(x - s * .2f, y + s * .3f), s * .30f)
}

// ---------------------------------------------------------------- Город

private fun DrawScope.drawCityScene(w: Float, h: Float, pal: ScenePalette, scene: DynamicScene, t: Float) {
    skyline(w, h, .65f, 17, h * .19f, pal.far, pal, 1.1f, false, scene)
    cityStreet(w, h, pal, scene)
    skyline(w, h, .79f, 12, h * .31f, pal.mid, pal, 4.2f, true, scene)
    clockTower(w, h, pal, scene)
    skyline(w, h, .99f, 8, h * .21f, pal.near, pal, 7.7f, false, scene)
    streetLights(w, h, pal, scene, t)
}

private fun DrawScope.skyline(
    w: Float,
    h: Float,
    baseline: Float,
    count: Int,
    maxHeight: Float,
    color: Color,
    pal: ScenePalette,
    seed: Float,
    windows: Boolean,
    scene: DynamicScene
) {
    val step = w / count
    val unit = min(w, h)
    val glow = scene == DynamicScene.NIGHT || scene == DynamicScene.RAIN || scene == DynamicScene.DAWN
    for (i in 0 until count) {
        val bw = step * (0.60f + frac(sin(i * 13.7f + seed) * 43758.5f) * 0.40f)
        val bh = maxHeight * (0.33f + frac(sin(i * 7.31f + seed) * 24634.7f) * 1.0f)
        val x = i * step + (step - bw) * .5f
        val y = h * baseline - bh
        drawRect(
            brush = Brush.verticalGradient(
                listOf(tint(color, pal.glow, .22f), color, tint(color, Color(0xFF08131E), .22f)),
                startY = y,
                endY = h * baseline
            ),
            topLeft = Offset(x, y),
            size = Size(bw, bh)
        )
        if (frac(sin(i * 3.71f + seed) * 9871.3f) > .70f) {
            drawPath(
                Path().apply {
                    moveTo(x, y)
                    lineTo(x + bw * .5f, y - bh * .14f)
                    lineTo(x + bw, y)
                    close()
                },
                tint(color, Color(0xFF0B1722), .20f)
            )
        }
        if (windows) {
            val cols = max(1, (bw / (unit * .024f)).toInt())
            val rows = max(1, (bh / (unit * .032f)).toInt())
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    if (frac(sin(i * 31.7f + r * 7.3f + c * 3.1f + seed) * 43758.5f) <= .48f) continue
                    val wx = x + bw * (c + .5f) / cols - unit * .006f
                    val wy = y + bh * (r + .5f) / rows - unit * .009f
                    val a = if (glow) .85f else .20f
                    drawRect(
                        pal.glow.copy(alpha = a),
                        Offset(wx, wy),
                        Size(unit * .012f, unit * .018f)
                    )
                }
            }
        }
    }
}

private fun DrawScope.cityStreet(w: Float, h: Float, pal: ScenePalette, scene: DynamicScene) {
    val road = Path().apply {
        moveTo(w * .40f, h * .64f)
        cubicTo(w * .56f, h * .74f, w * .30f, h * .85f, w * .40f, h)
        lineTo(w * .77f, h)
        cubicTo(w * .64f, h * .84f, w * .85f, h * .74f, w * .58f, h * .64f)
        close()
    }
    drawPath(
        road,
        Brush.verticalGradient(
            listOf(tint(pal.water, pal.glow, .20f), pal.water, tint(pal.near, Color(0xFF050D16), .35f)),
            startY = h * .64f,
            endY = h
        )
    )
    clipPath(road) {
        repeat(16) { i ->
            val y = h * (0.70f + i * 0.019f)
            val x = w * (0.44f + frac(i * .37f) * .16f)
            drawLine(
                pal.glow.copy(alpha = if (scene == DynamicScene.SNOW) .10f else .20f),
                Offset(x - w * .035f, y),
                Offset(x + w * .035f, y),
                strokeWidth = 1.3f
            )
        }
    }
}

private fun DrawScope.clockTower(w: Float, h: Float, pal: ScenePalette, scene: DynamicScene) {
    val u = min(w, h)
    val tw = u * .075f
    val th = h * .35f
    val x = w * .72f
    val base = h * .79f
    val glow = scene == DynamicScene.NIGHT || scene == DynamicScene.RAIN || scene == DynamicScene.DAWN
    drawRect(
        brush = Brush.horizontalGradient(
            listOf(tint(pal.near, pal.glow, .18f), pal.near, tint(pal.near, Color(0xFF060E17), .30f)),
            startX = x - tw * .5f,
            endX = x + tw * .5f
        ),
        topLeft = Offset(x - tw * .5f, base - th),
        size = Size(tw, th)
    )
    drawPath(
        Path().apply {
            moveTo(x - tw * .68f, base - th)
            lineTo(x, base - th - u * .08f)
            lineTo(x + tw * .68f, base - th)
            close()
        },
        tint(pal.near, Color(0xFF0A1520), .25f)
    )
    val face = Offset(x, base - th * .80f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFFFFF0), pal.glow),
            center = face,
            radius = u * .028f
        ),
        radius = u * .028f,
        center = face
    )
    drawLine(pal.near, face, Offset(face.x, face.y - u * .018f), u * .003f)
    drawLine(pal.near, face, Offset(face.x + u * .014f, face.y), u * .003f)
    if (glow) {
        repeat(3) { i ->
            drawRect(
                pal.glow.copy(alpha = .75f),
                Offset(x - tw * .28f, base - th * (.52f - i * .13f)),
                Size(tw * .56f, u * .014f)
            )
        }
    }
}

private fun DrawScope.streetLights(w: Float, h: Float, pal: ScenePalette, scene: DynamicScene, t: Float) {
    val u = min(w, h)
    val glow = scene == DynamicScene.NIGHT || scene == DynamicScene.RAIN || scene == DynamicScene.DAWN
    repeat(5) { i ->
        val x = w * (0.12f + i * 0.19f)
        val y = h * (0.72f + i * 0.045f)
        drawLine(pal.near, Offset(x, y), Offset(x, y - h * .075f), u * .004f)
        if (glow) {
            val lamp = Offset(x, y - h * .075f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(pal.glow.copy(alpha = .8f), Color(0x00FFFFFF)),
                    center = lamp,
                    radius = u * .05f
                ),
                radius = u * .05f,
                center = lamp
            )
            drawLine(
                pal.glow.copy(alpha = .16f),
                Offset(x, y),
                Offset(x, y + h * .11f),
                u * .012f
            )
        }
    }
    val speed = if (scene == DynamicScene.NIGHT) .045f else .020f
    repeat(4) { i ->
        val cx = frac(i * .29f + t * speed) * w
        val cy = h * (.87f + frac(i * .17f) * .10f)
        drawRect(pal.near, Offset(cx, cy), Size(u * .035f, u * .012f))
        if (glow) drawCircle(Color(0xFFFFF2B0), u * .004f, Offset(cx + u * .035f, cy + u * .006f))
    }
}

// ---------------------------------------------------------------- Лес

private fun DrawScope.drawForestScene(w: Float, h: Float, pal: ScenePalette, scene: DynamicScene, t: Float) {
    val back = ridge(w, h, listOf(0f to .62f, .14f to .51f, .30f to .60f, .48f to .45f, .66f to .58f, .84f to .49f, 1f to .60f))
    val middle = ridge(w, h, listOf(0f to .74f, .18f to .63f, .36f to .72f, .55f to .59f, .74f to .70f, 1f to .65f))
    val front = ridge(w, h, listOf(0f to .90f, .20f to .82f, .42f to .90f, .62f to .80f, .82f to .88f, 1f to .84f))

    shadedRidge(back, w, h, pal.far, pal, scene, .34f)
    haze(h, w, .46f, .64f, pal.far)
    pineLine(w, h, .64f, 21, h * .085f, tint(pal.tree, pal.far, .50f), 2.1f, t, scene)
    shadedRidge(middle, w, h, pal.mid, pal, scene, .22f)
    forestLake(w, h, pal, scene)
    pineLine(w, h, .81f, 25, h * .12f, pal.tree, 5.5f, t, scene)
    cabin(w, h, pal, scene, t)
    broadleaf(w, h, .86f, pal, t, scene)
    deer(w, h, pal)
    shadedRidge(front, w, h, pal.near, pal, scene, .16f)
    pineLine(w, h, .98f, 18, h * .145f, pal.tree, 9.3f, t, scene)

    if (scene == DynamicScene.NIGHT) {
        repeat(22) { i ->
            val x = frac(i * .618034f) * w
            val y = h * (.60f + frac(i * .414f) * .30f) + sin(t * 1.4f + i) * h * .012f
            drawCircle(
                Color(0xFFFFE68A).copy(alpha = .25f + .55f * frac(sin(t + i) * 43758.5f)),
                1.6f + frac(i * .27f) * 1.6f,
                Offset(x, y)
            )
        }
    }
}

private fun DrawScope.forestLake(w: Float, h: Float, pal: ScenePalette, scene: DynamicScene) {
    val lake = Path().apply {
        moveTo(w * .15f, h * .77f)
        cubicTo(w * .30f, h * .73f, w * .66f, h * .73f, w * .85f, h * .77f)
        cubicTo(w * .72f, h * .91f, w * .30f, h * .91f, w * .15f, h * .77f)
        close()
    }
    drawPath(
        lake,
        Brush.verticalGradient(
            listOf(tint(pal.water, pal.sky.last(), .32f), pal.water, tint(pal.water, Color(0xFF09252A), .32f)),
            startY = h * .73f,
            endY = h * .91f
        )
    )
    clipPath(lake) {
        repeat(10) { i ->
            val y = h * (.77f + i * .014f)
            drawLine(
                Color.White.copy(alpha = .10f),
                Offset(w * (.24f + frac(i * .33f) * .10f), y),
                Offset(w * (.62f + frac(i * .21f) * .12f), y),
                strokeWidth = 1f
            )
        }
    }
    if (scene == DynamicScene.SNOW) drawPath(lake, Color.White.copy(alpha = .28f))
}

private fun DrawScope.cabin(w: Float, h: Float, pal: ScenePalette, scene: DynamicScene, t: Float) {
    val u = min(w, h)
    val x = w * .18f
    val y = h * .72f
    val cw = u * .11f
    val ch = u * .075f
    val warm = scene == DynamicScene.NIGHT || scene == DynamicScene.RAIN || scene == DynamicScene.DAWN
    drawRect(
        brush = Brush.verticalGradient(listOf(tint(pal.near, pal.glow, .16f), pal.near)),
        topLeft = Offset(x - cw * .5f, y - ch),
        size = Size(cw, ch)
    )
    drawPath(
        Path().apply {
            moveTo(x - cw * .62f, y - ch)
            lineTo(x, y - ch - u * .055f)
            lineTo(x + cw * .62f, y - ch)
            close()
        },
        tint(pal.near, Color(0xFF08151E), .28f)
    )
    if (warm) {
        val window = Offset(x - u * .012f, y - ch * .72f)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(pal.glow.copy(alpha = .65f), Color(0x00FFFFFF)),
                center = window,
                radius = u * .07f
            ),
            radius = u * .07f,
            center = window
        )
        drawRect(pal.glow.copy(alpha = .95f), Offset(window.x - u * .006f, window.y - u * .006f), Size(u * .012f, u * .012f))
    }
    repeat(3) { i ->
        val sy = y - ch - u * (.06f + i * .045f)
        val sx = x + sin(t * .6f + i) * u * .012f + i * u * .006f
        drawCircle(Color.White.copy(alpha = if (warm) .12f else .08f), u * (.018f + i * .008f), Offset(sx, sy))
    }
}

private fun DrawScope.broadleaf(w: Float, h: Float, baseline: Float, pal: ScenePalette, t: Float, scene: DynamicScene) {
    val u = min(w, h)
    repeat(5) { i ->
        val x = w * (.30f + i * .13f)
        val y = h * baseline
        val th = u * (.10f + frac(i * .37f) * .05f)
        val bend = if (scene == DynamicScene.WIND) sin(t * 1.6f + i) * th * .09f else 0f
        drawLine(pal.near, Offset(x, y), Offset(x + bend, y - th), u * .006f)
        drawCircle(tint(pal.tree, pal.glow, .10f), th * .42f, Offset(x + bend - th * .18f, y - th * 1.05f))
        drawCircle(pal.tree, th * .38f, Offset(x + bend + th * .20f, y - th))
        drawCircle(tint(pal.tree, pal.glow, .14f), th * .44f, Offset(x + bend, y - th * 1.28f))
    }
}

private fun DrawScope.deer(w: Float, h: Float, pal: ScenePalette) {
    val u = min(w, h)
    val x = w * .66f
    val y = h * .78f
    val s = u * .032f
    drawOval(pal.near, Offset(x - s, y - s * .55f), Size(s * 2f, s))
    drawLine(pal.near, Offset(x - s * .55f, y), Offset(x - s * .65f, y + s * .65f), s * .13f)
    drawLine(pal.near, Offset(x + s * .55f, y), Offset(x + s * .45f, y + s * .65f), s * .13f)
    drawLine(pal.near, Offset(x + s * .85f, y - s * .35f), Offset(x + s * 1.05f, y - s * 1.05f), s * .15f)
    drawCircle(pal.near, s * .28f, Offset(x + s * 1.12f, y - s * 1.15f))
    drawLine(pal.near, Offset(x + s * 1.05f, y - s * 1.35f), Offset(x + s * .92f, y - s * 1.75f), s * .08f)
    drawLine(pal.near, Offset(x + s * 1.18f, y - s * 1.35f), Offset(x + s * 1.32f, y - s * 1.75f), s * .08f)
}

// ---------------------------------------------------------------- Деревья и погода

private fun DrawScope.pineLine(
    w: Float,
    h: Float,
    baseline: Float,
    count: Int,
    baseHeight: Float,
    color: Color,
    seed: Float,
    t: Float,
    scene: DynamicScene
) {
    repeat(count) { i ->
        val x = (i + .5f) / count * w + (frac(sin(i * 17.13f + seed) * 43758.5f) - .5f) * w * .04f
        val y = h * baseline + (frac(sin(i * 9.71f + seed) * 12345.6f) - .5f) * h * .025f
        val th = baseHeight * (.75f + frac(sin(i * 4.17f + seed) * 9876.5f) * .5f)
        pine(x, y, th, color, scene == DynamicScene.WIND, t, i)
    }
}

private fun DrawScope.pine(
    x: Float,
    baseY: Float,
    height: Float,
    color: Color,
    wind: Boolean,
    t: Float,
    seed: Int
) {
    val bend = if (wind) (sin(t * 1.8f + seed) * .06f + .08f) * height else 0f
    val half = height * .32f
    drawPath(
        Path().apply {
            moveTo(x - height * .045f, baseY)
            lineTo(x + height * .045f, baseY)
            lineTo(x + bend * .4f + height * .025f, baseY - height * .62f)
            lineTo(x + bend * .4f - height * .025f, baseY - height * .62f)
            close()
        },
        tint(color, Color(0xFF0A1A14), .25f)
    )
    val crown = Path().apply {
        moveTo(x + bend, baseY - height)
        lineTo(x - half, baseY - height * .34f)
        lineTo(x + half * .75f, baseY - height * .40f)
        lineTo(x - half * 1.08f, baseY - height * .05f)
        lineTo(x + half, baseY - height * .10f)
        close()
    }
    drawPath(
        crown,
        Brush.linearGradient(
            listOf(tint(color, Color(0xFFFFFFFF), .16f), color, tint(color, Color(0xFF06140F), .30f)),
            start = Offset(x - half, baseY - height),
            end = Offset(x + half, baseY)
        )
    )
}

private fun DrawScope.drawWeather(scene: DynamicScene, pal: ScenePalette, t: Float, w: Float, h: Float) {
    when (scene) {
        DynamicScene.DAWN -> {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color(0x00000000), Color.White.copy(alpha = .18f), Color(0x00000000)),
                    startY = h * .55f,
                    endY = h * .86f
                ),
                size = Size(w, h)
            )
            birds(w, h, Color(0xFF5A4650), t)
        }

        DynamicScene.DAY -> birds(w, h, Color(0xFF3E5B48), t)

        DynamicScene.RAIN -> {
            repeat(170) { i ->
                val x = frac(i * .7548776f + sin(t + i) * .002f) * w
                val y = frac(i * .3183099f + t * .55f) * h
                drawLine(
                    Color.White.copy(alpha = .16f + frac(i * .17f) * .26f),
                    Offset(x, y),
                    Offset(x - w * .006f, y + h * .034f),
                    strokeWidth = 1.4f
                )
            }
            haze(h, w, .60f, .90f, Color(0xFF9BB0AE))
        }

        DynamicScene.SNOW -> {
            repeat(130) { i ->
                val drift = sin(t * .7f + i) * w * .012f
                val x = frac(i * .618034f) * w + drift
                val y = frac(i * .4142135f + t * .055f) * h
                val r = 1.4f + frac(i * .19f) * 2.8f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = .90f), Color(0x00FFFFFF)),
                        center = Offset(x, y),
                        radius = r * 2.2f
                    ),
                    radius = r * 2.2f,
                    center = Offset(x, y)
                )
            }
            haze(h, w, .68f, .92f, Color(0xFFE8F2F9))
        }

        DynamicScene.WIND -> {
            repeat(7) { i ->
                val cx = frac(i * .31f + t * .035f) * (w * 1.4f) - w * .2f
                val cy = h * (.10f + frac(i * .47f) * .26f)
                drawLine(
                    Color.White.copy(alpha = .26f),
                    Offset(cx, cy),
                    Offset(cx + w * .18f, cy - h * .012f),
                    strokeWidth = h * .010f,
                    cap = StrokeCap.Round
                )
            }
            repeat(34) { i ->
                val x = frac(i * .618034f + t * (.08f + frac(i * .13f) * .05f)) * w
                val y = h * (.44f + frac(i * .371f) * .46f) + sin(t * 2f + i) * h * .015f
                drawOval(pal.glow.copy(alpha = .70f), Offset(x, y), Size(5f, 3f))
            }
        }

        DynamicScene.NIGHT -> Unit
    }
}

private fun DrawScope.birds(w: Float, h: Float, color: Color, t: Float) {
    repeat(5) { i ->
        val bx = w * (.22f + i * .11f + sin(t * .35f + i) * .01f)
        val by = h * (.21f + frac(i * .31f) * .08f)
        drawLine(color, Offset(bx, by), Offset(bx + w * .022f, by - h * .012f), 2f, StrokeCap.Round)
        drawLine(color, Offset(bx + w * .022f, by - h * .012f), Offset(bx + w * .044f, by), 2f, StrokeCap.Round)
    }
}

/** Мелкая текстура и виньетка убирают «плоский» вид векторной графики. */
private fun DrawScope.drawGrainAndVignette(scene: DynamicScene, w: Float, h: Float) {
    repeat(210) { i ->
        val x = frac(i * .6180339887f) * w
        val y = frac(i * .4142135623f) * h
        drawRect(
            Color.White.copy(alpha = .018f + frac(i * .137f) * .028f),
            Offset(x, y),
            Size(1.6f, 1.6f)
        )
    }
    drawRect(
        brush = Brush.radialGradient(
            listOf(Color(0x00000000), Color(0x00000000), Color(0x8A050B17)),
            center = Offset(w * .5f, h * .46f),
            radius = max(w, h) * .78f
        ),
        size = Size(w, h)
    )
    if (scene == DynamicScene.NIGHT || scene == DynamicScene.RAIN) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0x183B5B), Color(0x00000000)),
                startY = 0f,
                endY = h * .5f
            ),
            size = Size(w, h)
        )
    }
}
