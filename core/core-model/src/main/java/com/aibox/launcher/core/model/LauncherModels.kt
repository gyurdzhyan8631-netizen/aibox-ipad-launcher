package com.aibox.launcher.core.model

enum class VisualStyle(val title: String) {
    IPADOS_MINIMAL("iPadOS Minimal"),
    IOS_DARK("iOS Dark"),
    PREMIUM_GLASS("Premium Glass"),
    CLEAN_MINIMAL("Clean Minimal"),
    AUTOMOTIVE_PREMIUM("Automotive Premium")
}

enum class IconShape(val title: String, val cornerRadiusFraction: Float) {
    ROUNDED("Скруглённые", 0.22f),
    CIRCLE("Круглые", 0.5f),
    SQUARE("Квадратные", 0f)
}

enum class WidgetCardStyle(val title: String) {
    GLASS("Стекло"),
    DARK("Тёмная"),
    ACCENT("Акцентная")
}

data class WidgetPlacement(
    val xFraction: Float,
    val yFraction: Float,
    val widthFraction: Float,
    val heightFraction: Float
) {
    fun constrained(minWidth: Float, minHeight: Float): WidgetPlacement {
        val width = widthFraction.coerceIn(minWidth.coerceAtMost(1f), 1f)
        val height = heightFraction.coerceIn(minHeight.coerceAtMost(1f), 1f)
        return copy(
            xFraction = xFraction.coerceIn(0f, 1f - width),
            yFraction = yFraction.coerceIn(0f, 1f - height),
            widthFraction = width,
            heightFraction = height
        )
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

enum class WidgetSize(val title: String) {
    SMALL("Small"),
    MEDIUM("Medium"),
    LARGE("Large"),
    CUSTOM("Custom")
}

enum class ElementType {
    APP_ICON,
    FOLDER,
    WIDGET_CLOCK,
    WIDGET_DATE,
    WIDGET_WEATHER,
    WIDGET_FORECAST,
    WIDGET_MUSIC,
    WIDGET_NAV,
    WIDGET_MAP,
    WIDGET_SPEED,
    WIDGET_TRIP,
    WIDGET_VPN,
    WIDGET_WIFI,
    WIDGET_SYSTEM,
    WIDGET_BATTERY,
    WIDGET_CALENDAR,
    CUSTOM_LOGO,
    CUSTOM_WIDGET
}

data class DesktopElement(
    val id: String,
    val screenIndex: Int, // 0 = Home Screen 1, 1 = Home Screen 2
    val title: String,
    val type: ElementType,
    val xFraction: Float,
    val yFraction: Float,
    val widthDp: Float,
    val heightDp: Float,
    val alpha: Float = 0.85f,
    val cornerRadiusDp: Float = 26f,
    val shadowElevationDp: Float = 8f,
    val accentHex: Long? = null,
    val widgetSize: WidgetSize = WidgetSize.MEDIUM,
    val packageName: String? = null,
    val customImageUri: String? = null,
    val folderItems: List<String> = emptyList(),
    val isHidden: Boolean = false
)

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
