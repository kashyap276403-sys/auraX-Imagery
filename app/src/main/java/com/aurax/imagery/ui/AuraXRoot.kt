package com.aurax.imagery.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aurax.imagery.ui.components.AuraBackground
import com.aurax.imagery.ui.components.GlassBottomBar
import com.aurax.imagery.ui.components.GlassNavItem
import com.aurax.imagery.ui.components.LocalFloatingBarInset
import com.aurax.imagery.ui.navigation.AuraDestination
import com.aurax.imagery.ui.navigation.AuraNavHost
import com.aurax.imagery.ui.navigation.AuraRoutes

/** Root composable: background, navigation graph, and the floating glass bar on top. */
@Composable
fun AuraXRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val inAlbumDetail = AuraRoutes.isAlbumDetail(currentRoute)
    val inViewer = AuraRoutes.isViewer(currentRoute)
    val navItems = remember {
        AuraDestination.entries.map { GlassNavItem(it.route, it.label, it.icon) }
    }

    AuraBackground {
        // 72dp bar + 12dp bottom margin + breathing room; screens add this as bottom padding.
        CompositionLocalProvider(LocalFloatingBarInset provides 108.dp) {
            AuraNavHost(navController = navController, modifier = Modifier.fillMaxSize())
        }
        // The viewer is full-screen and immersive, so the tab bar slides away while it is open.
        AnimatedVisibility(
            visible = !inViewer,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
        GlassBottomBar(
            items = navItems,
            // An album opened from the Albums tab keeps that tab highlighted.
            selectedRoute = if (inAlbumDetail) AuraDestination.Albums.route else currentRoute,
            onSelect = { item ->
                if (inAlbumDetail && item.route == AuraDestination.Albums.route) {
                    // Tapping the active tab from inside an album returns to the album list.
                    navController.popBackStack(AuraDestination.Albums.route, inclusive = false)
                } else {
                    navController.navigate(item.route) {
                        // Standard bottom-nav behaviour: one copy of each tab, state preserved.
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
        }
    }
}
