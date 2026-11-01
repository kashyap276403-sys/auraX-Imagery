package com.aurax.imagery.domain.player

/** Which player gestures are switched on (Settings > Video player). */
data class VideoGestureSettings(
    val doubleTapSeek: Boolean = true,
    val brightness: Boolean = true,
    val volume: Boolean = true,
)

enum class TapZone { Left, Center, Right }

/** Left and right thirds seek, the middle third resets zoom. */
fun tapZone(x: Float, width: Float): TapZone = when {
    width <= 0f -> TapZone.Center
    x < width / 3f -> TapZone.Left
    x > width * 2f / 3f -> TapZone.Right
    else -> TapZone.Center
}

/** Playback position after seeking by [deltaMs], kept inside the video. Unknown duration only clamps at 0. */
fun seekTarget(positionMs: Long, deltaMs: Long, durationMs: Long): Long {
    val moved = (positionMs + deltaMs).coerceAtLeast(0L)
    return if (durationMs > 0L) moved.coerceAtMost(durationMs) else moved
}

/**
 * A 0..1 level (brightness or volume) after a vertical drag. Dragging up (negative [dragPx]) raises
 * it; a full swipe over [extentPx] changes it by 1.
 */
fun adjustLevel(level: Float, dragPx: Float, extentPx: Float): Float =
    if (extentPx <= 0f) level else (level - dragPx / extentPx).coerceIn(0f, 1f)

/** Which half of the screen a vertical swipe belongs to: left = brightness, right = volume. */
fun isLeftHalf(x: Float, width: Float): Boolean = x < width / 2f

val PLAYBACK_SPEEDS = listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

fun speedLabel(speed: Float): String =
    (if (speed % 1f == 0f) speed.toInt().toString() else speed.toString().trimEnd('0')) + "x"

/** How the screen orientation is held while a video is open. */
enum class OrientationLock(val label: String) {
    Auto("Auto-rotate"),
    Portrait("Portrait"),
    Landscape("Landscape");

    fun next(): OrientationLock = entries[(ordinal + 1) % entries.size]
}
