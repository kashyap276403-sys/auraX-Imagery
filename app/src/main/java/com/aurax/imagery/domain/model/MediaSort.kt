package com.aurax.imagery.domain.model

enum class SortField(val label: String, val defaultAscending: Boolean) {
    Date("Date", defaultAscending = false), // newest first
    Name("Name", defaultAscending = true), // A to Z
    Size("Size", defaultAscending = false), // largest first
}

data class SortOrder(
    val field: SortField = SortField.Date,
    val ascending: Boolean = false,
)

/** How the date-sorted timeline is split into headed sections. */
enum class Grouping(val label: String) {
    Day("Day"),
    Month("Month"),
    Year("Year"),
}

/** Persisted gallery view options. */
data class GalleryPreferences(
    val sort: SortOrder = SortOrder(),
    val grouping: Grouping = Grouping.Day,
)

/**
 * A run of items under one header. [title] is null for the flat list used by name/size
 * sorting, where date headers would be meaningless.
 */
data class TimelineSection(
    val key: String,
    val title: String?,
    val items: List<MediaItem>,
)
