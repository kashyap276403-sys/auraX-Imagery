package com.aurax.imagery.domain.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
        assertEquals(ScreenMode.Best, ScreenMode.Fit.next())
        assertEquals(ScreenMode.Fill, ScreenMode.Best.next())
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

    @Test
    fun bestViewFillsWhenLittleIsCroppedAndStopsEarlyOtherwise() {
        // 16:9 video on a 20:9 phone: Fill would crop ~19% of the height, so Best stops at the 12% limit.
        val fit = computeFrame(2000f, 900f, 1920f, 1080f, ScreenMode.Fit, null)
        val best = computeFrame(2000f, 900f, 1920f, 1080f, ScreenMode.Best, null)
        assertEquals(fit.width / fit.height, best.width / best.height, 0.001f)
        assertEquals(fit.width / (1f - BEST_VIEW_MAX_CROP), best.width, 0.5f)
        // 16:9 video on a 17:9 screen: Fill crops only ~5%, so Best fills completely.
        val fill = computeFrame(1700f, 900f, 1920f, 1080f, ScreenMode.Fill, null)
        val best2 = computeFrame(1700f, 900f, 1920f, 1080f, ScreenMode.Best, null)
        assertEquals(fill.width, best2.width, 0.5f)
        // Never smaller than Fit.
        assertTrue(best.width >= fit.width)
    }
}
