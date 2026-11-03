package com.aurax.imagery.ui.theme

import androidx.compose.ui.graphics.Color
import com.aurax.imagery.ui.components.windowColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassWindowStyleTest {
    @Test
    fun classicWindowColourIsTheOriginalNearBlack() {
        assertEquals(Color(0xFF0B0B10).copy(alpha = 0.96f), GlassStyle.Natural.windowColor())
    }

    @Test
    fun waterDropWindowStaysReadableAcrossTheTransparencyRange() {
        for (opacity in listOf(0f, 0.18f, 0.6f)) {
            val c = GlassStyle.Natural.copy(family = GlassFamily.WaterDrop, opacity = opacity, tint = Color.White).windowColor()
            assertTrue("alpha ${c.alpha}", c.alpha in 0.79f..0.97f)
        }
    }

    @Test
    fun frostedAndCrystalReshapeTheRenderedStyleButNotTheStoredOne() {
        val base = GlassStyle.Natural.copy(blur = 0.8f, distortion = 0.5f)
        val frosted = base.copy(family = GlassFamily.Frosted).effective()
        assertTrue(frosted.blur < base.blur)
        assertEquals(0f, frosted.distortion, 0f)
        val crystal = base.copy(family = GlassFamily.Crystal).effective()
        assertTrue(crystal.blur < frosted.blur)
        assertTrue(crystal.edgeBrightness >= 0.85f)
        assertEquals(base, base.effective())
        assertEquals(base.copy(family = GlassFamily.WaterDrop), base.copy(family = GlassFamily.WaterDrop).effective())
    }

    @Test
    fun allFourFamiliesSurviveSaving() {
        GlassFamily.entries.forEach { f ->
            val s = GlassStyle.Natural.copy(family = f)
            assertEquals(s, GlassStyleCodec.decode(GlassStyleCodec.encode(s)))
        }
        assertEquals(4, GlassFamily.entries.size)
    }

    @Test
    fun switchingFamilyKeepsEveryOtherSetting() {
        val a = GlassStyle.PureLiquid.copy(blur = 0.3f, tint = Color.Red)
        val b = a.copy(family = GlassFamily.WaterDrop)
        assertEquals(a, b.copy(family = GlassFamily.Classic))
    }
}
