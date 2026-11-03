package com.aurax.imagery.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.aurax.imagery.AuraXApp
import com.aurax.imagery.data.media.FileHasher
import com.aurax.imagery.domain.insights.LibraryInsights
import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.ui.components.GlassButton
import com.aurax.imagery.ui.components.GlassCard
import com.aurax.imagery.ui.components.ScreenScaffold
import com.aurax.imagery.utils.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Storage insights and duplicate finder, all computed on the device from the local index. Duplicates are
 * confirmed by hashing file contents, never by name or date alone, and nothing is removed without the
 * system's own trash confirmation (items stay recoverable from Trash).
 */
@Composable
fun InsightsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as AuraXApp).container }
    val scope = rememberCoroutineScope()
    val media by remember { container.mediaRepository.observeMedia() }
        .collectAsStateWithLifecycle(initialValue = emptyList<MediaItem>())

    var scanning by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var groups by remember { mutableStateOf<List<List<MediaItem>>?>(null) }

    val trash = rememberTrashAction { trashed ->
        val gone = trashed.map { it.id }.toSet()
        groups = groups?.map { g -> g.filter { it.id !in gone } }?.filter { it.size > 1 }
        container.indexer.enqueue()
    }

    fun scan() {
        scanning = true
        progress = 0f
        scope.launch {
            val found = withContext(Dispatchers.IO) {
                val candidates = LibraryInsights.duplicateCandidates(media)
                val out = mutableListOf<List<MediaItem>>()
                candidates.forEachIndexed { i, group ->
                    val hashes = group.associate { it.id to FileHasher.sha256(context, it.uri) }
                    out += LibraryInsights.confirm(group) { hashes[it.id] }
                    progress = (i + 1f) / candidates.size
                }
                out.sortedByDescending { it[0].sizeBytes * (it.size - 1) }
            }
            groups = found
            scanning = false
        }
    }

    val (photoBytes, videoBytes) = LibraryInsights.bytesByType(media)
    val folders = LibraryInsights.folderUsage(media)
    val largest = LibraryInsights.largest(media)

    ScreenScaffold(title = "Storage insights", subtitle = "${media.size} items, ${formatFileSize(photoBytes + videoBytes)}", onBack = onBack) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassCard {
                Text("Photos vs videos", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                Line("Photos", formatFileSize(photoBytes))
                Line("Videos", formatFileSize(videoBytes))
            }
            GlassCard {
                Text("Biggest folders", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                val top = folders.firstOrNull()?.bytes?.coerceAtLeast(1L) ?: 1L
                folders.forEach { f ->
                    Line(f.name, "${formatFileSize(f.bytes)} · ${f.count}")
                    LinearProgressIndicator(progress = { f.bytes.toFloat() / top }, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp))
                }
            }
            GlassCard {
                Text("Largest files", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                largest.forEach { Thumbnail(it, extra = formatFileSize(it.sizeBytes)) }
            }
            GlassCard {
                Text("Duplicates", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
                val g = groups
                Text(
                    "Finds files whose contents are byte-for-byte identical. Similar-looking photos are not matched.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                if (scanning) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                else GlassButton(text = if (g == null) "Scan for duplicates" else "Scan again", onClick = ::scan)
                if (g != null && !scanning) {
                    if (g.isEmpty()) Text("No exact duplicates found.", modifier = Modifier.padding(top = 8.dp))
                    else {
                        Text(
                            "${g.size} sets, ${formatFileSize(LibraryInsights.reclaimable(g))} could be freed",
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                        g.forEach { set ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                                set.take(4).forEach { m ->
                                    AsyncImage(
                                        model = m.uri, contentDescription = null, contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                                    )
                                }
                            }
                            Text("${set.size} copies of ${set[0].displayName} · ${formatFileSize(set[0].sizeBytes)} each", style = MaterialTheme.typography.bodySmall)
                            GlassButton(text = "Keep oldest, trash ${set.size - 1}", onClick = { trash(set.drop(1)) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f), maxLines = 1)
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Thumbnail(item: MediaItem, extra: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AsyncImage(
            model = item.uri, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
        )
        Text(item.displayName, modifier = Modifier.weight(1f), maxLines = 1)
        Text(extra, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
