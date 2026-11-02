package com.aurax.imagery.ui.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import com.aurax.imagery.domain.player.AUTO_HIDE_OPTIONS_SEC
import com.aurax.imagery.domain.player.OrientationLock
import com.aurax.imagery.domain.player.PLAYBACK_SPEEDS
import com.aurax.imagery.domain.player.SEEK_INTERVALS_SEC
import com.aurax.imagery.domain.player.ScreenMode
import com.aurax.imagery.domain.player.VideoPlayerSettings
import com.aurax.imagery.domain.player.autoHideLabel
import com.aurax.imagery.domain.player.speedLabel
import com.aurax.imagery.domain.player.VideoGestureSettings
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import com.aurax.imagery.AuraXApp
import com.aurax.imagery.R
import com.aurax.imagery.domain.model.LibraryStats
import com.aurax.imagery.ui.components.GlassButton
import com.aurax.imagery.ui.components.GlassCard
import com.aurax.imagery.ui.components.LocalFloatingBarInset
import com.aurax.imagery.ui.components.ScreenScaffold
import com.aurax.imagery.ui.permissions.rememberMediaAccessState
import com.aurax.imagery.utils.MediaAccess
import com.aurax.imagery.utils.formatFileSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Settings: Glass Studio, performance switches, library and storage numbers, privacy controls,
 * app information and About. Every row reads real state (the index, the permission, the image
 * cache, the manifest) or performs a real action.
 */
@Composable
fun SettingsScreen(onOpenGlassStudio: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as AuraXApp).container }
    val scope = rememberCoroutineScope()
    val glass = container.glassSettings
    val style by glass.style.collectAsStateWithLifecycle()
    val stats by remember { container.mediaRepository.observeStats() }
        .collectAsStateWithLifecycle(initialValue = LibraryStats())
    val access = rememberMediaAccessState(onAccessChanged = { container.indexer.enqueue() })
    val versionName = remember { packageInfo(context)?.versionName ?: "unknown" }
    val versionCode = remember { packageInfo(context)?.longVersionCode ?: 0L }
    val hasInternetPermission = remember { requestsInternet(context) }

    var cacheBytes by remember { mutableLongStateOf(0L) }
    var confirmClearFavorites by remember { mutableStateOf(false) }
    var confirmClearHistory by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { cacheBytes = thumbnailCacheBytes(context) }

    ScreenScaffold(title = "Settings", onBack = onBack) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = LocalFloatingBarInset.current),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassCard(onClick = onOpenGlassStudio) {
                Heading("Glass Studio")
                Body("Blur, refraction, tint, reflection, text colour, motion and presets. Tap to open.")
            }

            GlassCard {
                Heading("Appearance")
                InfoRow("Theme", "AMOLED black (dark only)")
                Body("The glass look depends on a true-black base, so there is no light theme. Everything else is in Glass Studio.")
            }

            GlassCard {
                Heading("Performance")
                SwitchRow("Performance mode", "Cheaper blur, no shader, no shadows", style.performanceMode) { on ->
                    glass.update { it.copy(performanceMode = on) }
                }
                SwitchRow("Motion effects", "Tilt-reactive light and stretching nav lens", style.motionEffects) { on ->
                    glass.update { it.copy(motionEffects = on) }
                }
                SwitchRow("Haptic feedback", "Tick when switching tabs and selecting", style.haptics) { on ->
                    glass.update { it.copy(haptics = on) }
                }
                Body("If scrolling stutters on this phone, turn Performance mode on.")
            }

            GlassCard {
                Heading("Video player")
                val gestures by container.preferences.videoGestures
                    .collectAsStateWithLifecycle(initialValue = VideoGestureSettings())
                val player by container.preferences.videoPlayer
                    .collectAsStateWithLifecycle(initialValue = VideoPlayerSettings())
                fun save(next: VideoPlayerSettings) {
                    scope.launch { container.preferences.setVideoPlayer(next) }
                }

                ChoiceChips(
                    title = "Default playback speed",
                    options = PLAYBACK_SPEEDS,
                    selected = player.defaultSpeed,
                    label = ::speedLabel,
                ) { save(player.copy(defaultSpeed = it)) }
                ChoiceChips(
                    title = "Default screen mode",
                    options = ScreenMode.entries.toList(),
                    selected = player.defaultScreenMode,
                    label = { it.label },
                ) { save(player.copy(defaultScreenMode = it)) }
                SwitchRow("Remember last screen mode", "Open videos in the mode you used last", player.rememberScreenMode) { on ->
                    save(player.copy(rememberScreenMode = on))
                }
                ChoiceChips(
                    title = "Seek interval",
                    options = SEEK_INTERVALS_SEC,
                    selected = player.seekIntervalSec,
                    label = { "$it s" },
                ) { save(player.copy(seekIntervalSec = it)) }
                ChoiceChips(
                    title = "Hide controls after",
                    options = AUTO_HIDE_OPTIONS_SEC,
                    selected = player.autoHideSec,
                    label = ::autoHideLabel,
                ) { save(player.copy(autoHideSec = it)) }
                ChoiceChips(
                    title = "Orientation",
                    options = OrientationLock.entries.toList(),
                    selected = player.orientation,
                    label = { it.label },
                ) { save(player.copy(orientation = it)) }

                SwitchRow("Play next video automatically", "When a video ends, continue with the next one", player.autoPlayNext) { on ->
                    save(player.copy(autoPlayNext = on))
                }
                SwitchRow("Subtitles on by default", "Turn on the first subtitle track when a video has one", player.subtitlesOnByDefault) { on ->
                    save(player.copy(subtitlesOnByDefault = on))
                }
                SwitchRow("Player haptics", "A small tick on double-tap seek", player.haptics) { on ->
                    save(player.copy(haptics = on))
                }
                SwitchRow("Double-tap to seek", "Double-tap the left or right side to jump", gestures.doubleTapSeek) { on ->
                    scope.launch { container.preferences.setVideoGestures(gestures.copy(doubleTapSeek = on)) }
                }
                SwitchRow("Brightness gesture", "Swipe up or down on the left half", gestures.brightness) { on ->
                    scope.launch { container.preferences.setVideoGestures(gestures.copy(brightness = on)) }
                }
                SwitchRow("Volume gesture", "Swipe up or down on the right half", gestures.volume) { on ->
                    scope.launch { container.preferences.setVideoGestures(gestures.copy(volume = on)) }
                }
                Body("With a vertical gesture off, swiping down on that half closes the video instead.")
                val resume by container.preferences.videoResume.collectAsStateWithLifecycle(initialValue = true)
                SwitchRow("Resume watching", "Continue a video from where you stopped", resume) { on ->
                    scope.launch { container.preferences.setVideoResume(on) }
                }
                val historyCount by container.history.observeCount().collectAsStateWithLifecycle(initialValue = 0)
                InfoRow("Watch history", if (historyCount == 1) "1 video" else "$historyCount videos")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { confirmClearHistory = true }, enabled = historyCount > 0) { Text("Clear watch history") }
                    TextButton(onClick = {
                        scope.launch {
                            container.preferences.resetVideoPlayer()
                            Toast.makeText(context, "Player settings reset", Toast.LENGTH_SHORT).show()
                        }
                    }) { Text("Reset player settings") }
                }
            }

            GlassCard {
                Heading("Library and storage")
                InfoRow("Photos and videos", "${stats.total}")
                InfoRow("Photos", "${stats.photos}")
                InfoRow("Videos", "${stats.videos}")
                InfoRow("Size of indexed media", formatFileSize(stats.bytes))
                InfoRow("Thumbnail cache", formatFileSize(cacheBytes))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassButton(text = "Rescan library", onClick = {
                        container.indexer.enqueue()
                        Toast.makeText(context, "Scanning your library", Toast.LENGTH_SHORT).show()
                    })
                    GlassButton(text = "Clear cache", onClick = {
                        scope.launch {
                            clearThumbnailCache(context)
                            cacheBytes = thumbnailCacheBytes(context)
                            Toast.makeText(context, "Thumbnail cache cleared", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
                Body("Clearing the cache only deletes generated thumbnails. Your photos are never touched.")
            }

            GlassCard {
                Heading("Privacy")
                InfoRow(
                    "Media access",
                    when (access.access) {
                        MediaAccess.Full -> "Full library"
                        MediaAccess.Partial -> "Selected photos only"
                        MediaAccess.None -> "Not allowed"
                    },
                )
                InfoRow("Network permission", if (hasInternetPermission) "Requested" else "None (cannot go online)")
                InfoRow("Cloud backup of app data", "Off")
                Body("Photos, videos and favorites stay on this device. AuraX has no internet permission, so nothing can be uploaded.")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassButton(text = "Manage access", onClick = access.openSettings)
                    GlassButton(text = "Clear favorites", onClick = { confirmClearFavorites = true })
                }
            }

            GlassCard {
                Heading("App information")
                InfoRow("Version", "$versionName ($versionCode)")
                InfoRow("Package", context.packageName)
                InfoRow("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                InfoRow("Device", "${Build.MANUFACTURER} ${Build.MODEL}")
            }

            GlassCard {
                Heading("About ${stringResource(R.string.app_name)}")
                Text(
                    text = stringResource(R.string.tagline),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                Body("A premium offline-first gallery with a liquid-glass interface. Your photos and videos never leave this device.")
            }
        }
    }

    if (confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { confirmClearHistory = false },
            title = { Text("Clear watch history?") },
            text = { Text("Videos will start from the beginning and Continue watching empties. Your videos and bookmarks are not touched.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearHistory = false
                    scope.launch {
                        container.history.clear()
                        Toast.makeText(context, "Watch history cleared", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClearHistory = false }) { Text("Cancel") } },
        )
    }

    if (confirmClearFavorites) {
        AlertDialog(
            onDismissRequest = { confirmClearFavorites = false },
            title = { Text("Clear all favorites?") },
            text = { Text("This only removes the heart from your photos and videos. Nothing is deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearFavorites = false
                    scope.launch {
                        container.mediaRepository.clearFavorites()
                        Toast.makeText(context, "Favorites cleared", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClearFavorites = false }) { Text("Cancel") } },
        )
    }
}

/** A titled row of single-choice chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceChips(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            options.forEach { option ->
                FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun Body(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1.2f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

private fun packageInfo(context: Context) =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()

/** Reads the real manifest, so this row cannot drift from what the APK actually requests. */
private fun requestsInternet(context: Context): Boolean = runCatching {
    context.packageManager
        .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        .requestedPermissions
        .orEmpty()
        .contains(Manifest.permission.INTERNET)
}.getOrDefault(false)

private suspend fun thumbnailCacheBytes(context: Context): Long = withContext(Dispatchers.IO) {
    val loader = SingletonImageLoader.get(context)
    (loader.diskCache?.size ?: 0L) + (loader.memoryCache?.size?.toLong() ?: 0L)
}

private suspend fun clearThumbnailCache(context: Context) = withContext(Dispatchers.IO) {
    val loader = SingletonImageLoader.get(context)
    loader.memoryCache?.clear()
    loader.diskCache?.clear()
}
