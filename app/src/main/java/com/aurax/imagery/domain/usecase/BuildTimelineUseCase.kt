package com.aurax.imagery.domain.usecase

import com.aurax.imagery.domain.model.Grouping
import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.domain.model.SortField
import com.aurax.imagery.domain.model.SortOrder
import com.aurax.imagery.domain.model.TimelineSection
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Sorts media and, for date sorting, splits it into day / month / year sections.
 *
 * Pure Kotlin with the clock, zone and locale passed in, so it is deterministic and unit
 * tested on the JVM. Name and size sorting return one flat, header-less section.
 */
class BuildTimelineUseCase {

    operator fun invoke(
        items: List<MediaItem>,
        sort: SortOrder,
        grouping: Grouping,
        today: LocalDate,
        zone: ZoneId,
        locale: Locale,
    ): List<TimelineSection> {
        if (items.isEmpty()) return emptyList()
        val sorted = sortMedia(items, sort)
        if (sort.field != SortField.Date) {
            return listOf(TimelineSection(key = "all", title = null, items = sorted))
        }

        val sections = ArrayList<TimelineSection>()
        var currentKey: String? = null
        var currentDate: LocalDate? = null
        var bucket = ArrayList<MediaItem>()

        fun flush() {
            val key = currentKey ?: return
            val date = currentDate ?: return
            sections.add(TimelineSection(key, titleFor(date, grouping, today, locale), bucket))
            bucket = ArrayList()
        }

        for (item in sorted) {
            val date = Instant.ofEpochMilli(item.dateTakenMillis).atZone(zone).toLocalDate()
            val key = keyFor(date, grouping)
            if (key != currentKey) {
                flush()
                currentKey = key
                currentDate = date
            }
            bucket.add(item)
        }
        flush()
        return sections
    }

    private fun keyFor(date: LocalDate, grouping: Grouping): String = when (grouping) {
        Grouping.Day -> date.toString()
        Grouping.Month -> "%04d-%02d".format(Locale.ROOT, date.year, date.monthValue)
        Grouping.Year -> date.year.toString()
    }

    private fun titleFor(date: LocalDate, grouping: Grouping, today: LocalDate, locale: Locale): String =
        when (grouping) {
            Grouping.Day -> when {
                date == today -> "Today"
                date == today.minusDays(1) -> "Yesterday"
                date.year == today.year -> date.format(DateTimeFormatter.ofPattern("EEE, d MMM", locale))
                else -> date.format(DateTimeFormatter.ofPattern("d MMM yyyy", locale))
            }
            Grouping.Month -> date.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
            Grouping.Year -> date.year.toString()
        }
}

/** Ties always break on id so the order is stable between emissions. */
internal fun sortMedia(items: List<MediaItem>, sort: SortOrder): List<MediaItem> {
    val base: Comparator<MediaItem> = when (sort.field) {
        SortField.Date -> compareBy<MediaItem> { it.dateTakenMillis }
        SortField.Name -> Comparator<MediaItem> { a, b ->
            String.CASE_INSENSITIVE_ORDER.compare(a.displayName, b.displayName)
        }
        SortField.Size -> compareBy<MediaItem> { it.sizeBytes }
    }
    val ordered = if (sort.ascending) base else base.reversed()
    return items.sortedWith(ordered.thenBy { it.id })
}
