package com.aurax.imagery.data.local.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** A named timestamp in one video. Kept apart from the media index so rescans never touch it. */
@Entity(tableName = "video_bookmarks", indices = [Index("mediaId")])
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mediaId: Long,
    val positionMs: Long,
    val name: String,
    val createdAt: Long,
)

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM video_bookmarks WHERE mediaId = :mediaId ORDER BY positionMs")
    fun observe(mediaId: Long): Flow<List<BookmarkEntity>>

    @Insert
    suspend fun insert(entity: BookmarkEntity): Long

    @Query("DELETE FROM video_bookmarks WHERE id = :id")
    suspend fun delete(id: Long)
}
