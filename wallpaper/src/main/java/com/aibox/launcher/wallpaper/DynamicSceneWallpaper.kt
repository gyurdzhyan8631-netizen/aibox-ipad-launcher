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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.aibox.launcher.core.model.WeatherCondition
import kotlin.math.abs
import kotlin.math.floor

/**
 * Визуальный слой обоев первого стола.
 *
 * Стиль многослойных «живых» сцен (реальное фото + отдельный передний слой с параллаксом,
 * цветовая grading-подложка и мягкая затемняющая дымка) взят из tvOS-Launcher:
 * https://github.com/gyurdzhyan8631-netizen/tvOS-Launcher  (Apache License 2.0).
 *
 * Логика выбора сцены, GPS/погода и настройки не изменены — здесь только отрисовка.
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

private data class SceneGrade(val top: Color, val bottom: Color, val strength: Float)

/** Цветовая подложка, которая перекрашивает одну и ту же фотографию под погоду/время суток. */
private fun gradeFor(scene: DynamicScene): SceneGrade = when (scene) {
    DynamicScene.DAY -> SceneGrade(Color(0xFFBBD9FF), Color(0xFFFFE2A8), 0.05f)
    DynamicScene.DAWN -> SceneGrade(Color(0xFF7A4A6B), Color(0xFFFFB06A), 0.20f)
    DynamicScene.NIGHT -> SceneGrade(Color(0xFF06102C), Color(0xFF101A36), 0.42f)
    DynamicScene.RAIN -> SceneGrade(Color(0xFF33474C), Color(0xFF5C6B6A), 0.24f)
    DynamicScene.SNOW -> SceneGrade(Color(0xFFDCEBF6), Color(0xFFFFFFFF), 0.20f)
    DynamicScene.WIND -> SceneGrade(Color(0xFF9A8E6B), Color(0xFFD9C7A0), 0.12f)
}

/**
 * Живые обои первого стола: реальное фото, медленно дрейфующий передний слой для эффекта
 * глубины, лёгкая цветовая коррекция и очень дешёвые погодные частицы.
 *
 * Всё движение идёт через graphicsLayer/infinite transition без перерисовки тяжёлых векторных
 * сцен, поэтому анимация остаётся плавной даже на слабом устройстве.
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
    val assets = assetsFor(pack, scene)
    val grade = gradeFor(scene)
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
    val particleDuration = when (scene) {
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

        val foreground = assets.foreground?.let { painterResource(id = it) }
        if (foreground != null) {
            Image(
                painter = foreground,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val sx = (drift - 0.5f) * size.width * 0.014f
                        val sy = (drift - 0.5f) * size.height * 0.010f
                        alpha = 0.78f
                        scaleX = 1.10f
                        scaleY = 1.10f
                        translationX = -sx * 1.9f
                        translationY = -sy * 1.5f
                    }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            grade.top.copy(alpha = grade.strength),
                            grade.bottom.copy(alpha = grade.strength * 0.75f)
                        )
                    )
                )
        )

        if (scene == DynamicScene.RAIN || scene == DynamicScene.SNOW || scene == DynamicScene.WIND) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawWeatherParticles(scene, phase, size.width, size.height)
            }
        }

        // Мягкая кинематографичная дымка сверху и снизу — как в живых сценах tvOS.
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
