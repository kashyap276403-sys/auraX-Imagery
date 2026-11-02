package com.aurax.imagery.domain.player

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CinematicAndSettingsTest {
    @Test fun originalIsIdentity() {
        val identity = floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f)
        assertArrayEquals(identity, CinematicMode.Original.matrix, 0f)
    }

    @Test fun everyLookIsAFullMatrix() {
        CinematicMode.entries.forEach { assertEquals(20, it.matrix.size) }
    }

    @Test fun saturationOneChangesNothing() {
        assertArrayEquals(CinematicMode.Original.matrix, saturation(1f), 0.0001f)
    }

    @Test fun saturationZeroGivesGrey() {
        val m = saturation(0f)
        // All three colour rows are the same luminance weights.
        for (col in 0 until 3) {
            assertEquals(m[col], m[5 + col], 0.0001f)
            assertEquals(m[col], m[10 + col], 0.0001f)
        }
    }

    @Test fun contrastKeepsMidGrey() {
        val m = contrast(1.2f)
        // 127.5 in -> 127.5 out for the red channel.
        assertEquals(127.5f, m[0] * 127.5f + m[4], 0.01f)
    }

    @Test fun concatWithIdentityIsNoop() {
        val c = contrast(1.1f)
        assertArrayEquals(c, concat(CinematicMode.Original.matrix, c), 0.0001f)
        assertArrayEquals(c, concat(c, CinematicMode.Original.matrix), 0.0001f)
    }

    @Test fun warmAddsRedRemovesBlue() {
        val m = CinematicMode.Warm.matrix
        assertTrue(m[0] > 1f)
        assertTrue(m[12] < 1f)
        val cool = CinematicMode.Cool.matrix
        assertTrue(cool[0] < 1f)
        assertTrue(cool[12] > 1f)
    }

    @Test fun settingsAreSnappedToOfferedChoices() {
        val s = VideoPlayerSettings(defaultSpeed = 1.4f, seekIntervalSec = 12, autoHideSec = 99).sanitized()
        assertEquals(1.5f, s.defaultSpeed, 0f)
        assertEquals(10, s.seekIntervalSec)
        assertEquals(4, s.autoHideSec)
    }

    @Test fun autoHideLabels() {
        assertEquals("Never", autoHideLabel(0))
        assertEquals("4 s", autoHideLabel(4))
    }
}
