package com.aurax.imagery.domain.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackLogicTest {
    @Test fun abNeedsAFirst() {
        val r = AbRange().withEnd(5_000L)
        assertNull(r.endMs)
        assertFalse(r.isActive)
    }

    @Test fun abRangeLoops() {
        val r = AbRange().withStart(2_000L).withEnd(8_000L)
        assertTrue(r.isActive)
        assertNull(r.loopTarget(7_999L))
        assertEquals(2_000L, r.loopTarget(8_000L))
        assertEquals(2_000L, r.loopTarget(9_500L))
    }

    @Test fun abEndTooCloseIsIgnored() {
        val r = AbRange().withStart(2_000L).withEnd(2_200L)
        assertNull(r.endMs)
    }

    @Test fun movingAPastBDropsB() {
        val r = AbRange(2_000L, 8_000L).withStart(8_000L)
        assertEquals(8_000L, r.startMs)
        assertNull(r.endMs)
    }

    @Test fun queueStepsAndWraps() {
        val q = listOf(10L, 20L, 30L)
        assertEquals(20L, queueStep(q, 10L, true, false))
        assertNull(queueStep(q, 30L, true, false))
        assertEquals(10L, queueStep(q, 30L, true, true))
        assertNull(queueStep(q, 10L, false, false))
        assertEquals(30L, queueStep(q, 10L, false, true))
    }

    @Test fun queueEdgeCases() {
        assertNull(queueStep(emptyList(), 1L, true, true))
        assertNull(queueStep(listOf(5L), 5L, true, true))
        assertEquals(10L, queueStep(listOf(10L, 20L), 99L, true, false))
        assertEquals(20L, queueStep(listOf(10L, 20L), 99L, false, false))
    }

    @Test fun repeatModeCycles() {
        assertEquals(RepeatMode.One, RepeatMode.Off.next())
        assertEquals(RepeatMode.Off, RepeatMode.Queue.next())
    }

    @Test fun channelLabels() {
        assertEquals("Stereo", channelLabel(2))
        assertEquals("5.1", channelLabel(6))
        assertEquals("4 ch", channelLabel(4))
        assertNull(channelLabel(0))
    }

    @Test fun trackTitles() {
        assertEquals("Hindi", trackTitle(null, "hi", 0, "Audio"))
        assertEquals("Director commentary", trackTitle(" Director commentary ", "en", 0, "Audio"))
        assertEquals("Subtitle 2", trackTitle(null, "und", 1, "Subtitle"))
        assertEquals("Audio 1", trackTitle("", null, 0, "Audio"))
    }
}
