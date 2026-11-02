package com.aurax.imagery.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aurax.imagery.ui.theme.AuraCyan
import com.aurax.imagery.ui.theme.GlassStyle
import com.aurax.imagery.ui.theme.LocalGlassStyle
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/** Largest blur radius the "Blur intensity" slider can reach. */
private const val MAX_BLUR_DP = 40f

/** Opacity of the glass tint at the slider's most opaque end. Kept low so the backdrop always shows through. */
const val MAX_GLASS_OPACITY = 0.6f

/**
 * The backdrop layer of a glass surface: real blur of whatever is behind it, a translucent
 * colour tint over that blur, and (unless performance mode is on) the AGSL liquid shader.
 *
 * Fills its parent ([BoxScope.matchParentSize]) and must be a sibling of, never inside, the
 * content marked `hazeSource`. Without a [hazeState] it degrades to a translucent tint, and if
 * the shader cannot be created it degrades to plain blurred glass.
 *
 * Layer order, inside out: Haze draws blurred backdrop + tint, then the shader (a RenderEffect
 * on the layer containing that drawing) refracts it, then the clip rounds it.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun BoxScope.LiquidBackdrop(
    hazeState: HazeState?,
    style: GlassStyle,
    shape: Shape,
    cornerRadius: Dp,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val tilt = LocalGlassTilt.current
    // One compiled shader per surface, reused for every frame. Null if AGSL compilation fails.
    val shader = remember { runCatching { LiquidShader() }.getOrNull() }
    val useShader = shader != null && !style.performanceMode
    val radiusPx = with(density) { cornerRadius.toPx() }

    val fillAlpha = style.opacity.coerceIn(0f, MAX_GLASS_OPACITY)
    val blurRadius = (1f + (MAX_BLUR_DP - 1f) * style.blur.coerceIn(0f, 1f)).dp

    var layer = modifier.matchParentSize().clip(shape)
    if (hazeState == null) {
        // No backdrop to sample: honest fallback is a see-through tint, not a fake blur.
        Box(layer.background(Brush.verticalGradient(listOf(
            style.tint.copy(alpha = (fillAlpha * 1.25f).coerceIn(0f, 1f)),
            style.tint.copy(alpha = (fillAlpha * 0.6f).coerceIn(0f, 1f)),
        ))))
        return
    }

    if (useShader) {
        layer = layer.graphicsLayer {
            if (size.minDimension <= 0f) return@graphicsLayer
            val t = tilt?.value
            renderEffect = shader!!.effect(
                width = size.width,
                height = size.height,
                radiusPx = radiusPx,
                refraction = style.distortion,
                edgeBrightness = style.edgeBrightness,
                reflection = style.reflection,
                tiltX = t?.x ?: 0f,
                tiltY = t?.y ?: 0f,
            )
        }
    }

    Box(
        layer.hazeEffect(state = hazeState) {
            this.blurRadius = blurRadius
            noiseFactor = if (style.performanceMode) 0f else 0.04f
            tints = listOf(HazeTint(style.tint.copy(alpha = fillAlpha)))
            backgroundColor = Color.Black
            // Blurring a half-resolution copy is visually identical at large radii and much cheaper.
            inputScale = when {
                style.performanceMode -> HazeInputScale.Fixed(0.33f)
                blurRadius >= 16.dp -> HazeInputScale.Fixed(0.5f)
                else -> HazeInputScale.None
            }
        },
    )
}

/**
 * A complete liquid-glass panel: shadow/glow, backdrop (blur + shader), a fine gradient rim,
 * then [content]. Use for surfaces that sit on top of a `hazeSource`.
 */
@Composable
fun LiquidGlassPanel(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = LocalHazeState.current,
    style: GlassStyle = LocalGlassStyle.current,
    cornerRadius: Dp = style.cornerRadius,
    shape: Shape = RoundedCornerShape(cornerRadius),
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    var outer = modifier
    if (!style.performanceMode && style.shadow > 0f) {
        outer = outer.shadow(
            elevation = (24f * style.shadow).dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black,
            spotColor = AuraCyan.copy(alpha = (0.35f * style.glow).coerceIn(0f, 1f)),
        )
    }
    val rim = remember(style.reflection, style.edgeBrightness, style.borderOpacity) {
        val a = (0.35f + 0.5f * style.edgeBrightness) * style.reflection.coerceAtLeast(0.2f) * style.borderOpacity
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = a.coerceIn(0f, 1f)),
                Color.White.copy(alpha = 0.05f * style.borderOpacity),
                Color.White.copy(alpha = (a * 0.5f).coerceIn(0f, 1f)),
            ),
        )
    }

    Box(modifier = outer, contentAlignment = contentAlignment) {
        LiquidBackdrop(hazeState, style, shape, cornerRadius)
        if (style.borderWidth > 0.dp) {
            Box(Modifier.matchParentSize().border(style.borderWidth, rim, shape))
        }
        content()
    }
}
