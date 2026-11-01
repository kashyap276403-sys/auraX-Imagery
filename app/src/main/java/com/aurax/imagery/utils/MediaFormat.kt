package com.aurax.imagery.utils

import java.util.Locale

/** Whole numbers print without a decimal point (26.0 -> "26"), everything else with one (5.4 -> "5.4"). */
private fun trimmed(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.ROOT, "%.1f", value)

/** 1024-based sizes with short labels: "812 KB", "2.4 MB". */
fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "${bytes.coerceAtLeast(0)} B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes / 1024.0
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return String.format(Locale.ROOT, "%.1f %s", value, units[unit])
}

/** Shutter speed: 0.008 -> "1/125 s", 2.0 -> "2 s", 1.5 -> "1.5 s". */
fun formatExposure(seconds: Double): String = when {
    seconds <= 0.0 -> ""
    seconds >= 1.0 -> "${trimmed(seconds)} s"
    else -> "1/${Math.round(1.0 / seconds)} s"
}

fun formatAperture(fNumber: Double): String = "f/${trimmed(fNumber)}"

fun formatFocalLength(millimetres: Double): String = "${trimmed(millimetres)} mm"

/** Resolution as megapixels ("12.0 MP"), or null when the size is unknown. */
fun formatMegapixels(width: Int, height: Int): String? {
    if (width <= 0 || height <= 0) return null
    return String.format(Locale.ROOT, "%.1f MP", width.toLong() * height / 1_000_000.0)
}
