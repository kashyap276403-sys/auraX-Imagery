package com.aurax.imagery.ui.gallery

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.domain.model.MediaType
import com.aurax.imagery.domain.model.TimelineSection
import com.aurax.imagery.ui.components.LocalFloatingBarInset
import com.aurax.imagery.utils.formatDuration

/**
 * Date-grouped thumbnail grid. Headers span the full row; thumbnails use stable keys and let
 * Coil size each request to its cell, so only small decoded bitmaps stay in memory. No blur
 * or glass effect is applied per cell, which keeps scrolling cheap on large libraries.
 */
@Composable
fun MediaTimelineGrid(
    rows: List<TimelineRow>,
    selectedIds: Set<Long>,
    selecting: Boolean,
    onItemClick: (MediaItem) -> Unit,
    onItemLongClick: (MediaItem) -> Unit,
    onSectionToggle: (TimelineSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 104.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 2.dp,
            end = 2.dp,
            top = 4.dp,
            bottom = LocalFloatingBarInset.current,
        ),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        items(
            items = rows,
            key = { it.key },
            span = { row ->
                if (row is TimelineRow.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1)
            },
            contentType = { it::class },
        ) { row ->
            when (row) {
                is TimelineRow.Header -> SectionHeader(
                    section = row.section,
                    selecting = selecting,
                    allSelected = selecting && row.section.items.all { it.id in selectedIds },
                    onToggle = { onSectionToggle(row.section) },
                )
                is TimelineRow.Media -> MediaThumbnail(
                    item = row.item,
                    selecting = selecting,
                    selected = row.item.id in selectedIds,
                    onClick = { onItemClick(row.item) },
                    onLongClick = { onItemLongClick(row.item) },
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    section: TimelineSection,
    selecting: Boolean,
    allSelected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 10.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = section.title.orEmpty(),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = section.items.size.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (selecting) {
            Spacer(Modifier.size(8.dp))
            Icon(
                imageVector = if (allSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (allSelected) "Deselect section" else "Select section",
                tint = if (allSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .size(24.dp)
                    .clickable(role = Role.Checkbox, onClick = onToggle),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaThumbnail(
    item: MediaItem,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val scale by animateFloatAsState(targetValue = if (selected) 0.86f else 1f, label = "thumbScale")

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(Color(0xFF111114))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // The scale is read inside the lambda, so animating it never recomposes the cell.
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(if (selected) 10.dp else 0.dp)),
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
        if (selecting) {
            Icon(
                imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (selected) "Selected" else "Not selected",
                tint = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(22.dp),
            )
        }
    }
}
