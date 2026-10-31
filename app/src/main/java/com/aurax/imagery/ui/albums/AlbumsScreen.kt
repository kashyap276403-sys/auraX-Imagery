package com.aurax.imagery.ui.albums

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.runtime.Composable
import com.aurax.imagery.ui.components.EmptyState
import com.aurax.imagery.ui.components.ScreenScaffold

/** Phase 1 scaffold. Device albums are discovered from MediaStore in Phase 3. */
@Composable
fun AlbumsScreen() {
    ScreenScaffold(title = "Albums") {
        EmptyState(
            icon = Icons.Rounded.PhotoAlbum,
            title = "No albums yet",
            message = "Camera, Screenshots, Downloads and other albums on this device will be listed here.",
        )
    }
}
