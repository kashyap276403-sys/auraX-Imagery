package com.aurax.imagery.ui.search

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import com.aurax.imagery.ui.components.EmptyState
import com.aurax.imagery.ui.components.ScreenScaffold

/** Phase 1 scaffold. Search by filename, album and metadata arrives in Phase 5. */
@Composable
fun SearchScreen() {
    ScreenScaffold(title = "Search") {
        EmptyState(
            icon = Icons.Rounded.Search,
            title = "Search your library",
            message = "Find photos and videos by filename, album or metadata.",
        )
    }
}
