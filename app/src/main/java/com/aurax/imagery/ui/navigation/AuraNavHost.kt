package com.aurax.imagery.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aurax.imagery.ui.albums.AlbumDetailScreen
import com.aurax.imagery.ui.albums.AlbumsScreen
import com.aurax.imagery.ui.favorites.FavoritesScreen
import com.aurax.imagery.ui.home.HomeScreen
import com.aurax.imagery.ui.search.SearchScreen
import com.aurax.imagery.ui.settings.SettingsScreen

/**
 * Top-level navigation graph. Tabs cross-fade with a slight scale-in, which reads as the
 * content "settling" behind the glass bar instead of sliding sideways.
 */
@Composable
fun AuraNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = AuraDestination.Photos.route,
        modifier = modifier,
        enterTransition = { fadeIn(tween(280)) + scaleIn(initialScale = 0.97f, animationSpec = tween(280)) },
        exitTransition = { fadeOut(tween(160)) },
        popEnterTransition = { fadeIn(tween(280)) + scaleIn(initialScale = 0.97f, animationSpec = tween(280)) },
        popExitTransition = { fadeOut(tween(160)) },
    ) {
        composable(AuraDestination.Photos.route) { HomeScreen() }
        composable(AuraDestination.Albums.route) {
            AlbumsScreen(onOpenAlbum = { album -> navController.navigate(AuraRoutes.album(album)) })
        }
        composable(AuraDestination.Search.route) { SearchScreen() }
        composable(AuraDestination.Favorites.route) { FavoritesScreen() }
        composable(AuraDestination.Settings.route) { SettingsScreen() }

        composable(
            route = AuraRoutes.ALBUM_PATTERN,
            arguments = listOf(
                navArgument(AuraRoutes.ARG_KIND) { type = NavType.StringType },
                navArgument(AuraRoutes.ARG_ID) { type = NavType.LongType },
                navArgument(AuraRoutes.ARG_NAME) {
                    type = NavType.StringType
                    defaultValue = "Album"
                },
            ),
        ) { entry ->
            val args = entry.arguments
            AlbumDetailScreen(
                ref = AuraRoutes.albumRef(
                    kind = args?.getString(AuraRoutes.ARG_KIND).orEmpty(),
                    id = args?.getLong(AuraRoutes.ARG_ID) ?: 0L,
                ),
                title = args?.getString(AuraRoutes.ARG_NAME) ?: "Album",
                onBack = { navController.popBackStack() },
            )
        }
    }
}
