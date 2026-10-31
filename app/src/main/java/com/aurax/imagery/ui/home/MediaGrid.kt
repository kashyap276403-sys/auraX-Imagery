package com.aurax.imagery.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.domain.model.MediaType
import com.aurax.imagery.ui.components.LocalFloatingBarInset
import com.aurax.imagery.utils.formatDuration

/**
 * Thumbnail grid. Uses a lazy grid with stable keys, and Coil sizes each request to its cell
 * so only small decoded bitmaps are held in memory. No blur or glass effects are applied per
 * cell, which keeps scrolling cheap. The date-grouped timeline replaces this in Phase 3.
 */
@Composable
fun MediaGrid(
    items: List<MediaItem>,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 104.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 2.dp,
            end = 2.dp,
            top = 8.dp,
            bottom = LocalFloatingBarInset.current,
        ),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(items = items, key = { it.id }, contentType = { it.type }) { item ->
            MediaThumbnail(item)
        }
    }
}

@Composable
private fun MediaThumbnail(item: MediaItem) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(Color(0xFF111114)),
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (item.type == MediaType.Video && item.durationMillis != null) {
            Text(
                text = formatDuration(item.durationMillis),
                color = Color.White,
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}
