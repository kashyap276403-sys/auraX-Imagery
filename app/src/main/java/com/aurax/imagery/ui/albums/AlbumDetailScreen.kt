package com.aurax.imagery.ui.albums

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aurax.imagery.AuraXApp
import com.aurax.imagery.domain.model.AlbumRef
import com.aurax.imagery.ui.gallery.MediaListScreen
import com.aurax.imagery.ui.gallery.MediaListViewModel
import com.aurax.imagery.ui.gallery.MediaSource

/** Contents of one album, with the same timeline, sorting and selection as the Photos tab. */
@Composable
fun AlbumDetailScreen(
    ref: AlbumRef,
    title: String,
    onBack: () -> Unit,
) {
    val container = (LocalContext.current.applicationContext as AuraXApp).container
    val viewModel: MediaListViewModel = viewModel(
        key = "album-$ref",
        factory = MediaListViewModel.factory(container, MediaSource.Album(ref)),
    )
    MediaListScreen(
        title = title,
        viewModel = viewModel,
        emptyIcon = Icons.Rounded.PhotoAlbum,
        emptyTitle = "This album is empty",
        emptyMessage = "Photos and videos in this album will show up here.",
        onBack = onBack,
    )
}
