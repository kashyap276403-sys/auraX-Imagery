package com.aurax.imagery.ui.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurax.imagery.AuraXApp
import com.aurax.imagery.domain.model.GalleryPreferences
import com.aurax.imagery.domain.model.Grouping
import com.aurax.imagery.domain.model.SortField
import com.aurax.imagery.domain.model.SortOrder
import com.aurax.imagery.ui.components.EmptyState
import com.aurax.imagery.ui.components.GlassTopBar
import com.aurax.imagery.ui.components.LoadingState
import com.aurax.imagery.ui.theme.LocalGlassStyle

/**
 * Complete list screen used for Photos, Favorites and album detail: glass top bar, sort/group
 * menu, the date-grouped grid, and multi-select mode (long-press to start, with favorite,
 * select-all and share actions).
 */
@Composable
fun MediaListScreen(
    title: String,
    source: MediaSource,
    viewModel: MediaListViewModel,
    emptyIcon: ImageVector,
    emptyTitle: String,
    emptyMessage: String,
    onOpenViewer: (MediaSource, Long) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    header: @Composable () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val container = (context.applicationContext as AuraXApp).container
    // System-confirmed move to trash; afterwards leave selection mode and re-sync the index.
    val trash = rememberTrashAction {
        viewModel.clearSelection()
        container.indexer.enqueue()
    }
    val haptics = LocalHapticFeedback.current
    val glass = LocalGlassStyle.current
    val selecting = state.selectedIds.isNotEmpty()

    // Back leaves selection mode first, then the screen.
    BackHandler(enabled = selecting) { viewModel.clearSelection() }

    Column(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        if (selecting) {
            GlassTopBar(
                title = "${state.selectedIds.size} selected",
                navigationIcon = Icons.Rounded.Close,
                navigationDescription = "Clear selection",
                onNavigationClick = viewModel::clearSelection,
                actions = {
                    IconButton(onClick = viewModel::selectAll) {
                        Icon(Icons.Rounded.SelectAll, contentDescription = "Select all")
                    }
                    IconButton(onClick = viewModel::toggleFavoriteForSelection) {
                        Icon(
                            imageVector = if (state.selectionAllFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (state.selectionAllFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (state.selectionAllFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = { shareItems(context, viewModel.selectedItems()) }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { trash(viewModel.selectedItems()) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Move to trash")
                    }
                },
            )
        } else {
            GlassTopBar(
                title = title,
                subtitle = subtitle,
                navigationIcon = if (onBack != null) Icons.AutoMirrored.Rounded.ArrowBack else null,
                navigationDescription = "Back",
                onNavigationClick = { onBack?.invoke() },
                actions = {
                    if (state.itemCount > 0) {
                        SortGroupMenu(
                            preferences = state.preferences,
                            onSort = viewModel::setSort,
                            onGroup = viewModel::setGrouping,
                        )
                    }
                },
            )
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                !state.hasLoaded || (state.itemCount == 0 && state.isIndexing) -> LoadingState()

                state.itemCount == 0 -> EmptyState(icon = emptyIcon, title = emptyTitle, message = emptyMessage)

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    header()
                    MediaTimelineGrid(
                        rows = state.rows,
                        selectedIds = state.selectedIds,
                        selecting = selecting,
                        onItemClick = { item ->
                            if (selecting) viewModel.toggleSelection(item.id) else onOpenViewer(source, item.id)
                        },
                        onItemLongClick = { item ->
                            if (glass.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleSelection(item.id)
                        },
                        onSectionToggle = viewModel::toggleSection,
                    )
                }
            }
        }
    }
}

/** Overflow-style menu: choose what to sort by (tap the active one to flip direction) and how to group. */
@Composable
private fun SortGroupMenu(
    preferences: GalleryPreferences,
    onSort: (SortOrder) -> Unit,
    onGroup: (Grouping) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val sort = preferences.sort

    IconButton(onClick = { expanded = true }) {
        Icon(Icons.Rounded.Tune, contentDescription = "Sort and group")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        Text(
            text = "SORT BY",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        SortField.entries.forEach { field ->
            val active = sort.field == field
            DropdownMenuItem(
                text = { Text(field.label) },
                trailingIcon = if (active) {
                    {
                        Icon(
                            imageVector = if (sort.ascending) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                            contentDescription = if (sort.ascending) "Ascending" else "Descending",
                        )
                    }
                } else {
                    null
                },
                onClick = {
                    onSort(if (active) sort.copy(ascending = !sort.ascending) else SortOrder(field, field.defaultAscending))
                },
            )
        }
        HorizontalDivider()
        Text(
            text = "GROUP BY",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Grouping.entries.forEach { grouping ->
            DropdownMenuItem(
                // Grouping only applies to the date timeline; name/size sorts are one flat list.
                enabled = sort.field == SortField.Date,
                text = { Text(grouping.label) },
                leadingIcon = if (preferences.grouping == grouping) {
                    { Icon(Icons.Rounded.Check, contentDescription = "Selected") }
                } else {
                    null
                },
                onClick = { onGroup(grouping) },
            )
        }
    }
}
