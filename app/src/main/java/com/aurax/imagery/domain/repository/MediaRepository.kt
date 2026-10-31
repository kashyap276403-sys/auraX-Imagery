package com.aurax.imagery.domain.repository

import com.aurax.imagery.domain.model.Album
import com.aurax.imagery.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

/**
 * Source of truth for the user's local library.
 *
 * Phase 2 implements this on top of MediaStore plus a Room index. Keeping it an interface
 * means a future cloud-backed source can be added without touching the UI or use cases.
 */
interface MediaRepository {
    fun observeMedia(): Flow<List<MediaItem>>
    fun observeAlbums(): Flow<List<Album>>
    fun observeFavorites(): Flow<List<MediaItem>>
    suspend fun setFavorite(mediaId: Long, favorite: Boolean)
}
