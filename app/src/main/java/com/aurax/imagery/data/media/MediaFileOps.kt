package com.aurax.imagery.data.media

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.aurax.imagery.domain.model.AlbumNames
import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.domain.model.MediaType
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Copy and move between albums, the scoped-storage way. A copy is a new MediaStore file written by the
 * app (no consent needed). A move changes the file's folder in place, which needs the system's
 * "allow AuraX to modify these files" confirmation first; [moveRequest] builds that prompt.
 */
class MediaFileOps(private val context: Context) {

    suspend fun copy(items: List<MediaItem>, album: String): Result<Int> = withContext(Dispatchers.IO) {
        val folder = AlbumNames.clean(album)
        if (folder.isEmpty()) return@withContext Result.failure(IOException("Album name is empty"))
        var done = 0
        try {
            for (item in items) {
                copyOne(item, folder)
                done++
            }
            Result.success(done)
        } catch (e: Exception) {
            // Items copied before the failure stay; the caller reports how many.
            Result.failure(IOException("Copied $done of ${items.size}: ${e.message}", e))
        }
    }

    private fun copyOne(item: MediaItem, folder: String) {
        val resolver = context.contentResolver
        val isVideo = item.type == MediaType.Video
        val collection = if (isVideo) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, item.displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, AlbumNames.relativePath(folder, isVideo))
            put(MediaStore.MediaColumns.DATE_TAKEN, item.dateTakenMillis)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val target = resolver.insert(collection, values) ?: throw IOException("Couldn't create ${item.displayName}")
        try {
            resolver.openInputStream(Uri.parse(item.uri))?.use { input ->
                resolver.openOutputStream(target)?.use { output -> input.copyTo(output) }
                    ?: throw IOException("Couldn't write ${item.displayName}")
            } ?: throw IOException("Couldn't read ${item.displayName}")
            resolver.update(target, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        } catch (e: Exception) {
            resolver.delete(target, null, null)
            throw e
        }
    }

    /** The system prompt to ask before [applyMove]. */
    fun moveRequest(items: List<MediaItem>): PendingIntent =
        MediaStore.createWriteRequest(context.contentResolver, items.map { Uri.parse(it.uri) })

    /** Moves already-approved items into [album]. Returns how many were moved. */
    suspend fun applyMove(items: List<MediaItem>, album: String): Int = withContext(Dispatchers.IO) {
        val folder = AlbumNames.clean(album)
        if (folder.isEmpty()) return@withContext 0
        var moved = 0
        for (item in items) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.RELATIVE_PATH, AlbumNames.relativePath(folder, item.type == MediaType.Video))
            }
            val rows = runCatching { context.contentResolver.update(Uri.parse(item.uri), values, null, null) }.getOrDefault(0)
            if (rows > 0) moved++
        }
        moved
    }
}
