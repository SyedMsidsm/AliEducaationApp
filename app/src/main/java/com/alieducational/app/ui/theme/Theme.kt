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

private val ErrorColor = Color(0xFFCF6679)

private val DarkColorScheme = darkColorScheme(
    primary = GoldDark,
    onPrimary = DarkBackground,
    primaryContainer = GoldDark.copy(alpha = 0.2f),
    onPrimaryContainer = GoldDark,
    secondary = GoldDark,
    onSecondary = DarkBackground,
    secondaryContainer = GoldDark.copy(alpha = 0.15f),
    onSecondaryContainer = GoldDark,
    tertiary = SuccessDark,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline.copy(alpha = 0.5f),
    error = ErrorColor,
    onError = DarkBackground,
)

private val LightColorScheme = lightColorScheme(
    primary = GoldLight,
    onPrimary = LightSurface,
    primaryContainer = GoldLight.copy(alpha = 0.12f),
    onPrimaryContainer = GoldLight,
    secondary = GoldLight,
    onSecondary = LightSurface,
    secondaryContainer = GoldLight.copy(alpha = 0.1f),
    onSecondaryContainer = GoldLight,
    tertiary = SuccessLight,
    onTertiary = LightSurface,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightBackground,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline.copy(alpha = 0.5f),
    error = ErrorColor,
    onError = LightSurface,
)

@Composable
fun AliEducationalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AliTypography,
        content = content,
    )
}
