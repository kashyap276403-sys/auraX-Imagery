package com.aurax.imagery.ui.viewer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aurax.imagery.domain.player.AbRange
import com.aurax.imagery.domain.player.Bookmark
import com.aurax.imagery.domain.player.RepeatMode
import com.aurax.imagery.domain.player.SubtitlePosition
import com.aurax.imagery.domain.player.SubtitleSize
import com.aurax.imagery.ui.components.LiquidBottomSheet
import com.aurax.imagery.utils.formatDuration

/** One selectable audio or subtitle track, as shown in the picker. */
data class TrackOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val title: String,
    val detail: String?,
    val selected: Boolean,
    val supported: Boolean,
)

@Composable
private fun SheetLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun ChoiceRow(title: String, detail: String?, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (selected) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
    }
}

/** Subtitles (off / embedded / external SRT, size, position) and audio track picker. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TracksSheet(
    subtitles: List<TrackOption>,
    audio: List<TrackOption>,
    size: SubtitleSize,
    position: SubtitlePosition,
    onSelectSubtitle: (TrackOption?) -> Unit,
    onPickSrt: () -> Unit,
    onSize: (SubtitleSize) -> Unit,
    onPosition: (SubtitlePosition) -> Unit,
    onSelectAudio: (TrackOption) -> Unit,
    onDismiss: () -> Unit,
) {
    LiquidBottomSheet(onDismiss = onDismiss) {
        Text("Subtitles and audio", style = MaterialTheme.typography.titleLarge)

        SheetLabel("SUBTITLES")
        ChoiceRow("Off", null, subtitles.none { it.selected }) { onSelectSubtitle(null) }
        subtitles.forEach { option ->
            ChoiceRow(option.title, option.detail, option.selected, option.supported) { onSelectSubtitle(option) }
        }
        if (subtitles.isEmpty()) {
            Text(
                "No subtitle track in this video.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(onClick = onPickSrt, modifier = Modifier.padding(top = 8.dp)) { Text("Add .srt file") }

        SheetLabel("SUBTITLE SIZE")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubtitleSize.entries.forEach {
                FilterChip(selected = it == size, onClick = { onSize(it) }, label = { Text(it.label) })
            }
        }
        SheetLabel("SUBTITLE POSITION")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SubtitlePosition.entries.forEach {
                FilterChip(selected = it == position, onClick = { onPosition(it) }, label = { Text(it.label) })
            }
        }
        Text(
            "Subtitle delay is not available: the player has no timing offset for subtitles.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        SheetLabel("AUDIO TRACK")
        if (audio.isEmpty()) {
            Text("No audio track.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        audio.forEach { option ->
            ChoiceRow(
                title = option.title,
                detail = if (option.supported) option.detail else listOfNotNull(option.detail, "not supported on this phone").joinToString(" · "),
                selected = option.selected,
                enabled = option.supported,
            ) { onSelectAudio(option) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Mark A and B at the current position to loop that section. */
@Composable
fun AbRepeatSheet(
    range: AbRange,
    positionMs: Long,
    onSetA: () -> Unit,
    onSetB: () -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    LiquidBottomSheet(onDismiss = onDismiss) {
        Text("A-B repeat", style = MaterialTheme.typography.titleLarge)
        Text(
            text = when {
                range.isActive -> "Looping ${formatDuration(range.startMs!!)} to ${formatDuration(range.endMs!!)}"
                range.startMs != null -> "A set at ${formatDuration(range.startMs)}. Move to the end of the section and set B."
                else -> "Move to where the section starts and set A."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        Text("Now at ${formatDuration(positionMs)}", style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onSetA) { Text("Set A") }
            Button(onClick = onSetB, enabled = range.startMs != null) { Text("Set B") }
            OutlinedButton(onClick = onClear, enabled = range.startMs != null || range.endMs != null) { Text("Clear") }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Save the current time with a name; tap a bookmark to jump to it. */
@Composable
fun BookmarksSheet(
    bookmarks: List<Bookmark>,
    positionMs: Long,
    onAdd: (String) -> Unit,
    onJump: (Bookmark) -> Unit,
    onDelete: (Bookmark) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    LiquidBottomSheet(onDismiss = onDismiss) {
        Text("Bookmarks", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(60) },
            singleLine = true,
            label = { Text("Name (optional)") },
            keyboardOptions = KeyboardOptions.Default,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        )
        Button(
            onClick = {
                onAdd(name.trim().ifEmpty { "Bookmark at ${formatDuration(positionMs)}" })
                name = ""
            },
            modifier = Modifier.padding(top = 8.dp),
        ) { Text("Save ${formatDuration(positionMs)}") }

        SheetLabel("SAVED")
        if (bookmarks.isEmpty()) {
            Text("No bookmarks in this video yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        bookmarks.forEach { bookmark ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onJump(bookmark) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(bookmark.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatDuration(bookmark.positionMs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onDelete(bookmark) }) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete bookmark")
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

data class QueueEntry(val id: Long, val title: String, val durationLabel: String?, val isCurrent: Boolean)

/** Video queue (the videos of the list you opened), repeat mode, autoplay and removal. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QueueSheet(
    entries: List<QueueEntry>,
    repeatMode: RepeatMode,
    autoPlayNext: Boolean,
    onRepeatMode: (RepeatMode) -> Unit,
    onAutoPlayNext: (Boolean) -> Unit,
    onPlay: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
) {
    LiquidBottomSheet(onDismiss = onDismiss) {
        Text("Queue", style = MaterialTheme.typography.titleLarge)

        SheetLabel("REPEAT")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RepeatMode.entries.forEach {
                FilterChip(selected = it == repeatMode, onClick = { onRepeatMode(it) }, label = { Text(it.label) })
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Play next video automatically", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Repeat queue always continues and wraps around.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = autoPlayNext, onCheckedChange = onAutoPlayNext)
        }

        SheetLabel("UP NEXT (${entries.size})")
        entries.forEach { entry ->
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onPlay(entry.id) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (entry.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    entry.durationLabel?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (!entry.isCurrent) {
                    IconButton(onClick = { onRemove(entry.id) }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Remove from queue")
                    }
                }
            }
        }
        TextButton(onClick = onRestore) { Text("Restore removed videos") }
        Spacer(Modifier.height(8.dp))
    }
}
