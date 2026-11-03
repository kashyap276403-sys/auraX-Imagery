package com.aurax.imagery.domain.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EqPresetsTest {
    @Test fun flatIsAllZero() = assertTrue(EqPreset.Flat.levels(5).all { it == 0f })

    @Test fun spreadsToAnyBandCount() {
        EqPreset.entries.forEach { p ->
            listOf(1, 3, 5, 7, 10).forEach { n ->
                val l = p.levels(n)
                assertEquals(n, l.size)
                assertTrue(l.all { it in -1f..1f })
            }
        }
        assertEquals(0, EqPreset.BassBoost.levels(0).size)
    }

    @Test fun bassBoostLiftsLowBandsMore() {
        val l = EqPreset.BassBoost.levels(5)
        assertTrue(l.first() > l.last())
    }

    @Test fun millibelsStayInsideDeviceRange() {
        assertEquals(1500.toShort(), EqMath.toMillibels(1f, -1500, 1500))
        assertEquals((-1500).toShort(), EqMath.toMillibels(-1f, -1500, 1500))
        assertEquals(0.toShort(), EqMath.toMillibels(0f, -1500, 1500))
        assertEquals(1500.toShort(), EqMath.toMillibels(9f, -1500, 1500))
        assertEquals(750.toShort(), EqMath.toMillibels(0.5f, -1500, 1500))
    }
}
