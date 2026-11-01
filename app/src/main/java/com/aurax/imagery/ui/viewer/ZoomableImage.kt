package com.aurax.imagery.ui.viewer

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.aurax.imagery.domain.model.MediaItem
import kotlinx.coroutines.launch

private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f

/**
 * A photo you can pinch, pan and double-tap.
 *
 * The gesture handler only consumes touches while two fingers are down or the image is
 * already zoomed in. At 1x a single finger is left alone, so the surrounding pager can still
 * swipe between photos and the viewer can still be dragged down to close.
 */
@Composable
fun ZoomableImage(
    item: MediaItem,
    isCurrent: Boolean,
    onZoomChanged: (Boolean) -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()
    val zoomed = scale > 1.01f

    LaunchedEffect(zoomed) { onZoomChanged(zoomed) }
    // A page that scrolled out of view comes back un-zoomed.
    LaunchedEffect(isCurrent) {
        if (!isCurrent) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().clipToBounds()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        /** Keeps the zoomed image from being dragged past its own edges. */
        fun clamp(value: Offset, atScale: Float): Offset {
            val maxX = (width * atScale - width) / 2f
            val maxY = (height * atScale - height) / 2f
            return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(width, height) {
                    detectTapGestures(
                        onTap = { onTap() },
                        onDoubleTap = { tap ->
                            scope.launch {
                                val startScale = scale
                                val startOffset = offset
                                val targetScale = if (scale > 1.01f) 1f else DOUBLE_TAP_SCALE
                                // Zoom towards the tapped point; zooming out recentres.
                                val center = Offset(width / 2f, height / 2f)
                                val targetOffset = if (targetScale == 1f) {
                                    Offset.Zero
                                } else {
                                    clamp((center - tap) * (targetScale - 1f), targetScale)
                                }
                                animate(0f, 1f, animationSpec = tween(220)) { t, _ ->
                                    scale = startScale + (targetScale - startScale) * t
                                    offset = startOffset + (targetOffset - startOffset) * t
                                }
                            }
                        },
                    )
                }
                .pointerInput(width, height) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val multiTouch = event.changes.count { it.pressed } > 1
                            if (multiTouch || scale > 1f) {
                                val zoomChange = event.calculateZoom()
                                val pan = event.calculatePan()
                                val centroid = event.calculateCentroid(useCurrent = false)
                                val newScale = (scale * zoomChange).coerceIn(1f, MAX_SCALE)
                                if (newScale == 1f) {
                                    offset = Offset.Zero
                                } else {
                                    // Keep the point between the fingers fixed while zooming.
                                    val around = if (centroid.isSpecified) centroid - Offset(width / 2f, height / 2f) else Offset.Zero
                                    val ratio = newScale / scale
                                    offset = clamp((offset - around) * ratio + around + pan, newScale)
                                }
                                scale = newScale
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.uri)
                    // Cap decode size: sharp enough to zoom, safe on 100 MP photos.
                    .size(3072, 3072)
                    .build(),
                contentDescription = item.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
