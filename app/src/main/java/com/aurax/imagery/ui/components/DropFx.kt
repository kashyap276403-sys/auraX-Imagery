package com.aurax.imagery.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import com.aurax.imagery.ui.glass.rememberReducedMotion
import com.aurax.imagery.ui.theme.LocalGlassStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * The tap animation used on buttons across the app: a quick small dip and a soft spring back, with a
 * faint water ring spreading out. Kept subtle on purpose; the button never disappears.
 *
 * It is switched off when the system's "remove animations" is on or Motion effects is off in
 * Glass Studio. All values live in [Animatable]s read inside draw/graphics lambdas, so playing it
 * redraws the button only and never recomposes anything.
 */
class DropFx internal constructor(private val enabled: Boolean) {
    /** 1 = normal size. Dips toward [play]'s floor and springs back. */
    val scale = Animatable(1f)

    /** 0..1 progress of the water rings; 1 means finished, nothing is drawn. */
    val ring = Animatable(1f)

    /** [floor] is how small the button gets: 0 makes it vanish, 0.8 is just a dip. */
    suspend fun play(floor: Float = 0.88f) {
        if (!enabled) return
        coroutineScope {
            launch {
                ring.snapTo(0f)
                ring.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
            }
            launch {
                scale.animateTo(floor, tween(70, easing = FastOutSlowInEasing))
                scale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 500f))
            }
        }
    }

    fun launchIn(scope: CoroutineScope, floor: Float = 0.88f) {
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

/** Two water rings spreading from the centre. Draws outside the node's bounds, so keep it off clipped parents. */
fun Modifier.dropRing(fx: DropFx, color: Color = Color.White): Modifier = drawBehind {
    val p = fx.ring.value
    if (p >= 1f) return@drawBehind
    val base = size.minDimension / 2f
    val centre = Offset(size.width / 2f, size.height / 2f)
    // A soft droplet that swells and fades behind the button.
    drawCircle(color.copy(alpha = 0.08f * (1f - p)), radius = base * (0.5f + 0.9f * p), center = centre)
    drawCircle(
        color.copy(alpha = 0.32f * (1f - p)),
        radius = base * (0.55f + 1.15f * p),
        center = centre,
        style = Stroke(width = (3f * (1f - p)).coerceAtLeast(0.5f) * density),
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
