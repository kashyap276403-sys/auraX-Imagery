package com.aurax.imagery.domain.usecase

import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.domain.model.MediaType
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchMediaUseCaseTest {
    private val search = SearchMediaUseCase()
    private val zone = ZoneOffset.UTC
    private val locale = Locale.ENGLISH

    private fun taken(year: Int, month: Int, day: Int) =
        LocalDate.of(year, month, day).atStartOfDay(zone).toInstant().toEpochMilli()

    private fun item(
        id: Long,
        name: String,
        album: String,
        date: Long,
        video: Boolean = false,
        favorite: Boolean = false,
    ) = MediaItem(
        id = id, uri = "content://media/$id", displayName = name,
        mimeType = if (video) "video/mp4" else "image/jpeg",
        type = if (video) MediaType.Video else MediaType.Image,
        dateTakenMillis = date, sizeBytes = 1, width = 1, height = 1,
        albumName = album, isFavorite = favorite,
    )

    private val library = listOf(
        item(1, "IMG_0001.jpg", "Camera", taken(2025, 10, 5)),
        item(2, "Screenshot_1.png", "Screenshots", taken(2025, 10, 6)),
        item(3, "VID_0002.mp4", "Camera", taken(2024, 3, 1), video = true, favorite = true),
    )

    private fun run(query: String, filter: SearchFilter = SearchFilter()) =
        search(library, query, filter, zone, locale).map { it.id }

    @Test
    fun emptyQueryWithNoFilterReturnsNothing() = assertTrue(run("  ").isEmpty())

    @Test
    fun matchesFileNameCaseInsensitively() = assertEquals(listOf(2L), run("screenshot"))

    @Test
    fun matchesAlbumMonthAndYearTogether() {
        assertEquals(listOf(1L), run("camera october"))
        assertEquals(listOf(3L), run("camera 2024"))
    }

    @Test
    fun everyWordMustMatch() = assertTrue(run("camera screenshot").isEmpty())

    @Test
    fun matchesKindAndExtension() {
        assertEquals(listOf(3L), run("video"))
        assertEquals(listOf(3L), run("mp4"))
    }

    @Test
    fun filtersWorkWithoutAQuery() {
        assertEquals(listOf(3L), run("", SearchFilter(favoritesOnly = true)))
        assertEquals(listOf(1L, 2L), run("", SearchFilter(type = SearchType.Photos)))
    }

    @Test
    fun filtersNarrowAQuery() = assertTrue(run("camera", SearchFilter(type = SearchType.Videos, favoritesOnly = false)) == listOf(3L))
}
