package com.aibox.launcher.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aibox.launcher.core.model.VisualStyle

@Immutable
data class LauncherStyleTokens(
    val panelBackground: Color,
    val panelBorder: Color,
    val cardBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val cornerRadius: Dp,
    val isDark: Boolean
)

val LocalLauncherTokens = staticCompositionLocalOf {
    LauncherStyleTokens(
        panelBackground = Color(0x521C1C1E),
        panelBorder = Color(0x38FFFFFF),
        cardBackground = Color(0x422C2C2E),
        textPrimary = Color.White,
        textSecondary = Color(0xCCFFFFFF),
        accent = Color(0xFF0A84FF),
        cornerRadius = 28.dp,
        isDark = true
    )
}

fun tokensForStyle(style: VisualStyle): LauncherStyleTokens = when (style) {
    VisualStyle.IPADOS_MINIMAL -> LauncherStyleTokens(
        panelBackground = Color(0x541C1C1E),
        panelBorder = Color(0x3DFFFFFF),
        cardBackground = Color(0x452C2C2E),
        textPrimary = Color.White,
        textSecondary = Color(0xCCFFFFFF),
        accent = Color(0xFF0A84FF),
        cornerRadius = 28.dp,
        isDark = true
    )
    VisualStyle.IOS_DARK -> LauncherStyleTokens(
        panelBackground = Color(0xCC0D0D0F),
        panelBorder = Color(0x26FFFFFF),
        cardBackground = Color(0xB818181B),
        textPrimary = Color.White,
        textSecondary = Color(0x99EBEBF5),
        accent = Color(0xFF5E5CE6),
        cornerRadius = 26.dp,
        isDark = true
    )
    VisualStyle.PREMIUM_GLASS -> LauncherStyleTokens(
        panelBackground = Color(0x36FFFFFF),
        panelBorder = Color(0x66FFFFFF),
        cardBackground = Color(0x2EFFFFFF),
        textPrimary = Color.White,
        textSecondary = Color(0xE6FFFFFF),
        accent = Color(0xFF32ADE6),
        cornerRadius = 32.dp,
        isDark = true
    )
    VisualStyle.CLEAN_MINIMAL -> LauncherStyleTokens(
        panelBackground = Color(0xDCF5F5F7),
        panelBorder = Color(0x33000000),
        cardBackground = Color(0xE6FFFFFF),
        textPrimary = Color(0xFF1C1C1E),
        textSecondary = Color(0x993C3C43),
        accent = Color(0xFF007AFF),
        cornerRadius = 24.dp,
        isDark = false
    )
    VisualStyle.AUTOMOTIVE_PREMIUM -> LauncherStyleTokens(
        panelBackground = Color(0xE0111318),
        panelBorder = Color(0x4D38BDF8),
        cardBackground = Color(0xCC1A1E26),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFFCBD5E1),
        accent = Color(0xFFFF9F0A),
        cornerRadius = 22.dp,
        isDark = true
    )
}

private fun appleTypography(fontFamily: FontFamily): Typography {
    val base = Typography()
    fun TextStyle.withLauncherFont() = copy(fontFamily = fontFamily)

    return base.copy(
        displayLarge = base.displayLarge.withLauncherFont().copy(
            fontWeight = FontWeight.Bold,
            fontSize = 60.sp,
            letterSpacing = (-1.2).sp
        ),
        displayMedium = base.displayMedium.withLauncherFont(),
        displaySmall = base.displaySmall.withLauncherFont(),
        headlineLarge = base.headlineLarge.withLauncherFont(),
        headlineMedium = base.headlineMedium.withLauncherFont().copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            letterSpacing = (-0.4).sp
        ),
        headlineSmall = base.headlineSmall.withLauncherFont(),
        titleLarge = base.titleLarge.withLauncherFont(),
        titleMedium = base.titleMedium.withLauncherFont().copy(
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp
        ),
        titleSmall = base.titleSmall.withLauncherFont(),
        bodyLarge = base.bodyLarge.withLauncherFont(),
        bodyMedium = base.bodyMedium.withLauncherFont().copy(
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp
        ),
        bodySmall = base.bodySmall.withLauncherFont(),
        labelLarge = base.labelLarge.withLauncherFont(),
        labelMedium = base.labelMedium.withLauncherFont(),
        labelSmall = base.labelSmall.withLauncherFont().copy(
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp
        )
    )
}

@Composable
fun AiBoxLauncherTheme(
    style: VisualStyle = VisualStyle.IPADOS_MINIMAL,
    fontFamily: FontFamily = FontFamily.SansSerif,
    content: @Composable () -> Unit
) {
    val tokens = tokensForStyle(style)
    val colorScheme = if (tokens.isDark) {
        darkColorScheme(
            primary = tokens.accent,
            background = Color(0xFF0B0F17),
            surface = tokens.panelBackground,
            onPrimary = Color.White,
            onBackground = tokens.textPrimary,
            onSurface = tokens.textPrimary
        )
    } else {
        lightColorScheme(
            primary = tokens.accent,
            background = Color(0xFFE5E5EA),
            surface = tokens.panelBackground,
            onPrimary = Color.White,
            onBackground = tokens.textPrimary,
            onSurface = tokens.textPrimary
        )
    }

    val shapes = Shapes(
        small = RoundedCornerShape(tokens.cornerRadius * 0.5f),
        medium = RoundedCornerShape(tokens.cornerRadius * 0.8f),
        large = RoundedCornerShape(tokens.cornerRadius)
    )

    CompositionLocalProvider(LocalLauncherTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = appleTypography(fontFamily),
            shapes = shapes,
            content = content
        )
    }
}
