package com.aurax.imagery.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import java.net.URLDecoder
import java.net.URLEncoder

/** A named, user-saved [GlassStyle]. */
data class GlassPreset(val name: String, val style: GlassStyle)

/**
 * Text form of a [GlassStyle] for DataStore. Decoding is lenient on purpose: unknown or missing
 * keys fall back to the Natural defaults and every number is clamped into its valid range, so a
 * damaged or older file can never produce an unusable look.
 */
object GlassStyleCodec {
    const val MAX_PRESETS = 20
    const val MAX_NAME_LENGTH = 24

    fun encode(style: GlassStyle): String = listOf(
        "m=${style.mode.name}",
        "bl=${style.blur}",
        "op=${style.opacity}",
        "tn=${style.tint.toArgb()}",
        "rf=${style.reflection}",
        "bw=${style.borderWidth.value}",
        "cr=${style.cornerRadius.value}",
        "sh=${style.shadow}",
        "gl=${style.glow}",
        "ds=${style.distortion}",
        "eb=${style.edgeBrightness}",
        "sp=${style.animationSpeed}",
        "hp=${style.haptics}",
        "mo=${style.motionEffects}",
        "pf=${style.performanceMode}",
    ).joinToString(";")

    fun decode(text: String?): GlassStyle {
        val base = GlassStyle.Natural
        if (text.isNullOrBlank()) return base
        val values = text.split(';').mapNotNull { part ->
            val i = part.indexOf('=')
            if (i <= 0) null else part.substring(0, i) to part.substring(i + 1)
        }.toMap()

        fun float(key: String, default: Float, min: Float, max: Float): Float =
            (values[key]?.toFloatOrNull()?.takeIf { it.isFinite() } ?: default).coerceIn(min, max)

        return GlassStyle(
            mode = GlassMode.entries.firstOrNull { it.name == values["m"] } ?: base.mode,
            blur = float("bl", base.blur, 0f, 1f),
            opacity = float("op", base.opacity, 0f, 0.6f),
            tint = values["tn"]?.toIntOrNull()?.let { Color(it) } ?: base.tint,
            reflection = float("rf", base.reflection, 0f, 1f),
            borderWidth = float("bw", base.borderWidth.value, 0f, 3f).dp,
            cornerRadius = float("cr", base.cornerRadius.value, 8f, 48f).dp,
            shadow = float("sh", base.shadow, 0f, 1f),
            glow = float("gl", base.glow, 0f, 1f),
            distortion = float("ds", base.distortion, 0f, 1f),
            edgeBrightness = float("eb", base.edgeBrightness, 0f, 1f),
            animationSpeed = float("sp", base.animationSpeed, 0.25f, 3f),
            haptics = values["hp"]?.toBooleanStrictOrNull() ?: base.haptics,
            motionEffects = values["mo"]?.toBooleanStrictOrNull() ?: base.motionEffects,
            performanceMode = values["pf"]?.toBooleanStrictOrNull() ?: base.performanceMode,
        )
    }

    fun cleanName(raw: String): String =
        raw.filter { !it.isISOControl() }.trim().take(MAX_NAME_LENGTH)

    /** One preset per line: `urlEncodedName|encodedStyle`. */
    fun encodePresets(presets: List<GlassPreset>): String =
        presets.joinToString("\n") { "${URLEncoder.encode(it.name, "UTF-8")}|${encode(it.style)}" }

    fun decodePresets(text: String?): List<GlassPreset> {
        if (text.isNullOrBlank()) return emptyList()
        return text.lineSequence().mapNotNull { line ->
            val i = line.indexOf('|')
            if (i <= 0) return@mapNotNull null
            val name = runCatching { URLDecoder.decode(line.substring(0, i), "UTF-8") }.getOrNull()
                ?.let(::cleanName)?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            GlassPreset(name, decode(line.substring(i + 1)))
        }.distinctBy { it.name.lowercase() }.take(MAX_PRESETS).toList()
    }

    /** Adds or replaces (case-insensitively by name) and keeps at most [MAX_PRESETS]. */
    fun withPreset(presets: List<GlassPreset>, preset: GlassPreset): List<GlassPreset> =
        (presets.filterNot { it.name.equals(preset.name, ignoreCase = true) } + preset).takeLast(MAX_PRESETS)
}
