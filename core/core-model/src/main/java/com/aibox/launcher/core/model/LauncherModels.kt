package com.aibox.launcher.core.model

enum class VisualStyle(val title: String) {
    IPADOS_MINIMAL("iPadOS Minimal"),
    IOS_DARK("iOS Dark"),
    PREMIUM_GLASS("Premium Glass"),
    CLEAN_MINIMAL("Clean Minimal"),
    AUTOMOTIVE_PREMIUM("Automotive Premium"),

    // Стили брендовых обложек из раздела «Стили OS».
    IPAD("iPad"),
    SAMSUNG_TAB("Samsung Tab"),
    APPLE_DUO("Apple Duo"),
    PIXEL_FOLD("Google Pixel Fold");

    companion object {
        fun fromId(id: String?): VisualStyle =
            entries.firstOrNull { it.name == id } ?: IPADOS_MINIMAL
    }
}

enum class AnimationIntensity(val title: String, val scaleFactor: Float) {
    DISABLED("Отключено", 0f),
    MINIMAL("Минимальная", 0.5f),
    STANDARD("Стандартная", 1.0f),
    MAXIMUM("Максимальная", 1.35f)
}

enum class DayPeriod {
    MORNING, DAY, EVENING, NIGHT
}

enum class WeatherCondition {
    CLEAR, CLOUDY, OVERCAST, RAIN, HEAVY_RAIN, SNOW, THUNDERSTORM, FOG, WIND
}

data class DockAppItem(
    val id: String,
    val packageName: String,
    val label: String,
    val customIconUri: String? = null,
    val cornerRadiusDp: Float = 18f,
    val order: Int = 0
)

data class CustomAppIconOverride(
    val packageName: String,
    val customIconUri: String?,
    val cornerRadiusDp: Float = 18f,
    val backgroundColorHex: Long? = null,
    val hasShadow: Boolean = true
)

data class CapsuleConfig(
    val enabled: Boolean = true,
    val capsuleScale: Float = 1.0f,
    val buttonSizeDp: Float = 48f,
    val spacingDp: Float = 12f,
    val opacity: Float = 0.72f
)

data class LauncherConfig(
    val visualStyle: VisualStyle = VisualStyle.IPADOS_MINIMAL,
    val automotiveMode: Boolean = false,
    val animationIntensity: AnimationIntensity = AnimationIntensity.STANDARD,
    val dockOpacity: Float = 0.72f,
    val dockScale: Float = 1.0f,
    val dockSpacingDp: Float = 16f,
    val blurStrength: Float = 1.0f,
    val is24HourFormat: Boolean = true,
    val showSeconds: Boolean = false,
    val clockFontWeight: Int = 700,
    val manualCity: String = "",
    val ecoWallpaperMode: Boolean = false,
    val capsuleConfig: CapsuleConfig = CapsuleConfig()
)
