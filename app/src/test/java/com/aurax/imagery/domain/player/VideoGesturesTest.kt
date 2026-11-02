package com.aurax.imagery.domain.player

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoGesturesTest {
    @Test fun tapZonesAreThirds() {
        assertEquals(TapZone.Left, tapZone(100f, 900f))
        assertEquals(TapZone.Center, tapZone(450f, 900f))
        assertEquals(TapZone.Right, tapZone(800f, 900f))
        assertEquals(TapZone.Center, tapZone(10f, 0f))
    }

    @Test fun seekIsClamped() {
        assertEquals(0L, seekTarget(4_000L, -10_000L, 60_000L))
        assertEquals(60_000L, seekTarget(55_000L, 10_000L, 60_000L))
        assertEquals(30_000L, seekTarget(20_000L, 10_000L, 60_000L))
        assertEquals(90_000L, seekTarget(80_000L, 10_000L, 0L))
    }

    @Test fun dragUpRaisesLevel() {
        assertEquals(0.6f, adjustLevel(0.5f, -100f, 1000f), 0.001f)
        assertEquals(0.4f, adjustLevel(0.5f, 100f, 1000f), 0.001f)
        assertEquals(1f, adjustLevel(0.9f, -900f, 1000f), 0.001f)
        assertEquals(0f, adjustLevel(0.1f, 900f, 1000f), 0.001f)
        assertEquals(0.5f, adjustLevel(0.5f, 50f, 0f), 0.001f)
    }

    @Test fun halves() {
        assertEquals(true, isLeftHalf(10f, 100f))
        assertEquals(false, isLeftHalf(60f, 100f))
    }

    @Test fun speedLabels() {
        assertEquals("1x", speedLabel(1f))
        assertEquals("2x", speedLabel(2f))
        assertEquals("0.25x", speedLabel(0.25f))
        assertEquals("1.5x", speedLabel(1.5f))
    }

    @Test fun orientationCycles() {
        assertEquals(OrientationLock.Portrait, OrientationLock.Auto.next())
        assertEquals(OrientationLock.Auto, OrientationLock.Landscape.next())
    }

    @Test
    fun swipeSeekScalesWithWidthAndStaysInside() {
        // 2 h film: a quarter is 30 min, capped at 10 min per full-width swipe.
        assertEquals(300_000L + 300_000L, swipeSeekTarget(300_000L, 540f, 1080f, 7_200_000L))
        // 1 min clip: 60 s span, so half a width is 30 s.
        assertEquals(40_000L, swipeSeekTarget(10_000L, 540f, 1080f, 60_000L))
        assertEquals(0L, swipeSeekTarget(5_000L, -1080f, 1080f, 60_000L))
        assertEquals(60_000L, swipeSeekTarget(50_000L, 1080f, 1080f, 60_000L))
        assertEquals(7L, swipeSeekTarget(7L, 100f, 0f, 60_000L))
    }

    @Test
    fun swipeDeltaLabelIsSignedMinutesAndSeconds() {
        assertEquals("+0:23", swipeDeltaLabel(23_000L))
        assertEquals("-1:05", swipeDeltaLabel(-65_000L))
        assertEquals("+0:00", swipeDeltaLabel(0L))
    }
}
