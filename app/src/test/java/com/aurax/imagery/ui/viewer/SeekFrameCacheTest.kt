package com.aurax.imagery.ui.viewer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SeekFrameCacheTest {
    @Test
    fun positionsInTheSameSecondShareAKey() {
        assertEquals(SeekFrameCache.key("a", 1_100), SeekFrameCache.key("a", 1_900))
        assertNotEquals(SeekFrameCache.key("a", 1_900), SeekFrameCache.key("a", 2_000))
        assertNotEquals(SeekFrameCache.key("a", 1_000), SeekFrameCache.key("b", 1_000))
        assertEquals(SeekFrameCache.key("a", 0), SeekFrameCache.key("a", -5))
    }
}
