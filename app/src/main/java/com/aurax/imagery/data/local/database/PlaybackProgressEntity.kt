package com.aurax.imagery.data.local.database

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** Last position of a video; its row doubles as the recently-played history. */
@Entity(tableName = "playback_progress", indices = [Index("lastPlayedAt")])
data class PlaybackProgressEntity(
    @PrimaryKey val mediaId: Long,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedAt: Long,
)

/** A history row joined with its media entry (videos deleted from the device simply drop out). */
data class RecentRow(
    @Embedded val media: MediaEntity,
    val positionMs: Long,
    // Not "durationMs": the embedded media row already has a column of that name.
    val progressDurationMs: Long,
    val lastPlayedAt: Long,
)

@Dao
interface PlaybackProgressDao {
    @Query("SELECT * FROM playback_progress WHERE mediaId = :mediaId")
    suspend fun get(mediaId: Long): PlaybackProgressEntity?

    @Upsert
    suspend fun upsert(entity: PlaybackProgressEntity)

    @Query(
        "SELECT m.*, p.positionMs AS positionMs, p.durationMs AS progressDurationMs, p.lastPlayedAt AS lastPlayedAt " +
            "FROM playback_progress p INNER JOIN media m ON m.id = p.mediaId " +
            "ORDER BY p.lastPlayedAt DESC LIMIT :limit",
    )
    fun observeRecent(limit: Int): Flow<List<RecentRow>>

    @Query("SELECT COUNT(*) FROM playback_progress")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM playback_progress")
    suspend fun clear()
}
