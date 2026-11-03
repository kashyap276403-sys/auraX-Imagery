package com.aurax.imagery.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumNamesTest {
    @Test
    fun removesPathCharactersAndTidiesSpaces() {
        assertEquals("Trip 2025 Goa", AlbumNames.clean("  Trip/2025:  Goa  "))
        assertEquals("", AlbumNames.clean("///"))
        assertEquals("", AlbumNames.clean(".."))
    }

    @Test
    fun longNamesAreCutAndPathsDependOnType() {
        assertEquals(AlbumNames.MAX_LENGTH, AlbumNames.clean("x".repeat(100)).length)
        assertEquals("Pictures/Family", AlbumNames.relativePath("Family", isVideo = false))
        assertEquals("Movies/Family", AlbumNames.relativePath("Family", isVideo = true))
    }
}
