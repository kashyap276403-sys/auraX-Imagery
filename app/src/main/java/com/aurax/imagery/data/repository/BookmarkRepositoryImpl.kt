package com.aurax.imagery.data.repository

import com.aurax.imagery.data.local.database.BookmarkDao
import com.aurax.imagery.data.local.database.BookmarkEntity
import com.aurax.imagery.domain.player.Bookmark
import com.aurax.imagery.domain.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookmarkRepositoryImpl(private val dao: BookmarkDao) : BookmarkRepository {
    override fun observe(mediaId: Long): Flow<List<Bookmark>> = dao.observe(mediaId).map { rows ->
        rows.map { Bookmark(it.id, it.mediaId, it.positionMs, it.name, it.createdAt) }
    }

    override suspend fun add(mediaId: Long, positionMs: Long, name: String) {
        dao.insert(BookmarkEntity(mediaId = mediaId, positionMs = positionMs, name = name, createdAt = System.currentTimeMillis()))
    }

    override suspend fun delete(id: Long) = dao.delete(id)
}
