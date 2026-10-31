package com.aurax.imagery

import android.content.Context
import com.aurax.imagery.data.local.database.AuraDatabase
import com.aurax.imagery.data.local.prefs.DataStorePreferencesRepository
import com.aurax.imagery.data.media.MediaIndexUpdater
import com.aurax.imagery.data.media.MediaIndexer
import com.aurax.imagery.data.media.MediaStoreScanner
import com.aurax.imagery.data.repository.MediaRepositoryImpl
import com.aurax.imagery.domain.repository.MediaRepository
import com.aurax.imagery.domain.repository.PreferencesRepository
import com.aurax.imagery.domain.usecase.BuildTimelineUseCase
import com.aurax.imagery.domain.usecase.ObserveAlbumMediaUseCase
import com.aurax.imagery.domain.usecase.ObserveAlbumsUseCase
import com.aurax.imagery.domain.usecase.ObserveMediaUseCase
import com.aurax.imagery.domain.usecase.SetFavoritesUseCase

/**
 * Hand-written dependency container (no DI framework). Everything is lazy so cold start does
 * not open the database or touch WorkManager until something needs it.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: AuraDatabase by lazy { AuraDatabase.create(appContext) }

    val mediaRepository: MediaRepository by lazy { MediaRepositoryImpl(database.mediaDao()) }
    val preferences: PreferencesRepository by lazy { DataStorePreferencesRepository(appContext) }

    val indexUpdater: MediaIndexUpdater by lazy {
        MediaIndexUpdater(appContext, database.mediaDao(), MediaStoreScanner(appContext.contentResolver))
    }
    val indexer: MediaIndexer by lazy { MediaIndexer(appContext) }

    val observeMedia: ObserveMediaUseCase by lazy { ObserveMediaUseCase(mediaRepository) }
    val observeAlbums: ObserveAlbumsUseCase by lazy { ObserveAlbumsUseCase(mediaRepository) }
    val observeAlbumMedia: ObserveAlbumMediaUseCase by lazy { ObserveAlbumMediaUseCase(mediaRepository) }
    val setFavorites: SetFavoritesUseCase by lazy { SetFavoritesUseCase(mediaRepository) }
    val buildTimeline: BuildTimelineUseCase by lazy { BuildTimelineUseCase() }
}
