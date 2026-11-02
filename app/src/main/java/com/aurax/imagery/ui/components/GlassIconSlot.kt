package com.aurax.imagery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** A small round glass chip that holds one icon button (or any compact content). */
@Composable
fun GlassIconSlot(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    GlassSurface(
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        cornerRadius = 24.dp,
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** A faint dark round chip for icons that float over photos: no glass, no blur, just legibility. */
@Composable
fun ScrimIconSlot(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Plain icon button on a [ScrimIconSlot]. */
@Composable
fun ScrimIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScrimIconSlot(modifier) {
        DropIconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = contentDescription, tint = androidx.compose.ui.graphics.Color.White)
        }
    }
}

/** Round glass icon button. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassIconSlot(modifier) {
        DropIconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = contentDescription)
        }
    }
}

/**
 * Layout for the three main tabs: no title header, just a slim row of small glass buttons at the
 * top right (screen-specific [actions], then Settings), then the content.
 */
@Composable
fun TabScaffold(
    onOpenSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        TabTopRow(onOpenSettings, actions = actions)
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), content = content)
    }
}

/** The slim button row used at the top of the main tabs. */
@Composable
fun TabTopRow(
    onOpenSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        actions()
        if (onOpenSettings != null) {
            ScrimIconButton(Icons.Rounded.Settings, "Settings", onOpenSettings)
        }
    }
}
