package com.aurax.imagery.domain.model

import kotlin.math.sqrt

/**
 * Dominant colours of a picture. Pixels (ARGB ints, typically from a thumbnail of ~64 px) are grouped into
 * 4-bit-per-channel buckets; the most populated buckets give the palette, skipping colours too close to
 * one already chosen so five swatches do not all look the same. Transparent pixels are ignored.
 */
object ColorPalette {
    private const val MIN_DISTANCE = 56.0

    fun dominant(pixels: IntArray, count: Int = 5): List<Int> {
        class Bucket(var n: Int = 0, var r: Long = 0, var g: Long = 0, var b: Long = 0)
        val buckets = HashMap<Int, Bucket>()
        for (p in pixels) {
            if ((p ushr 24) < 128) continue
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val key = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
            val bucket = buckets.getOrPut(key) { Bucket() }
            bucket.n++
            bucket.r += r
            bucket.g += g
            bucket.b += b
        }
        val picked = ArrayList<Int>()
        for (bucket in buckets.values.sortedByDescending { it.n }) {
            val r = (bucket.r / bucket.n).toInt()
            val g = (bucket.g / bucket.n).toInt()
            val b = (bucket.b / bucket.n).toInt()
            val far = picked.none { c ->
                val dr = ((c shr 16) and 0xFF) - r
                val dg = ((c shr 8) and 0xFF) - g
                val db = (c and 0xFF) - b
                sqrt((dr * dr + dg * dg + db * db).toDouble()) < MIN_DISTANCE
            }
            if (far) picked += (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            if (picked.size == count) break
        }
        return picked
    }

    fun hex(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)
}
