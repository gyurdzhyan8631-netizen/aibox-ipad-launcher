package com.aibox.launcher.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.aibox.launcher.core.model.WeatherCondition
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color as ComposeColor
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Визуальный слой обоев первого стола.
 *
 * Механика перенесена из tvOS-Launcher (Apache License 2.0):
 * https://github.com/gyurdzhyan8631-netizen/tvOS-Launcher
 *
 * - сцена = фон + передний вырез + запечённая 480x270 текстура стекла;
 * - blur/цветокоррекция стекла делаются один раз при загрузке сцены, не в кадре;
 * - готовый «стеклянный» оверлей часов кэшируется в Bitmap;
 * - смена сцен — 1600 мс crossfade с интервалом 10..120 с;
 * - лёгкий параллакс слоёв и дешёвые погодные частицы остаются как и раньше.
 */
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

private data class SceneAssets(
    @DrawableRes val background: Int,
    @DrawableRes val foreground: Int? = null
)

/** Подбор реальной фотографии и слоя глубины под набор/состояние. */
private fun assetsFor(pack: DynamicScenePack, scene: DynamicScene): SceneAssets = when (pack) {
    DynamicScenePack.MOUNTAINS -> when (scene) {
        DynamicScene.NIGHT -> SceneAssets(R.drawable.scene_night, R.drawable.scene_night_fg)
        DynamicScene.RAIN, DynamicScene.SNOW -> SceneAssets(R.drawable.scene_fog, R.drawable.scene_fog_fg)
        else -> SceneAssets(R.drawable.scene_golden, R.drawable.scene_golden_fg)
    }

    DynamicScenePack.CITY -> SceneAssets(R.drawable.scene_city)

    DynamicScenePack.FOREST -> when (scene) {
        DynamicScene.DAWN -> SceneAssets(R.drawable.scene_dawn, R.drawable.scene_dawn_fg)
        DynamicScene.NIGHT -> SceneAssets(R.drawable.scene_night, R.drawable.scene_night_fg)
        DynamicScene.RAIN, DynamicScene.WIND -> SceneAssets(R.drawable.scene_fog, R.drawable.scene_fog_fg)
        else -> SceneAssets(R.drawable.scene_forest, R.drawable.scene_dawn_fg)
    }
}

private data class SceneGrade(val top: ComposeColor, val bottom: ComposeColor, val strength: Float)

/** Цветовая подложка, перекрашивающая одну фотографию под погоду/время суток. */
private fun gradeFor(scene: DynamicScene): SceneGrade = when (scene) {
    DynamicScene.DAY -> SceneGrade(ComposeColor(0xFFBBD9FF), ComposeColor(0xFFFFE2A8), 0.05f)
    DynamicScene.DAWN -> SceneGrade(ComposeColor(0xFF7A4A6B), ComposeColor(0xFFFFB06A), 0.20f)
    DynamicScene.NIGHT -> SceneGrade(ComposeColor(0xFF06102C), ComposeColor(0xFF101A36), 0.42f)
    DynamicScene.RAIN -> SceneGrade(ComposeColor(0xFF33474C), ComposeColor(0xFF5C6B6A), 0.24f)
    DynamicScene.SNOW -> SceneGrade(ComposeColor(0xFFDCEBF6), ComposeColor(0xFFFFFFFF), 0.20f)
    DynamicScene.WIND -> SceneGrade(ComposeColor(0xFF9A8E6B), ComposeColor(0xFFD9C7A0), 0.12f)
}

/** Всё тяжёлое по сцене подготовлено заранее и переиспользуется при rotation. */
private class BakedScene(
    val background: Bitmap,
    val foreground: Bitmap?,
    val glass: Bitmap,
    val wetGlass: Bitmap
) {
    var clockOverlay: Bitmap? = null
    var clockKey: String? = null

    fun recycle() {
        if (!background.isRecycled) background.recycle()
        foreground?.let { if (!it.isRecycled) it.recycle() }
        if (!glass.isRecycled) glass.recycle()
        if (!wetGlass.isRecycled) wetGlass.recycle()
        clockOverlay?.let { if (!it.isRecycled) it.recycle() }
        clockOverlay = null
        clockKey = null
    }
}

private fun loadBitmap(context: Context, @DrawableRes resId: Int, maxW: Int, maxH: Int): Bitmap {
    val drawable = ContextCompat.getDrawable(context, resId)
    if (drawable is android.graphics.drawable.BitmapDrawable) {
        val src = drawable.bitmap
        if (!src.isRecycled) {
            val scale = min(1f, min(maxW.toFloat() / src.width, maxH.toFloat() / src.height))
            return if (scale >= 1f) {
                src.copy(Bitmap.Config.ARGB_8888, true)
            } else {
                Bitmap.createScaledBitmap(
                    src,
                    max(1, (src.width * scale).toInt()),
                    max(1, (src.height * scale).toInt()),
                    true
                )
            }
        }
    }
    val out = Bitmap.createBitmap(maxW, maxH, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(out)
    drawable?.setBounds(0, 0, maxW, maxH)
    drawable?.draw(canvas)
    return out
}

private fun luminance(color: Int): Float =
    (0.2126f * Color.red(color) + 0.7152f * Color.green(color) + 0.0722f * Color.blue(color)) / 255f

private fun tintFor(lum: Float): Int = if (lum >= 0.45f) {
    val alpha = (0x1F + (min(lum, 0.85f) - 0.45f) / 0.4f * (0x5C - 0x1F)).toInt() shl 24
    alpha
} else {
    val alpha = ((0.45f - max(lum, 0.05f)) / 0.4f * 0x24).toInt() shl 24
    alpha or 0xFFFFFF
}

/**
 * Печёт маленькую размытую текстуру стекла из фона — как в tvOS Scenes.
 * downscale -> upscale даёт дешёвый blur без RenderScript и без затрат в кадре.
 */
private fun bakeScene(
    context: Context,
    assets: SceneAssets,
    darkClock: Boolean,
    waterSplit: Boolean
): BakedScene {
    val bg = loadBitmap(context, assets.background, 1280, 720)
    val fg = assets.foreground?.let { loadBitmap(context, it, 1280, 720) }

    val w = 480
    val h = 270
    val cropped = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val cropCanvas = AndroidCanvas(cropped)
    val scale = max(w.toFloat() / bg.width, h.toFloat() / bg.height)
    val sw = w / scale
    val sh = h / scale
    val sx = (bg.width - sw) / 2f
    val sy = (bg.height - sh) / 2f
    val left = sx.toInt().coerceIn(0, bg.width - 1)
    val top = sy.toInt().coerceIn(0, bg.height - 1)
    val right = min(bg.width, (sx + sw).toInt() + 1)
    val bottom = min(bg.height, (sy + sh).toInt() + 1)
    cropCanvas.drawBitmap(
        bg,
        Rect(left, top, right, bottom),
        RectF(0f, 0f, w.toFloat(), h.toFloat()),
        Paint(Paint.FILTER_BITMAP_FLAG)
    )

    var sumLum = 0f
    var count = 0
    for (py in (h / 10) until (h * 55 / 100) step 6) {
        for (px in (w / 8)..(w * 7 / 8) step 8) {
            sumLum += luminance(cropped.getPixel(px, py))
            count++
        }
    }
    val lum = if (count > 0) sumLum / count else 0.35f

    val small = Bitmap.createScaledBitmap(cropped, 160, 90, true)
    val base = Bitmap.createScaledBitmap(small, w, h, true)
    small.recycle()
    cropped.recycle()

    val cm = ColorMatrix().apply {
        setSaturation(if (darkClock) 0.95f else 1.6f)
        postConcat(
            ColorMatrix(
                floatArrayOf(
                    1.05f, 0f, 0f, 0f, 14f,
                    0f, 1.05f, 0f, 0f, 14f,
                    0f, 0f, 1.05f, 0f, 14f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }
    val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(cm) }

    val glass = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val glassCanvas = AndroidCanvas(glass)
    glassCanvas.drawBitmap(base, 0f, 0f, paint)
    if (darkClock) {
        val sheen = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w * 0.45f, h.toFloat(),
                intArrayOf(
                    Color.argb(46, 255, 255, 255),
                    Color.argb(22, 255, 255, 255),
                    Color.argb(10, 255, 255, 255),
                    Color.argb(26, 255, 255, 255)
                ),
                floatArrayOf(0f, 0.22f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        glassCanvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), sheen)
    } else {
        glassCanvas.drawColor(tintFor(lum))
    }

    val wetGlass = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val wetCanvas = AndroidCanvas(wetGlass)
    wetCanvas.drawBitmap(glass, 0f, 0f, null)
    wetCanvas.drawColor(0x33446699)
    if (!waterSplit) {
        // текстура всё равно есть — дешевле держать один тип BakedScene
        wetCanvas.drawColor(0x00000000)
    }

    base.recycle()
    return BakedScene(bg, fg, glass, wetGlass)
}

private fun drawClockText(
    canvas: AndroidCanvas,
    text: String,
    texture: Bitmap,
    x: Float,
    baseline: Float,
    textSize: Float,
    w: Int,
    h: Int,
    clipTop: Float? = null,
    clipBottom: Float? = null
) {
    canvas.save()
    if (clipTop != null || clipBottom != null) {
        canvas.clipRect(0f, clipTop ?: 0f, w.toFloat(), clipBottom ?: h.toFloat())
    }
    val shader = BitmapShader(texture, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    shader.setLocalMatrix(
        Matrix().apply { setScale(w.toFloat() / texture.width, h.toFloat() / texture.height) }
    )
    val fill = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        this.shader = shader
        this.textSize = textSize
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        isFakeBoldText = true
    }
    canvas.drawText(text, x, baseline, fill)
    canvas.restore()
}

private fun drawGlassClock(
    canvas: AndroidCanvas,
    time: String,
    baked: BakedScene,
    darkClock: Boolean,
    waterSplit: Boolean,
    scale: Float,
    w: Int,
    h: Int
) {
    val u = min(w / 960f, h / 540f)
    val textSize = 178f * u * scale
    val x = w / 2f
    val baseline = 62f * u + textSize * 0.78f

    val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (darkClock) Color.argb(38, 255, 255, 255) else Color.argb(60, 0, 0, 0)
        this.textSize = textSize
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        isFakeBoldText = true
        setShadowLayer(10f * u, 0f, 2f * u, 0xAA000000)
    }
    canvas.drawText(time, x, baseline, shadow)

    if (waterSplit) {
        val waterY = h * 0.55f
        drawClockText(canvas, time, baked.glass, x, baseline, textSize, w, h, clipBottom = waterY)
        drawClockText(canvas, time, baked.wetGlass, x, baseline, textSize, w, h, clipTop = waterY)
    } else {
        drawClockText(canvas, time, baked.glass, x, baseline, textSize, w, h)
    }

    val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.4f * u * scale
        this.textSize = textSize
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
        isFakeBoldText = true
        shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(
                Color.argb(217, 255, 255, 255),
                Color.argb(64, 255, 255, 255),
                Color.argb(20, 255, 255, 255),
                Color.argb(140, 255, 255, 255)
            ),
            floatArrayOf(0f, 0.22f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawText(time, x, baseline, rim)
}

private fun DrawScope.drawSceneLayer(
    baked: BakedScene,
    grade: SceneGrade,
    drift: Float,
    alpha: Float,
    overlay: androidx.compose.ui.graphics.ImageBitmap?
) {
    val w = size.width
    val h = size.height

    val bgScale = 1.055f
    val bw = w * bgScale
    val bh = h * bgScale
    val dx = (drift - 0.5f) * w * 0.014f
    val dy = (drift - 0.5f) * h * 0.010f
    val bgBitmap = baked.background.asImageBitmap()
    drawImage(
        image = bgBitmap,
        srcOffset = IntOffset.Zero,
        srcSize = IntSize(bgBitmap.width, bgBitmap.height),
        dstOffset = IntOffset(
            ((w - bw) / 2f + dx).toInt(),
            ((h - bh) / 2f + dy).toInt()
        ),
        dstSize = IntSize(bw.toInt(), bh.toInt()),
        alpha = alpha
    )

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                grade.top.copy(alpha = grade.strength),
                grade.bottom.copy(alpha = grade.strength * 0.75f)
            )
        ),
        alpha = alpha
    )

    if (overlay != null) drawImage(image = overlay, alpha = alpha)

    baked.foreground?.let { fg ->
        val fw = w * 1.10f
        val fh = h * 1.10f
        val fgBitmap = fg.asImageBitmap()
        drawImage(
            image = fgBitmap,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(fgBitmap.width, fgBitmap.height),
            dstOffset = IntOffset(
                ((w - fw) / 2f - dx * 1.9f).toInt(),
                ((h - fh) / 2f - dy * 1.5f).toInt()
            ),
            dstSize = IntSize(fw.toInt(), fh.toInt()),
            alpha = 0.78f * alpha
        )
    }

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                ComposeColor(0x4D000000),
                ComposeColor(0x00000000),
                ComposeColor(0x00000000),
                ComposeColor(0xA6000000)
            )
        ),
        alpha = alpha
    )
}

private fun DrawScope.drawWeatherParticles(scene: DynamicScene, phase: Float, w: Float, h: Float) {
    when (scene) {
        DynamicScene.RAIN -> repeat(90) { i ->
            val x = frac(i * 0.618034f + phase * 0.13f) * (w + 40f) - 20f
            val y = frac(i * 0.381966f + phase) * h
            drawLine(
                color = ComposeColor(0x66DCE8F5),
                start = Offset(x, y),
                end = Offset(x - 9f, y + 26f),
                strokeWidth = 1.2f
            )
        }

        DynamicScene.SNOW -> repeat(70) { i ->
            val x = frac(i * 0.754878f + phase * 0.17f) * w
            val y = frac(i * 0.445043f + phase) * h
            drawCircle(
                color = ComposeColor(0xAAFFFFFF),
                radius = 1.1f + frac(i * 0.219f) * 1.6f,
                center = Offset(x, y)
            )
        }

        DynamicScene.WIND -> repeat(34) { i ->
            val x = frac(i * 0.618034f + phase * 1.35f) * (w + 80f) - 40f
            val y = frac(i * 0.297057f + phase * 0.21f) * h
            drawLine(
                color = ComposeColor(0x55FFFFFF),
                start = Offset(x, y),
                end = Offset(x + 34f, y - 7f),
                strokeWidth = 1.1f
            )
        }

        else -> Unit
    }
}

/**
 * Живые обои первого стола: слои, запечённое стекло, смена сцен с кроссфейдом
 * и дешёвые погодные частицы.
 */
@Composable
fun DynamicSceneWallpaper(
    scene: DynamicScene,
    pack: DynamicScenePack = DynamicScenePack.MOUNTAINS,
    nowMinutes: Int,
    sunriseMin: Int,
    sunsetMin: Int,
    rotationSeconds: Int = 60,
    rotate: Boolean = true,
    glassClock: Boolean = false,
    clockScale: Float = 1f,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentScene by remember(pack) { mutableStateOf(scene) }
    var previousScene by remember { mutableStateOf<DynamicScene?>(null) }
    val fade = remember { Animatable(1f) }
    val fadeValue by fade.asState()

    val cache = remember { mutableMapOf<String, BakedScene>() }
    DisposableEffect(Unit) {
        onDispose {
            cache.values.forEach { it.recycle() }
            cache.clear()
        }
    }

    LaunchedEffect(scene, rotate) {
        if (!rotate && scene != currentScene) {
            previousScene = currentScene
            currentScene = scene
            fade.snapTo(0f)
            fade.animateTo(1f, animationSpec = tween(1600, easing = FastOutSlowInEasing))
            previousScene = null
        }
    }

    LaunchedEffect(pack, rotationSeconds, rotate) {
        if (!rotate || rotationSeconds <= 0) return@LaunchedEffect
        val order = DynamicScene.entries
        while (true) {
            delay(rotationSeconds * 1000L)
            val next = order[(order.indexOf(currentScene) + 1) % order.size]
            previousScene = currentScene
            currentScene = next
            fade.snapTo(0f)
            fade.animateTo(1f, animationSpec = tween(1600, easing = FastOutSlowInEasing))
            previousScene = null
        }
    }

    val transition = rememberInfiniteTransition(label = "scene-depth")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "depth-drift"
    )
    val particleDuration = when (currentScene) {
        DynamicScene.RAIN -> 1_500
        DynamicScene.SNOW -> 7_000
        DynamicScene.WIND -> 2_400
        else -> 6_000
    }
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = particleDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "weather-phase"
    )

    val time = "%02d:%02d".format(Locale.getDefault(), nowMinutes / 60, nowMinutes % 60)

    fun bakedFor(which: DynamicScene): BakedScene? {
        val key = "${pack.id}|${which.id}"
        cache[key]?.let { return it }
        val baked = bakeScene(
            context,
            assetsFor(pack, which),
            which == DynamicScene.NIGHT,
            which == DynamicScene.RAIN || which == DynamicScene.SNOW
        )
        if (cache.size >= 2) {
            val oldest = cache.keys.first()
            cache.remove(oldest)?.recycle()
        }
        cache[key] = baked
        return baked
    }

    fun overlayFor(which: DynamicScene, baked: BakedScene, w: Int, h: Int): Bitmap? {
        if (!glassClock) return null
        val key = "$time|$w x $h|$clockScale"
        if (baked.clockKey == key && baked.clockOverlay != null) return baked.clockOverlay
        baked.clockOverlay?.let { if (!it.isRecycled) it.recycle() }
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = AndroidCanvas(out)
        drawGlassClock(
            canvas, time, baked,
            which == DynamicScene.NIGHT,
            which == DynamicScene.RAIN || which == DynamicScene.SNOW,
            clockScale, w, h
        )
        baked.clockOverlay = out
        baked.clockKey = key
        return out
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width.toInt().coerceAtLeast(1)
            val h = size.height.toInt().coerceAtLeast(1)

            val prevScene = previousScene
            val prevBaked = prevScene?.let { bakedFor(it) }
            if (prevScene != null && prevBaked != null && fadeValue < 1f) {
                drawSceneLayer(
                    baked = prevBaked,
                    grade = gradeFor(prevScene),
                    drift = drift,
                    alpha = 1f,
                    overlay = overlayFor(prevScene, prevBaked, w, h)?.asImageBitmap()
                )
            }

            bakedFor(currentScene)?.let { baked ->
                drawSceneLayer(
                    baked = baked,
                    grade = gradeFor(currentScene),
                    drift = drift,
                    alpha = if (prevScene != null && fadeValue < 1f) fadeValue else 1f,
                    overlay = overlayFor(currentScene, baked, w, h)?.asImageBitmap()
                )
                if (currentScene == DynamicScene.RAIN ||
                    currentScene == DynamicScene.SNOW ||
                    currentScene == DynamicScene.WIND
                ) {
                    drawWeatherParticles(currentScene, phase, size.width, size.height)
                }
            }
        }
    }
}
