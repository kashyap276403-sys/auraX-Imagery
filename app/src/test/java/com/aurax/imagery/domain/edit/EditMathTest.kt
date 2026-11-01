package com.aurax.imagery.domain.edit

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditMathTest {

    private fun apply(m: FloatArray, r: Float, g: Float, b: Float): Triple<Float, Float, Float> = Triple(
        m[0] * r + m[1] * g + m[2] * b + m[4],
        m[5] * r + m[6] * g + m[7] * b + m[9],
        m[10] * r + m[11] * g + m[12] * b + m[14],
    )

    @Test
    fun defaultEditLeavesColoursUntouched() {
        assertArrayEquals(ColorMath.Identity, ColorMath.forEdit(EditState()), 0.0001f)
        assertFalse(EditState().isEdited)
    }

    @Test
    fun brightnessShiftsEveryChannel() {
        val (r, g, b) = apply(ColorMath.forEdit(EditState(brightness = 0.5f)), 100f, 50f, 0f)
        assertEquals(150f, r, 0.01f)
        assertEquals(100f, g, 0.01f)
        assertEquals(50f, b, 0.01f)
    }

    @Test
    fun contrastKeepsMidGreyFixed() {
        val (r, _, _) = apply(ColorMath.contrast(1.5f), 127.5f, 127.5f, 127.5f)
        assertEquals(127.5f, r, 0.01f)
    }

    @Test
    fun monoFilterProducesEqualChannels() {
        val (r, g, b) = apply(PhotoFilter.Mono.matrix, 200f, 40f, 90f)
        assertEquals(r, g, 0.01f)
        assertEquals(g, b, 0.01f)
    }

    @Test
    fun zeroSaturationIsGreyscale() {
        val (r, g, b) = apply(ColorMath.forEdit(EditState(saturation = -1f)), 255f, 0f, 0f)
        assertEquals(r, g, 0.01f)
        assertEquals(g, b, 0.01f)
    }

    @Test
    fun concatAppliesFirstThenSecond() {
        val first = ColorMath.brightness(10f)
        val second = ColorMath.channelScale(2f, 2f, 2f)
        val (r, _, _) = apply(ColorMath.concat(second, first), 5f, 0f, 0f)
        assertEquals(30f, r, 0.01f) // (5 + 10) * 2
    }

    @Test
    fun rotatingAndFlippingResetTheCrop() {
        val cropped = EditState(crop = CropRect(0.1f, 0.1f, 0.8f, 0.8f))
        assertTrue(cropped.isEdited)
        assertTrue(cropped.rotated(clockwise = true).crop.isFull)
        assertTrue(cropped.mirroredHorizontally().crop.isFull)
        assertEquals(3, EditState().rotated(clockwise = false).quarterTurns)
        assertEquals(0, EditState().rotated(true).rotated(false).quarterTurns)
    }

    @Test
    fun cropPixelsStayInsideTheImage() {
        val px = CropRect(0.25f, 0.5f, 1f, 1f).toPixels(400, 200)
        assertEquals(PixelRect(100, 100, 300, 100), px)
        val tiny = CropRect(0.999f, 0.999f, 1f, 1f).toPixels(100, 100)
        assertTrue(tiny.x + tiny.width <= 100 && tiny.width >= 1)
    }

    @Test
    fun fitAspectCentresTheLargestMatchingRect() {
        // Square crop of a 2:1 landscape image: half the width, full height, centred.
        val r = CropRect.fitAspect(ratio = 1f, imageAspect = 2f)
        assertEquals(0.25f, r.left, 0.001f)
        assertEquals(0.75f, r.right, 0.001f)
        assertEquals(0f, r.top, 0.001f)
        assertEquals(1f, r.bottom, 0.001f)
    }

    @Test
    fun lockedCornerDragKeepsAspectAndStaysInBounds() {
        val start = CropRect.fitAspect(1f, 1.5f)
        val moved = CropMath.dragCorner(start, CropCorner.BottomRight, dx = 0.5f, dy = 0.5f, imageAspect = 1.5f, lockRatio = 1f)
        assertTrue(moved.right <= 1f && moved.bottom <= 1f)
        assertEquals(1f, (moved.width * 1.5f) / moved.height, 0.01f)
        assertEquals(start.left, moved.left, 0.001f)
    }

    @Test
    fun freeCornerDragCannotShrinkBelowMinimum() {
        val moved = CropMath.dragCorner(CropRect(), CropCorner.BottomRight, dx = -2f, dy = -2f, imageAspect = 1f, lockRatio = null)
        assertEquals(CropMath.MIN_SIZE, moved.width, 0.001f)
        assertEquals(CropMath.MIN_SIZE, moved.height, 0.001f)
    }

    @Test
    fun movingTheCropKeepsItInsideTheImage() {
        val moved = CropMath.move(CropRect(0.2f, 0.2f, 0.6f, 0.6f), dx = 5f, dy = -5f)
        assertEquals(1f, moved.right, 0.001f)
        assertEquals(0f, moved.top, 0.001f)
        assertEquals(0.4f, moved.width, 0.001f)
    }
}
