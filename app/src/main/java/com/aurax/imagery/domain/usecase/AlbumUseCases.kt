package com.aurax.imagery.domain.usecase

import com.aurax.imagery.domain.model.Album
import com.aurax.imagery.domain.model.AlbumRef
import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow

class ObserveAlbumsUseCase(private val repository: MediaRepository) {
    operator fun invoke(): Flow<List<Album>> = repository.observeAlbums()
}

class ObserveAlbumMediaUseCase(private val repository: MediaRepository) {
    operator fun invoke(ref: AlbumRef): Flow<List<MediaItem>> = repository.observeAlbumMedia(ref)
}

class SetFavoritesUseCase(private val repository: MediaRepository) {
    suspend operator fun invoke(mediaIds: List<Long>, favorite: Boolean) =
        repository.setFavorites(mediaIds, favorite)
}
