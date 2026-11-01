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
}
