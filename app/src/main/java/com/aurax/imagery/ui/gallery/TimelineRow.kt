package com.aurax.imagery.ui.gallery

import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.domain.model.TimelineSection

/** One cell of the flattened timeline grid: either a full-width section header or a thumbnail. */
sealed interface TimelineRow {
    /** Stable, unique key for LazyGrid (String for headers, Long for media, so they never clash). */
    val key: Any

    data class Header(val section: TimelineSection) : TimelineRow {
        override val key: Any get() = "h:${section.key}"
    }

    data class Media(val item: MediaItem) : TimelineRow {
        override val key: Any get() = item.id
    }
}

/** Sections without a title (flat name/size sorting) contribute no header row. */
fun flattenTimeline(sections: List<TimelineSection>): List<TimelineRow> = buildList {
    for (section in sections) {
        if (section.title != null) add(TimelineRow.Header(section))
        for (item in section.items) add(TimelineRow.Media(item))
    }
}
