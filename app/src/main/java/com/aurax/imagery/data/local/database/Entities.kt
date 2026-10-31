package com.aurax.imagery.data.local.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Cached snapshot of one MediaStore row. The index exists so the UI never queries MediaStore
 * directly: lists render from Room instantly and a background worker keeps it in sync.
 */
@Entity(tableName = "media", indices = [Index("dateTaken"), Index("bucketId")])
data class MediaEntity(
    /** MediaStore `_ID`; unique across images and videos because we query the Files table. */
    @PrimaryKey val id: Long,
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val isVideo: Boolean,
    val dateTaken: Long,
    val dateModified: Long,
    val sizeBytes: Long,
    val width: Int,
    val height: Int,
    /** 0 for images. */
    val durationMs: Long,
    val bucketId: Long,
    val bucketName: String,
)

/**
 * Favorites live in their own table, keyed by MediaStore id, so re-indexing the library
 * (which rewrites `media`) can never lose them.
 */
@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val mediaId: Long,
    val addedAt: Long,
)

/** A media row joined with its favorite flag. */
data class MediaRow(
    @Embedded val media: MediaEntity,
    val isFavorite: Boolean,
)

/** Aggregated album (MediaStore bucket) row. */
data class AlbumRow(
    val id: Long,
    val name: String,
    val itemCount: Int,
    val coverUri: String?,
)

/** Count and cover of a smart album (Favorites, Videos). */
data class SummaryRow(
    val itemCount: Int,
    val coverUri: String?,
)
