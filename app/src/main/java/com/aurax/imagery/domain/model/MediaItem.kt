package com.aurax.imagery.domain.model

enum class MediaType { Image, Video }

/**
 * A single photo or video, as the rest of the app sees it.
 *
 * The domain layer is deliberately free of Android types: [uri] is the content URI as a
 * string, so use cases and tests run on the plain JVM.
 */
data class MediaItem(
    val id: Long,
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val type: MediaType,
    val dateTakenMillis: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    /** Only set for videos. */
    val durationMillis: Long? = null,
    val albumId: Long? = null,
    val albumName: String? = null,
    val isFavorite: Boolean = false,
)
