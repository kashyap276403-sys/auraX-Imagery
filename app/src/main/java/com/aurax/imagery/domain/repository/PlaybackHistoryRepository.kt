package com.aurax.imagery.domain.repository

import com.aurax.imagery.domain.player.PlaybackProgress
import com.aurax.imagery.domain.player.RecentVideo
import kotlinx.coroutines.flow.Flow

interface PlaybackHistoryRepository {
    suspend fun get(mediaId: Long): PlaybackProgress?
    suspend fun save(mediaId: Long, positionMs: Long, durationMs: Long)
    fun observeRecent(limit: Int): Flow<List<RecentVideo>>
    fun observeCount(): Flow<Int>
    suspend fun clear()
}
