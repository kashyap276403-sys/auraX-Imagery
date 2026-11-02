package com.aurax.imagery.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class GlassMode(val label: String) {
    Natural("Natural Glass"),
    PureLiquid("Pure Liquid Glass"),
    FullTransparent("Full Transparent Glass"),
}

/** Which glass look is drawn. Both families share every other setting. */
enum class GlassFamily(val label: String, val hint: String) {
    Classic("Classic Liquid Glass", "Frosted, translucent, soft blur and fine borders"),
    WaterDrop("Water Drop Liquid Glass", "Glossy, lens-like, stronger refraction and moving highlights"),
}

/**
 * Every tunable property of the glass look, in one immutable value.
 *
 * All glass components read this through [LocalGlassStyle], so Glass Studio (Phase 6) only has
 * to provide a different [GlassStyle] at the root for the whole app to change.
 *
 * [blur], [distortion] (refraction), [edgeBrightness] and [motionEffects] drive the backdrop
 * engine in `ui.glass` (Haze blur + AGSL shader); the rest are also used by the base GlassSurface.
 */
@Immutable
data class GlassStyle(
    val mode: GlassMode = GlassMode.Natural,
    val family: GlassFamily = GlassFamily.Classic,
    /** Water Drop only: how strong the lens bulge, gloss and caustic light are, 0..1. */
    val waterDrop: Float = 0.6f,
    /** Scales the opacity of every glass border and rim, 0..1. 1 keeps the original look. */
    val borderOpacity: Float = 1f,
    /** Backdrop blur strength, 0..1. */
    val blur: Float = 0.6f,
    /** Opacity of the glass fill, 0..1 (lower = more transparent). */
    val opacity: Float = 0.18f,
    val tint: Color = Color.White,
    /** Strength of the specular highlight and border sheen, 0..1. */
    val reflection: Float = 0.6f,
    val borderWidth: Dp = 1.dp,
    val cornerRadius: Dp = 28.dp,
    /** Drop-shadow strength, 0..1. */
    val shadow: Float = 0.5f,
    /** Accent glow strength, 0..1. */
    val glow: Float = 0.3f,
    /** Liquid refraction / edge distortion, 0..1. */
    val distortion: Float = 0.3f,
    /** Brightness of the light caught along the glass edge, 0..1. */
    val edgeBrightness: Float = 0.6f,
    /** Multiplier for spring/transition speed. 1 = normal. */
    val animationSpeed: Float = 1f,
    /** Colour of all primary text and icons in the app. */
    val textColor: Color = AuraTextPrimary,
    /** Colour of icons, independent of the text colour. */
    val iconColor: Color = AuraTextPrimary,
    /** Opacity of secondary text (captions, hints) relative to [textColor], 0.3..1. */
    val secondaryTextOpacity: Float = 0.72f,
    val haptics: Boolean = true,
    val motionEffects: Boolean = true,
    /** Disables shadows and glow on slower devices. */
    val performanceMode: Boolean = false,
) {
    companion object {
        val Natural = GlassStyle()

        val PureLiquid = GlassStyle(
            mode = GlassMode.PureLiquid,
            blur = 0.85f,
            opacity = 0.10f,
            reflection = 0.9f,
            borderWidth = 1.2.dp,
            cornerRadius = 32.dp,
            shadow = 0.4f,
            glow = 0.55f,
            distortion = 0.65f,
            edgeBrightness = 0.9f,
        )

        val FullTransparent = GlassStyle(
            mode = GlassMode.FullTransparent,
            blur = 0.25f,
            opacity = 0.04f,
            reflection = 0.45f,
            borderWidth = 0.8.dp,
            cornerRadius = 30.dp,
            shadow = 0.2f,
            glow = 0.15f,
            distortion = 0.1f,
            edgeBrightness = 0.45f,
        )
    }
}

/** The baseline style of each mode; selecting a mode in Glass Studio starts from this. */
fun GlassMode.baseStyle(): GlassStyle = when (this) {
    GlassMode.Natural -> GlassStyle.Natural
    GlassMode.PureLiquid -> GlassStyle.PureLiquid
    GlassMode.FullTransparent -> GlassStyle.FullTransparent
}

val LocalGlassStyle = staticCompositionLocalOf { GlassStyle.Natural }
