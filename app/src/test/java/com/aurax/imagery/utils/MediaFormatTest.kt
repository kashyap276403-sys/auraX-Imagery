package com.aurax.imagery.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaFormatTest {
    @Test fun smallSizesAreBytes() {
        assertEquals("0 B", formatFileSize(0))
        assertEquals("1023 B", formatFileSize(1023))
    }

    @Test fun sizesScaleUp() {
        assertEquals("1.0 KB", formatFileSize(1024))
        assertEquals("1.5 KB", formatFileSize(1536))
        assertEquals("5.0 MB", formatFileSize(5L * 1024 * 1024))
        assertEquals("2.0 GB", formatFileSize(2L * 1024 * 1024 * 1024))
    }

    @Test fun exposureBelowOneSecondIsAFraction() {
        assertEquals("1/125 s", formatExposure(0.008))
        assertEquals("1/2 s", formatExposure(0.5))
    }

    @Test fun exposureOfOneSecondOrMore() {
        assertEquals("2 s", formatExposure(2.0))
        assertEquals("1.5 s", formatExposure(1.5))
    }

    @Test fun apertureAndFocalLength() {
        assertEquals("f/1.8", formatAperture(1.8))
        assertEquals("f/2", formatAperture(2.0))
        assertEquals("26 mm", formatFocalLength(26.0))
        assertEquals("5.4 mm", formatFocalLength(5.4))
    }

    @Test fun megapixels() {
        assertEquals("12.0 MP", formatMegapixels(4000, 3000))
        assertNull(formatMegapixels(0, 3000))
    }
}
