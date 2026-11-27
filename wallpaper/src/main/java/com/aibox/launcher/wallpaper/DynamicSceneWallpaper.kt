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
import com.aibox.launcher.core.model.WeatherCondition
import kotlinx.coroutines.delay
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin

/** Шесть состояний обоев первого рабочего стола. */
enum class DynamicScene(val id: String, val title: String, val subtitle: String) {
    DAWN("dawn", "Рассвет", "Розовое небо, туман в долине"),
    DAY("day", "День", "Жёлто-зелёное небо, спокойная погода"),
    NIGHT("night", "Ночь", "Луна, звёзды, огни храма"),
    RAIN("rain", "Дождь", "Тяжёлые облака и мокрые камни"),
    SNOW("snow", "Снег", "Белые горы и замёрзшая река"),
    WIND("wind", "Ветер", "Гнущиеся деревья и летящие листья");

    companion object {
        fun fromId(id: String?): DynamicScene = entries.firstOrNull { it.id == id } ?: DAY
    }
}

/**
 * Выбор сцены по реальной погоде и времени суток. Погода берётся из Open-Meteo
 * (по городу или по GPS), время восхода/заката — из координат города.
 */
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
        val nearSunrise = kotlin.math.abs(nowMinutes - rise) <= 45
        val nearSunset = kotlin.math.abs(nowMinutes - set) <= 45
        when {
            nearSunrise || nearSunset -> DynamicScene.DAWN
            nowMinutes in rise..set -> DynamicScene.DAY
            else -> DynamicScene.NIGHT
        }
    }
}

private fun frac(x: Float): Float = x - floor(x)

private data class ScenePalette(
    val sky: List<Color>,
    val far: Color,
    val mid: Color,
    val near: Color,
    val river: Color,
    val tree: Color,
    val accent: Color
)

private fun palette(scene: DynamicScene): ScenePalette = when (scene) {
    DynamicScene.DAY -> ScenePalette(
        listOf(Color(0xFFE1F58B), Color(0xFFAFD96E), Color(0xFF6FBE72)),
        Color(0xFF86B96F), Color(0xFF55965B), Color(0xFF2F6D48),
        Color(0xFF69CBD4), Color(0xFF1E5039), Color(0xFFFFF0A8)
    )

    DynamicScene.DAWN -> ScenePalette(
        listOf(Color(0xFFF4AFA4), Color(0xFFF6D0A4), Color(0xFFFFE7C0)),
        Color(0xFFA08FA9), Color(0xFF716F93), Color(0xFF414D66),
        Color(0xFFD8A6B3), Color(0xFF2B3548), Color(0xFFFFD49B)
    )

    DynamicScene.NIGHT -> ScenePalette(
        listOf(Color(0xFF080D25), Color(0xFF1A224E), Color(0xFF3A2B63)),
        Color(0xFF26305D), Color(0xFF172045), Color(0xFF0C122E),
        Color(0xFF28406F), Color(0xFF060B1E), Color(0xFFFFD98A)
    )

    DynamicScene.RAIN -> ScenePalette(
        listOf(Color(0xFF52646C), Color(0xFF748D93), Color(0xFF9BB0AE)),
        Color(0xFF61797B), Color(0xFF405C5F), Color(0xFF263D40),
        Color(0xFF6E9B9B), Color(0xFF162A2D), Color(0xFFC6D8D4)
    )

    DynamicScene.SNOW -> ScenePalette(
        listOf(Color(0xFFB8D4EE), Color(0xFFDCEBF7), Color(0xFFF8FCFF)),
        Color(0xFFA9C6DD), Color(0xFF83A9C5), Color(0xFF587F9D),
        Color(0xFFB9DFEE), Color(0xFFEAF5FD), Color(0xFFFFFFFF)
    )

    DynamicScene.WIND -> ScenePalette(
        listOf(Color(0xFFA7BAB0), Color(0xFFC7D2BE), Color(0xFFDACDAB)),
        Color(0xFF849879), Color(0xFF60755C), Color(0xFF3D523F),
        Color(0xFF8CB0A6), Color(0xFF273C2F), Color(0xFFE8D9A6)
    )
}

/**
 * Векторная сцена «горы, pine forest, река, храм, человек на скале» в стиле flat art.
 * Рисуется Compose Canvas: никаких bitmap-ассетов, поэтому работает офлайн и масштабируется
 * под любой экран. Дождь, снег, листья и звёзды анимируются.
 */
@Composable
fun DynamicSceneWallpaper(
    scene: DynamicScene,
    nowMinutes: Int,
    sunriseMin: Int,
    sunsetMin: Int,
    modifier: Modifier = Modifier
) {
    var seconds by remember { mutableFloatStateOf(0f) }
    val tickMs = when (scene) {
        DynamicScene.RAIN, DynamicScene.SNOW -> 33L
        DynamicScene.WIND -> 90L
        else -> 250L
    }

    LaunchedEffect(scene, tickMs) {
        while (true) {
            val nanos = withFrameNanos { it }
            seconds = (nanos % 60_000_000_000L) / 1_000_000_000f
            delay(tickMs)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        drawDynamicScene(scene, palette(scene), seconds, nowMinutes, sunriseMin, sunsetMin)
    }
}

private fun DrawScope.drawDynamicScene(
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

    drawRect(
        brush = Brush.verticalGradient(pal.sky, startY = 0f, endY = h * 0.86f),
        size = Size(w, h)
    )

    drawSkyBodies(scene, pal, t, nowMinutes, sunriseMin, sunsetMin)

    val far = mountainPath(
        w, h,
        listOf(0f to .58f, .12f to .40f, .25f to .55f, .43f to .27f, .60f to .49f, .76f to .35f, 1f to .52f)
    )
    val mid = mountainPath(
        w, h,
        listOf(0f to .70f, .16f to .47f, .33f to .64f, .51f to .40f, .69f to .60f, .86f to .44f, 1f to .61f)
    )
    val near = mountainPath(
        w, h,
        listOf(0f to .84f, .18f to .68f, .38f to .82f, .58f to .65f, .78f to .79f, 1f to .72f)
    )

    drawPath(far, pal.far)
    drawPineLine(w, h, .60f, 13, h * .075f, pal.tree.copy(alpha = 0.65f), 1.7f, t, scene)
    drawPath(mid, pal.mid)

    drawRiver(w, h, pal.river, scene)
    drawPineLine(w, h, .77f, 17, h * .10f, pal.tree, 4.3f, t, scene)

    drawTemple(w, h, pal, scene)
    drawManOnCliff(w, h, pal.tree)
    drawPath(near, pal.near)
    drawPineLine(w, h, .92f, 15, h * .12f, pal.tree, 8.1f, t, scene)

    drawWeather(scene, pal, t)
    drawRect(
        brush = Brush.verticalGradient(
            listOf(Color.Transparent, Color(0x99050B17)),
            startY = h * .58f,
            endY = h
        ),
        size = Size(w, h)
    )
}

private fun mountainPath(w: Float, h: Float, points: List<Pair<Float, Float>>): Path = Path().apply {
    moveTo(0f, h)
    lineTo(0f, h * points.first().second)
    points.forEach { (x, y) -> lineTo(w * x, h * y) }
    lineTo(w, h)
    close()
}

private fun DrawScope.drawRiver(w: Float, h: Float, color: Color, scene: DynamicScene) {
    val path = Path().apply {
        moveTo(w * .44f, h * .56f)
        cubicTo(w * .58f, h * .68f, w * .27f, h * .78f, w * .44f, h)
        lineTo(w * .74f, h)
        cubicTo(w * .61f, h * .82f, w * .79f, h * .67f, w * .54f, h * .56f)
        close()
    }
    drawPath(path, color)
    if (scene == DynamicScene.RAIN) {
        drawPath(path, Color.White.copy(alpha = 0.10f))
    }
    if (scene == DynamicScene.SNOW) {
        drawPath(path, Color.White.copy(alpha = 0.22f))
    }
}

private fun DrawScope.drawPineLine(
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
    for (i in 0 until count) {
        val x = (i + 0.5f) / count * w + (frac(sin(i * 17.13f + seed) * 43758.5f) - 0.5f) * w * 0.04f
        val y = h * baseline + (frac(sin(i * 9.71f + seed) * 12345.6f) - 0.5f) * h * 0.025f
        val treeH = baseHeight * (0.75f + frac(sin(i * 4.17f + seed) * 9876.5f) * 0.5f)
        drawPine(x, y, treeH, color, scene == DynamicScene.WIND, t, i)
    }
}

private fun DrawScope.drawPine(
    x: Float,
    baseY: Float,
    height: Float,
    color: Color,
    wind: Boolean,
    t: Float,
    seed: Int
) {
    val bend = if (wind) (sin(t * 1.8f + seed) * 0.06f + 0.08f) * height else 0f
    val half = height * 0.32f
    val trunk = Path().apply {
        moveTo(x - height * 0.045f, baseY)
        lineTo(x + height * 0.045f, baseY)
        lineTo(x + bend * 0.4f + height * 0.025f, baseY - height * 0.62f)
        lineTo(x + bend * 0.4f - height * 0.025f, baseY - height * 0.62f)
        close()
    }
    val crown = Path().apply {
        moveTo(x + bend, baseY - height)
        lineTo(x - half, baseY - height * 0.34f)
        lineTo(x + half * 0.75f, baseY - height * 0.40f)
        lineTo(x - half * 1.08f, baseY - height * 0.05f)
        lineTo(x + half, baseY - height * 0.10f)
        close()
    }
    drawPath(trunk, color)
    drawPath(crown, color)
}

private fun DrawScope.drawTemple(w: Float, h: Float, pal: ScenePalette, scene: DynamicScene) {
    val unit = min(w, h) * 0.052f
    val x = w * 0.79f
    val y = h * 0.50f
    val glow = scene == DynamicScene.NIGHT || scene == DynamicScene.RAIN

    val stairs = Path().apply {
        moveTo(x - unit * 1.5f, y + unit * 0.9f)
        lineTo(x + unit * 1.5f, y + unit * 0.9f)
        lineTo(x + unit * 1.15f, y + unit * 0.45f)
        lineTo(x - unit * 1.15f, y + unit * 0.45f)
        close()
    }
    drawPath(stairs, pal.near)

    drawRect(pal.mid, Offset(x - unit, y - unit * 0.75f), Size(unit * 2f, unit * 1.2f))

    val roof = Path().apply {
        moveTo(x - unit * 1.35f, y - unit * 0.75f)
        lineTo(x, y - unit * 1.45f)
        lineTo(x + unit * 1.35f, y - unit * 0.75f)
        close()
    }
    drawPath(roof, pal.near)

    for (i in -1..1) {
        drawLine(
            color = pal.near,
            start = Offset(x + i * unit * 0.65f, y - unit * 0.68f),
            end = Offset(x + i * unit * 0.65f, y + unit * 0.40f),
            strokeWidth = unit * 0.12f
        )
    }

    if (glow) {
        for (i in -1..1) {
            drawCircle(
                color = pal.accent.copy(alpha = 0.92f),
                radius = unit * 0.12f,
                center = Offset(x + i * unit * 0.65f, y - unit * 0.12f)
            )
        }
    }
}

private fun DrawScope.drawManOnCliff(w: Float, h: Float, color: Color) {
    val s = min(w, h) * 0.010f
    val x = w * 0.36f
    val y = h * 0.55f

    val cliff = Path().apply {
        moveTo(x - s * 2.6f, y + s * 1.2f)
        lineTo(x + s * 2.2f, y + s * 1.2f)
        lineTo(x + s * 0.7f, y - s * 0.2f)
        lineTo(x - s * 1.2f, y - s * 0.5f)
        close()
    }
    drawPath(cliff, color)
    drawCircle(color, s * 0.42f, Offset(x, y - s * 1.5f))
    drawLine(color, Offset(x, y - s), Offset(x - s * 0.2f, y + s * 0.3f), s * 0.30f)
}

private fun DrawScope.drawSkyBodies(
    scene: DynamicScene,
    pal: ScenePalette,
    t: Float,
    nowMinutes: Int,
    sunriseMin: Int,
    sunsetMin: Int
) {
    val w = size.width
    val h = size.height
    val rise = if (sunsetMin > sunriseMin) sunriseMin else 390
    val set = if (sunsetMin > sunriseMin) sunsetMin else 1110
    val progress = ((nowMinutes - rise).toFloat() / (set - rise).toFloat()).coerceIn(0f, 1f)

    if (scene == DynamicScene.NIGHT) {
        for (i in 0 until 80) {
            val sx = frac(i * 0.618034f + 0.11f) * w
            val sy = frac(i * 0.754878f + 0.07f) * h * 0.56f
            val twinkle = 0.45f + 0.55f * sin(t * 1.4f + i * 1.7f)
            drawCircle(
                color = Color.White.copy(alpha = (0.35f + 0.65f * frac(i * 0.371f)) * twinkle),
                radius = (0.8f + frac(i * 0.219f) * 1.5f),
                center = Offset(sx, sy)
            )
        }
        val moon = Offset(w * 0.74f, h * 0.22f)
        drawCircle(pal.accent.copy(alpha = 0.10f), min(w, h) * 0.10f, moon)
        drawCircle(Color(0xFFF4F1D8), min(w, h) * 0.038f, moon)
        drawCircle(Color(0xFF1A224E), min(w, h) * 0.033f, Offset(moon.x + min(w, h) * 0.014f, moon.y - min(w, h) * 0.008f))
        return
    }

    val sunX = when (scene) {
        DynamicScene.DAWN -> w * (0.12f + 0.20f * (1f - progress))
        DynamicScene.DAY -> w * (0.18f + 0.64f * progress)
        else -> w * 0.55f
    }
    val sunY = when (scene) {
        DynamicScene.DAWN -> h * 0.48f
        DynamicScene.DAY -> h * (0.34f - 0.18f * sin(progress * 3.14159f))
        else -> h * 0.32f
    }
    val r = min(w, h) * if (scene == DynamicScene.DAWN) 0.055f else 0.042f

    val discAlpha = if (scene == DynamicScene.RAIN || scene == DynamicScene.SNOW) 0.35f else 0.85f
    drawCircle(pal.accent.copy(alpha = 0.16f), r * 4.2f, Offset(sunX, sunY))
    drawCircle(pal.accent.copy(alpha = discAlpha), r, Offset(sunX, sunY))
}

private fun DrawScope.drawWeather(scene: DynamicScene, pal: ScenePalette, t: Float) {
    val w = size.width
    val h = size.height

    when (scene) {
        DynamicScene.DAWN -> {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.20f), Color.Transparent),
                    startY = h * .56f,
                    endY = h * .86f
                ),
                size = Size(w, h)
            )
            for (i in 0..4) {
                val bx = w * (0.08f + i * 0.2f + sin(t * 0.4f + i) * 0.01f)
                val by = h * (0.50f + frac(i * 0.27f) * 0.10f)
                drawLine(
                    Color(0xFF6D4F4B),
                    Offset(bx, by),
                    Offset(bx + w * 0.035f, by - h * 0.014f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    Color(0xFF6D4F4B),
                    Offset(bx + w * 0.035f, by - h * 0.014f),
                    Offset(bx + w * 0.065f, by),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
            }
        }

        DynamicScene.DAY -> {
            for (i in 0..3) {
                val bx = w * (0.24f + i * 0.11f)
                val by = h * (0.22f + frac(i * 0.31f) * 0.08f)
                drawLine(
                    Color(0xFF3E5B48),
                    Offset(bx, by),
                    Offset(bx + w * 0.022f, by - h * 0.012f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    Color(0xFF3E5B48),
                    Offset(bx + w * 0.022f, by - h * 0.012f),
                    Offset(bx + w * 0.044f, by),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
            }
        }

        DynamicScene.RAIN -> {
            repeat(140) { i ->
                val x = frac(i * 0.7548776f) * w
                val y = frac(i * 0.3183099f + t * 0.55f) * h
                drawLine(
                    Color.White.copy(alpha = 0.22f + frac(i * 0.17f) * 0.25f),
                    Offset(x, y),
                    Offset(x - w * 0.006f, y + h * 0.035f),
                    strokeWidth = 1.6f
                )
            }
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, Color(0x338CA0A0), Color.Transparent),
                    startY = h * .62f,
                    endY = h * .90f
                ),
                size = Size(w, h)
            )
        }

        DynamicScene.SNOW -> {
            repeat(110) { i ->
                val drift = sin(t * 0.7f + i) * w * 0.012f
                val x = frac(i * 0.618034f) * w + drift
                val y = frac(i * 0.4142135f + t * 0.055f) * h
                drawCircle(
                    Color.White.copy(alpha = 0.55f + frac(i * 0.23f) * 0.45f),
                    1.6f + frac(i * 0.19f) * 2.6f,
                    Offset(x, y)
                )
            }
        }

        DynamicScene.WIND -> {
            for (i in 0..5) {
                val cx = frac(i * 0.31f + t * 0.035f) * (w * 1.4f) - w * 0.2f
                val cy = h * (0.12f + frac(i * 0.47f) * 0.28f)
                drawLine(
                    Color.White.copy(alpha = 0.28f),
                    Offset(cx, cy),
                    Offset(cx + w * 0.16f, cy - h * 0.012f),
                    strokeWidth = h * 0.012f,
                    cap = StrokeCap.Round
                )
            }
            repeat(28) { i ->
                val x = frac(i * 0.618034f + t * (0.08f + frac(i * 0.13f) * 0.05f)) * w
                val y = h * (0.45f + frac(i * 0.371f) * 0.45f) + sin(t * 2f + i) * h * 0.015f
                drawCircle(
                    pal.accent.copy(alpha = 0.65f),
                    2.4f + frac(i * 0.29f) * 2.2f,
                    Offset(x, y)
                )
            }
        }

        DynamicScene.NIGHT -> Unit
    }
}
