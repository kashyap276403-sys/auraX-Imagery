package com.aurax.imagery

import android.app.Application

/**
 * Application entry point. Owns the [AppContainer] and starts listening for MediaStore changes.
 * The refresh itself is queued from the UI (see MainActivity), not here, so a worker-started
 * process does not schedule yet another worker.
 */
class AuraXApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.indexer.startObservingChanges()
    }
}
