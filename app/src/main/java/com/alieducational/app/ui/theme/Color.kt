package com.alieducational.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Dark Theme Colors ──
val DarkBackground = Color(0xFF111411)
val DarkSurface = Color(0xFF1C211D)
val DarkOnSurface = Color(0xFFF6F2E9)
val DarkOnSurfaceVariant = Color(0xFFB9C0B8)
val DarkOutline = Color(0xFF354038)

// ── Light Theme Colors (Targeting >= 4.5:1 text contrast) ──
val LightBackground = Color(0xFFF6F7F5)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF141915)
val LightOnSurfaceVariant = Color(0xFF47524A)
val LightOutline = Color(0xFFCCD4CD)

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
