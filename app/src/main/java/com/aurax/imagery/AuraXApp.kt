package com.aurax.imagery

import android.app.Application

/**
 * Application entry point.
 *
 * Kept intentionally thin in Phase 1. Later phases wire the dependency graph here
 * (Room database, MediaStore repository, DataStore, WorkManager scheduling) through a
 * small hand-written container, so the data layer stays swappable without a DI framework.
 */
class AuraXApp : Application()
