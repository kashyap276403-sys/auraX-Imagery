package com.aurax.imagery.ui.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurax.imagery.AppContainer
import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.domain.repository.PreferencesRepository
import com.aurax.imagery.domain.usecase.SetFavoritesUseCase
import com.aurax.imagery.domain.usecase.sortMedia
import com.aurax.imagery.ui.gallery.MediaSource
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

data class ViewerUiState(
    val items: List<MediaItem> = emptyList(),
    val loaded: Boolean = false,
)

/**
 * Feeds the full-screen pager with the same items, in the same order, as the grid the user
 * tapped in (same source, same sort), so swiping left/right matches what they were looking at.
 */
class ViewerViewModel(
    source: Flow<List<MediaItem>>,
    preferences: PreferencesRepository,
    private val setFavorites: SetFavoritesUseCase,
) : ViewModel() {

    /** Items moved to trash: hidden at once, because the index only catches up a moment later. */
    private val hidden = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<ViewerUiState> =
        combine(source, preferences.gallery, hidden) { list, prefs, hiddenIds ->
            val visible = if (hiddenIds.isEmpty()) list else list.filter { it.id !in hiddenIds }
            ViewerUiState(items = sortMedia(visible, prefs.sort), loaded = true)
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ViewerUiState())

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch { setFavorites(listOf(item.id), !item.isFavorite) }
    }

    fun markTrashed(ids: List<Long>) {
        hidden.update { it + ids }
    }

    companion object {
        fun factory(container: AppContainer, source: MediaSource) = viewModelFactory {
            initializer {
                ViewerViewModel(
                    source = when (source) {
                        MediaSource.All -> container.observeMedia()
                        is MediaSource.Album -> container.observeAlbumMedia(source.ref)
                    },
                    preferences = container.preferences,
                    setFavorites = container.setFavorites,
                )
            }
        }
    }
}
