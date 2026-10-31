package com.aurax.imagery.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
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
        composable(AuraDestination.Albums.route) { AlbumsScreen() }
        composable(AuraDestination.Search.route) { SearchScreen() }
        composable(AuraDestination.Favorites.route) { FavoritesScreen() }
        composable(AuraDestination.Settings.route) { SettingsScreen() }
    }
}
