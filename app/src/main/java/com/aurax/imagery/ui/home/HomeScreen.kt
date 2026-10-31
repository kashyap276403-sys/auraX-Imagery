package com.aurax.imagery.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aurax.imagery.AuraXApp
import com.aurax.imagery.ui.components.EmptyState
import com.aurax.imagery.ui.components.ScreenScaffold
import com.aurax.imagery.ui.permissions.MediaAccessPrompt
import com.aurax.imagery.ui.permissions.PartialAccessBanner
import com.aurax.imagery.ui.permissions.rememberMediaAccessState
import com.aurax.imagery.utils.MediaAccess

/**
 * Photos tab. Phase 2 shows a flat thumbnail grid of the real indexed library, with the full
 * permission flow (denied / partial / full). The date-grouped timeline, selection mode and
 * sorting arrive in Phase 3.
 */
@Composable
fun HomeScreen() {
    val container = (LocalContext.current.applicationContext as AuraXApp).container
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Any access change (first grant, switch to partial, revoke in settings) re-syncs the index.
    val access = rememberMediaAccessState(onAccessChanged = { container.indexer.enqueue() })

    ScreenScaffold(title = "Photos", subtitle = "EVERY MEMORY, BEAUTIFULLY.") {
        when {
            access.access == MediaAccess.None -> MediaAccessPrompt(access)

            !state.hasLoaded || (state.items.isEmpty() && state.isIndexing) -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.items.isEmpty() -> EmptyState(
                icon = Icons.Rounded.PhotoLibrary,
                title = "No photos or videos",
                message = "Nothing was found on this device yet. Take a photo and it will show up here.",
            )

            else -> Column {
                PartialAccessBanner(access)
                MediaGrid(items = state.items)
            }
        }
    }
}
