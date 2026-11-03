package com.aurax.imagery.ui.viewer

import android.media.audiofx.Equalizer
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.aurax.imagery.domain.player.EqMath
import com.aurax.imagery.domain.player.EqPreset
import com.aurax.imagery.ui.components.GlassAlertDialog

/** Equaliser choice that carries from one video to the next while the app is open. */
object EqState {
    var enabled by mutableStateOf(false)
    val levels = mutableStateListOf<Float>()
    var preset by mutableStateOf(EqPreset.Flat)
}

/** A platform Equalizer on the player's audio session; follows [EqState]. Released with the player. */
@Composable
fun EqualizerEffect(player: ExoPlayer?) {
    DisposableEffect(player) {
        val active = player ?: return@DisposableEffect onDispose { }
        var eq: Equalizer? = null
        fun attach() {
            runCatching { eq?.release() }
            eq = null
            val session = active.audioSessionId
            if (session == 0) return
            eq = runCatching { Equalizer(0, session) }.getOrNull()
        }
        val listener = object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) = attach()
        }
        active.addListener(listener)
        attach()
        EqualizerBridge.current = { eq }
        onDispose {
            active.removeListener(listener)
            if (EqualizerBridge.current != null) EqualizerBridge.current = null
            runCatching { eq?.release() }
        }
    }
}

object EqualizerBridge {
    var current: (() -> Equalizer?)? = null

    /** Pushes [EqState] to the effect; returns band count and centre frequencies in Hz, or null if unsupported. */
    fun apply(): List<Int>? {
        val eq = current?.invoke() ?: return null
        return runCatching {
            val bands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            if (EqState.levels.size != bands) {
                EqState.levels.clear()
                EqState.levels.addAll(EqState.preset.levels(bands))
            }
            eq.enabled = EqState.enabled
            for (b in 0 until bands) {
                val level = if (EqState.enabled) EqState.levels[b] else 0f
                eq.setBandLevel(b.toShort(), EqMath.toMillibels(level, range[0].toInt(), range[1].toInt()))
            }
            List(bands) { eq.getCenterFreq(it.toShort()) / 1000 }
        }.getOrNull()
    }
}

@Composable
fun EqualizerDialog(onDismiss: () -> Unit) {
    // Re-applies whenever the state read below changes.
    val enabled = EqState.enabled
    val levels = EqState.levels.toList()
    val freqs = EqualizerBridge.apply()
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Equalizer") },
        text = {
            if (freqs == null) {
                Text("This device does not offer an equalizer for this audio.")
            } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Enabled")
                    Switch(checked = enabled, onCheckedChange = { EqState.enabled = it })
                }
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    EqPreset.entries.forEach { p ->
                        FilterChip(
                            selected = EqState.preset == p,
                            onClick = {
                                EqState.preset = p
                                EqState.enabled = true
                                EqState.levels.clear()
                                EqState.levels.addAll(p.levels(freqs.size))
                            },
                            label = { Text(p.label) },
                        )
                    }
                }
                freqs.forEachIndexed { i, hz ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text(if (hz >= 1000) "${hz / 1000}k" else "$hz", modifier = Modifier.padding(end = 8.dp))
                        Slider(
                            value = levels.getOrElse(i) { 0f },
                            onValueChange = { if (i < EqState.levels.size) { EqState.levels[i] = it; EqState.enabled = true } },
                            valueRange = -1f..1f,
                        )
                    }
                }
            }
        },
    )
}
