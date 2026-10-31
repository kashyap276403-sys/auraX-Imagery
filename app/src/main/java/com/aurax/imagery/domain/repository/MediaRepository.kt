package com.aurax.imagery.domain.repository

import com.aurax.imagery.domain.model.Album
import com.aurax.imagery.domain.model.AlbumRef
import com.aurax.imagery.domain.model.MediaItem
import kotlinx.coroutines.flow.Flow

/**
 * Source of truth for the user's local library, backed by the Room index. Keeping it an
 * interface means a future cloud-backed source can be added without touching the UI.
 */
interface MediaRepository {
    fun observeMedia(): Flow<List<MediaItem>>

    /** Smart albums (Favorites, Videos) first when non-empty, then device folders. */
    fun observeAlbums(): Flow<List<Album>>

    fun observeAlbumMedia(ref: AlbumRef): Flow<List<MediaItem>>

    suspend fun setFavorites(mediaIds: List<Long>, favorite: Boolean)
}
