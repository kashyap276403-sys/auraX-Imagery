package com.aurax.imagery.ui.viewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aurax.imagery.domain.player.AspectPreset
import com.aurax.imagery.domain.player.MAX_ZOOM
import com.aurax.imagery.domain.player.PLAYBACK_SPEEDS
import com.aurax.imagery.domain.player.speedLabel
import com.aurax.imagery.domain.player.MIN_ZOOM
import com.aurax.imagery.domain.player.ScreenMode
import com.aurax.imagery.ui.components.LiquidBottomSheet
import kotlin.math.roundToInt

/**
 * Screen ratio sheet: screen mode, frame-shape presets and zoom. The last mode and preset are
 * remembered by the caller.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScreenRatioSheet(
    mode: ScreenMode,
    preset: AspectPreset,
    zoom: Float,
    onMode: (ScreenMode) -> Unit,
    onPreset: (AspectPreset) -> Unit,
    onZoom: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val presetApplies = mode == ScreenMode.Fit || mode == ScreenMode.Fill
    LiquidBottomSheet(onDismiss = onDismiss) {
        Text("Screen ratio", style = MaterialTheme.typography.titleLarge)
        Text(
            text = buildString {
                append(mode.label)
                if (presetApplies && preset != AspectPreset.Original) append(" · ").append(preset.label)
                append(" · Zoom ").append((zoom * 100).roundToInt()).append('%')
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        SectionLabel("SCREEN MODE")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScreenMode.entries.forEach { m ->
                FilterChip(selected = m == mode, onClick = { onMode(m) }, label = { Text(m.label) })
            }
        }
        Text(
            text = mode.hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )

        SectionLabel("ASPECT RATIO")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AspectPreset.entries.forEach { p ->
                FilterChip(
                    selected = p == preset,
                    onClick = { onPreset(p) },
                    enabled = presetApplies,
                    label = { Text(p.label) },
                )
            }
        }
        Text(
            text = when {
                !presetApplies -> "Presets apply to Fit and Fill only."
                preset == AspectPreset.Original -> "The video keeps its own ratio."
                else -> "The picture is reshaped to ${preset.label}, so it may look stretched."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )

        SectionLabel("ZOOM")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Slider(
                value = zoom,
                onValueChange = onZoom,
                valueRange = MIN_ZOOM..MAX_ZOOM,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onZoom(MIN_ZOOM) }, enabled = zoom > MIN_ZOOM) { Text("Reset") }
        }
        Text(
            text = "Drag the video to move it while zoomed.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

/** Playback speed chooser, 0.25x to 2x. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlaybackSpeedSheet(speed: Float, onSpeed: (Float) -> Unit, onDismiss: () -> Unit) {
    LiquidBottomSheet(onDismiss = onDismiss) {
        Text("Playback speed", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Now ${speedLabel(speed)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PLAYBACK_SPEEDS.forEach { value ->
                FilterChip(selected = value == speed, onClick = { onSpeed(value) }, label = { Text(speedLabel(value)) })
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** Cinematic display looks. They only change how the picture is shown, never the file. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CinematicSheet(
    mode: com.aurax.imagery.domain.player.CinematicMode,
    onMode: (com.aurax.imagery.domain.player.CinematicMode) -> Unit,
    onDismiss: () -> Unit,
) {
    LiquidBottomSheet(onDismiss = onDismiss) {
        Text("Cinematic look", style = MaterialTheme.typography.titleLarge)
        Text(
            text = mode.hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.aurax.imagery.domain.player.CinematicMode.entries.forEach {
                FilterChip(selected = it == mode, onClick = { onMode(it) }, label = { Text(it.label) })
            }
        }
        TextButton(
            onClick = { onMode(com.aurax.imagery.domain.player.CinematicMode.Original) },
            enabled = !mode.isOriginal,
            modifier = Modifier.padding(top = 8.dp),
        ) { Text("Reset to Original") }
        Text(
            text = "Applies to what you see only. Saved frames and the video file stay untouched.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }
}
