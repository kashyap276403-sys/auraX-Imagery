package com.aurax.imagery.data.repository

import com.aurax.imagery.data.local.database.MediaRow
import com.aurax.imagery.data.local.database.PlaybackProgressDao
import com.aurax.imagery.data.local.database.PlaybackProgressEntity
import com.aurax.imagery.domain.player.PlaybackProgress
import com.aurax.imagery.domain.player.RecentVideo
import com.aurax.imagery.domain.repository.PlaybackHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlaybackHistoryRepositoryImpl(private val dao: PlaybackProgressDao) : PlaybackHistoryRepository {
    override suspend fun get(mediaId: Long): PlaybackProgress? =
        dao.get(mediaId)?.let { PlaybackProgress(it.mediaId, it.positionMs, it.durationMs, it.lastPlayedAt) }

    override suspend fun save(mediaId: Long, positionMs: Long, durationMs: Long) {
        dao.upsert(PlaybackProgressEntity(mediaId, positionMs.coerceAtLeast(0L), durationMs.coerceAtLeast(0L), System.currentTimeMillis()))
    }

    override fun observeRecent(limit: Int): Flow<List<RecentVideo>> = dao.observeRecent(limit).map { rows ->
        rows.map { RecentVideo(MediaRow(it.media, false).toDomain(), it.positionMs, it.progressDurationMs, it.lastPlayedAt) }
    }

    override fun observeCount(): Flow<Int> = dao.observeCount()

    override suspend fun clear() = dao.clear()
}
