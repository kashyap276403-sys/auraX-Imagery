package com.aurax.imagery.data.media

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.aurax.imagery.domain.edit.EditState
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Writes an edited photo to MediaStore as a NEW file in Pictures/AuraX Imagery. The original is
 * never opened for writing, so an edit can never damage or replace it. No storage permission is
 * needed: an app may always add files to its own MediaStore entries.
 */
class EditedImageSaver(private val context: Context) {

    suspend fun save(sourceUri: Uri, edit: EditState): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            val sourceMime = resolver.getType(sourceUri)
            val asPng = sourceMime == "image/png"

            val source = BitmapEdit.decode(resolver, sourceUri, MAX_EXPORT_EDGE)
            val result = BitmapEdit.render(source, edit)

            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "AuraX_$stamp.${if (asPng) "png" else "jpg"}")
                put(MediaStore.Images.Media.MIME_TYPE, if (asPng) "image/png" else "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AuraX Imagery")
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val target = resolver.insert(collection, values) ?: throw IOException("Couldn't create the new file")

            try {
                resolver.openOutputStream(target)?.use { out ->
                    val ok = result.compress(
                        if (asPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
                        JPEG_QUALITY,
                        out,
                    )
                    if (!ok) throw IOException("Couldn't encode the image")
                } ?: throw IOException("Couldn't open the new file")
                // Publishing the file (clearing IS_PENDING) is the last step, so no half-written copy is ever visible.
                resolver.update(target, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            } catch (e: Exception) {
                resolver.delete(target, null, null)
                throw e
            } finally {
                if (result !== source) result.recycle()
                source.recycle()
            }
            Result.success(target)
        } catch (e: OutOfMemoryError) {
            Result.failure(IOException("Not enough memory to save this photo at full size", e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private companion object {
        /** Longest side of the saved copy. Larger originals are scaled down to keep memory in check. */
        const val MAX_EXPORT_EDGE = 4096
        const val JPEG_QUALITY = 95
    }
}
