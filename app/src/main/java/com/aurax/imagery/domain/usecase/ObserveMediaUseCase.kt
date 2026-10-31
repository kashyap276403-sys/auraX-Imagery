package com.aurax.imagery.domain.usecase

import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.domain.repository.MediaRepository
import kotlinx.coroutines.flow.Flow

/** Streams the full library, newest first. Sorting options are added with the Home screen in Phase 3. */
class ObserveMediaUseCase(private val repository: MediaRepository) {
    operator fun invoke(): Flow<List<MediaItem>> = repository.observeMedia()
}
