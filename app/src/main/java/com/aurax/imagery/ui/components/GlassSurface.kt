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
import androidx.compose.ui.unit.dp
import com.aurax.imagery.ui.theme.AuraCyan
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
    content: @Composable BoxScope.() -> Unit,
) {
    val fill = remember(style.tint, style.opacity) {
        Brush.verticalGradient(
            listOf(
                style.tint.copy(alpha = (style.opacity * 1.25f).coerceIn(0f, 1f)),
                style.tint.copy(alpha = (style.opacity * 0.6f).coerceIn(0f, 1f)),
            ),
        )
    }
    val borderBrush = remember(style.reflection) {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = (0.55f * style.reflection).coerceIn(0f, 1f)),
                Color.White.copy(alpha = 0.06f),
                Color.White.copy(alpha = (0.25f * style.reflection).coerceIn(0f, 1f)),
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
        }
    if (style.borderWidth > 0.dp) {
        surface = surface.border(style.borderWidth, borderBrush, shape)
    }

    Box(modifier = surface, contentAlignment = contentAlignment, content = content)
}
