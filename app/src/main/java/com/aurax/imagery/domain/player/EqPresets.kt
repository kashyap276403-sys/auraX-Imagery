package com.aurax.imagery.domain.player

/** Equaliser presets as per-band levels in -1..1 (fraction of the device's supported gain range). */
enum class EqPreset(val label: String, private val curve: List<Float>) {
    Flat("Flat", listOf(0f, 0f, 0f, 0f, 0f)),
    BassBoost("Bass boost", listOf(0.8f, 0.5f, 0f, 0f, 0f)),
    TrebleBoost("Treble boost", listOf(0f, 0f, 0f, 0.5f, 0.8f)),
    Vocal("Vocal", listOf(-0.3f, 0f, 0.6f, 0.4f, -0.1f)),
    Cinema("Cinema", listOf(0.6f, 0.2f, -0.1f, 0.3f, 0.5f)),
    ;

    /** Spreads the 5-point curve over however many bands the device exposes (usually 5). */
    fun levels(bandCount: Int): List<Float> {
        if (bandCount <= 0) return emptyList()
        if (bandCount == 1) return listOf(curve.average().toFloat())
        return List(bandCount) { i ->
            val pos = i * (curve.size - 1f) / (bandCount - 1)
            val lo = pos.toInt().coerceIn(0, curve.size - 1)
            val hi = (lo + 1).coerceAtMost(curve.size - 1)
            curve[lo] + (curve[hi] - curve[lo]) * (pos - lo)
        }
    }
}

object EqMath {
    /** Level -1..1 to millibels within the device range. */
    fun toMillibels(level: Float, minMb: Int, maxMb: Int): Short {
        val l = level.coerceIn(-1f, 1f)
        val mb = if (l >= 0f) l * maxMb else -l * minMb
        return mb.toInt().coerceIn(minMb, maxMb).toShort()
    }
}
