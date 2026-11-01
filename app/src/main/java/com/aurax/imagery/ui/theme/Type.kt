package com.aurax.imagery.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Base = Typography()

// Light, slightly tracked headings give the "system app" feel; body text stays regular for legibility.
val AuraTypography = Typography(
    headlineLarge = Base.headlineLarge.copy(fontWeight = FontWeight.Light, letterSpacing = (-0.5).sp),
    headlineMedium = Base.headlineMedium.copy(fontWeight = FontWeight.Light, letterSpacing = (-0.25).sp),
    titleLarge = Base.titleLarge.copy(fontWeight = FontWeight.Normal, letterSpacing = 0.sp),
    titleMedium = Base.titleMedium.copy(fontWeight = FontWeight.Medium),
    bodyLarge = Base.bodyLarge,
    bodyMedium = Base.bodyMedium,
    labelLarge = Base.labelLarge.copy(fontWeight = FontWeight.Medium),
    labelMedium = Base.labelMedium.copy(letterSpacing = 0.6.sp),
    labelSmall = Base.labelSmall.copy(letterSpacing = 1.2.sp),
)

/** The app typography with every style in the user's text colour. */
fun auraTypography(color: androidx.compose.ui.graphics.Color): Typography = AuraTypography.copy(
    displayLarge = AuraTypography.displayLarge.copy(color = color),
    displayMedium = AuraTypography.displayMedium.copy(color = color),
    displaySmall = AuraTypography.displaySmall.copy(color = color),
    headlineLarge = AuraTypography.headlineLarge.copy(color = color),
    headlineMedium = AuraTypography.headlineMedium.copy(color = color),
    headlineSmall = AuraTypography.headlineSmall.copy(color = color),
    titleLarge = AuraTypography.titleLarge.copy(color = color),
    titleMedium = AuraTypography.titleMedium.copy(color = color),
    titleSmall = AuraTypography.titleSmall.copy(color = color),
    bodyLarge = AuraTypography.bodyLarge.copy(color = color),
    bodyMedium = AuraTypography.bodyMedium.copy(color = color),
    bodySmall = AuraTypography.bodySmall.copy(color = color),
    labelLarge = AuraTypography.labelLarge.copy(color = color),
    labelMedium = AuraTypography.labelMedium.copy(color = color),
    labelSmall = AuraTypography.labelSmall.copy(color = color),
)
