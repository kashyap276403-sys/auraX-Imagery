package com.aurax.imagery.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aurax.imagery.domain.edit.CropAspect
import com.aurax.imagery.domain.edit.CropCorner
import com.aurax.imagery.domain.edit.CropMath
import com.aurax.imagery.domain.edit.CropRect

private sealed interface DragMode {
    data class Corner(val corner: CropCorner) : DragMode
    data object Move : DragMode
}

/**
 * Draws the crop frame over the picture (dimming everything outside it, with thirds guides and
 * corner handles). When [interactive], drag a corner to resize or drag inside to move; the
 * geometry itself lives in the unit-tested [CropMath].
 */
@Composable
fun CropOverlay(
    crop: CropRect,
    aspect: CropAspect,
    imageAspect: Float,
    interactive: Boolean,
    onCropChange: (CropRect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestCrop by rememberUpdatedState(crop)
    val latestAspect by rememberUpdatedState(aspect)
    val latestChange by rememberUpdatedState(onCropChange)
    val hitRadius = with(LocalDensity.current) { 40.dp.toPx() }
    val handleRadius = with(LocalDensity.current) { 7.dp.toPx() }
    val lineWidth = with(LocalDensity.current) { 1.5.dp.toPx() }
    val accent = MaterialTheme.colorScheme.primary

    val gestures = if (interactive) {
        Modifier.pointerInput(imageAspect) {
            var mode: DragMode? = null
            var live = latestCrop
            detectDragGestures(
                onDragStart = { start ->
                    live = latestCrop
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val corners = mapOf(
                        CropCorner.TopLeft to Offset(live.left * w, live.top * h),
                        CropCorner.TopRight to Offset(live.right * w, live.top * h),
                        CropCorner.BottomLeft to Offset(live.left * w, live.bottom * h),
                        CropCorner.BottomRight to Offset(live.right * w, live.bottom * h),
                    )
                    val nearest = corners.minByOrNull { (_, p) -> (p - start).getDistance() }
                    mode = when {
                        nearest != null && (nearest.value - start).getDistance() <= hitRadius -> DragMode.Corner(nearest.key)
                        start.x in live.left * w..live.right * w && start.y in live.top * h..live.bottom * h -> DragMode.Move
                        else -> null
                    }
                },
                onDragEnd = { mode = null },
                onDragCancel = { mode = null },
                onDrag = { change, drag ->
                    val current = mode ?: return@detectDragGestures
                    change.consume()
                    val dx = drag.x / size.width
                    val dy = drag.y / size.height
                    live = when (current) {
                        is DragMode.Corner -> CropMath.dragCorner(
                            live, current.corner, dx, dy, imageAspect, latestAspect.resolve(imageAspect),
                        )
                        DragMode.Move -> CropMath.move(live, dx, dy)
                    }
                    latestChange(live)
                },
            )
        }
    } else {
        Modifier
    }

    Canvas(modifier = modifier.then(gestures)) {
        val l = crop.left * size.width
        val t = crop.top * size.height
        val r = crop.right * size.width
        val b = crop.bottom * size.height
        val dim = Color.Black.copy(alpha = 0.6f)

        // Four rectangles around the crop window.
        drawRect(dim, Offset.Zero, Size(size.width, t))
        drawRect(dim, Offset(0f, b), Size(size.width, size.height - b))
        drawRect(dim, Offset(0f, t), Size(l, b - t))
        drawRect(dim, Offset(r, t), Size(size.width - r, b - t))

        if (interactive || !crop.isFull) {
            drawRect(Color.White, Offset(l, t), Size(r - l, b - t), style = Stroke(lineWidth))
        }
        if (interactive) {
            val guide = Color.White.copy(alpha = 0.35f)
            for (i in 1..2) {
                val x = l + (r - l) * i / 3f
                val y = t + (b - t) * i / 3f
                drawLine(guide, Offset(x, t), Offset(x, b), lineWidth)
                drawLine(guide, Offset(l, y), Offset(r, y), lineWidth)
            }
            listOf(Offset(l, t), Offset(r, t), Offset(l, b), Offset(r, b)).forEach { corner ->
                drawCircle(Color.White, handleRadius, corner)
                drawCircle(accent, handleRadius - lineWidth, corner)
            }
        }
    }
}
