package com.aurax.imagery.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorPaletteTest {
    private val red = 0xFFFF0000.toInt()
    private val blue = 0xFF0000FF.toInt()

    @Test
    fun mostCommonColourComesFirst() {
        val pixels = IntArray(100) { if (it < 70) red else blue }
        val palette = ColorPalette.dominant(pixels)
        assertEquals(red, palette[0])
        assertEquals(blue, palette[1])
    }

    @Test
    fun nearlyIdenticalShadesAreMerged() {
        val pixels = IntArray(100) { if (it % 2 == 0) 0xFFFF0000.toInt() else 0xFFF80808.toInt() }
        assertEquals(1, ColorPalette.dominant(pixels).size)
    }

    @Test
    fun transparentPixelsAreIgnoredAndHexIsFormatted() {
        assertTrue(ColorPalette.dominant(IntArray(10) { 0x00FFFFFF }).isEmpty())
        assertEquals("#FF0000", ColorPalette.hex(red))
    }
}
