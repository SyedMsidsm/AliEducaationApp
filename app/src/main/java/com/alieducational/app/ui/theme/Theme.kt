package com.alieducational.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ErrorColorDark = Color(0xFFCF6679)
private val ErrorColorLight = Color(0xFFBA1A1A)

object AliDimens {
    val spacingXs = 4.dp
    val spacingSm = 8.dp
    val spacingMd = 12.dp
    val spacingLg = 16.dp
    val spacingXl = 24.dp

    val radiusSm = 8.dp
    val radiusMd = 12.dp
    val radiusLg = 16.dp
    val radiusPill = 28.dp

    val minTouchTarget = 48.dp
    val minChipTextSize = 13.sp
}

private val DarkColorScheme = darkColorScheme(
    primary = GoldDark,
    onPrimary = DarkBackground,
    primaryContainer = GoldDark.copy(alpha = 0.2f),
    onPrimaryContainer = GoldDark,
    secondary = GoldDark,
    onSecondary = DarkBackground,
    secondaryContainer = GoldDark.copy(alpha = 0.15f),
    onSecondaryContainer = GoldDark,
    tertiary = PositiveGreenDark,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutline.copy(alpha = 0.5f),
    error = ErrorColorDark,
    onError = DarkBackground,
)

private val LightColorScheme = lightColorScheme(
    primary = GoldLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF4EAD7),
    onPrimaryContainer = Color(0xFF5D400A),
    secondary = GoldLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4EAD7),
    onSecondaryContainer = Color(0xFF5D400A),
    tertiary = PositiveGreenLight,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightBackground,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = LightOutline,
    outlineVariant = LightOutline.copy(alpha = 0.5f),
    error = ErrorColorLight,
    onError = Color.White,
)

@Composable
fun AliEducationalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appTextSize: String = "Default",
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val statusColors = if (darkTheme) DarkAliStatusColors else LightAliStatusColors

    val view = LocalView.current
    if (!view.isInEditMode && view.context is Activity) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    val scaleFactor = when (appTextSize) {
        "Small" -> 0.85f
        "Large" -> 1.15f
        "Extra Large" -> 1.30f
        else -> 1.0f
    }

    val density = androidx.compose.ui.platform.LocalDensity.current
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, density.fontScale * scaleFactor),
        LocalAliStatusColors provides statusColors
    ) {
        MaterialTheme(colorScheme = colorScheme, typography = getScaledTypography(1f), content = content)
    }
}
