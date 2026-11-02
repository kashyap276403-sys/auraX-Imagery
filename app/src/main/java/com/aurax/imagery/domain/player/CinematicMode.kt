package com.aurax.imagery.domain.player

/**
 * Display looks applied to the picture on screen only. The video file and the saved frames are
 * never changed. Each look is a 4x5 colour matrix (row-major, Android ColorMatrix layout), so the
 * GPU does one cheap per-pixel multiply and Original costs nothing at all.
 */
enum class CinematicMode(val label: String, val hint: String) {
    Original("Original", "The picture as recorded"),
    Cinema("Cinema", "Slightly deeper contrast, calmer colour"),
    Vivid("Vivid", "Richer colour and a little more punch"),
    Warm("Warm", "Golden, sunset-like tone"),
    Cool("Cool", "Crisp, blue-leaning tone");

    val matrix: FloatArray
        get() = when (this) {
            Original -> IDENTITY.copyOf()
            Cinema -> concat(contrast(1.12f), saturation(0.88f))
            Vivid -> concat(contrast(1.05f), saturation(1.35f))
            Warm -> channelGain(1.08f, 1.0f, 0.90f)
            Cool -> channelGain(0.92f, 1.0f, 1.09f)
        }

    val isOriginal: Boolean get() = this == Original
}

private val IDENTITY = floatArrayOf(
    1f, 0f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f, 0f,
    0f, 0f, 1f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

internal fun saturation(s: Float): FloatArray {
    val r = 0.213f
    val g = 0.715f
    val b = 0.072f
    return floatArrayOf(
        r + (1 - r) * s, g - g * s, b - b * s, 0f, 0f,
        r - r * s, g + (1 - g) * s, b - b * s, 0f, 0f,
        r - r * s, g - g * s, b + (1 - b) * s, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )
}

internal fun contrast(c: Float): FloatArray {
    // Scale around mid-grey (0.5 of 255).
    val shift = 127.5f * (1 - c)
    return floatArrayOf(
        c, 0f, 0f, 0f, shift,
        0f, c, 0f, 0f, shift,
        0f, 0f, c, 0f, shift,
        0f, 0f, 0f, 1f, 0f,
    )
}

internal fun channelGain(r: Float, g: Float, b: Float): FloatArray = floatArrayOf(
    r, 0f, 0f, 0f, 0f,
    0f, g, 0f, 0f, 0f,
    0f, 0f, b, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

/** Matrix for "apply [first], then [second]" (4x5 matrices with an implicit [0 0 0 0 1] row). */
internal fun concat(second: FloatArray, first: FloatArray): FloatArray {
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
