package com.aurax.imagery.domain.insights

import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.domain.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryInsightsTest {
    private fun item(id: Long, size: Long, w: Int = 100, h: Int = 100, album: String = "A", t: Long = id, video: Boolean = false) =
        MediaItem(id, "content://$id", "n$id", if (video) "video/mp4" else "image/jpeg",
            if (video) MediaType.Video else MediaType.Image, t, size, w, h, if (video) 1000L else null, 1L, album)

    @Test fun candidatesNeedSameSizeAndDimensions() {
        val g = LibraryInsights.duplicateCandidates(listOf(item(1, 500), item(2, 500), item(3, 500, w = 99), item(4, 700)))
        assertEquals(1, g.size)
        assertEquals(listOf(1L, 2L), g[0].map { it.id })
    }

    @Test fun emptyFilesAreNeverCandidates() =
        assertEquals(0, LibraryInsights.duplicateCandidates(listOf(item(1, 0), item(2, 0))).size)

    @Test fun confirmKeepsOnlyRealHashMatches() {
        val group = listOf(item(1, 5), item(2, 5), item(3, 5))
        val hashes = mapOf(1L to "x", 2L to "x", 3L to "y")
        val out = LibraryInsights.confirm(group) { hashes[it.id] }
        assertEquals(listOf(listOf(1L, 2L)), out.map { g -> g.map { it.id } })
        assertEquals(0, LibraryInsights.confirm(group) { null }.size)
    }

    @Test fun reclaimableKeepsOnePerGroup() =
        assertEquals(1000L, LibraryInsights.reclaimable(listOf(listOf(item(1, 500), item(2, 500), item(3, 500)))))

    @Test fun folderUsageSortsBySize() {
        val u = LibraryInsights.folderUsage(listOf(item(1, 10, album = "S"), item(2, 90, album = "B"), item(3, 5, album = "S")))
        assertEquals(listOf("B", "S"), u.map { it.name })
        assertEquals(15L, u[1].bytes)
    }

    @Test fun largestAndTypeSplit() {
        val l = listOf(item(1, 10), item(2, 90, video = true), item(3, 40))
        assertEquals(2L, LibraryInsights.largest(l, 1).first().id)
        assertEquals(50L to 90L, LibraryInsights.bytesByType(l))
    }
}
