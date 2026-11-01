package com.aurax.imagery.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.LocalContentColor
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

/** AuraX is dark-only by design: the glass look depends on a true-black base. */
@Composable
fun AuraXTheme(
    glassStyle: GlassStyle = GlassStyle.Natural,
    content: @Composable () -> Unit,
) {
    // Text colour and icon colour are separate settings. LocalContentColor is always set, because
    // plain Text/Icon outside a Surface would otherwise fall back to black on black.
    val text = glassStyle.textColor
    val colorScheme = AuraColorScheme.copy(
        onBackground = text,
        onSurface = text,
        onSurfaceVariant = text.copy(alpha = glassStyle.secondaryTextOpacity),
    )
    CompositionLocalProvider(
        LocalGlassStyle provides glassStyle,
        // Text takes its colour from the typography; LocalContentColor is what icons default to.
        LocalContentColor provides glassStyle.iconColor,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = auraTypography(text),
            content = content,
        )
    }
}
