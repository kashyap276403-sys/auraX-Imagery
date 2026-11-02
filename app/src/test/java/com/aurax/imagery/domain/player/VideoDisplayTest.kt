package com.aurax.imagery.domain.player

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoDisplayTest {
    private val d = 0.01f

    @Test fun fitWideVideoInTallContainer() {
        val f = computeFrame(1000f, 2000f, 1920f, 1080f, ScreenMode.Fit, null)
        assertEquals(1000f, f.width, d)
        assertEquals(562.5f, f.height, d)
    }

    @Test fun fitPortraitVideoInWideContainer() {
        val f = computeFrame(2000f, 1000f, 1080f, 1920f, ScreenMode.Fit, null)
        assertEquals(1000f, f.height, d)
        assertEquals(562.5f, f.width, d)
    }

    @Test fun fillCoversContainer() {
        val f = computeFrame(1000f, 2000f, 1920f, 1080f, ScreenMode.Fill, null)
        assertEquals(2000f, f.height, d)
        assertEquals(3555.56f, f.width, 0.1f)
    }

    @Test fun stretchIgnoresRatio() {
        val f = computeFrame(1000f, 2000f, 1920f, 1080f, ScreenMode.Stretch, 1f)
        assertEquals(1000f, f.width, d)
        assertEquals(2000f, f.height, d)
    }

    @Test fun originalNeverUpscales() {
        val f = computeFrame(1000f, 2000f, 400f, 300f, ScreenMode.Original, null)
        assertEquals(400f, f.width, d)
        assertEquals(300f, f.height, d)
    }

    @Test fun originalShrinksLargeVideo() {
        val f = computeFrame(1000f, 2000f, 2000f, 1000f, ScreenMode.Original, null)
        assertEquals(1000f, f.width, d)
        assertEquals(500f, f.height, d)
    }

    @Test fun presetChangesFitFrame() {
        val f = computeFrame(1000f, 2000f, 1920f, 1080f, ScreenMode.Fit, 1f)
        assertEquals(1000f, f.width, d)
        assertEquals(1000f, f.height, d)
    }

    @Test fun unknownVideoSizeFallsBackToContainer() {
        val f = computeFrame(1000f, 2000f, 0f, 0f, ScreenMode.Original, null)
        assertEquals(1000f, f.width, d)
        assertEquals(2000f, f.height, d)
    }

    @Test fun emptyContainerGivesEmptyFrame() {
        assertEquals(0f, computeFrame(0f, 100f, 10f, 10f, ScreenMode.Fit, null).width, d)
    }

    @Test fun panIsLimitedToOverflow() {
        assertEquals(0f, clampPan(300f, 1000f, 1000f, 1f), d)
        assertEquals(500f, clampPan(900f, 1000f, 1000f, 2f), d)
        assertEquals(-500f, clampPan(-900f, 1000f, 1000f, 2f), d)
    }

    @Test fun zoomIsClamped() {
        assertEquals(1f, 0.2f.clampZoom(), d)
        assertEquals(4f, 9f.clampZoom(), d)
    }

    @Test fun ratioCycleOrder() {
        assertEquals(ScreenMode.Fill, ScreenMode.Fit.next())
        assertEquals(ScreenMode.Original, ScreenMode.Fill.next())
        assertEquals(ScreenMode.Stretch, ScreenMode.Original.next())
        assertEquals(ScreenMode.Fit, ScreenMode.Stretch.next())
    }

    @Test fun zoomCycle() {
        assertEquals(1.5f, nextZoomPreset(1f), d)
        assertEquals(2f, nextZoomPreset(1.5f), d)
        assertEquals(1f, nextZoomPreset(2f), d)
        assertEquals(1f, nextZoomPreset(3.2f), d)
        assertEquals(2f, nextZoomPreset(1.6f), d)
        assertEquals("150%", zoomLabel(1.5f))
    }
}
