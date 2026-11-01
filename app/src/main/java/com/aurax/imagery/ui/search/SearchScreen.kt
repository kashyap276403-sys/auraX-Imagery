package com.aurax.imagery.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aurax.imagery.AuraXApp
import com.aurax.imagery.domain.usecase.SearchType
import com.aurax.imagery.ui.components.EmptyState
import com.aurax.imagery.ui.components.GlassSurface
import com.aurax.imagery.ui.components.TabScaffold
import com.aurax.imagery.ui.gallery.MediaSource
import com.aurax.imagery.ui.gallery.MediaTimelineGrid

/**
 * Search tab: a glass search field, type/favorite filters and the results as the same date-grouped
 * grid used elsewhere. Everything is matched on-device against the local index.
 */
@Composable
fun SearchScreen(onOpenViewer: (MediaSource, Long) -> Unit, onOpenSettings: () -> Unit) {
    val container = (LocalContext.current.applicationContext as AuraXApp).container
    val viewModel: SearchViewModel = viewModel(key = "search", factory = SearchViewModel.factory(container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current

    TabScaffold(onOpenSettings = onOpenSettings) {
        Column(modifier = Modifier.fillMaxSize()) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                TextField(
                    value = state.query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Name, album, month, year, type…") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setQuery("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    ),
                )
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(SearchType.entries) { type ->
                    FilterChip(
                        selected = state.filter.type == type,
                        onClick = { viewModel.setType(type) },
                        label = { Text(type.label) },
                    )
                }
                item {
                    FilterChip(
                        selected = state.filter.favoritesOnly,
                        onClick = viewModel::toggleFavoritesOnly,
                        label = { Text("Favorites") },
                        leadingIcon = { Icon(Icons.Rounded.Favorite, contentDescription = null) },
                    )
                }
            }

            when {
                !state.active -> EmptyState(
                    icon = Icons.Rounded.Search,
                    title = "Search your library",
                    message = "Try a file name, an album, a month like “october”, a year, or “video”. Everything is searched on this device.",
                )

                state.resultCount == 0 -> EmptyState(
                    icon = Icons.Rounded.SearchOff,
                    title = "No results",
                    message = "Nothing matches. Check the spelling or loosen the filters.",
                )

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = if (state.resultCount == 1) "1 result" else "${state.resultCount} results",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                    MediaTimelineGrid(
                        rows = state.rows,
                        selectedIds = emptySet(),
                        selecting = false,
                        // Search results open in the viewer over the whole library, positioned on the tapped item.
                        onItemClick = { item -> onOpenViewer(MediaSource.All, item.id) },
                        onItemLongClick = {},
                        onSectionToggle = {},
                    )
                }
            }
        }
    }
}
