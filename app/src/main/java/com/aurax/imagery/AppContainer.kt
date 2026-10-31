package com.aurax.imagery

import android.content.Context
import com.aurax.imagery.data.local.database.AuraDatabase
import com.aurax.imagery.data.media.MediaIndexUpdater
import com.aurax.imagery.data.media.MediaIndexer
import com.aurax.imagery.data.media.MediaStoreScanner
import com.aurax.imagery.data.repository.MediaRepositoryImpl
import com.aurax.imagery.domain.repository.MediaRepository
import com.aurax.imagery.domain.usecase.ObserveMediaUseCase

/**
 * Hand-written dependency container (no DI framework). Everything is lazy so cold start does
 * not open the database or touch WorkManager until something needs it.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val database: AuraDatabase by lazy { AuraDatabase.create(appContext) }

    val mediaRepository: MediaRepository by lazy { MediaRepositoryImpl(database.mediaDao()) }

    val indexUpdater: MediaIndexUpdater by lazy {
        MediaIndexUpdater(appContext, database.mediaDao(), MediaStoreScanner(appContext.contentResolver))
    }

    val indexer: MediaIndexer by lazy { MediaIndexer(appContext) }

    val observeMedia: ObserveMediaUseCase by lazy { ObserveMediaUseCase(mediaRepository) }
}
