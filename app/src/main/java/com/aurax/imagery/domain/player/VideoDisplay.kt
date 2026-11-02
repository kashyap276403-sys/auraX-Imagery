package com.aurax.imagery.domain.player

import kotlin.math.max
import kotlin.math.min

/** How the video frame is laid out inside the player area. */
enum class ScreenMode(val label: String, val hint: String) {
    Fit("Fit", "Whole video, never cropped"),
    Fill("Fill", "Covers the screen, edges may be cropped"),
    Original("Original", "Native size, never enlarged"),
    Stretch("Stretch", "Fills the screen, picture may be distorted"),
}

/** Frame shape presets. [ratio] is width / height; null keeps the video's own ratio. */
enum class AspectPreset(val label: String, val ratio: Float?) {
    Original("Best fit", null),
    R16x9("16:9", 16f / 9f),
    R16x10("16:10", 16f / 10f),
    R4x3("4:3", 4f / 3f),
    R5x4("5:4", 5f / 4f),
    R1x1("1:1", 1f),
    R221("2.21:1", 2.21f),
    R235("2.35:1", 2.35f),
    R239("2.39:1", 2.39f),
    R18x9("18:9", 18f / 9f),
    R195x9("19.5:9", 19.5f / 9f),
    R20x9("20:9", 20f / 9f),
    R21x9("21:9", 21f / 9f),
}

data class FrameSize(val width: Float, val height: Float)

const val MIN_ZOOM = 1f
const val MAX_ZOOM = 4f

/**
 * Size of the video frame inside a container, all in pixels.
 *
 * - Fit / Fill use the preset's ratio (or the video's own when [ratio] is null). The picture is
 *   stretched into that frame, so a preset different from the real ratio does distort it.
 * - Original keeps the native pixel size and only shrinks when the video is larger than the area.
 * - Stretch always fills the whole container.
 */
fun computeFrame(
    containerWidth: Float,
    containerHeight: Float,
    videoWidth: Float,
    videoHeight: Float,
    mode: ScreenMode,
    ratio: Float?,
): FrameSize {
    if (containerWidth <= 0f || containerHeight <= 0f) return FrameSize(0f, 0f)
    val nativeAspect = if (videoWidth > 0f && videoHeight > 0f) videoWidth / videoHeight else containerWidth / containerHeight
    val aspect = (ratio ?: nativeAspect).takeIf { it > 0f } ?: nativeAspect
    return when (mode) {
        ScreenMode.Stretch -> FrameSize(containerWidth, containerHeight)
        ScreenMode.Fit ->
            if (containerWidth / containerHeight > aspect) FrameSize(containerHeight * aspect, containerHeight)
            else FrameSize(containerWidth, containerWidth / aspect)
        ScreenMode.Fill ->
            if (containerWidth / containerHeight > aspect) FrameSize(containerWidth, containerWidth / aspect)
            else FrameSize(containerHeight * aspect, containerHeight)
        ScreenMode.Original -> {
            if (videoWidth <= 0f || videoHeight <= 0f) {
                FrameSize(containerWidth, containerHeight)
            } else {
                val scale = min(1f, min(containerWidth / videoWidth, containerHeight / videoHeight))
                FrameSize(videoWidth * scale, videoHeight * scale)
            }
        }
    }
}

/** Keeps a panned, zoomed frame covering the container: no empty gap opens on the pan axis. */
fun clampPan(offset: Float, frameExtent: Float, containerExtent: Float, zoom: Float): Float {
    val limit = max(0f, (frameExtent * zoom - containerExtent) / 2f)
    return offset.coerceIn(-limit, limit)
}

fun Float.clampZoom(): Float = coerceIn(MIN_ZOOM, MAX_ZOOM)

/** One-tap cycle used by the ratio button: Fit, Fill, Original, Stretch, then Fit again. */
fun ScreenMode.next(): ScreenMode = ScreenMode.entries[(ordinal + 1) % ScreenMode.entries.size]

/** One-tap zoom cycle: normal, 1.5x, 2x, then back to normal. Any in-between pinch value snaps forward. */
fun nextZoomPreset(zoom: Float): Float = when {
    zoom < 1.25f -> 1.5f
    zoom < 1.75f -> 2f
    else -> 1f
}

/** "100%", "150%" for the temporary zoom indicator. */
fun zoomLabel(zoom: Float): String = "${(zoom * 100).toInt()}%"
