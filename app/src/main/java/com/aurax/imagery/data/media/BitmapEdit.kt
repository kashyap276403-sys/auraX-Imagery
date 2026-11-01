package com.aurax.imagery.data.media

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import com.aurax.imagery.domain.edit.ColorMath
import com.aurax.imagery.domain.edit.EditState
import kotlin.math.max
import kotlin.math.roundToInt

/** Bitmap operations behind the editor, shared by the live preview and the final export. */
object BitmapEdit {

    /**
     * Decodes [uri] with its EXIF orientation already applied, scaled down so the longest side
     * is at most [maxEdge]. A software bitmap is used so it can be drawn on and transformed.
     */
    fun decode(resolver: ContentResolver, uri: Uri, maxEdge: Int): Bitmap {
        val source = ImageDecoder.createSource(resolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = max(info.size.width, info.size.height)
            if (longest > maxEdge) {
                val scale = maxEdge.toFloat() / longest
                decoder.setTargetSize(
                    (info.size.width * scale).roundToInt().coerceAtLeast(1),
                    (info.size.height * scale).roundToInt().coerceAtLeast(1),
                )
            }
        }
    }

    /** Applies the edit's rotation and mirroring. Returns [source] itself when there is nothing to do. */
    fun orient(source: Bitmap, edit: EditState): Bitmap {
        val turns = edit.quarterTurns % 4
        if (turns == 0 && !edit.flipHorizontal && !edit.flipVertical) return source
        val matrix = Matrix().apply {
            postRotate(turns * 90f)
            postScale(if (edit.flipHorizontal) -1f else 1f, if (edit.flipVertical) -1f else 1f)
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    /**
     * Produces the final picture: orient, crop, then colour adjustments. Intermediate bitmaps
     * are recycled as soon as they are no longer needed, since full-size photos are large.
     */
    fun render(source: Bitmap, edit: EditState): Bitmap {
        val oriented = orient(source, edit)
        val px = edit.crop.toPixels(oriented.width, oriented.height)
        val cropped = if (px.x == 0 && px.y == 0 && px.width == oriented.width && px.height == oriented.height) {
            oriented
        } else {
            Bitmap.createBitmap(oriented, px.x, px.y, px.width, px.height).also {
                if (oriented !== source && oriented !== it) oriented.recycle()
            }
        }

        val matrix = ColorMath.forEdit(edit)
        if (matrix.contentEquals(ColorMath.Identity)) return cropped

        val out = Bitmap.createBitmap(cropped.width, cropped.height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(ColorMatrix(matrix)) }
        Canvas(out).drawBitmap(cropped, 0f, 0f, paint)
        if (cropped !== source) cropped.recycle()
        return out
    }
}
