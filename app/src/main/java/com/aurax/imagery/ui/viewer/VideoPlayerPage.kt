package com.aurax.imagery.ui.viewer

import android.view.TextureView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import coil3.compose.AsyncImage
import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.ui.components.GlassSurface
import com.aurax.imagery.utils.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.media3.common.MediaItem as ExoMediaItem

/**
 * One video page in the viewer pager.
 *
 * A player exists only while this page is the settled, visible one: it is created when you
 * arrive, released the moment you swipe away or leave the screen, and paused when the app goes
 * to the background. Video renders into a TextureView because, unlike a SurfaceView, it
 * stays correct while the pager translates and fades it during swipe-to-close.
 */
@Composable
fun VideoPlayerPage(
    item: MediaItem,
    isCurrentPage: Boolean,
    controlsVisible: Boolean,
    isLandscape: Boolean,
    onToggleControls: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var player by remember(item.id) { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember(item.id) { mutableStateOf(false) }
    var ended by remember(item.id) { mutableStateOf(false) }
    var firstFrameShown by remember(item.id) { mutableStateOf(false) }
    var positionMs by remember(item.id) { mutableLongStateOf(0L) }
    var durationMs by remember(item.id) { mutableLongStateOf(item.durationMillis ?: 0L) }
    var aspect by remember(item.id) { mutableFloatStateOf(0f) }
    var failed by remember(item.id) { mutableStateOf(false) }

    // Create the player when this page becomes the visible one; release it when it stops being so.
    DisposableEffect(item.id, isCurrentPage) {
        if (!isCurrentPage) return@DisposableEffect onDispose { }
        val created = ExoPlayer.Builder(context)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                setAudioAttributes(AudioAttributes.DEFAULT, true)
                setMediaItem(ExoMediaItem.fromUri(item.uri))
                prepare()
            }
        player = created
        onDispose {
            created.release()
            player = null
            isPlaying = false
            firstFrameShown = false
        }
    }

    // Mirror player state into Compose state.
    DisposableEffect(player) {
        val active = player ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                ended = playbackState == Player.STATE_ENDED
                if (playbackState == Player.STATE_READY && active.duration != C.TIME_UNSET) {
                    durationMs = active.duration
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    aspect = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                }
            }

            override fun onRenderedFirstFrame() {
                firstFrameShown = true
            }

            override fun onPlayerError(error: PlaybackException) {
                failed = true
            }
        }
        active.addListener(listener)
        onDispose { active.removeListener(listener) }
    }

    // Poll the playback position; slower while paused.
    LaunchedEffect(player) {
        val active = player ?: return@LaunchedEffect
        while (isActive) {
            positionMs = active.currentPosition.coerceAtLeast(0L)
            delay(if (isPlaying) 250L else 600L)
        }
    }

    // Never keep playing in the background.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) player?.pause()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures(onTap = { onToggleControls() }) },
        contentAlignment = Alignment.Center,
    ) {
        // Poster frame until the first video frame is on screen.
        if (!firstFrameShown) {
            AsyncImage(
                model = item.uri,
                contentDescription = item.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }

        player?.let { active ->
            val ratio = when {
                aspect > 0f -> aspect
                item.width > 0 && item.height > 0 -> item.width.toFloat() / item.height
                else -> 16f / 9f
            }
            key(active) {
                AndroidView(
                    factory = { ctx -> TextureView(ctx).also { active.setVideoTextureView(it) } },
                    modifier = Modifier.aspectRatio(ratio),
                )
            }
        }

        if (failed) {
            Text(
                text = "This video can't be played",
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
            )
        }

        val active = player
        if (active != null && !failed && (controlsVisible || !isPlaying)) {
            GlassSurface(
                modifier = Modifier.size(76.dp),
                shape = CircleShape,
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    onClick = {
                        when {
                            ended -> {
                                active.seekTo(0L)
                                active.play()
                            }
                            isPlaying -> active.pause()
                            else -> active.play()
                        }
                    },
                    modifier = Modifier.size(76.dp),
                ) {
                    Icon(
                        imageVector = when {
                            ended -> Icons.Rounded.Replay
                            isPlaying -> Icons.Rounded.Pause
                            else -> Icons.Rounded.PlayArrow
                        },
                        contentDescription = when {
                            ended -> "Replay"
                            isPlaying -> "Pause"
                            else -> "Play"
                        },
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }

        if (active != null && !failed && controlsVisible) {
            SeekControls(
                positionMs = positionMs,
                durationMs = durationMs,
                isLandscape = isLandscape,
                onSeek = { target ->
                    active.seekTo(target)
                    positionMs = target
                    ended = false
                },
                onToggleFullscreen = onToggleFullscreen,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

@Composable
private fun SeekControls(
    positionMs: Long,
    durationMs: Long,
    isLandscape: Boolean,
    onSeek: (Long) -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragFraction by remember { mutableFloatStateOf(-1f) }
    val dragging = dragFraction >= 0f
    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val shownPosition = if (dragging) (dragFraction * durationMs).toLong() else positionMs

    // Sits above the glass action bar (about 96dp tall) that the viewer shows with the controls.
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, bottom = 104.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = formatDuration(shownPosition), style = MaterialTheme.typography.labelMedium)
            Slider(
                value = if (dragging) dragFraction else fraction,
                onValueChange = { dragFraction = it },
                onValueChangeFinished = {
                    if (dragging && durationMs > 0) onSeek((dragFraction * durationMs).toLong())
                    dragFraction = -1f
                },
                enabled = durationMs > 0,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f),
                ),
                modifier = Modifier.weight(1f),
            )
            Text(text = formatDuration(durationMs), style = MaterialTheme.typography.labelMedium)
            IconButton(onClick = onToggleFullscreen) {
                Icon(
                    imageVector = if (isLandscape) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                    contentDescription = if (isLandscape) "Exit full screen" else "Full screen",
                )
            }
        }
    }
}
