package com.aurax.imagery.ui.viewer

import android.util.LruCache

/**
 * Small in-memory cache of scrub-preview frames, so dragging back over a spot you already passed shows
 * its frame at once instead of decoding it again. Positions are bucketed to [BUCKET_MS] because the
 * retriever snaps to the nearest keyframe anyway. Sized by bytes, so it can never grow past [maxBytes].
 */
class SeekFrameCache<T : Any>(maxBytes: Int, private val sizeOf: (T) -> Int) {
    private val cache = object : LruCache<String, T>(maxBytes.coerceAtLeast(1)) {
        override fun sizeOf(key: String, value: T): Int = sizeOf(value).coerceAtLeast(1)
    }

    @Synchronized
    fun get(uri: String, positionMs: Long): T? = cache.get(key(uri, positionMs))

    @Synchronized
    fun put(uri: String, positionMs: Long, frame: T) {
        cache.put(key(uri, positionMs), frame)
    }

    @Synchronized
    fun clear() = cache.evictAll()

    companion object {
        const val BUCKET_MS = 1_000L
        fun key(uri: String, positionMs: Long) = "$uri#${positionMs.coerceAtLeast(0L) / BUCKET_MS}"
    }
}
