package com.aurax.imagery.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
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
import com.aurax.imagery.ui.components.GlassNavItem
import com.aurax.imagery.ui.components.LocalFloatingBarInset
import com.aurax.imagery.ui.glass.LiquidNavCapsule
import com.aurax.imagery.ui.glass.LocalGlassTilt
import com.aurax.imagery.ui.glass.LocalHazeState
import com.aurax.imagery.ui.glass.rememberGlassTilt
import com.aurax.imagery.ui.glass.rememberReducedMotion
import com.aurax.imagery.ui.navigation.AuraDestination
import com.aurax.imagery.ui.navigation.AuraNavHost
import com.aurax.imagery.ui.navigation.AuraRoutes
import com.aurax.imagery.ui.theme.LocalGlassStyle
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * Root composable. The whole app (background + current screen) is the Haze backdrop source; the
 * floating liquid nav capsule is a sibling drawn above it, so it blurs and refracts whatever
 * photos are scrolling underneath. The source never contains the capsule, which is what keeps
 * the capture free of feedback loops.
 */
@Composable
fun AuraXRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val inAlbumDetail = AuraRoutes.isAlbumDetail(currentRoute)
    val hideBar = AuraRoutes.hidesBottomBar(currentRoute)
    val navItems = remember {
        AuraDestination.entries.map { GlassNavItem(it.route, it.label, it.icon) }
    }

    val style = LocalGlassStyle.current
    val hazeState = rememberHazeState()
    val reducedMotion = rememberReducedMotion()
    val tilt = rememberGlassTilt(enabled = style.motionEffects && !reducedMotion)
    // An album opened from the Albums tab keeps that tab highlighted.
    val activeRoute = if (inAlbumDetail) AuraDestination.Albums.route else currentRoute
    val selectedIndex = navItems.indexOfFirst { it.route == activeRoute }.coerceAtLeast(0)

    Box(modifier = Modifier.fillMaxSize()) {
        AuraBackground(modifier = Modifier.hazeSource(hazeState)) {
            // 72dp bar + 12dp bottom margin + breathing room; screens add this as bottom padding.
            CompositionLocalProvider(LocalFloatingBarInset provides style.dockHeight + 36.dp) {
                AuraNavHost(navController = navController, modifier = Modifier.fillMaxSize())
            }
        }

        // The viewer and editor are full-screen, so the tab bar slides away while they are open.
        AnimatedVisibility(
            visible = !hideBar,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            CompositionLocalProvider(LocalHazeState provides hazeState, LocalGlassTilt provides tilt) {
                LiquidNavCapsule(
                    items = navItems,
                    selectedIndex = selectedIndex,
                    onSelect = { index ->
                        val item = navItems[index]
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
}
