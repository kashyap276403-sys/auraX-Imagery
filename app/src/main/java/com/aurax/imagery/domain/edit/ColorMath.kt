package com.aurax.imagery.domain.edit

/**
 * 4x5 colour matrices as 20 floats in row-major order, the layout shared by
 * android.graphics.ColorMatrix and Compose's ColorMatrix: `out = M * [r g b a 1]`, with the
 * fifth column a 0..255 offset. Kept as plain float arrays so the maths is unit-testable on the JVM.
 */
object ColorMath {
    val Identity: FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )

    /** Returns the matrix that applies [first] and then [second]. */
    fun concat(second: FloatArray, first: FloatArray): FloatArray {
        val out = FloatArray(20)
        for (row in 0 until 4) {
            for (col in 0 until 5) {
                var sum = 0f
                for (k in 0 until 4) sum += second[row * 5 + k] * first[k * 5 + col]
                if (col == 4) sum += second[row * 5 + 4]
                out[row * 5 + col] = sum
            }
        }
        return out
    }

    /** [amount] 0 = greyscale, 1 = unchanged, above 1 = more vivid. */
    fun saturation(amount: Float): FloatArray {
        val inv = 1f - amount
        val r = 0.2126f * inv
        val g = 0.7152f * inv
        val b = 0.0722f * inv
        return floatArrayOf(
            r + amount, g, b, 0f, 0f,
            r, g + amount, b, 0f, 0f,
            r, g, b + amount, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    /** [scale] 1 = unchanged; pivots around mid-grey so highlights and shadows spread evenly. */
    fun contrast(scale: Float): FloatArray {
        val offset = (1f - scale) * 127.5f
        return floatArrayOf(
            scale, 0f, 0f, 0f, offset,
            0f, scale, 0f, 0f, offset,
            0f, 0f, scale, 0f, offset,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    /** [offset] in 0..255 units added to every colour channel. */
    fun brightness(offset: Float): FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f, offset,
        0f, 1f, 0f, 0f, offset,
        0f, 0f, 1f, 0f, offset,
        0f, 0f, 0f, 1f, 0f,
    )

    fun channelScale(r: Float, g: Float, b: Float): FloatArray = floatArrayOf(
        r, 0f, 0f, 0f, 0f,
        0f, g, 0f, 0f, 0f,
        0f, 0f, b, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )

    /** Final matrix for an edit: filter first, then saturation, contrast and brightness. */
    fun forEdit(state: EditState): FloatArray {
        var m = state.filter.matrix
        if (state.saturation != 0f) m = concat(saturation(1f + state.saturation), m)
        if (state.contrast != 0f) m = concat(contrast(1f + state.contrast), m)
        if (state.brightness != 0f) m = concat(brightness(state.brightness * BRIGHTNESS_RANGE), m)
        return m
    }

    /** At full slider travel brightness shifts each channel by this many 0..255 levels. */
    const val BRIGHTNESS_RANGE = 100f
}
