package com.aurax.imagery.ui.favorites

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aurax.imagery.AuraXApp
import com.aurax.imagery.domain.model.AlbumRef
import com.aurax.imagery.ui.gallery.MediaListScreen
import com.aurax.imagery.ui.gallery.MediaListViewModel
import com.aurax.imagery.ui.gallery.MediaSource

/** Favorites tab. Same timeline and selection behaviour as Photos, limited to favorited items. */
@Composable
fun FavoritesScreen(onOpenViewer: (MediaSource, Long) -> Unit) {
    val container = (LocalContext.current.applicationContext as AuraXApp).container
    val source = MediaSource.Album(AlbumRef.Favorites)
    val viewModel: MediaListViewModel = viewModel(
        key = "favorites",
        factory = MediaListViewModel.factory(container, source),
    )
    MediaListScreen(
        title = "Favorites",
        source = source,
        viewModel = viewModel,
        emptyIcon = Icons.Rounded.Favorite,
        emptyTitle = "No favorites yet",
        emptyMessage = "Long-press photos or videos, then tap the heart to collect them here.",
        onOpenViewer = onOpenViewer,
    )
}
