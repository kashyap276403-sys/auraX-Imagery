package com.aurax.imagery.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurax.imagery.AppContainer
import com.aurax.imagery.domain.model.Grouping
import com.aurax.imagery.domain.model.SortField
import com.aurax.imagery.domain.model.SortOrder
import com.aurax.imagery.domain.usecase.BuildTimelineUseCase
import com.aurax.imagery.domain.usecase.SearchFilter
import com.aurax.imagery.domain.usecase.SearchMediaUseCase
import com.aurax.imagery.domain.usecase.SearchType
import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.ui.gallery.TimelineRow
import com.aurax.imagery.ui.gallery.flattenTimeline
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class SearchUiState(
    val query: String = "",
    val filter: SearchFilter = SearchFilter(),
    val rows: List<TimelineRow> = emptyList(),
    val resultCount: Int = 0,
    /** True when there is something to search for (text or a filter), so "no results" is meaningful. */
    val active: Boolean = false,
)

/** Searches the on-device index. Typing is debounced; filtering and grouping run off the main thread. */
@OptIn(FlowPreview::class)
class SearchViewModel(
    media: Flow<List<MediaItem>>,
    private val search: SearchMediaUseCase,
    private val buildTimeline: BuildTimelineUseCase,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(SearchFilter())

    /** The text box shows [query] immediately; only the heavy work waits for a pause in typing. */
    private val debouncedQuery = query.debounce(200)

    private val newestFirst = SortOrder(SortField.Date, ascending = false)

    private val results: Flow<SearchUiState> =
        combine(media, debouncedQuery, filter) { items, q, f ->
            val zone = ZoneId.systemDefault()
            val locale = Locale.getDefault()
            val hits = search(items, q, f, zone, locale)
            val sections = buildTimeline(hits, newestFirst, Grouping.Day, LocalDate.now(zone), zone, locale)
            SearchUiState(
                query = q,
                filter = f,
                rows = flattenTimeline(sections),
                resultCount = hits.size,
                active = q.isNotBlank() || f != SearchFilter(),
            )
        }.flowOn(Dispatchers.Default)

    val uiState: StateFlow<SearchUiState> =
        combine(results, query) { r, typed -> r.copy(query = typed) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchUiState())

    fun setQuery(text: String) = query.update { text }

    fun setType(type: SearchType) = filter.update { it.copy(type = type) }

    fun toggleFavoritesOnly() = filter.update { it.copy(favoritesOnly = !it.favoritesOnly) }

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer {
                SearchViewModel(container.observeMedia(), container.searchMedia, container.buildTimeline)
            }
        }
    }
}
