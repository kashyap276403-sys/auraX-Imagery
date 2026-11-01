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

/** Pick a sleep timer; playback pauses when it runs out. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SleepTimerSheet(
    endsAtElapsed: Long?,
    onSet: (Int) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    var custom by remember { mutableStateOf("") }
    val remaining by androidx.compose.runtime.produceState(initialValue = 0L, endsAtElapsed) {
        while (endsAtElapsed != null) {
            value = endsAtElapsed - android.os.SystemClock.elapsedRealtime()
            kotlinx.coroutines.delay(1000L)
        }
        value = 0L
    }
    LiquidBottomSheet(onDismiss = onDismiss) {
        Text("Sleep timer", style = MaterialTheme.typography.titleLarge)
        Text(
            text = if (endsAtElapsed != null) "Pauses in ${com.aurax.imagery.domain.player.countdownLabel(remaining)}" else "Playback pauses when the timer ends.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.aurax.imagery.domain.player.SLEEP_TIMER_MINUTES.forEach { minutes ->
                FilterChip(selected = false, onClick = { onSet(minutes); onDismiss() }, label = { Text("$minutes min") })
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = custom,
                onValueChange = { custom = it.filter(Char::isDigit).take(3) },
                singleLine = true,
                label = { Text("Custom (minutes)") },
                keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { custom.toIntOrNull()?.takeIf { it in 1..999 }?.let { onSet(it); onDismiss() } },
                enabled = custom.toIntOrNull()?.let { it in 1..999 } == true,
            ) { Text("Start") }
        }
        if (endsAtElapsed != null) {
            TextButton(onClick = { onCancel(); onDismiss() }) { Text("Turn timer off") }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** What the player reports about the stream it is decoding right now. */
data class VideoTechInfo(
    val resolution: String?,
    val frameRate: String?,
    val videoCodec: String?,
    val videoBitrate: String?,
    val audio: String?,
)

/** Full video details: file facts from the index plus technical facts from the player. */
@Composable
fun VideoDetailsSheet(item: com.aurax.imagery.domain.model.MediaItem, tech: VideoTechInfo?, location: String?, onDismiss: () -> Unit) {
    val taken = remember(item.dateTakenMillis) {
        java.time.Instant.ofEpochMilli(item.dateTakenMillis).atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofLocalizedDateTime(java.time.format.FormatStyle.MEDIUM, java.time.format.FormatStyle.SHORT))
    }
    LiquidBottomSheet(onDismiss = onDismiss) {
        Text("Video details", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        DetailRow("Name", item.displayName)
        DetailRow("Created", taken)
        DetailRow("File size", com.aurax.imagery.utils.formatFileSize(item.sizeBytes))
        item.durationMillis?.let { DetailRow("Duration", formatDuration(it)) }
        DetailRow("Type", item.mimeType)
        DetailRow("Location", location ?: item.albumName ?: "Unknown")
        val resolution = tech?.resolution ?: if (item.width > 0 && item.height > 0) "${item.width} × ${item.height}" else null
        resolution?.let { DetailRow("Resolution", it) }
        tech?.frameRate?.let { DetailRow("Frame rate", it) }
        tech?.videoCodec?.let { DetailRow("Video codec", it) }
        tech?.videoBitrate?.let { DetailRow("Video bitrate", it) }
        tech?.audio?.let { DetailRow("Audio", it) }
        if (tech == null) {
            Text(
                "Codec, frame rate and bitrate show once the video has started loading.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.padding(horizontal = 8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}
