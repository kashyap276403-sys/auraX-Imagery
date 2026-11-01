package com.aurax.imagery.data.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Saves a captured video frame as a new PNG in Pictures/AuraX Imagery. The video is never touched. */
class VideoFrameSaver(private val context: Context) {
    suspend fun save(frame: Bitmap): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "AuraX_frame_$stamp.png")
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AuraX Imagery")
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val target = resolver.insert(collection, values) ?: throw IOException("Couldn't create the file")
            try {
                resolver.openOutputStream(target)?.use { out ->
                    if (!frame.compress(Bitmap.CompressFormat.PNG, 100, out)) throw IOException("Couldn't encode the frame")
                } ?: throw IOException("Couldn't open the file")
                resolver.update(target, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            } catch (e: Exception) {
                resolver.delete(target, null, null)
                throw e
            }
            Result.success(target)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            frame.recycle()
        }
    }
}
