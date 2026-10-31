package com.aurax.imagery.data.media

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aurax.imagery.AuraXApp
import kotlinx.coroutines.CancellationException

/** Background job that refreshes the media index off the main thread. */
class MediaIndexWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as AuraXApp).container
        return try {
            container.indexUpdater.sync()
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Transient failures (e.g. the volume going away mid-scan) get a few retries.
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 3
    }
}
