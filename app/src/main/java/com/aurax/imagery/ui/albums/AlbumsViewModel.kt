package com.aurax.imagery.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurax.imagery.AppContainer
import com.aurax.imagery.domain.model.Album
import com.aurax.imagery.domain.usecase.ObserveAlbumsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AlbumsUiState(
    val albums: List<Album> = emptyList(),
    /** False until the first database emission, so the UI shows loading, not "no albums". */
    val hasLoaded: Boolean = false,
)

class AlbumsViewModel(observeAlbums: ObserveAlbumsUseCase) : ViewModel() {

    val uiState: StateFlow<AlbumsUiState> = observeAlbums()
        .map { AlbumsUiState(albums = it, hasLoaded = true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AlbumsUiState())

    companion object {
        fun factory(container: AppContainer) = viewModelFactory {
            initializer { AlbumsViewModel(container.observeAlbums) }
        }
    }
}
