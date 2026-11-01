package com.aurax.imagery.ui.albums

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import com.aurax.imagery.domain.player.RecentVideo
import com.aurax.imagery.ui.gallery.MediaSource
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.item
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.aurax.imagery.AuraXApp
import com.aurax.imagery.domain.model.Album
import com.aurax.imagery.domain.model.AlbumRef
import com.aurax.imagery.ui.components.EmptyState
import com.aurax.imagery.ui.components.LoadingState
import com.aurax.imagery.ui.components.LocalFloatingBarInset
import com.aurax.imagery.ui.components.TabScaffold
import com.aurax.imagery.ui.permissions.MediaAccessPrompt
import com.aurax.imagery.ui.permissions.rememberMediaAccessState
import com.aurax.imagery.utils.MediaAccess

/**
 * Albums tab: Favorites and Videos (when non-empty), then the device folders found in
 * MediaStore (Camera, Screenshots, Download, WhatsApp media where accessible, ...).
 */
@Composable
fun AlbumsScreen(
    onOpenAlbum: (Album) -> Unit,
    onOpenViewer: (MediaSource, Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val container = (LocalContext.current.applicationContext as AuraXApp).container
    val access = rememberMediaAccessState(onAccessChanged = { container.indexer.enqueue() })

    TabScaffold(onOpenSettings = onOpenSettings) {
        if (access.access == MediaAccess.None) {
            MediaAccessPrompt(access)
        } else {
            val viewModel: AlbumsViewModel = viewModel(factory = AlbumsViewModel.factory(container))
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            // Favorites is always reachable from here, even before the first heart is tapped.
            val albums = remember(state.albums) {
                if (state.albums.any { it.ref == AlbumRef.Favorites }) state.albums
                else listOf(Album(AlbumRef.Favorites, "Favorites", null, 0)) + state.albums
            }
            when {
                !state.hasLoaded -> LoadingState()

                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = LocalFloatingBarInset.current,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    val unfinished = state.recent.filter { it.resumeAt != null }.take(10)
                    val played = state.recent.filter { it.resumeAt == null }.take(10)
                    if (unfinished.isNotEmpty()) {
                        item(key = "continue", span = { GridItemSpan(maxLineSpan) }) {
                            RecentRow("Continue watching", unfinished) { onOpenViewer(MediaSource.All, it.item.id) }
                        }
                    }
                    if (played.isNotEmpty()) {
                        item(key = "played", span = { GridItemSpan(maxLineSpan) }) {
                            RecentRow("Recently played", played) { onOpenViewer(MediaSource.All, it.item.id) }
                        }
                    }
                    items(items = albums, key = { it.ref.toString() }) { album ->
                        AlbumTile(album = album, onClick = { onOpenAlbum(album) })
                    }
                }
            }
        }
    }
}

/** Horizontal strip of video cards; unfinished ones carry a thin progress bar. */
@Composable
private fun RecentRow(title: String, videos: List<RecentVideo>, onOpen: (RecentVideo) -> Unit) {
    Column {
        Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items = videos, key = { it.item.id }) { video ->
                val shape = RoundedCornerShape(16.dp)
                Column(modifier = Modifier.width(156.dp).clip(shape).clickable { onOpen(video) }) {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(shape).background(Color(0xFF111114))) {
                        AsyncImage(
                            model = video.item.uri,
                            contentDescription = video.item.displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (video.resumeAt != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(Color.White.copy(alpha = 0.3f)),
                            ) {
                                Box(Modifier.fillMaxWidth(video.fraction).fillMaxHeight().background(Color.White))
                            }
                        }
                    }
                    Text(
                        text = video.item.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 2.dp, top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumTile(album: Album, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(modifier = Modifier.clip(shape).clickable(onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(Color(0xFF111114))
                .border(1.dp, Color.White.copy(alpha = 0.12f), shape),
        ) {
            if (album.coverUri != null) {
                AsyncImage(
                    model = album.coverUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = if (album.ref == AlbumRef.Favorites) Icons.Rounded.Favorite else Icons.Rounded.PhotoAlbum,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).size(40.dp),
                )
            }
            if (album.ref == AlbumRef.Favorites) {
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(22.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Text(
            text = if (album.itemCount == 1) "1 item" else "${album.itemCount} items",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
