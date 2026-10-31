package com.aurax.imagery.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurax.imagery.AppContainer
import com.aurax.imagery.data.media.MediaIndexer
import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.domain.usecase.ObserveMediaUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val items: List<MediaItem> = emptyList(),
    val isIndexing: Boolean = false,
    /** False until the first database emission, so the UI can show a loading state, not "empty". */
    val hasLoaded: Boolean = false,
)

class HomeViewModel(
    observeMedia: ObserveMediaUseCase,
    indexer: MediaIndexer,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> =
        combine(observeMedia(), indexer.isIndexing) { items, indexing ->
            HomeUiState(items = items, isIndexing = indexing, hasLoaded = true)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { HomeViewModel(container.observeMedia, container.indexer) }
        }
    }
}
