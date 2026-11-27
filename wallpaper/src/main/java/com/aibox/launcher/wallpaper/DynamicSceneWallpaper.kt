package com.aibox.launcher.wallpaper

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.aibox.launcher.core.model.WeatherCondition
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.floor

/**
 * Визуальный слой обоев первого стола.
 *
 * Единый стиль: flat art, минимализм, векторная графика. Все сцены — локальные
 * VectorDrawable в res/drawable/scene_flat_*.xml, без фотографий и тяжёлых градиентов.
 * Набор сцен и погодная логика не изменены — здесь только отрисовка.
 */
enum class DynamicScenePack(val id: String, val title: String, val subtitle: String) {
    MOUNTAINS(
        "mountains",
        "Горы и храм",
        "Плоские векторные горы, туман, река, храм и человек на скале"
    ),
    CITY(
        "city",
        "Город",
        "Плоский векторный город, башня с часами и огни окон"
    ),
    FOREST(
        "forest",
        "Лес",
        "Плоский векторный лес, озеро, изба и олени"
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

private data class SceneAssets(@DrawableRes val background: Int)

/** Подбор плоской векторной сцены под набор и состояние. */
private fun assetsFor(pack: DynamicScenePack, scene: DynamicScene): SceneAssets = when (pack) {
    DynamicScenePack.MOUNTAINS -> when (scene) {
        DynamicScene.DAWN -> SceneAssets(R.drawable.scene_flat_dawn)
        DynamicScene.NIGHT -> SceneAssets(R.drawable.scene_flat_night)
        DynamicScene.RAIN -> SceneAssets(R.drawable.scene_flat_rain)
        DynamicScene.SNOW -> SceneAssets(R.drawable.scene_flat_snow)
        DynamicScene.WIND -> SceneAssets(R.drawable.scene_flat_wind)
        else -> SceneAssets(R.drawable.scene_flat_day)
    }

    DynamicScenePack.CITY -> SceneAssets(R.drawable.scene_flat_city)
    DynamicScenePack.FOREST -> SceneAssets(R.drawable.scene_flat_forest)
}

/**
 * Живые обои первого стола: один векторный слой с медленным параллаксом,
 * дешёвые погодные частицы и кинематографичная дымка.
 *
 * Движение идёт через graphicsLayer, а частицы обновляются отдельным composable
 * с частотой ~24fps — корневой экран не перерисовывается каждый кадр.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
fun DynamicSceneWallpaper(
    scene: DynamicScene,
    pack: DynamicScenePack = DynamicScenePack.MOUNTAINS,
    nowMinutes: Int,
    sunriseMin: Int,
    sunsetMin: Int,
    modifier: Modifier = Modifier
) {
    val assets = assetsFor(pack, scene)
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

    Box(modifier = modifier.fillMaxSize()) {
        val background = painterResource(id = assets.background)
        Image(
            painter = background,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val sx = (drift - 0.5f) * size.width * 0.014f
                    val sy = (drift - 0.5f) * size.height * 0.010f
                    scaleX = 1.055f
                    scaleY = 1.055f
                    translationX = sx
                    translationY = sy
                }
        )

        if (scene == DynamicScene.RAIN || scene == DynamicScene.SNOW || scene == DynamicScene.WIND) {
            WeatherParticles(scene = scene, modifier = Modifier.fillMaxSize())
        }

        // Мягкая кинематографичная дымка сверху и снизу — статична, не перерисовывается.
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x4D000000),
                        Color(0x00000000),
                        Color(0x00000000),
                        Color(0xA6000000)
                    ),
                    startY = 0f,
                    endY = size.height
                )
            )
        }
    }
}

/** Частицы живут в отдельном composable: рекомпозируется только этот Canvas. */
@Composable
private fun WeatherParticles(scene: DynamicScene, modifier: Modifier = Modifier) {
    val durationMillis = when (scene) {
        DynamicScene.RAIN -> 1_500
        DynamicScene.SNOW -> 7_000
        DynamicScene.WIND -> 2_400
        else -> 6_000
    }
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(scene) {
        val frameDelay = 1_000L / 24L
        while (true) {
            phase = (phase + frameDelay.toFloat() / durationMillis) % 1f
            delay(frameDelay)
        }
    }
    Canvas(modifier = modifier) {
        drawWeatherParticles(scene, phase, size.width, size.height)
    }
}

private fun DrawScope.drawWeatherParticles(
    scene: DynamicScene,
    phase: Float,
    w: Float,
    h: Float
) {
    when (scene) {
        DynamicScene.RAIN -> repeat(90) { i ->
            val x = frac(i * 0.618034f + phase * 0.13f) * (w + 40f) - 20f
            val y = frac(i * 0.381966f + phase) * h
            drawLine(
                color = Color(0x66DCE8F5),
                start = Offset(x, y),
                end = Offset(x - 9f, y + 26f),
                strokeWidth = 1.2f
            )
        }

        DynamicScene.SNOW -> repeat(70) { i ->
            val x = frac(i * 0.754878f + phase * 0.17f) * w
            val y = frac(i * 0.445043f + phase) * h
            drawCircle(
                color = Color(0xAAFFFFFF),
                radius = 1.1f + frac(i * 0.219f) * 1.6f,
                center = Offset(x, y)
            )
        }

        DynamicScene.WIND -> repeat(34) { i ->
            val x = frac(i * 0.618034f + phase * 1.35f) * (w + 80f) - 40f
            val y = frac(i * 0.297057f + phase * 0.21f) * h
            drawLine(
                color = Color(0x55FFFFFF),
                start = Offset(x, y),
                end = Offset(x + 34f, y - 7f),
                strokeWidth = 1.1f
            )
        }

        else -> Unit
    }
}
