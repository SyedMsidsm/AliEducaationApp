package com.alieducational.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val NotoSans = FontFamily.Default
val NotoNastaliqUrdu = FontFamily.Default

val AliTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = NotoSans,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = NotoSans,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = NotoSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = NotoSans,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = NotoSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = NotoSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = NotoSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = NotoSans,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = NotoSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = NotoSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = NotoSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = NotoSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = NotoSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = NotoSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = NotoSans,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
    ),
)

fun getScaledTypography(scaleFactor: Float): Typography {
    if (scaleFactor == 1.0f) return AliTypography
    return Typography(
        displayLarge = AliTypography.displayLarge.copy(fontSize = (32 * scaleFactor).sp, lineHeight = (40 * scaleFactor).sp),
        displayMedium = AliTypography.displayMedium.copy(fontSize = (28 * scaleFactor).sp, lineHeight = (36 * scaleFactor).sp),
        displaySmall = AliTypography.displaySmall.copy(fontSize = (24 * scaleFactor).sp, lineHeight = (32 * scaleFactor).sp),
        headlineLarge = AliTypography.headlineLarge.copy(fontSize = (28 * scaleFactor).sp, lineHeight = (36 * scaleFactor).sp),
        headlineMedium = AliTypography.headlineMedium.copy(fontSize = (24 * scaleFactor).sp, lineHeight = (32 * scaleFactor).sp),
        headlineSmall = AliTypography.headlineSmall.copy(fontSize = (20 * scaleFactor).sp, lineHeight = (28 * scaleFactor).sp),
        titleLarge = AliTypography.titleLarge.copy(fontSize = (22 * scaleFactor).sp, lineHeight = (28 * scaleFactor).sp),
        titleMedium = AliTypography.titleMedium.copy(fontSize = (18 * scaleFactor).sp, lineHeight = (24 * scaleFactor).sp),
        titleSmall = AliTypography.titleSmall.copy(fontSize = (14 * scaleFactor).sp, lineHeight = (20 * scaleFactor).sp),
        bodyLarge = AliTypography.bodyLarge.copy(fontSize = (16 * scaleFactor).sp, lineHeight = (24 * scaleFactor).sp),
        bodyMedium = AliTypography.bodyMedium.copy(fontSize = (14 * scaleFactor).sp, lineHeight = (20 * scaleFactor).sp),
        bodySmall = AliTypography.bodySmall.copy(fontSize = (12 * scaleFactor).sp, lineHeight = (16 * scaleFactor).sp),
        labelLarge = AliTypography.labelLarge.copy(fontSize = (14 * scaleFactor).sp, lineHeight = (20 * scaleFactor).sp),
        labelMedium = AliTypography.labelMedium.copy(fontSize = (12 * scaleFactor).sp, lineHeight = (16 * scaleFactor).sp),
        labelSmall = AliTypography.labelSmall.copy(fontSize = (10 * scaleFactor).sp, lineHeight = (14 * scaleFactor).sp),
    )
}
