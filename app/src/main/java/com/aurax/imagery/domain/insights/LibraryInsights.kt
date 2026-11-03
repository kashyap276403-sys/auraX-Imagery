package com.aurax.imagery.domain.insights

import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.domain.model.MediaType

data class FolderUsage(val name: String, val bytes: Long, val count: Int)

/** Pure analysis of the indexed library. No Android types, so it runs in JVM tests. */
object LibraryInsights {
    fun largest(items: List<MediaItem>, limit: Int = 10): List<MediaItem> =
        items.sortedByDescending { it.sizeBytes }.take(limit)

    fun folderUsage(items: List<MediaItem>, limit: Int = 8): List<FolderUsage> =
        items.groupBy { it.albumName ?: "Unknown" }
            .map { (name, list) -> FolderUsage(name, list.sumOf { it.sizeBytes }, list.size) }
            .sortedByDescending { it.bytes }
            .take(limit)

    fun bytesByType(items: List<MediaItem>): Pair<Long, Long> {
        val videos = items.filter { it.type == MediaType.Video }.sumOf { it.sizeBytes }
        return (items.sumOf { it.sizeBytes } - videos) to videos
    }

    /**
     * Groups that might be identical copies: same byte size, same dimensions and same duration. This is only
     * a cheap pre-filter; the files are compared by content hash before anything is called a duplicate.
     */
    fun duplicateCandidates(items: List<MediaItem>): List<List<MediaItem>> =
        items.filter { it.sizeBytes > 0 }
            .groupBy { Triple(it.sizeBytes, it.width to it.height, it.durationMillis) }
            .values
            .filter { it.size > 1 }
            .map { g -> g.sortedBy { it.dateTakenMillis } }
            .sortedByDescending { it[0].sizeBytes * (it.size - 1) }

    /** Bytes freed by keeping one file per group. */
    fun reclaimable(groups: List<List<MediaItem>>): Long = groups.sumOf { g -> g.drop(1).sumOf { it.sizeBytes } }

    /** Splits a candidate group by a content hash, keeping only sets with at least two true matches. */
    fun confirm(group: List<MediaItem>, hashOf: (MediaItem) -> String?): List<List<MediaItem>> =
        group.groupBy { hashOf(it) }
            .filterKeys { it != null }
            .values
            .filter { it.size > 1 }
}
