package com.aurax.imagery.domain.player

import java.util.Locale

const val MIN_AB_GAP_MS = 500L

/** A-B repeat range. B can only be set after A, and must come at least [MIN_AB_GAP_MS] later. */
data class AbRange(val startMs: Long? = null, val endMs: Long? = null) {
    val isActive: Boolean get() = startMs != null && endMs != null

    fun withStart(positionMs: Long): AbRange =
        if (endMs != null && positionMs > endMs - MIN_AB_GAP_MS) AbRange(positionMs, null) else copy(startMs = positionMs)

    fun withEnd(positionMs: Long): AbRange {
        val start = startMs ?: return this
        return if (positionMs >= start + MIN_AB_GAP_MS) copy(endMs = positionMs) else this
    }

    /** Where to jump when playback has reached B; null while playing normally. */
    fun loopTarget(positionMs: Long): Long? {
        val start = startMs ?: return null
        val end = endMs ?: return null
        return if (positionMs >= end) start else null
    }
}

enum class RepeatMode(val label: String) {
    Off("Repeat off"),
    One("Repeat one"),
    Queue("Repeat queue");

    fun next(): RepeatMode = entries[(ordinal + 1) % entries.size]
}

/**
 * The video to play after/before [currentId] in [queue]. Null at the ends (unless [wrap]) or when
 * there is nothing else to play. If the current video is no longer in the queue, the first
 * (forward) or last (backward) entry is used.
 */
fun queueStep(queue: List<Long>, currentId: Long, forward: Boolean, wrap: Boolean): Long? {
    if (queue.isEmpty()) return null
    val index = queue.indexOf(currentId)
    if (index < 0) return if (forward) queue.first() else queue.last()
    if (queue.size == 1) return null
    val target = if (forward) index + 1 else index - 1
    return when {
        target in queue.indices -> queue[target]
        wrap -> queue[(target + queue.size) % queue.size]
        else -> null
    }
}

fun channelLabel(count: Int): String? = when {
    count <= 0 -> null
    count == 1 -> "Mono"
    count == 2 -> "Stereo"
    count == 6 -> "5.1"
    count == 8 -> "7.1"
    else -> "$count ch"
}

/** Name for a track in a picker: its own label, else the language name, else "Audio 2" style. */
fun trackTitle(label: String?, language: String?, index: Int, kind: String): String {
    label?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    val lang = language?.trim()?.takeIf { it.isNotEmpty() && it != "und" }
        ?.let { Locale.forLanguageTag(it).getDisplayLanguage(Locale.ENGLISH) }
        ?.takeIf { it.isNotEmpty() }
    return lang ?: "$kind ${index + 1}"
}

enum class SubtitleSize(val label: String, val scale: Float) {
    Small("Small", 0.8f),
    Medium("Medium", 1f),
    Large("Large", 1.3f),
    ExtraLarge("Extra large", 1.7f),
}

enum class SubtitlePosition(val label: String, val bottomDp: Int) {
    Low("Low", 28),
    Middle("Middle", 96),
    High("High", 170),
}

data class Bookmark(
    val id: Long,
    val mediaId: Long,
    val positionMs: Long,
    val name: String,
    val createdAt: Long,
)
