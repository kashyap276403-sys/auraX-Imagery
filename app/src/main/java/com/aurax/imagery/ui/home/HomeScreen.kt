package com.aurax.imagery.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.runtime.Composable
import com.aurax.imagery.ui.components.EmptyState
import com.aurax.imagery.ui.components.ScreenScaffold

/**
 * Phase 1 scaffold. The real timeline (MediaStore-backed, grouped by day/month/year, with
 * selection mode and sorting) replaces the empty state in Phases 2-3.
 */
@Composable
fun HomeScreen() {
    ScreenScaffold(title = "Photos", subtitle = "EVERY MEMORY, BEAUTIFULLY.") {
        EmptyState(
            icon = Icons.Rounded.PhotoLibrary,
            title = "Your library is empty",
            message = "Photos and videos from this device will appear here once media access is set up.",
        )
    }
}
