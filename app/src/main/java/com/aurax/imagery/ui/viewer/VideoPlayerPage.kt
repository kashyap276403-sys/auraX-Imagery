package com.aurax.imagery.ui.viewer

import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.FitScreen
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
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
import com.aurax.imagery.AuraXApp
import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.domain.player.AspectPreset
import com.aurax.imagery.domain.player.ScreenMode
import com.aurax.imagery.domain.player.clampPan
import com.aurax.imagery.domain.player.clampZoom
import com.aurax.imagery.domain.player.computeFrame
import com.aurax.imagery.utils.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.media3.common.MediaItem as ExoMediaItem

private const val SEEK_STEP_MS = 10_000L
private val ScrimColor = Color.Black.copy(alpha = 0.38f)

/**
 * One video page in the viewer pager: the video frame (screen mode, aspect preset, zoom) plus
 * its own chrome - top bar, centre transport, thin seek row.
 *
 * A player exists only while this page is the settled, visible one: it is created when you
 * arrive, released the moment you swipe away or leave the screen, and paused when the app goes
 * to the background. Video renders into a TextureView because, unlike a SurfaceView, it
 * stays correct while the pager translates and fades it during swipe-to-close.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerPage(
    item: MediaItem,
    isCurrentPage: Boolean,
    controlsVisible: Boolean,
    isLandscape: Boolean,
    actionBarVisible: Boolean,
    onToggleControls: () -> Unit,
    onToggleFullscreen: () -> Unit,
    onClose: () -> Unit,
    onShowDetails: () -> Unit,
    onPlayingChanged: (Boolean) -> Unit,
    onBusyChanged: (Boolean) -> Unit,
    onInteraction: () -> Unit,
    onZoomChanged: (Boolean) -> Unit,
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
    var videoW by remember(item.id) { mutableFloatStateOf(item.width.toFloat()) }
    var videoH by remember(item.id) { mutableFloatStateOf(item.height.toFloat()) }

    // Screen mode / ratio / zoom. The last mode and preset are remembered across videos and launches.
    val container = remember { (context.applicationContext as AuraXApp).container }
    val scope = rememberCoroutineScope()
    var screenMode by remember { mutableStateOf(ScreenMode.Fit) }
    var preset by remember { mutableStateOf(AspectPreset.Original) }
    var zoom by remember(item.id) { mutableFloatStateOf(1f) }
    var panX by remember(item.id) { mutableFloatStateOf(0f) }
    var panY by remember(item.id) { mutableFloatStateOf(0f) }
    var showRatioSheet by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        container.preferences.videoScreenMode.first()
            ?.let { saved -> ScreenMode.entries.firstOrNull { it.name == saved } }
            ?.let { screenMode = it }
        container.preferences.videoAspectPreset.first()
            ?.let { saved -> AspectPreset.entries.firstOrNull { it.name == saved } }
            ?.let { preset = it }
    }
    fun applyDisplay(mode: ScreenMode, newPreset: AspectPreset) {
        screenMode = mode
        preset = newPreset
        scope.launch { container.preferences.setVideoDisplay(mode.name, newPreset.name) }
    }
    LaunchedEffect(zoom) { onZoomChanged(zoom > 1.001f) }
    LaunchedEffect(showRatioSheet, showMenu, dragging) { onBusyChanged(showRatioSheet || showMenu || dragging) }
    LaunchedEffect(isPlaying, isCurrentPage) { if (isCurrentPage) onPlayingChanged(isPlaying) }

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
                    val w = videoSize.width * videoSize.pixelWidthHeightRatio
                    val h = videoSize.height.toFloat()
                    val turned = videoSize.unappliedRotationDegrees % 180 != 0
                    videoW = if (turned) h else w
                    videoH = if (turned) w else h
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

    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .pointerInput(Unit) { detectTapGestures(onTap = { onToggleControls() }) },
        contentAlignment = Alignment.Center,
    ) {
        val containerW = constraints.maxWidth.toFloat()
        val containerH = constraints.maxHeight.toFloat()
        val ratio = preset.ratio.takeIf { screenMode == ScreenMode.Fit || screenMode == ScreenMode.Fill }
        val frame = computeFrame(containerW, containerH, videoW, videoH, screenMode, ratio)
        val frameWDp = with(density) { frame.width.toDp() }
        val frameHDp = with(density) { frame.height.toDp() }

        // The picture frame. Pan is clamped so zooming never opens a gap; drag moves it while zoomed.
        Box(
            modifier = Modifier
                .size(frameWDp, frameHDp)
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = panX
                    translationY = panY
                }
                .pointerInput(zoom > 1f, frame, containerW, containerH) {
                    if (zoom > 1f) {
                        detectDragGestures(
                            onDragStart = { dragging = true },
                            onDragEnd = { dragging = false },
                            onDragCancel = { dragging = false },
                        ) { change, amount ->
                            change.consume()
                            panX = clampPan(panX + amount.x, frame.width, containerW, zoom)
                            panY = clampPan(panY + amount.y, frame.height, containerH, zoom)
                        }
                    }
                },
        ) {
            // Poster frame until the first video frame is on screen.
            if (!firstFrameShown) {
                AsyncImage(
                    model = item.uri,
                    contentDescription = item.displayName,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            player?.let { active ->
                key(active) {
                    AndroidView(
                        factory = { ctx -> TextureView(ctx).also { active.setVideoTextureView(it) } },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
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
        val ready = active != null && !failed

        // Top bar: back, title, details, more.
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .displayCutoutPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text(
                    text = item.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                )
                IconButton(onClick = onShowDetails) {
                    Icon(Icons.Rounded.Info, contentDescription = "Details", tint = Color.White)
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More options", tint = Color.White)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Screen ratio") },
                            leadingIcon = { Icon(Icons.Rounded.AspectRatio, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showRatioSheet = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Video details") },
                            leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onShowDetails()
                            },
                        )
                    }
                }
            }
        }

        // Centre transport: back 10s, play/pause, forward 10s.
        if (active != null && ready) {
            AnimatedVisibility(
                visible = controlsVisible || !isPlaying,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TransportButton(Icons.Rounded.Replay10, "Back 10 seconds", 48.dp) {
                        active.seekTo((active.currentPosition - SEEK_STEP_MS).coerceAtLeast(0L))
                        ended = false
                        onInteraction()
                    }
                    TransportButton(
                        icon = when {
                            ended -> Icons.Rounded.Replay
                            isPlaying -> Icons.Rounded.Pause
                            else -> Icons.Rounded.PlayArrow
                        },
                        description = when {
                            ended -> "Replay"
                            isPlaying -> "Pause"
                            else -> "Play"
                        },
                        size = 68.dp,
                    ) {
                        when {
                            ended -> {
                                active.seekTo(0L)
                                active.play()
                            }
                            isPlaying -> active.pause()
                            else -> active.play()
                        }
                        onInteraction()
                    }
                    TransportButton(Icons.Rounded.Forward10, "Forward 10 seconds", 48.dp) {
                        val limit = if (durationMs > 0) durationMs else Long.MAX_VALUE
                        active.seekTo((active.currentPosition + SEEK_STEP_MS).coerceAtMost(limit))
                        onInteraction()
                    }
                }
            }
        }

        // Bottom: thin seek row with ratio shortcuts.
        if (active != null && ready) {
            AnimatedVisibility(
                visible = controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                SeekControls(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    isLandscape = isLandscape,
                    fillActive = screenMode == ScreenMode.Fill,
                    bottomInset = if (actionBarVisible) 104.dp else 16.dp,
                    onSeek = { target ->
                        active.seekTo(target)
                        positionMs = target
                        ended = false
                        onInteraction()
                    },
                    onDragging = { dragging = it },
                    onToggleFitFill = {
                        applyDisplay(if (screenMode == ScreenMode.Fill) ScreenMode.Fit else ScreenMode.Fill, preset)
                        onInteraction()
                    },
                    onOpenRatio = { showRatioSheet = true },
                    onToggleFullscreen = onToggleFullscreen,
                )
            }
        }
    }

    if (showRatioSheet) {
        ScreenRatioSheet(
            mode = screenMode,
            preset = preset,
            zoom = zoom,
            onMode = { applyDisplay(it, preset) },
            onPreset = { applyDisplay(screenMode, it) },
            onZoom = { value ->
                zoom = value.clampZoom()
                if (zoom <= 1f) {
                    panX = 0f
                    panY = 0f
                }
            },
            onDismiss = { showRatioSheet = false },
        )
    }
}

@Composable
private fun TransportButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    size: Dp,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(size).clip(CircleShape).background(ScrimColor),
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(size * 0.55f))
    }
}

/** Slim, glass-free seek row: small times, a 2dp track with a tiny thumb, and ratio shortcuts. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeekControls(
    positionMs: Long,
    durationMs: Long,
    isLandscape: Boolean,
    fillActive: Boolean,
    bottomInset: Dp,
    onSeek: (Long) -> Unit,
    onDragging: (Boolean) -> Unit,
    onToggleFitFill: () -> Unit,
    onOpenRatio: () -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragFraction by remember { mutableFloatStateOf(-1f) }
    val dragging = dragFraction >= 0f
    val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val shown = if (dragging) dragFraction else fraction
    val shownPosition = if (dragging) (dragFraction * durationMs).toLong() else positionMs
    val timeStyle = MaterialTheme.typography.labelSmall
    val dim = Color.White.copy(alpha = 0.85f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .displayCutoutPadding()
            .padding(start = 16.dp, end = 4.dp, bottom = bottomInset),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = formatDuration(shownPosition), style = timeStyle, color = dim)
        Slider(
            value = shown,
            onValueChange = {
                if (!dragging) onDragging(true)
                dragFraction = it
            },
            onValueChangeFinished = {
                if (dragging && durationMs > 0) onSeek((dragFraction * durationMs).toLong())
                dragFraction = -1f
                onDragging(false)
            },
            enabled = durationMs > 0,
            thumb = { Box(Modifier.size(10.dp).clip(CircleShape).background(Color.White)) },
            track = {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.28f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(shown)
                            .fillMaxHeight()
                            .background(Color.White),
                    )
                }
            },
            modifier = Modifier.weight(1f),
        )
        Text(text = formatDuration(durationMs), style = timeStyle, color = dim)
        SmallIcon(
            icon = if (fillActive) Icons.Rounded.FitScreen else Icons.Rounded.Crop,
            description = if (fillActive) "Fit to screen" else "Fill screen",
            onClick = onToggleFitFill,
        )
        SmallIcon(Icons.Rounded.AspectRatio, "Screen ratio", onOpenRatio)
        SmallIcon(
            icon = if (isLandscape) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
            description = if (isLandscape) "Exit full screen" else "Full screen",
            onClick = onToggleFullscreen,
        )
    }
}

@Composable
private fun SmallIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}
