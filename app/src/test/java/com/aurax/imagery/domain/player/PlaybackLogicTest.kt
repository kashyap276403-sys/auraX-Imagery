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

class PlaybackHistoryTest {
    @Test fun resumeIgnoresBarelyStarted() {
        assertNull(resumePosition(3_000L, 60_000L))
    }

    @Test fun resumeKeepsMiddle() {
        assertEquals(30_000L, resumePosition(30_000L, 60_000L))
    }

    @Test fun finishedVideosStartOver() {
        assertNull(resumePosition(55_000L, 60_000L))
        assertNull(resumePosition(1_190_000L, 1_200_000L))
        assertNull(resumePosition(1_150_000L, 1_200_000L))
    }

    @Test fun unknownDurationStillResumes() {
        assertEquals(20_000L, resumePosition(20_000L, 0L))
    }

    @Test fun countdown() {
        assertEquals("5:00", countdownLabel(300_000L))
        assertEquals("1:00:00", countdownLabel(3_600_000L))
        assertEquals("0:01", countdownLabel(1L))
        assertEquals("0:00", countdownLabel(-5L))
    }

    @Test fun pipAspectIsClamped() {
        assertEquals(16f / 9f, pipAspect(1920f, 1080f), 0.001f)
        assertEquals(2.39f, pipAspect(5000f, 1000f), 0.001f)
        assertEquals(1f / 2.39f, pipAspect(1000f, 5000f), 0.001f)
        assertEquals(16f / 9f, pipAspect(0f, 0f), 0.001f)
    }
}
