package com.aurax.imagery.domain.usecase

import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.domain.model.MediaType
import java.time.Instant
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

enum class SearchType(val label: String) { All("All"), Photos("Photos"), Videos("Videos") }

data class SearchFilter(
    val type: SearchType = SearchType.All,
    val favoritesOnly: Boolean = false,
)

/**
 * Library search over what is already indexed on the device. Every word typed must match
 * somewhere in the item's searchable text (file name, album, file type, kind, and the month,
 * year and weekday it was taken), so "camera 2025", "october screenshot" or "mp4" all work.
 * Pure Kotlin so it is unit tested on the JVM.
 */
class SearchMediaUseCase {

    operator fun invoke(
        items: List<MediaItem>,
        query: String,
        filter: SearchFilter,
        zone: ZoneId,
        locale: Locale,
    ): List<MediaItem> {
        val words = query.lowercase(locale).split(WHITESPACE).filter { it.isNotEmpty() }
        if (words.isEmpty() && filter == SearchFilter()) return emptyList()
        return items.filter { item ->
            matchesFilter(item, filter) && run {
                if (words.isEmpty()) return@run true
                val haystack = searchableText(item, zone, locale)
                words.all { it in haystack }
            }
        }
    }

    private fun matchesFilter(item: MediaItem, filter: SearchFilter): Boolean {
        if (filter.favoritesOnly && !item.isFavorite) return false
        return when (filter.type) {
            SearchType.All -> true
            SearchType.Photos -> item.type == MediaType.Image
            SearchType.Videos -> item.type == MediaType.Video
        }
    }

    internal fun searchableText(item: MediaItem, zone: ZoneId, locale: Locale): String {
        val date = Instant.ofEpochMilli(item.dateTakenMillis).atZone(zone)
        return buildString {
            append(item.displayName)
            append(' ').append(item.albumName.orEmpty())
            append(' ').append(item.mimeType)
            append(' ').append(if (item.type == MediaType.Video) "video videos" else "photo photos image picture")
            if (item.isFavorite) append(" favorite")
            append(' ').append(date.month.getDisplayName(TextStyle.FULL, locale))
            append(' ').append(date.dayOfWeek.getDisplayName(TextStyle.FULL, locale))
            append(' ').append(date.year)
        }.lowercase(locale)
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
