package com.aurax.imagery.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns
import com.aurax.imagery.data.local.database.MediaEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Reads every image and video the app is currently allowed to see from MediaStore.
 *
 * Scoped storage does the access control: with partial access the cursor only contains the
 * items the user selected, and with no access it contains nothing of the user's library.
 */
class MediaStoreScanner(private val resolver: ContentResolver) {

    suspend fun scan(): List<MediaEntity> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            FileColumns._ID,
            FileColumns.DISPLAY_NAME,
            FileColumns.MIME_TYPE,
            FileColumns.MEDIA_TYPE,
            FileColumns.DATE_TAKEN,
            FileColumns.DATE_ADDED,
            FileColumns.DATE_MODIFIED,
            FileColumns.SIZE,
            FileColumns.WIDTH,
            FileColumns.HEIGHT,
            FileColumns.DURATION,
            FileColumns.BUCKET_ID,
            FileColumns.BUCKET_DISPLAY_NAME,
        )
        val selection = "${FileColumns.MEDIA_TYPE} IN (?, ?)"
        val args = arrayOf(
            FileColumns.MEDIA_TYPE_IMAGE.toString(),
            FileColumns.MEDIA_TYPE_VIDEO.toString(),
        )

        val result = ArrayList<MediaEntity>()
        resolver.query(collection, projection, selection, args, "${FileColumns.DATE_ADDED} DESC")?.use { c ->
            val idCol = c.getColumnIndexOrThrow(FileColumns._ID)
            val nameCol = c.getColumnIndexOrThrow(FileColumns.DISPLAY_NAME)
            val mimeCol = c.getColumnIndexOrThrow(FileColumns.MIME_TYPE)
            val typeCol = c.getColumnIndexOrThrow(FileColumns.MEDIA_TYPE)
            val takenCol = c.getColumnIndexOrThrow(FileColumns.DATE_TAKEN)
            val addedCol = c.getColumnIndexOrThrow(FileColumns.DATE_ADDED)
            val modifiedCol = c.getColumnIndexOrThrow(FileColumns.DATE_MODIFIED)
            val sizeCol = c.getColumnIndexOrThrow(FileColumns.SIZE)
            val widthCol = c.getColumnIndexOrThrow(FileColumns.WIDTH)
            val heightCol = c.getColumnIndexOrThrow(FileColumns.HEIGHT)
            val durationCol = c.getColumnIndexOrThrow(FileColumns.DURATION)
            val bucketIdCol = c.getColumnIndexOrThrow(FileColumns.BUCKET_ID)
            val bucketNameCol = c.getColumnIndexOrThrow(FileColumns.BUCKET_DISPLAY_NAME)

            while (c.moveToNext()) {
                if (result.size % 500 == 0) ensureActive() // stay cancellable on huge libraries

                val id = c.getLong(idCol)
                val isVideo = c.getInt(typeCol) == FileColumns.MEDIA_TYPE_VIDEO
                val base = if (isVideo) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                val added = c.getLong(addedCol)
                val modified = c.getLong(modifiedCol)
                // DATE_TAKEN is in ms and often missing (screenshots, downloads); DATE_ADDED is in seconds.
                val taken = c.getLong(takenCol).takeIf { it > 0 } ?: (added.takeIf { it > 0 } ?: modified) * 1000

                result += MediaEntity(
                    id = id,
                    uri = ContentUris.withAppendedId(base, id).toString(),
                    displayName = c.getString(nameCol).orEmpty(),
                    mimeType = c.getString(mimeCol).orEmpty(),
                    isVideo = isVideo,
                    dateTaken = taken,
                    dateModified = modified * 1000,
                    sizeBytes = c.getLong(sizeCol),
                    width = c.getInt(widthCol),
                    height = c.getInt(heightCol),
                    durationMs = if (isVideo) c.getLong(durationCol) else 0L,
                    bucketId = c.getLong(bucketIdCol),
                    bucketName = c.getString(bucketNameCol).orEmpty().ifBlank { "Unknown" },
                )
            }
        }
        result
    }
}
