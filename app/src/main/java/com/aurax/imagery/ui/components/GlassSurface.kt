package com.aurax.imagery.ui.components

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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aurax.imagery.ui.glass.LiquidGlassPanel
import com.aurax.imagery.ui.glass.LocalHazeState
import com.aurax.imagery.ui.theme.AuraCyan
import com.aurax.imagery.ui.theme.GlassFamily
import com.aurax.imagery.ui.theme.GlassStyle
import com.aurax.imagery.ui.theme.LocalGlassStyle

/**
 * The base liquid-glass panel. Every other glass component is built on this.
 *
 * Layers, bottom to top: soft shadow/glow -> tinted translucent fill -> top specular
 * highlight -> fine gradient border -> content. It deliberately does not blur what is
 * behind it: blur is applied once per screen by the backdrop renderer (Phase 6), never per
 * list item, which keeps scrolling smooth.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(LocalGlassStyle.current.cornerRadius),
    style: GlassStyle = LocalGlassStyle.current,
    contentAlignment: Alignment = Alignment.TopStart,
    /** Corner radius of [shape], used by the liquid shader's edge maths. */
    cornerRadius: Dp = style.cornerRadius,
    content: @Composable BoxScope.() -> Unit,
) {
    // Inside a backdrop scope (see LocalHazeState) the panel blurs and refracts what is behind it.
    val haze = LocalHazeState.current
    if (haze != null) {
        LiquidGlassPanel(
            modifier = modifier,
            hazeState = haze,
            style = style,
            cornerRadius = cornerRadius,
            shape = shape,
            contentAlignment = contentAlignment,
            content = content,
        )
        return
    }

    val fill = remember(style.tint, style.opacity) {
        Brush.verticalGradient(
            listOf(
                style.tint.copy(alpha = (style.opacity * 1.25f).coerceIn(0f, 1f)),
                style.tint.copy(alpha = (style.opacity * 0.6f).coerceIn(0f, 1f)),
            ),
        )
    }
    val borderBrush = remember(style.reflection, style.borderOpacity) {
        val bo = style.borderOpacity
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = (0.55f * style.reflection * bo).coerceIn(0f, 1f)),
                Color.White.copy(alpha = 0.06f * bo),
                Color.White.copy(alpha = (0.25f * style.reflection * bo).coerceIn(0f, 1f)),
            ),
        )
    }

    var surface = modifier
    if (!style.performanceMode && style.shadow > 0f) {
        surface = surface.shadow(
            elevation = (24f * style.shadow).dp,
            shape = shape,
            clip = false,
            ambientColor = Color.Black,
            // The spot shadow doubles as the accent glow.
            spotColor = AuraCyan.copy(alpha = (0.35f * style.glow).coerceIn(0f, 1f)),
        )
    }
    surface = surface
        .clip(shape)
        .background(fill)
        .drawBehind {
            // Specular highlight: a soft white sheen fading out over the top half.
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = (0.22f * style.reflection).coerceIn(0f, 1f)),
                        Color.Transparent,
                    ),
                    startY = 0f,
                    endY = size.height * 0.5f,
                ),
            )
            if (style.family == GlassFamily.WaterDrop) {
                // Water Drop without a backdrop to refract: a wet inner glow along the bottom edge
                // and a small bright glint near the top-left, scaled by the Water Drop intensity.
                val w = style.waterDrop
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = (0.14f * w).coerceIn(0f, 1f))),
                        startY = size.height * 0.55f,
                        endY = size.height,
                    ),
                )
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = (0.30f * w * style.reflection).coerceIn(0f, 1f)), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.16f),
                        radius = size.minDimension * 0.28f,
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.04f, size.height * 0.02f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.32f, size.height * 0.28f),
                )
            }
        }
    if (style.borderWidth > 0.dp) {
        surface = surface.border(style.borderWidth, borderBrush, shape)
    }

    Box(modifier = surface, contentAlignment = contentAlignment, content = content)
}
