package com.aurax.imagery.ui

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

/** Root composable: background, navigation graph, and the floating glass bar on top. */
@Composable
fun AuraXRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val navItems = remember {
        AuraDestination.entries.map { GlassNavItem(it.route, it.label, it.icon) }
    }

    AuraBackground {
        // 72dp bar + 12dp bottom margin + breathing room; screens add this as bottom padding.
        CompositionLocalProvider(LocalFloatingBarInset provides 108.dp) {
            AuraNavHost(navController = navController, modifier = Modifier.fillMaxSize())
        }
        GlassBottomBar(
            items = navItems,
            selectedRoute = currentRoute,
            onSelect = { item ->
                navController.navigate(item.route) {
                    // Standard bottom-nav behaviour: one copy of each tab, state preserved.
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}
