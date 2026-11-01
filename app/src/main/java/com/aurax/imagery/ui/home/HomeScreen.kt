package com.aurax.imagery.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aurax.imagery.AuraXApp
import com.aurax.imagery.ui.components.TabScaffold
import com.aurax.imagery.ui.gallery.MediaListScreen
import com.aurax.imagery.ui.gallery.MediaListViewModel
import com.aurax.imagery.ui.gallery.MediaSource
import com.aurax.imagery.ui.permissions.MediaAccessPrompt
import com.aurax.imagery.ui.permissions.PartialAccessBanner
import com.aurax.imagery.ui.permissions.rememberMediaAccessState
import com.aurax.imagery.utils.MediaAccess

/** Photos tab: the whole library as a date-grouped timeline, behind the media-access flow. */
@Composable
fun HomeScreen(onOpenViewer: (MediaSource, Long) -> Unit, onOpenSettings: () -> Unit) {
    val container = (LocalContext.current.applicationContext as AuraXApp).container
    // Any access change (first grant, switch to partial, revoke in settings) re-syncs the index.
    val access = rememberMediaAccessState(onAccessChanged = { container.indexer.enqueue() })

    if (access.access == MediaAccess.None) {
        TabScaffold(onOpenSettings = onOpenSettings) {
            MediaAccessPrompt(access)
        }
    } else {
        val viewModel: MediaListViewModel = viewModel(
            key = "photos",
            factory = MediaListViewModel.factory(container, MediaSource.All),
        )
        MediaListScreen(
            title = "Photos",
            source = MediaSource.All,
            viewModel = viewModel,
            emptyIcon = Icons.Rounded.PhotoLibrary,
            emptyTitle = "No photos or videos",
            emptyMessage = "Nothing was found on this device yet. Take a photo and it will show up here.",
            onOpenViewer = onOpenViewer,
            onOpenSettings = onOpenSettings,
            header = { PartialAccessBanner(access) },
        )
    }
}
