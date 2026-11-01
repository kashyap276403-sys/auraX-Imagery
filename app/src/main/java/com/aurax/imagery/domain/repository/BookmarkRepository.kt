package com.aurax.imagery.domain.repository

import com.aurax.imagery.domain.player.Bookmark
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {
    fun observe(mediaId: Long): Flow<List<Bookmark>>
    suspend fun add(mediaId: Long, positionMs: Long, name: String)
    suspend fun delete(id: Long)
}
