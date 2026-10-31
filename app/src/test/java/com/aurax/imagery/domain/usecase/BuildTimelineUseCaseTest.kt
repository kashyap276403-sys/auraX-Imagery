package com.aurax.imagery.domain.usecase

import com.aurax.imagery.domain.model.Grouping
import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.domain.model.MediaType
import com.aurax.imagery.domain.model.SortField
import com.aurax.imagery.domain.model.SortOrder
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildTimelineUseCaseTest {
    private val useCase = BuildTimelineUseCase()
    private val today = LocalDate.of(2026, 10, 31) // a Saturday
    private val zone = ZoneOffset.UTC
    private val locale = Locale.ENGLISH
    private val newestFirst = SortOrder(SortField.Date, ascending = false)

    private fun at(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(zone).toInstant().toEpochMilli() + 3_600_000

    private fun item(id: Long, taken: Long, name: String = "img$id.jpg", size: Long = 100) = MediaItem(
        id = id, uri = "content://media/$id", displayName = name, mimeType = "image/jpeg",
        type = MediaType.Image, dateTakenMillis = taken, sizeBytes = size, width = 10, height = 10,
    )

    private val sample = listOf(
        item(1, at(2026, 10, 31)),
        item(2, at(2026, 10, 31) + 60_000),
        item(3, at(2026, 10, 30)),
        item(4, at(2026, 10, 1)),
        item(5, at(2025, 12, 25)),
    )

    private fun titles(grouping: Grouping) =
        useCase(sample, newestFirst, grouping, today, zone, locale).map { it.title }

    @Test fun emptyInputGivesNoSections() {
        assertTrue(useCase(emptyList(), newestFirst, Grouping.Day, today, zone, locale).isEmpty())
    }

    @Test fun dayGroupingUsesTodayYesterdayAndDates() {
        assertEquals(listOf("Today", "Yesterday", "Thu, 1 Oct", "25 Dec 2025"), titles(Grouping.Day))
    }

    @Test fun monthGrouping() {
        assertEquals(listOf("October 2026", "December 2025"), titles(Grouping.Month))
    }

    @Test fun yearGrouping() {
        assertEquals(listOf("2026", "2025"), titles(Grouping.Year))
    }

    @Test fun newestFirstWithinASection() {
        val firstSection = useCase(sample, newestFirst, Grouping.Day, today, zone, locale).first()
        assertEquals(listOf(2L, 1L), firstSection.items.map { it.id })
    }

    @Test fun oldestFirstReversesSectionOrder() {
        val sections = useCase(sample, SortOrder(SortField.Date, ascending = true), Grouping.Year, today, zone, locale)
        assertEquals(listOf("2025", "2026"), sections.map { it.title })
    }

    @Test fun nameSortIsFlatAndCaseInsensitive() {
        val items = listOf(item(1, 0, "b.jpg"), item(2, 0, "A.jpg"), item(3, 0, "c.jpg"))
        val sections = useCase(items, SortOrder(SortField.Name, ascending = true), Grouping.Day, today, zone, locale)
        assertEquals(1, sections.size)
        assertNull(sections.single().title)
        assertEquals(listOf("A.jpg", "b.jpg", "c.jpg"), sections.single().items.map { it.displayName })
    }

    @Test fun sizeSortLargestFirst() {
        val items = listOf(item(1, 0, size = 10), item(2, 0, size = 300), item(3, 0, size = 20))
        val sections = useCase(items, SortOrder(SortField.Size, ascending = false), Grouping.Day, today, zone, locale)
        assertEquals(listOf(2L, 3L, 1L), sections.single().items.map { it.id })
    }
}
