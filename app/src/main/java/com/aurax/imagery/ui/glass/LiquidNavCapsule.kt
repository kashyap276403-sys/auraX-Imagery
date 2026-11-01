package com.aurax.imagery.ui.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurax.imagery.ui.components.GlassNavItem
import com.aurax.imagery.ui.theme.GlassStyle
import com.aurax.imagery.ui.theme.LocalGlassStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Liquid-glass navigation capsule: a near-black rounded base with a transparent glass "lens"
 * floating over the active tab.
 *
 *  - The base is dark glass (heavy Haze blur under ~88% black), so it reads as black but is not
 *    a flat colour.
 *  - The lens is a second backdrop panel with its own, lighter blur, tint and the AGSL shader.
 *    Because it blurs the app content *behind the bar* rather than the black base, it looks like
 *    a clear window cut through the capsule.
 *  - Movement is a spring on a single position value (so it can overshoot and settle). The lens
 *    stretches and flattens with its velocity and swells slightly while pressed, via real layout
 *    size changes (not graphicsLayer scale) so Haze keeps tracking its position every frame.
 *  - Tap a tab, or press and drag the lens and let go; it settles on the nearest tab.
 *
 * Honours [GlassStyle.animationSpeed], [GlassStyle.motionEffects], [GlassStyle.haptics],
 * [GlassStyle.performanceMode] and the system "remove animations" setting.
 */
@Composable
fun LiquidNavCapsule(
    items: List<GlassNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = LocalHazeState.current,
    style: GlassStyle = LocalGlassStyle.current,
) {
    val count = items.size
    if (count == 0) return

    val reducedMotion = rememberReducedMotion()
    val morph = style.motionEffects && !reducedMotion
    val speed = style.animationSpeed.coerceIn(0.25f, 3f)
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentSelected by rememberUpdatedState(selectedIndex)

    val position = remember { Animatable(selectedIndex.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    var pressed by remember { mutableStateOf(false) }
    val settle = remember { mutableIntStateOf(0) }

    val press by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f * speed),
        label = "capsulePress",
    )

    // Spring to the selected tab whenever the selection changes (or a drag is released in place).
    LaunchedEffect(selectedIndex, settle.intValue) {
        if (dragging) return@LaunchedEffect
        val target = selectedIndex.toFloat()
        if (reducedMotion) {
            position.snapTo(target)
        } else {
            position.animateTo(target, spring(dampingRatio = 0.58f, stiffness = 220f * speed))
        }
    }

    val baseShape = RoundedCornerShape(50)
    val lensStyle = remember(style) {
        style.copy(
            // A clear window: barely any blur or fill, and the shader does the visible work.
            blur = (style.blur * 0.2f).coerceAtMost(0.15f),
            opacity = (style.opacity * 0.25f).coerceIn(0.0f, 0.08f),
            distortion = style.distortion.coerceAtLeast(0.55f),
            edgeBrightness = style.edgeBrightness.coerceAtLeast(0.7f),
            cornerRadius = 100.dp,
        )
    }
    val baseRim = remember {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.03f), Color.White.copy(alpha = 0.10f)))
    }
    val lensRim = remember(style.edgeBrightness) {
        val a = (0.35f + 0.6f * style.edgeBrightness).coerceIn(0f, 1f)
        Brush.linearGradient(listOf(Color.White.copy(alpha = a), Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = a * 0.6f)))
    }

    // Selecting runs the haptic tick and always re-triggers the settle spring (even when the
    // lens was dragged and released over the tab that was already selected).
    val commit: (Int) -> Unit = { index ->
        if (style.haptics && index != currentSelected) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        currentOnSelect(index)
        settle.intValue++
    }

    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(72.dp)) {
        val itemWidthPx = constraints.maxWidth / count.toFloat()

        // --- Black base capsule ------------------------------------------------------------
        Box(
            Modifier
                .matchParentSize()
                .clip(baseShape)
                .let { m ->
                    if (hazeState != null) {
                        m.hazeEffect(state = hazeState) {
                            blurRadius = 22.dp
                            noiseFactor = if (style.performanceMode) 0f else 0.03f
                            tints = listOf(HazeTint(Color(0xFF020203).copy(alpha = 0.62f)))
                            backgroundColor = Color.Black
                        }
                    } else {
                        m.background(Color(0xB0030304))
                    }
                }
                .border(1.dp, baseRim, baseShape),
        )

        // --- Floating glass lens -----------------------------------------------------------
        Box(
            Modifier
                .fillMaxSize()
                .layout { measurable, c ->
                    val pad = 5.dp.roundToPx()
                    val baseW = (itemWidthPx - 2 * pad).coerceAtLeast(1f)
                    val baseH = (c.maxHeight - 2 * pad).coerceAtLeast(1)
                    // Squash-and-stretch from speed: faster travel = longer, flatter lens.
                    val stretch = if (morph) (abs(position.velocity) * 0.09f).coerceIn(0f, 0.32f) else 0f
                    val swell = press * 0.07f
                    val w = (baseW * (1f + stretch + swell)).roundToInt().coerceAtLeast(1)
                    val h = (baseH * (1f - stretch * 0.30f + swell * 0.6f)).roundToInt().coerceAtLeast(1)
                    val placeable = measurable.measure(Constraints.fixed(w, h))
                    val x = (itemWidthPx * (position.value + 0.5f) - w / 2f).roundToInt()
                    val y = ((c.maxHeight - h) / 2f).roundToInt()
                    layout(c.maxWidth, c.maxHeight) { placeable.place(x, y) }
                },
        ) {
            val lensShape = RoundedCornerShape(50)
            LiquidBackdrop(
                hazeState = hazeState,
                style = lensStyle,
                shape = lensShape,
                cornerRadius = 100.dp,
            )
            Box(Modifier.matchParentSize().border(1.2.dp, lensRim, lensShape))
        }

        // --- Icons and labels (above the lens) ---------------------------------------------
        Row(
            Modifier
                .fillMaxSize()
                .pointerInput(count, itemWidthPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        pressed = true
                        val slop = awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                        if (slop != null) {
                            // Drag: the lens follows the finger, then settles on the nearest tab.
                            dragging = true
                            var target = (down.position.x / itemWidthPx - 0.5f).coerceIn(0f, count - 1f)
                            scope.launch { position.snapTo(target) }
                            horizontalDrag(slop.id) { change ->
                                change.consume()
                                target = (change.position.x / itemWidthPx - 0.5f).coerceIn(0f, count - 1f)
                                scope.launch { position.snapTo(target) }
                            }
                            dragging = false
                            commit(target.roundToInt().coerceIn(0, count - 1))
                        } else {
                            val up = waitForUpOrCancellation()
                            if (up != null) commit((down.position.x / itemWidthPx).toInt().coerceIn(0, count - 1))
                        }
                        pressed = false
                    }
                },
        ) {
            items.forEachIndexed { index, item ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics {
                            role = Role.Tab
                            selected = index == selectedIndex
                        }
                        .graphicsLayer {
                            // Brightest where the lens is; fades with distance, with no recomposition.
                            val d = abs(position.value - index).coerceAtMost(1f)
                            alpha = 0.55f + 0.45f * (1f - d)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = androidx.compose.material3.LocalContentColor.current,
                        modifier = Modifier.height(24.dp),
                    )
                    Text(text = item.label, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}
