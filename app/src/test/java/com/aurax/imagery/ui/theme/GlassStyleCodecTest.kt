package com.aurax.imagery.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassStyleCodecTest {

    @Test
    fun styleSurvivesARoundTrip() {
        val style = GlassStyle.PureLiquid.copy(
            tint = Color(0xFF3366CC),
            blur = 0.42f,
            cornerRadius = 20.dp,
            haptics = false,
            performanceMode = true,
        )
        assertEquals(style, GlassStyleCodec.decode(GlassStyleCodec.encode(style)))
    }

    @Test
    fun waterDropSettingsRoundTripAndOldDataStaysClassic() {
        val style = GlassStyle.Natural.copy(family = GlassFamily.WaterDrop, waterDrop = 0.8f, borderOpacity = 0.4f)
        assertEquals(style, GlassStyleCodec.decode(GlassStyleCodec.encode(style)))
        // Saved before Water Drop existed: no new keys, so it must load as the unchanged Classic look.
        val old = GlassStyleCodec.encode(GlassStyle.PureLiquid).split(';').filterNot { it.startsWith("fm=") || it.startsWith("wd=") || it.startsWith("bo=") }.joinToString(";")
        assertEquals(GlassStyle.PureLiquid, GlassStyleCodec.decode(old))
        assertEquals(1f, GlassStyleCodec.decode("bo=7").borderOpacity, 0f)
    }

    @Test
    fun damagedInputFallsBackToDefaults() {
        assertEquals(GlassStyle.Natural, GlassStyleCodec.decode(null))
        assertEquals(GlassStyle.Natural, GlassStyleCodec.decode("garbage;;==;x"))
    }

    @Test
    fun outOfRangeNumbersAreClamped() {
        val s = GlassStyleCodec.decode("bl=9;op=-4;cr=500;sp=0;ds=NaN")
        assertEquals(1f, s.blur, 0f)
        assertEquals(0f, s.opacity, 0f)
        assertEquals(48f, s.cornerRadius.value, 0f)
        assertEquals(0.25f, s.animationSpeed, 0f)
        assertEquals(GlassStyle.Natural.distortion, s.distortion, 0f)
    }

    @Test
    fun presetsRoundTripWithAwkwardNames() {
        val presets = listOf(
            GlassPreset("Night | drive; ✨", GlassStyle.FullTransparent),
            GlassPreset("Frost", GlassStyle.Natural.copy(blur = 0.9f)),
        )
        assertEquals(presets, GlassStyleCodec.decodePresets(GlassStyleCodec.encodePresets(presets)))
    }

    @Test
    fun savingWithTheSameNameReplacesAndTheListIsCapped() {
        var list = listOf(GlassPreset("A", GlassStyle.Natural))
        list = GlassStyleCodec.withPreset(list, GlassPreset("a", GlassStyle.PureLiquid))
        assertEquals(1, list.size)
        assertEquals(GlassStyle.PureLiquid, list.single().style)
        repeat(30) { list = GlassStyleCodec.withPreset(list, GlassPreset("p$it", GlassStyle.Natural)) }
        assertTrue(list.size <= GlassStyleCodec.MAX_PRESETS)
        assertEquals("p29", list.last().name)
    }

    @Test
    fun modeBaselinesMatchTheirMode() {
        GlassMode.entries.forEach { assertEquals(it, it.baseStyle().mode) }
    }
}
