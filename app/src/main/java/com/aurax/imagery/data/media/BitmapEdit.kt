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

    /**
     * Unsharp mask: out = original + amount * (original - blurred), with a 3x3 box blur. [amount] 0..1
     * maps to a gain of 0..1.6. Works on a copy; returns [source] when there is nothing to do.
     */
    fun sharpen(source: Bitmap, amount: Float): Bitmap {
        if (amount <= 0f) return source
        val w = source.width
        val h = source.height
        if (w < 3 || h < 3) return source
        val src = IntArray(w * h)
        source.getPixels(src, 0, w, 0, 0, w, h)
        val out = src.copyOf()
        val gain = amount * 1.6f
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val i = y * w + x
                val p = src[i]
                var sr = 0
                var sg = 0
                var sb = 0
                for (dy in -1..1) for (dx in -1..1) {
                    val q = src[i + dy * w + dx]
                    sr += (q shr 16) and 0xFF
                    sg += (q shr 8) and 0xFF
                    sb += q and 0xFF
                }
                val r = ((p shr 16) and 0xFF)
                val g = ((p shr 8) and 0xFF)
                val b = (p and 0xFF)
                val nr = (r + gain * (r - sr / 9f)).roundToInt().coerceIn(0, 255)
                val ng = (g + gain * (g - sg / 9f)).roundToInt().coerceIn(0, 255)
                val nb = (b + gain * (b - sb / 9f)).roundToInt().coerceIn(0, 255)
                out[i] = (p and 0xFF000000.toInt()) or (nr shl 16) or (ng shl 8) or nb
            }
        }
        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
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

        val sharpened = sharpen(cropped, edit.sharpness).also { if (it !== cropped && cropped !== source) cropped.recycle() }

        val matrix = ColorMath.forEdit(edit)
        if (matrix.contentEquals(ColorMath.Identity)) return sharpened

        val out = Bitmap.createBitmap(sharpened.width, sharpened.height, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(ColorMatrix(matrix)) }
        Canvas(out).drawBitmap(sharpened, 0f, 0f, paint)
        if (sharpened !== source) sharpened.recycle()
        return out
    }
}
