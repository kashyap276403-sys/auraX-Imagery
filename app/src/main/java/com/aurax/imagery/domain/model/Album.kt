package com.aurax.imagery.domain.model

/**
 * Identifies an album-like collection. [Bucket] is a real MediaStore folder (Camera,
 * Screenshots, Download...); [Favorites] and [Videos] are smart albums computed from the index.
 */
sealed interface AlbumRef {
    data class Bucket(val id: Long) : AlbumRef
    data object Favorites : AlbumRef
    data object Videos : AlbumRef
}

data class Album(
    val ref: AlbumRef,
    val name: String,
    /** URI of the most recent item, used as the album cover. */
    val coverUri: String?,
    val itemCount: Int,
)
