package com.aurax.imagery.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MediaDao {

    @Query(
        """
        SELECT m.*, (f.mediaId IS NOT NULL) AS isFavorite
        FROM media AS m
        LEFT JOIN favorites AS f ON f.mediaId = m.id
        ORDER BY m.dateTaken DESC
        """,
    )
    abstract fun observeMedia(): Flow<List<MediaRow>>

    @Query(
        """
        SELECT m.*, (f.mediaId IS NOT NULL) AS isFavorite
        FROM media AS m
        LEFT JOIN favorites AS f ON f.mediaId = m.id
        WHERE m.bucketId = :bucketId
        ORDER BY m.dateTaken DESC
        """,
    )
    abstract fun observeBucketMedia(bucketId: Long): Flow<List<MediaRow>>

    @Query(
        """
        SELECT m.*, (f.mediaId IS NOT NULL) AS isFavorite
        FROM media AS m
        LEFT JOIN favorites AS f ON f.mediaId = m.id
        WHERE m.isVideo = 1
        ORDER BY m.dateTaken DESC
        """,
    )
    abstract fun observeVideos(): Flow<List<MediaRow>>

    @Query(
        """
        SELECT m.*, (f.mediaId IS NOT NULL) AS isFavorite
        FROM media AS m
        LEFT JOIN favorites AS f ON f.mediaId = m.id
        WHERE m.dateAdded >= :sinceMs
        ORDER BY m.dateAdded DESC
        LIMIT 500
        """,
    )
    abstract fun observeRecentlyAdded(sinceMs: Long): Flow<List<MediaRow>>

    @Query(
        """
        SELECT m.*, (f.mediaId IS NOT NULL) AS isFavorite
        FROM media AS m
        LEFT JOIN favorites AS f ON f.mediaId = m.id
        WHERE m.id IN (:ids)
        """,
    )
    abstract fun observeByIds(ids: List<Long>): Flow<List<MediaRow>>

    @Query(
        """
        SELECT m.*, 1 AS isFavorite
        FROM media AS m
        INNER JOIN favorites AS f ON f.mediaId = m.id
        ORDER BY f.addedAt DESC
        """,
    )
    abstract fun observeFavorites(): Flow<List<MediaRow>>

    @Query(
        """
        SELECT m.bucketId AS id,
               m.bucketName AS name,
               COUNT(*) AS itemCount,
               (SELECT c.uri FROM media AS c WHERE c.bucketId = m.bucketId ORDER BY c.dateTaken DESC LIMIT 1) AS coverUri
        FROM media AS m
        GROUP BY m.bucketId
        ORDER BY name COLLATE NOCASE
        """,
    )
    abstract fun observeAlbums(): Flow<List<AlbumRow>>

    @Query(
        """
        SELECT COUNT(*) AS itemCount,
               (SELECT v.uri FROM media AS v INNER JOIN favorites AS w ON w.mediaId = v.id ORDER BY w.addedAt DESC LIMIT 1) AS coverUri
        FROM media AS m
        INNER JOIN favorites AS f ON f.mediaId = m.id
        """,
    )
    abstract fun observeFavoritesSummary(): Flow<SummaryRow>

    @Query(
        """
        SELECT COUNT(*) AS itemCount,
               (SELECT v.uri FROM media AS v WHERE v.isVideo = 1 ORDER BY v.dateTaken DESC LIMIT 1) AS coverUri
        FROM media AS m
        WHERE m.isVideo = 1
        """,
    )
    abstract fun observeVideosSummary(): Flow<SummaryRow>

    @Query(
        """
        SELECT COUNT(*) AS total,
               COALESCE(SUM(CASE WHEN isVideo = 1 THEN 1 ELSE 0 END), 0) AS videos,
               COALESCE(SUM(sizeBytes), 0) AS bytes
        FROM media
        """,
    )
    abstract fun observeStats(): Flow<StatsRow>

    @Query("DELETE FROM favorites")
    abstract suspend fun clearFavorites()

    @Upsert
    abstract suspend fun upsertAll(items: List<MediaEntity>)

    @Query("SELECT id FROM media")
    abstract suspend fun getAllIds(): List<Long>

    @Query("DELETE FROM media WHERE id IN (:ids)")
    abstract suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM media")
    abstract suspend fun clearMedia()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun addFavorites(favorites: List<FavoriteEntity>)

    @Query("DELETE FROM favorites WHERE mediaId IN (:ids)")
    abstract suspend fun removeFavorites(ids: List<Long>)

    /** Adds or removes many favorites in one transaction, chunked for SQLite's variable limit. */
    @Transaction
    open suspend fun setFavorites(ids: List<Long>, favorite: Boolean, now: Long) {
        ids.chunked(CHUNK).forEach { chunk ->
            if (favorite) {
                addFavorites(chunk.map { FavoriteEntity(mediaId = it, addedAt = now) })
            } else {
                removeFavorites(chunk)
            }
        }
    }

    /**
     * Makes the index equal to [items] in one transaction: upserts everything seen and removes
     * rows that are no longer in MediaStore. Work is chunked because SQLite caps the number of
     * bound variables in a single statement.
     *
     * Favorites are intentionally left alone: with partial access an item can disappear from
     * the scan and come back later, and its favorite flag should survive that.
     */
    @Transaction
    open suspend fun replaceIndex(items: List<MediaEntity>) {
        val seen = HashSet<Long>(items.size * 2)
        items.chunked(CHUNK).forEach { chunk ->
            chunk.forEach { seen.add(it.id) }
            upsertAll(chunk)
        }
        getAllIds().filter { it !in seen }.chunked(CHUNK).forEach { deleteByIds(it) }
    }

    private companion object {
        const val CHUNK = 500
    }
}
