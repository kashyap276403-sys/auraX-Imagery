package com.aurax.imagery.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import com.aurax.imagery.ui.theme.AuraMotion
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.aurax.imagery.ui.albums.AlbumDetailScreen
import com.aurax.imagery.ui.albums.AlbumsScreen
import com.aurax.imagery.ui.editor.EditorScreen
import com.aurax.imagery.ui.gallery.MediaSource
import com.aurax.imagery.ui.glass.studio.GlassStudioScreen
import com.aurax.imagery.ui.home.HomeScreen
import com.aurax.imagery.ui.search.SearchScreen
import com.aurax.imagery.ui.settings.SettingsScreen
import com.aurax.imagery.ui.viewer.ViewerScreen

/**
 * Top-level navigation graph. Tabs cross-fade with a slight scale-in, which reads as the
 * content "settling" behind the glass bar instead of sliding sideways.
 */
@Composable
fun AuraNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val openViewer: (MediaSource, Long) -> Unit = { source, mediaId ->
        navController.navigate(AuraRoutes.viewer(source, mediaId))
    }

    val openSettings: () -> Unit = { navController.navigate(AuraRoutes.SETTINGS) }

    NavHost(
        navController = navController,
        startDestination = AuraDestination.Photos.route,
        modifier = modifier,
        // Tab to tab: slides a little in the direction of travel while cross-fading. Other screens fade + scale.
        enterTransition = {
            val dir = tabDirection(initialState.destination.route, targetState.destination.route)
            if (dir != 0) fadeIn(tween(320, easing = AuraMotion.Enter)) + slideInHorizontally(tween(420, easing = AuraMotion.Enter)) { it / 14 * dir }
            else fadeIn(tween(280)) + scaleIn(initialScale = 0.97f, animationSpec = tween(280))
        },
        exitTransition = {
            val dir = tabDirection(initialState.destination.route, targetState.destination.route)
            if (dir != 0) fadeOut(tween(160, easing = AuraMotion.Exit)) + slideOutHorizontally(tween(300, easing = AuraMotion.Exit)) { -it / 20 * dir }
            else fadeOut(tween(160))
        },
        popEnterTransition = { fadeIn(tween(280)) + scaleIn(initialScale = 0.97f, animationSpec = tween(280)) },
        popExitTransition = { fadeOut(tween(160)) },
    ) {
        composable(AuraDestination.Photos.route) { HomeScreen(onOpenViewer = openViewer, onOpenSettings = openSettings) }
        composable(AuraDestination.Albums.route) {
            AlbumsScreen(
                onOpenAlbum = { album -> navController.navigate(AuraRoutes.album(album)) },
                onOpenViewer = openViewer,
                onOpenSettings = openSettings,
                onOpenTrash = { navController.navigate(AuraRoutes.TRASH) },
            )
        }
        composable(AuraDestination.Search.route) { SearchScreen(onOpenViewer = openViewer, onOpenSettings = openSettings) }
        composable(AuraRoutes.SETTINGS) {
            SettingsScreen(
                onOpenGlassStudio = { navController.navigate(AuraRoutes.GLASS_STUDIO) },
                onOpenInsights = { navController.navigate(AuraRoutes.INSIGHTS) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(AuraRoutes.INSIGHTS) { com.aurax.imagery.ui.gallery.InsightsScreen(onBack = { navController.popBackStack() }) }
        composable(AuraRoutes.TRASH) { com.aurax.imagery.ui.gallery.TrashScreen(onBack = { navController.popBackStack() }) }
        composable(AuraRoutes.GLASS_STUDIO) { GlassStudioScreen(onBack = { navController.popBackStack() }) }

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
                onOpenViewer = openViewer,
            )
        }

        composable(
            route = AuraRoutes.VIEWER_PATTERN,
            arguments = listOf(
                navArgument(AuraRoutes.ARG_KIND) { type = NavType.StringType },
                navArgument(AuraRoutes.ARG_ALBUM_ID) { type = NavType.LongType },
                navArgument(AuraRoutes.ARG_MEDIA_ID) { type = NavType.LongType },
            ),
            // The picture opens with a soft springy zoom and closes by settling back, instead of a plain fade.
            enterTransition = {
                fadeIn(tween(220, easing = AuraMotion.Enter)) + scaleIn(initialScale = 0.9f, animationSpec = tween(420, easing = AuraMotion.Enter))
            },
            popExitTransition = {
                fadeOut(tween(200, easing = AuraMotion.Exit)) + scaleOut(targetScale = 0.94f, animationSpec = tween(260, easing = AuraMotion.Exit))
            },
        ) { entry ->
            val args = entry.arguments
            ViewerScreen(
                source = AuraRoutes.mediaSource(
                    kind = args?.getString(AuraRoutes.ARG_KIND).orEmpty(),
                    albumId = args?.getLong(AuraRoutes.ARG_ALBUM_ID) ?: 0L,
                ),
                startMediaId = args?.getLong(AuraRoutes.ARG_MEDIA_ID) ?: 0L,
                onClose = { navController.popBackStack() },
                onEdit = { mediaId -> navController.navigate(AuraRoutes.editor(mediaId)) },
            )
        }

        composable(
            route = AuraRoutes.EDITOR_PATTERN,
            arguments = listOf(navArgument(AuraRoutes.ARG_MEDIA_ID) { type = NavType.LongType }),
        ) { entry ->
            EditorScreen(
                mediaId = entry.arguments?.getLong(AuraRoutes.ARG_MEDIA_ID) ?: 0L,
                onClose = { navController.popBackStack() },
            )
        }
    }
}

/** +1 when moving to a tab on the right, -1 to the left, 0 when either side is not a dock tab. */
internal fun tabDirection(from: String?, to: String?): Int {
    val tabs = AuraDestination.entries.map { it.route }
    val a = tabs.indexOf(from)
    val b = tabs.indexOf(to)
    return if (a < 0 || b < 0 || a == b) 0 else if (b > a) 1 else -1
}
