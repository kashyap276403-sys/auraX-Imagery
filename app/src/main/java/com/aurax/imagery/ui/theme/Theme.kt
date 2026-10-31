package com.aurax.imagery.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val AuraColorScheme = darkColorScheme(
    primary = AuraCyan,
    onPrimary = AuraBlack,
    secondary = AuraViolet,
    onSecondary = AuraBlack,
    background = AuraBlack,
    onBackground = AuraTextPrimary,
    surface = AuraInk,
    onSurface = AuraTextPrimary,
    surfaceVariant = AuraSurface,
    onSurfaceVariant = AuraTextSecondary,
    outline = AuraOutline,
)

/** auraX is dark-only by design: the glass look depends on a true-black base. */
@Composable
fun AuraXTheme(
    glassStyle: GlassStyle = GlassStyle.Natural,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalGlassStyle provides glassStyle) {
        MaterialTheme(
            colorScheme = AuraColorScheme,
            typography = AuraTypography,
            content = content,
        )
    }
}
