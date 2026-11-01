package com.aurax.imagery.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The tabs shown in the floating glass dock, in display order. Settings is opened from a small
 * button at the top of each tab, and Favorites from the Albums tab, so neither takes dock space.
 */
enum class AuraDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Photos("photos", "Photos", Icons.Rounded.PhotoLibrary),
    Albums("albums", "Albums", Icons.Rounded.PhotoAlbum),
    Search("search", "Search", Icons.Rounded.Search),
}
