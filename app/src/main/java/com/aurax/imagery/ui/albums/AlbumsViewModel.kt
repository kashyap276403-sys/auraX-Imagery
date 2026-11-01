package com.aurax.imagery.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurax.imagery.AppContainer
import com.aurax.imagery.domain.model.Album
import com.aurax.imagery.domain.player.RecentVideo
import com.aurax.imagery.domain.repository.PlaybackHistoryRepository
import com.aurax.imagery.domain.usecase.ObserveAlbumsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AlbumsUiState(
    val albums: List<Album> = emptyList(),
    /** False until the first database emission, so the UI shows loading, not "no albums". */
    val hasLoaded: Boolean = false,
    /** Recently played videos, newest first. */
    val recent: List<RecentVideo> = emptyList(),
)

class AlbumsViewModel(observeAlbums: ObserveAlbumsUseCase, history: PlaybackHistoryRepository) : ViewModel() {

    val uiState: StateFlow<AlbumsUiState> = combine(observeAlbums(), history.observeRecent(RECENT_LIMIT)) { albums, recent ->
        AlbumsUiState(albums = albums, hasLoaded = true, recent = recent)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlbumsUiState())

    companion object {
        private const val RECENT_LIMIT = 20

        fun factory(container: AppContainer) = viewModelFactory {
            initializer { AlbumsViewModel(container.observeAlbums, container.history) }
        }
    }
}
