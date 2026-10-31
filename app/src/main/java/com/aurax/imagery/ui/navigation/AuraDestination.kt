package com.aurax.imagery.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/** The five top-level tabs shown in the floating glass bar, in display order. */
enum class AuraDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Photos("photos", "Photos", Icons.Rounded.PhotoLibrary),
    Albums("albums", "Albums", Icons.Rounded.PhotoAlbum),
    Search("search", "Search", Icons.Rounded.Search),
    Favorites("favorites", "Favorites", Icons.Rounded.Favorite),
    Settings("settings", "Settings", Icons.Rounded.Settings),
}
