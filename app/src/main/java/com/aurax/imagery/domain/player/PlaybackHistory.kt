package com.aurax.imagery.domain.player

import com.aurax.imagery.domain.model.MediaItem

/** Where a video stopped last time. One row per video; also acts as the "recently played" history. */
data class PlaybackProgress(
    val mediaId: Long,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedAt: Long,
)

data class RecentVideo(
    val item: MediaItem,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedAt: Long,
) {
    /** Resume point if this video was left unfinished, else null. */
    val resumeAt: Long? get() = resumePosition(positionMs, durationMs)

    /** 0..1 progress for the little bar on a card. */
    val fraction: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

private const val MIN_RESUME_MS = 5_000L
private const val END_MARGIN_MS = 10_000L
private const val FINISHED_FRACTION = 0.95f

/**
 * The position to resume from, or null when there is nothing to resume: barely started (under
 * 5 s), or finished (within 10 s of the end, or past 95%). Finished videos play from the start.
 */
fun resumePosition(positionMs: Long, durationMs: Long): Long? {
    if (positionMs < MIN_RESUME_MS) return null
    if (durationMs <= 0L) return positionMs
    if (durationMs - positionMs <= END_MARGIN_MS) return null
    if (positionMs >= durationMs * FINISHED_FRACTION) return null
    return positionMs
}

/** Sleep timer choices in minutes. */
val SLEEP_TIMER_MINUTES = listOf(5, 10, 15, 30, 60)

/** "12:05" / "1:02:05" style countdown for the sleep timer sheet. */
fun countdownLabel(remainingMs: Long): String {
    val total = (remainingMs.coerceAtLeast(0L) + 999L) / 1000L
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** Aspect ratio safe for Picture-in-Picture, which only accepts roughly 1:2.39 to 2.39:1. */
fun pipAspect(width: Float, height: Float): Float {
    val raw = if (width > 0f && height > 0f) width / height else 16f / 9f
    return raw.coerceIn(1f / 2.39f, 2.39f)
}
