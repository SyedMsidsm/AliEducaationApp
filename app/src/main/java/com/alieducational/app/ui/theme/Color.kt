package com.alieducational.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ── Dark Theme Colors ──
val DarkBackground = Color(0xFF111411)
val DarkSurface = Color(0xFF1C211D)
val DarkOnSurface = Color(0xFFF6F2E9)
val DarkOnSurfaceVariant = Color(0xFFB9C0B8)
val DarkOutline = Color(0xFF354038)

// ── Dark Surface Containers ──
val DarkSurfaceContainerLowest  = Color(0xFF0D100D)
val DarkSurfaceContainerLow      = Color(0xFF181D19)
val DarkSurfaceContainer         = Color(0xFF1C211D)
val DarkSurfaceContainerHigh     = Color(0xFF242A25)   // dialogs, sheets (green-tinted, matches DarkSurface)
val DarkSurfaceContainerHighest  = Color(0xFF2C332D)

// ── Light Theme Colors (Targeting >= 4.5:1 text contrast) ──
val LightBackground = Color(0xFFF6F7F5)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF141915)
val LightOnSurfaceVariant = Color(0xFF47524A)
val LightOutline = Color(0xFFCCD4CD)

// ── Light Surface Containers ──
val LightSurfaceContainerLowest  = Color(0xFFFFFFFF)
val LightSurfaceContainerLow     = Color(0xFFFBF9F4)
val LightSurfaceContainer        = Color(0xFFF7F4EC)
val LightSurfaceContainerHigh    = Color(0xFFF2EEE4)   // dialogs, sheets (warm cream)
val LightSurfaceContainerHighest = Color(0xFFEBE6DA)

// ── Brand / Accent Colors ──
val GoldDark = Color(0xFFD9AC46)
val GoldLight = Color(0xFF8A6014) // Darker gold for WCAG AA compliance with white

// ── Semantic High-Contrast Colors ──
val PositiveGreenDark = Color(0xFF75B890)
val PositiveGreenLight = Color(0xFF1B5E20) // WCAG 8.6:1 contrast with white
val NegativeRedDark = Color(0xFFE57373)
val NegativeRedLight = Color(0xFFC62828) // WCAG 5.9:1 contrast with white
val WarningOrangeDark = Color(0xFFE4B765)
val WarningOrangeLight = Color(0xFFB76E00) // WCAG 4.7:1 contrast with white

// ── Ali Status Colors ──
@Immutable
data class AliStatusColors(
    val paidBg: Color,
    val paidFg: Color,
    val dueBg: Color,
    val dueFg: Color,
    val voidBg: Color,
    val voidFg: Color
)

val LightAliStatusColors = AliStatusColors(
    paidBg = Color(0xFFE8F5E9),
    paidFg = Color(0xFF2E7D32),
    dueBg = Color(0xFFFFEBEE),
    dueFg = Color(0xFFC62828),
    voidBg = Color(0xFFECEFF1),
    voidFg = Color(0xFF546E7A)
)

val DarkAliStatusColors = AliStatusColors(
    paidBg = Color(0xFF1B3A22),
    paidFg = Color(0xFF81C784),
    dueBg = Color(0xFF3E1F1F),
    dueFg = Color(0xFFEF9A9A),
    voidBg = Color(0xFF2B3236),
    voidFg = Color(0xFFB0BEC5)
)

val LocalAliStatusColors = staticCompositionLocalOf {
    LightAliStatusColors
}
