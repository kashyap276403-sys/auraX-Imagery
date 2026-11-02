package com.aurax.imagery.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import com.aurax.imagery.ui.glass.rememberReducedMotion
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.aurax.imagery.ui.glass.MAX_GLASS_OPACITY
import com.aurax.imagery.ui.theme.GlassFamily
import com.aurax.imagery.ui.theme.GlassStyle
import com.aurax.imagery.ui.theme.LocalGlassStyle

/*
 * Menus, dialogs and sheets are separate windows, so there is no backdrop to blur behind them.
 * They get the look of the selected glass style instead: its corner radius, tint, transparency
 * and border opacity, plus a wet rim and a gloss on Water Drop. This file is the one place that
 * turns a [GlassStyle] into those window colours, so every menu and dialog stays consistent.
 */

/** Body colour of a popup window. Classic keeps the original near-black; Water Drop is a little clearer and tinted. */
fun GlassStyle.windowColor(): Color {
    val dark = Color(0xFF0B0B10)
    return when (family) {
        GlassFamily.Classic -> dark.copy(alpha = 0.96f)
        GlassFamily.WaterDrop -> {
            // Higher "transparency" lowers the alpha, but never so far that text loses its contrast.
            val clarity = (opacity / MAX_GLASS_OPACITY).coerceIn(0f, 1f)
            lerp(dark, tint, 0.05f + 0.06f * waterDrop).copy(alpha = 0.80f + 0.16f * clarity)
        }
    }
}

/** Gradient used for the border of popup windows, scaled by Border opacity. */
fun GlassStyle.windowRim(): Brush {
    val bo = borderOpacity
    return when (family) {
        GlassFamily.Classic -> Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f * bo), Color.White.copy(alpha = 0.10f * bo)))
        GlassFamily.WaterDrop -> Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = (0.42f + 0.25f * waterDrop) * bo),
                Color.White.copy(alpha = 0.05f * bo),
                Color.White.copy(alpha = 0.22f * bo),
            ),
        )
    }
}

/** Water Drop gloss painted over a popup body: a bright glint top-left and a wet glow along the bottom. No-op for Classic. */
fun Modifier.glassGloss(style: GlassStyle): Modifier =
    if (style.family != GlassFamily.WaterDrop || style.performanceMode) this else drawBehind {
        val w = style.waterDrop
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.10f * w * style.reflection), Color.Transparent),
                startY = 0f,
                endY = size.height * 0.35f,
            ),
        )
        drawOval(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = (0.22f * w * style.reflection).coerceIn(0f, 1f)), Color.Transparent),
                center = Offset(size.width * 0.18f, size.height * 0.08f),
                radius = size.minDimension.coerceAtMost(size.width) * 0.30f,
            ),
            topLeft = Offset(size.width * 0.02f, 0f),
            size = Size(size.width * 0.34f, size.height * 0.16f),
        )
    }

/** The app's popup menu (three-dot menus and any nested menu): style-aware shape, colour and border. */
@Composable
fun GlassDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val style = LocalGlassStyle.current
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.glassGloss(style),
        shape = RoundedCornerShape((style.cornerRadius * 0.6f).coerceIn(14.dp, 28.dp)),
        containerColor = style.windowColor(),
        border = if (style.borderWidth > 0.dp) BorderStroke(style.borderWidth, style.windowRim()) else null,
    ) {
        // Opens like a drop landing: grows from the top-right corner (where the three-dot button is) with a springy overshoot.
        val reduced = rememberReducedMotion()
        val open = remember { Animatable(if (reduced || !style.motionEffects) 1f else 0f) }
        LaunchedEffect(Unit) { if (open.value < 1f) open.animateTo(1f, spring(dampingRatio = 0.58f, stiffness = 420f)) }
        Column(
            Modifier.graphicsLayer {
                val v = open.value
                val s = 0.8f + 0.2f * v
                scaleX = s
                scaleY = s
                alpha = (v * 1.6f).coerceIn(0f, 1f)
                transformOrigin = TransformOrigin(1f, 0f)
            },
        ) { content() }
    }
}

/** The app's dialog: same corner radius and body colour as sheets and menus. */
@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
) {
    val style = LocalGlassStyle.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        modifier = modifier.glassGloss(style),
        dismissButton = dismissButton,
        title = title,
        text = text,
        shape = RoundedCornerShape(style.cornerRadius.coerceIn(20.dp, 36.dp)),
        containerColor = style.windowColor(),
    )
}
