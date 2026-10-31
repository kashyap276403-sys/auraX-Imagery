package com.aurax.imagery.ui.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurax.imagery.AppContainer
import com.aurax.imagery.domain.model.AlbumRef
import com.aurax.imagery.domain.model.GalleryPreferences
import com.aurax.imagery.domain.model.Grouping
import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.domain.model.SortOrder
import com.aurax.imagery.domain.model.TimelineSection
import com.aurax.imagery.domain.repository.PreferencesRepository
import com.aurax.imagery.domain.usecase.BuildTimelineUseCase
import com.aurax.imagery.domain.usecase.SetFavoritesUseCase
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where a list screen gets its media from. */
sealed interface MediaSource {
    data object All : MediaSource
    data class Album(val ref: AlbumRef) : MediaSource
}

data class MediaListUiState(
    /** False until the first database emission, so the UI shows loading, not "empty". */
    val hasLoaded: Boolean = false,
    val isIndexing: Boolean = false,
    val itemCount: Int = 0,
    val rows: List<TimelineRow> = emptyList(),
    val preferences: GalleryPreferences = GalleryPreferences(),
    val selectedIds: Set<Long> = emptySet(),
    /** True when every selected item is already a favorite (the action then un-favorites). */
    val selectionAllFavorite: Boolean = false,
)

/**
 * Shared by Photos, Favorites and album detail: the same timeline, sorting, grouping and
 * selection behaviour over different media sources.
 *
 * The expensive part (sort + group + flatten) runs on a background dispatcher and is only
 * recomputed when the media or the view options change, not on every selection tap.
 */
class MediaListViewModel(
    source: Flow<List<MediaItem>>,
    private val preferences: PreferencesRepository,
    private val setFavorites: SetFavoritesUseCase,
    buildTimeline: BuildTimelineUseCase,
    indexing: Flow<Boolean>,
) : ViewModel() {

    private val selected = MutableStateFlow<Set<Long>>(emptySet())

    /** Latest media, written on a background thread and read for selection actions. */
    @Volatile
    private var items: List<MediaItem> = emptyList()

    private data class Content(
        val itemCount: Int,
        val rows: List<TimelineRow>,
        val preferences: GalleryPreferences,
    )

    private val content: Flow<Content> = combine(source, preferences.gallery) { list, prefs ->
        items = list
        // Drop selections that no longer exist (e.g. an item was deleted elsewhere).
        if (selected.value.isNotEmpty()) {
            val present = list.mapTo(HashSet()) { it.id }
            selected.update { current -> current.filterTo(HashSet()) { it in present } }
        }
        val zone = ZoneId.systemDefault()
        val sections = buildTimeline(list, prefs.sort, prefs.grouping, LocalDate.now(zone), zone, Locale.getDefault())
        Content(list.size, flattenTimeline(sections), prefs)
    }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<MediaListUiState> =
        combine(content, selected, indexing) { c, selection, isIndexing ->
            MediaListUiState(
                hasLoaded = true,
                isIndexing = isIndexing,
                itemCount = c.itemCount,
                rows = c.rows,
                preferences = c.preferences,
                selectedIds = selection,
                selectionAllFavorite = selection.isNotEmpty() &&
                    items.filter { it.id in selection }.let { chosen -> chosen.isNotEmpty() && chosen.all { it.isFavorite } },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MediaListUiState())

    fun toggleSelection(id: Long) = selected.update { if (id in it) it - id else it + id }

    /** Selects a whole section, or clears it if it was already fully selected. */
    fun toggleSection(section: TimelineSection) = selected.update { current ->
        val ids = section.items.map { it.id }
        if (current.containsAll(ids)) current - ids.toSet() else current + ids
    }

    fun selectAll() {
        selected.value = items.mapTo(HashSet()) { it.id }
    }

    fun clearSelection() {
        selected.value = emptySet()
    }

    fun selectedItems(): List<MediaItem> {
        val ids = selected.value
        return items.filter { it.id in ids }
    }

    /** Favorites the selection, or un-favorites it when everything selected is already a favorite. */
    fun toggleFavoriteForSelection() {
        val chosen = selectedItems()
        if (chosen.isEmpty()) return
        val makeFavorite = chosen.any { !it.isFavorite }
        viewModelScope.launch {
            setFavorites(chosen.map { it.id }, makeFavorite)
            clearSelection()
        }
    }

    fun setSort(sort: SortOrder) {
        viewModelScope.launch { preferences.setSort(sort) }
    }

    fun setGrouping(grouping: Grouping) {
        viewModelScope.launch { preferences.setGrouping(grouping) }
    }

    companion object {
        fun factory(container: AppContainer, source: MediaSource) = viewModelFactory {
            initializer {
                MediaListViewModel(
                    source = when (source) {
                        MediaSource.All -> container.observeMedia()
                        is MediaSource.Album -> container.observeAlbumMedia(source.ref)
                    },
                    preferences = container.preferences,
                    setFavorites = container.setFavorites,
                    buildTimeline = container.buildTimeline,
                    indexing = container.indexer.isIndexing,
                )
            }
        }
    }
}
