package com.aurax.imagery.ui.gallery

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
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
import com.aurax.imagery.ui.components.GlassDropdownMenu
import com.aurax.imagery.ui.components.DropIconButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.runtime.produceState
import kotlinx.coroutines.launch
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
import com.aurax.imagery.ui.components.ScrimIconSlot
import com.aurax.imagery.ui.components.GlassTopBar
import com.aurax.imagery.ui.components.TabTopRow
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
    onOpenSettings: (() -> Unit)? = null,
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
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val ops = remember { com.aurax.imagery.data.media.MediaFileOps(context.applicationContext) }
    // Batch Move / Copy: pick a folder first, then (for Move) the system asks to allow editing the files.
    var pickerMode by remember { mutableStateOf<String?>(null) } // "Move" or "Copy"
    var moveTarget by remember { mutableStateOf<Pair<List<com.aurax.imagery.domain.model.MediaItem>, String>?>(null) }
    val albumNames by produceState(emptyList<String>()) {
        container.mediaRepository.observeAlbums().collect { list ->
            value = list.filter { it.ref is com.aurax.imagery.domain.model.AlbumRef.Bucket }.map { it.name }
        }
    }
    val moveLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val target = moveTarget
        moveTarget = null
        if (result.resultCode == android.app.Activity.RESULT_OK && target != null) {
            scope.launch {
                val moved = ops.applyMove(target.first, target.second)
                android.widget.Toast.makeText(context, "Moved $moved to ${target.second}", android.widget.Toast.LENGTH_SHORT).show()
                viewModel.clearSelection()
                container.indexer.enqueue()
            }
        }
    }
    var moreMenu by remember { mutableStateOf(false) }
    val selecting = state.selectedIds.isNotEmpty()

    // Back leaves selection mode first, then the screen.
    BackHandler(enabled = selecting) { viewModel.clearSelection() }

    // Main tabs are full screen: the grid runs under the status bar and the buttons float on top.
    val tabMode = !selecting && onBack == null
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var headerPx by remember { mutableIntStateOf(0) }
    val headerDp = with(density) { headerPx.toDp() }
    val topInset = if (tabMode) statusTop + 64.dp else 0.dp

    Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize().then(if (tabMode) Modifier else Modifier.statusBarsPadding())) {
        if (selecting) {
            GlassTopBar(
                title = "${state.selectedIds.size} selected",
                navigationIcon = Icons.Rounded.Close,
                navigationDescription = "Clear selection",
                onNavigationClick = viewModel::clearSelection,
                actions = {
                    DropIconButton(onClick = viewModel::selectAll) {
                        Icon(Icons.Rounded.SelectAll, contentDescription = "Select all")
                    }
                    DropIconButton(onClick = viewModel::toggleFavoriteForSelection) {
                        Icon(
                            imageVector = if (state.selectionAllFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = if (state.selectionAllFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (state.selectionAllFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                        )
                    }
                    DropIconButton(onClick = { shareItems(context, viewModel.selectedItems()) }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Share")
                    }
                    DropIconButton(onClick = { trash(viewModel.selectedItems()) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Move to trash")
                    }
                    Box {
                        DropIconButton(onClick = { moreMenu = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "More actions")
                        }
                        GlassDropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                            DropdownMenuItem(text = { Text("Move to album") }, onClick = { moreMenu = false; pickerMode = "Move" })
                            DropdownMenuItem(text = { Text("Copy to album") }, onClick = { moreMenu = false; pickerMode = "Copy" })
                            val bucket = (source as? MediaSource.Album)?.ref as? com.aurax.imagery.domain.model.AlbumRef.Bucket
                            if (bucket != null && state.selectedIds.size == 1) {
                                DropdownMenuItem(
                                    text = { Text("Use as album cover") },
                                    onClick = {
                                        moreMenu = false
                                        val cover = viewModel.selectedItems().firstOrNull()
                                        if (cover != null) {
                                            scope.launch { container.preferences.setAlbumCover(bucket.id, cover.uri) }
                                            android.widget.Toast.makeText(context, "Album cover set", android.widget.Toast.LENGTH_SHORT).show()
                                            viewModel.clearSelection()
                                        }
                                    },
                                )
                            }
                        }
                    }
                },
            )
        } else if (onBack != null) {
            // Sub-screen (an album): keep a back button and the title.
            GlassTopBar(
                title = title,
                subtitle = subtitle,
                navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                navigationDescription = "Back",
                onNavigationClick = onBack,
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
                    if (!tabMode) header()
                    MediaTimelineGrid(
                        contentTopPadding = if (tabMode) topInset + headerDp else 4.dp,
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

    if (tabMode) {
        // Banner (e.g. partial access) sits under the buttons; the grid pads itself by its height.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topInset)
                .onSizeChanged { headerPx = it.height },
        ) { header() }
        TabTopRow(
            onOpenSettings = onOpenSettings,
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding(),
        ) {
            if (state.itemCount > 0) {
                ScrimIconSlot {
                    SortGroupMenu(
                        preferences = state.preferences,
                        onSort = viewModel::setSort,
                        onGroup = viewModel::setGrouping,
                    )
                }
            }
        }
    }
    }

    pickerMode?.let { mode ->
        FolderPickerDialog(
            title = "$mode to album",
            existing = albumNames,
            onDismiss = { pickerMode = null },
            onPick = { name ->
                pickerMode = null
                val picked = viewModel.selectedItems()
                if (picked.isNotEmpty()) {
                    if (mode == "Copy") {
                        scope.launch {
                            val result = ops.copy(picked, name)
                            android.widget.Toast.makeText(
                                context,
                                result.fold({ "Copied $it to $name" }, { it.message ?: "Couldn't copy" }),
                                android.widget.Toast.LENGTH_LONG,
                            ).show()
                            viewModel.clearSelection()
                            container.indexer.enqueue()
                        }
                    } else {
                        moveTarget = picked to name
                        runCatching { ops.moveRequest(picked) }
                            .onSuccess { moveLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(it.intentSender).build()) }
                            .onFailure {
                                moveTarget = null
                                android.widget.Toast.makeText(context, "Couldn't start the move", android.widget.Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            },
        )
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

    DropIconButton(onClick = { expanded = true }) {
        Icon(Icons.Rounded.Tune, contentDescription = "Sort and group")
    }
    GlassDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
