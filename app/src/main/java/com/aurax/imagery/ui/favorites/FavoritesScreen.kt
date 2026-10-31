package com.aurax.imagery.ui.favorites

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.runtime.Composable
import com.aurax.imagery.ui.components.EmptyState
import com.aurax.imagery.ui.components.ScreenScaffold

/** Phase 1 scaffold. Favorites are stored in Room and listed here from Phase 3. */
@Composable
fun FavoritesScreen() {
    ScreenScaffold(title = "Favorites") {
        EmptyState(
            icon = Icons.Rounded.Favorite,
            title = "No favorites yet",
            message = "Photos and videos you mark as favorite will be collected here.",
        )
    }
}
