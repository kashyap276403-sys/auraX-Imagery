package com.aurax.imagery.domain.edit

/** One-tap looks. Each is a colour matrix, so the same filter drives the live preview and the saved file. */
enum class PhotoFilter(val label: String, val matrix: FloatArray) {
    None("Original", ColorMath.Identity),
    Vivid("Vivid", ColorMath.concat(ColorMath.contrast(1.1f), ColorMath.saturation(1.4f))),
    Warm("Warm", ColorMath.channelScale(1.08f, 1.0f, 0.88f)),
    Cool("Cool", ColorMath.channelScale(0.9f, 1.0f, 1.1f)),
    Mono("Mono", ColorMath.saturation(0f)),
    Noir("Noir", ColorMath.concat(ColorMath.contrast(1.35f), ColorMath.saturation(0f))),
    Sepia(
        "Sepia",
        floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
    Fade("Fade", ColorMath.concat(ColorMath.brightness(24f), ColorMath.concat(ColorMath.contrast(0.8f), ColorMath.saturation(0.85f)))),
}

/** The crop rectangle as fractions (0..1) of the oriented image. */
data class CropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val isFull: Boolean get() = left <= 0f && top <= 0f && right >= 1f && bottom >= 1f

    /** Pixel bounds inside an image of the given size; always at least 1x1 and never out of range. */
    fun toPixels(imageWidth: Int, imageHeight: Int): PixelRect {
        val x = (left * imageWidth).toInt().coerceIn(0, imageWidth - 1)
        val y = (top * imageHeight).toInt().coerceIn(0, imageHeight - 1)
        val w = (width * imageWidth).toInt().coerceIn(1, imageWidth - x)
        val h = (height * imageHeight).toInt().coerceIn(1, imageHeight - y)
        return PixelRect(x, y, w, h)
    }

    companion object {
        /** Largest centred rectangle whose pixel aspect (width / height) is [ratio] inside an image of [imageAspect]. */
        fun fitAspect(ratio: Float, imageAspect: Float): CropRect {
            val k = ratio / imageAspect // normalized width / normalized height
            val w: Float
            val h: Float
            if (k >= 1f) {
                w = 1f
                h = 1f / k
            } else {
                h = 1f
                w = k
            }
            val l = (1f - w) / 2f
            val t = (1f - h) / 2f
            return CropRect(l, t, l + w, t + h)
        }
    }
}

data class PixelRect(val x: Int, val y: Int, val width: Int, val height: Int)

enum class CropCorner { TopLeft, TopRight, BottomLeft, BottomRight }

/** Fixed crop shapes offered in the editor. [ratio] null = free-form. */
enum class CropAspect(val label: String, val ratio: Float?) {
    Free("Free", null),
    Original("Original", -1f), // resolved against the image at use time
    Square("1:1", 1f),
    Landscape43("4:3", 4f / 3f),
    Portrait34("3:4", 3f / 4f),
    Wide169("16:9", 16f / 9f),
    Tall916("9:16", 9f / 16f),
    ;

    fun resolve(imageAspect: Float): Float? = if (this == Original) imageAspect else ratio
}

object CropMath {
    const val MIN_SIZE = 0.1f

    /**
     * Drags one corner by ([dx], [dy]) (fractions of the image), keeping the opposite corner fixed.
     * With a [lockRatio] (pixel width / height) the shape is preserved.
     */
    fun dragCorner(
        rect: CropRect,
        corner: CropCorner,
        dx: Float,
        dy: Float,
        imageAspect: Float,
        lockRatio: Float?,
    ): CropRect {
        val movesRight = corner == CropCorner.TopRight || corner == CropCorner.BottomRight
        val movesDown = corner == CropCorner.BottomLeft || corner == CropCorner.BottomRight
        val ax = if (movesRight) rect.left else rect.right
        val ay = if (movesDown) rect.top else rect.bottom
        val mx = (if (movesRight) rect.right else rect.left) + dx
        val my = (if (movesDown) rect.bottom else rect.top) + dy
        val sx = if (movesRight) 1f else -1f
        val sy = if (movesDown) 1f else -1f

        var w: Float
        var h: Float
        if (lockRatio == null) {
            w = (sx * (mx - ax)).coerceIn(MIN_SIZE, if (movesRight) 1f - ax else ax)
            h = (sy * (my - ay)).coerceIn(MIN_SIZE, if (movesDown) 1f - ay else ay)
        } else {
            val roomX = if (movesRight) 1f - ax else ax
            val roomY = if (movesDown) 1f - ay else ay
            // normalized h = normalized w * imageAspect / ratio
            w = (sx * (mx - ax)).coerceIn(MIN_SIZE, maxOf(MIN_SIZE, roomX))
            h = w * imageAspect / lockRatio
            if (h > roomY) {
                h = roomY
                w = h * lockRatio / imageAspect
            }
        }
        val left = if (movesRight) ax else ax - w
        val top = if (movesDown) ay else ay - h
        return CropRect(left, top, left + w, top + h)
    }

    /** Moves the whole rectangle, keeping it inside the image. */
    fun move(rect: CropRect, dx: Float, dy: Float): CropRect {
        val nx = (rect.left + dx).coerceIn(0f, 1f - rect.width)
        val ny = (rect.top + dy).coerceIn(0f, 1f - rect.height)
        return CropRect(nx, ny, nx + rect.width, ny + rect.height)
    }
}

/**
 * Everything the editor can change. Rotation and flips are applied to the picture as displayed
 * (rotate first, then mirror), the crop is a fraction of that result, and colour is a
 * per-pixel matrix, so nothing here depends on the original's resolution.
 */
data class EditState(
    val quarterTurns: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val crop: CropRect = CropRect(),
    val cropAspect: CropAspect = CropAspect.Free,
    /** Each of the three adjustments runs from -1 to 1; 0 leaves the picture unchanged. */
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val filter: PhotoFilter = PhotoFilter.None,
) {
    val isEdited: Boolean
        get() = quarterTurns % 4 != 0 || flipHorizontal || flipVertical || !crop.isFull ||
            brightness != 0f || contrast != 0f || saturation != 0f || filter != PhotoFilter.None

    /** Rotating or mirroring changes the picture's frame, so a previous crop no longer applies. */
    fun rotated(clockwise: Boolean): EditState = copy(
        quarterTurns = (quarterTurns + if (clockwise) 1 else 3) % 4,
        crop = CropRect(),
        cropAspect = CropAspect.Free,
    )

    fun mirroredHorizontally(): EditState =
        copy(flipHorizontal = !flipHorizontal, crop = CropRect(), cropAspect = CropAspect.Free)

    fun mirroredVertically(): EditState =
        copy(flipVertical = !flipVertical, crop = CropRect(), cropAspect = CropAspect.Free)
}
