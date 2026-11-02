package com.aurax.imagery.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import com.aurax.imagery.ui.theme.AuraMotion
import androidx.compose.ui.graphics.graphicsLayer
import com.aurax.imagery.ui.glass.rememberReducedMotion
import com.aurax.imagery.ui.theme.LocalGlassStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Tap feedback used on buttons across the app. The button eases in to a slightly smaller size and
 * a soft halo of light blooms behind it and fades, then it settles back with a gentle spring: no
 * bounce, no disappearing. Off when the system's "remove animations" is on or Motion effects is
 * off in Glass Studio. Values live in [Animatable]s read in draw/graphics lambdas, so playing it
 * never recomposes anything.
 */
class DropFx internal constructor(private val enabled: Boolean) {
    /** 1 = normal size. */
    val scale = Animatable(1f)

    /** 0..1 progress of the halo; 1 means finished and nothing is drawn. */
    val ring = Animatable(1f)

    suspend fun play(floor: Float = 0.92f) {
        if (!enabled) return
        coroutineScope {
            launch {
                ring.snapTo(0f)
                ring.animateTo(1f, tween(520, easing = AuraMotion.Enter))
            }
            launch {
                scale.animateTo(floor, tween(90, easing = AuraMotion.Enter))
                scale.animateTo(1f, AuraMotion.settle())
            }
        }
    }

    fun launchIn(scope: CoroutineScope, floor: Float = 0.92f) {
        scope.launch { play(floor) }
    }
}

@Composable
fun rememberDropFx(): DropFx {
    val reduced = rememberReducedMotion()
    val motion = LocalGlassStyle.current.motionEffects
    val enabled = !reduced && motion
    return remember(enabled) { DropFx(enabled) }
}

/** Applies the shrink-and-return to the node and, below 0.6 scale, fades it too. */
fun Modifier.dropScale(fx: DropFx): Modifier = graphicsLayer {
    val s = fx.scale.value
    scaleX = s
    scaleY = s
}

/** A soft halo that blooms from the centre and fades. Draws outside the node's bounds, so keep it off clipped parents. */
fun Modifier.dropRing(fx: DropFx, color: Color = Color.White): Modifier = drawBehind {
    val p = fx.ring.value
    if (p >= 1f) return@drawBehind
    val base = size.minDimension / 2f
    val centre = Offset(size.width / 2f, size.height / 2f)
    val radius = base * (0.9f + 0.55f * p)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.20f * (1f - p)), color.copy(alpha = 0.10f * (1f - p)), Color.Transparent),
            center = centre,
            radius = radius,
        ),
        radius = radius,
        center = centre,
    )
}

/** Drop-in replacement for [IconButton] with the water-drop tap animation. */
@Composable
fun DropIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val fx = rememberDropFx()
    val scope = rememberCoroutineScope()
    Box(modifier = modifier.dropRing(fx)) {
        IconButton(
            onClick = {
                fx.launchIn(scope)
                onClick()
            },
            modifier = Modifier.dropScale(fx),
            enabled = enabled,
            content = content,
        )
    }
}
