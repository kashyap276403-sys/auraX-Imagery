package com.aurax.imagery.data.media

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Schedules index refreshes and reports whether one is running.
 *
 * Refreshes are triggered by: app launch, the user granting access, and MediaStore change
 * notifications (new photo taken, file deleted...), debounced so a burst of changes causes
 * a single scan.
 */
class MediaIndexer(context: Context) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)

    // Held in fields: the resolver must not be the only reference to a registered observer.
    private val handler = Handler(Looper.getMainLooper())
    private val debouncedEnqueue = Runnable { enqueue() }
    private var observer: ContentObserver? = null

    val isIndexing: Flow<Boolean> =
        workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME)
            .map { infos ->
                infos.any {
                    it.state == WorkInfo.State.RUNNING ||
                        it.state == WorkInfo.State.ENQUEUED ||
                        it.state == WorkInfo.State.BLOCKED
                }
            }
            .distinctUntilChanged()

    /** Queues a refresh. If one is already running, the new one runs right after it. */
    fun enqueue() {
        workManager.enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<MediaIndexWorker>().build(),
        )
    }

    /** Starts listening for MediaStore changes for as long as the process lives. Idempotent. */
    fun startObservingChanges() {
        if (observer != null) return
        val newObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                handler.removeCallbacks(debouncedEnqueue)
                handler.postDelayed(debouncedEnqueue, DEBOUNCE_MS)
            }
        }
        val resolver = appContext.contentResolver
        resolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, newObserver)
        resolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, newObserver)
        observer = newObserver
    }

    private companion object {
        const val WORK_NAME = "media-index"
        const val DEBOUNCE_MS = 2_000L
    }
}
