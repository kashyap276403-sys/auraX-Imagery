package com.aurax.imagery.ui.viewer

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.media3.common.MimeTypes
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.text.CueGroup
import coil3.request.ImageRequest
import coil3.video.videoFrameMillis
import com.aurax.imagery.domain.player.AbRange
import com.aurax.imagery.domain.player.RepeatMode
import com.aurax.imagery.domain.player.SubtitlePosition
import com.aurax.imagery.domain.player.SubtitleSize
import com.aurax.imagery.domain.player.channelLabel
import com.aurax.imagery.domain.player.trackTitle
import android.media.AudioManager
import android.provider.Settings
import android.view.TextureView
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.PlaybackParameters
import com.aurax.imagery.domain.player.OrientationLock
import com.aurax.imagery.domain.player.TapZone
import com.aurax.imagery.domain.player.VideoGestureSettings
import com.aurax.imagery.domain.player.adjustLevel
import com.aurax.imagery.domain.player.isLeftHalf
import com.aurax.imagery.domain.player.seekTarget
import com.aurax.imagery.domain.player.speedLabel
import com.aurax.imagery.domain.player.tapZone
import kotlin.math.roundToInt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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

/** What the viewer-level queue offers the page: skip buttons, end-of-video handling, repeat mode. */
data class QueueNav(
    val canPrev: Boolean,
    val canNext: Boolean,
    val onPrev: () -> Unit,
    val onNext: () -> Unit,
    val onEnded: () -> Unit,
    val onShowQueue: () -> Unit,
    val repeatMode: RepeatMode,
    /** True once if the viewer moved here because the previous video ended or Next was pressed. */
    val consumeAutoPlay: () -> Boolean,
)

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
    orientationLock: OrientationLock,
    onCycleOrientation: () -> Unit,
    queue: QueueNav,
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
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showTracksSheet by remember { mutableStateOf(false) }
    var showAbSheet by remember { mutableStateOf(false) }
    var showBookmarks by remember { mutableStateOf(false) }
    var tracks by remember(item.id) { mutableStateOf(Tracks.EMPTY) }
    var cueText by remember(item.id) { mutableStateOf("") }
    var subtitleSize by remember { mutableStateOf(SubtitleSize.Medium) }
    var subtitlePosition by remember { mutableStateOf(SubtitlePosition.Low) }
    var abRange by remember(item.id) { mutableStateOf(AbRange()) }
    val latestQueue by rememberUpdatedState(queue)
    val srtPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val active = player
        if (uri != null && active != null) {
            val name = displayNameOf(context, uri)
            if (!name.endsWith(".srt", ignoreCase = true)) {
                Toast.makeText(context, "Choose an .srt subtitle file", Toast.LENGTH_SHORT).show()
            } else {
                val position = active.currentPosition
                val resume = active.playWhenReady
                val subtitle = ExoMediaItem.SubtitleConfiguration.Builder(uri)
                    .setMimeType(MimeTypes.APPLICATION_SUBRIP)
                    .setLabel("External · $name")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
                active.trackSelectionParameters = active.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .build()
                active.setMediaItem(
                    ExoMediaItem.Builder().setUri(item.uri).setSubtitleConfigurations(listOf(subtitle)).build(),
                    position,
                )
                active.prepare()
                active.playWhenReady = resume
            }
        }
    }
    var speed by remember(item.id) { mutableFloatStateOf(1f) }
    var indicator by remember { mutableStateOf<LevelIndicator?>(null) }
    var indicatorTick by remember { mutableIntStateOf(0) }
    var seekFlash by remember { mutableStateOf<TapZone?>(null) }
    var flashTick by remember { mutableIntStateOf(0) }
    val gestures by container.preferences.videoGestures
        .collectAsStateWithLifecycle(initialValue = VideoGestureSettings())
    LaunchedEffect(indicatorTick) {
        if (indicator != null) {
            delay(900L)
            indicator = null
        }
    }
    LaunchedEffect(flashTick) {
        if (seekFlash != null) {
            delay(650L)
            seekFlash = null
        }
    }
    LaunchedEffect(player, speed) { player?.setPlaybackParameters(PlaybackParameters(speed)) }
    LaunchedEffect(player, queue.repeatMode) {
        player?.repeatMode = if (queue.repeatMode == RepeatMode.One) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }
    LaunchedEffect(ended, isCurrentPage) { if (ended && isCurrentPage) latestQueue.onEnded() }
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
    LaunchedEffect(showRatioSheet, showSpeedSheet, showTracksSheet, showAbSheet, showBookmarks, showMenu, dragging) {
        onBusyChanged(showRatioSheet || showSpeedSheet || showTracksSheet || showAbSheet || showBookmarks || showMenu || dragging)
    }
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
                playWhenReady = latestQueue.consumeAutoPlay()
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

            override fun onTracksChanged(newTracks: Tracks) {
                tracks = newTracks
            }

            override fun onCues(cueGroup: CueGroup) {
                cueText = cueGroup.cues.mapNotNull { it.text?.toString() }.joinToString("\n")
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
            abRange.loopTarget(positionMs)?.let { target ->
                active.seekTo(target)
                positionMs = target
            }
            delay(if (abRange.isActive && isPlaying) 80L else if (isPlaying) 250L else 600L)
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
            .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        val containerW = constraints.maxWidth.toFloat()
        val containerH = constraints.maxHeight.toFloat()
        val ratio = preset.ratio.takeIf { screenMode == ScreenMode.Fit || screenMode == ScreenMode.Fill }
        val frame = computeFrame(containerW, containerH, videoW, videoH, screenMode, ratio)
        val frameWDp = with(density) { frame.width.toDp() }
        val frameHDp = with(density) { frame.height.toDp() }


        // Gesture layer under the picture and the controls: taps, double-taps, swipes, pinch.
        Box(
            modifier = Modifier
                .fillMaxSize()
            .pointerInput(gestures.doubleTapSeek) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = { offset ->
                        val zone = tapZone(offset.x, size.width.toFloat())
                        val active = player
                        if (gestures.doubleTapSeek && zone != TapZone.Center && active != null) {
                            val delta = if (zone == TapZone.Right) SEEK_STEP_MS else -SEEK_STEP_MS
                            active.seekTo(seekTarget(active.currentPosition, delta, durationMs))
                            ended = false
                            seekFlash = zone
                            flashTick++
                            onInteraction()
                        } else if (zoom > 1f) {
                            zoom = 1f
                            panX = 0f
                            panY = 0f
                        }
                    },
                )
            }
            // Left half: brightness. Right half: volume. A half whose gesture is off is left alone,
            // so swiping down there still closes the video.
            .pointerInput(gestures.brightness, gestures.volume) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val width = size.width.toFloat()
                    val left = isLeftHalf(down.position.x, width)
                    if (if (left) !gestures.brightness else !gestures.volume) return@awaitEachGesture
                    val extent = size.height * 0.7f
                    val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val maxVolume = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                    var level = if (left) currentBrightness(context) else audio.getStreamVolume(AudioManager.STREAM_MUSIC) / maxVolume.toFloat()
                    fun move(dy: Float) {
                        level = adjustLevel(level, dy, extent)
                        if (left) {
                            setBrightness(context, level)
                            indicator = LevelIndicator(LevelKind.Brightness, level)
                        } else {
                            audio.setStreamVolume(AudioManager.STREAM_MUSIC, (level * maxVolume).roundToInt(), 0)
                            indicator = LevelIndicator(LevelKind.Volume, level)
                        }
                        indicatorTick++
                    }
                    val first = awaitVerticalTouchSlopOrCancellation(down.id) { change, over ->
                        change.consume()
                        move(over)
                    }
                    if (first != null) {
                        verticalDrag(first.id) { change ->
                            move(change.positionChange().y)
                            change.consume()
                        }
                    }
                }
            }
            // Two fingers pinch-zoom and pan; one finger pans only while already zoomed.
            .pointerInput(containerW, containerH, frame) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var active = false
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.count { it.pressed }
                        val pinching = pressed >= 2
                        if (pinching || (pressed == 1 && zoom > 1f)) {
                            val factor = if (pinching) event.calculateZoom() else 1f
                            val pan = event.calculatePan()
                            if (factor != 1f || pan != Offset.Zero) {
                                zoom = (zoom * factor).clampZoom()
                                if (zoom <= 1f) {
                                    panX = 0f
                                    panY = 0f
                                } else {
                                    panX = clampPan(panX + pan.x, frame.width, containerW, zoom)
                                    panY = clampPan(panY + pan.y, frame.height, containerH, zoom)
                                }
                                active = true
                                dragging = true
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                    if (active) dragging = false
                }
            }
        )

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

        // Subtitles, drawn by the app from the player's cue text.
        if (cueText.isNotBlank()) {
            val lifted by animateDpAsState(
                targetValue = if (controlsVisible) maxOf(subtitlePosition.bottomDp, if (actionBarVisible) 160 else 72).dp else subtitlePosition.bottomDp.dp,
                label = "subtitleLift",
            )
            Text(
                text = cueText,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = (18f * subtitleSize.scale).sp, textAlign = TextAlign.Center),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 32.dp, end = 32.dp, bottom = lifted)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }

        // Brightness / volume level and double-tap seek feedback.
        indicator?.let { LevelPill(it, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 72.dp)) }
        seekFlash?.let { zone ->
            Text(
                text = if (zone == TapZone.Right) "+10s" else "-10s",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .align(if (zone == TapZone.Right) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 40.dp)
                    .clip(CircleShape)
                    .background(ScrimColor)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }

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
                            text = { Text("Playback speed · ${speedLabel(speed)}") },
                            leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showSpeedSheet = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Subtitles and audio") },
                            leadingIcon = { Icon(Icons.Rounded.Subtitles, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showTracksSheet = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (abRange.isActive) "A-B repeat · on" else "A-B repeat") },
                            leadingIcon = { Icon(Icons.Rounded.Repeat, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showAbSheet = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Bookmarks") },
                            leadingIcon = { Icon(Icons.Rounded.BookmarkBorder, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showBookmarks = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (queue.repeatMode == RepeatMode.Off) "Queue and repeat" else "Queue · ${queue.repeatMode.label}") },
                            leadingIcon = { Icon(Icons.Rounded.QueueMusic, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                queue.onShowQueue()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Rotation · ${orientationLock.label}") },
                            leadingIcon = { Icon(Icons.Rounded.ScreenRotation, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onCycleOrientation()
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
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (queue.canPrev || queue.canNext) {
                        TransportButton(Icons.Rounded.SkipPrevious, "Previous video", 40.dp, enabled = queue.canPrev) {
                            queue.onPrev()
                        }
                    }
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
                    if (queue.canPrev || queue.canNext) {
                        TransportButton(Icons.Rounded.SkipNext, "Next video", 40.dp, enabled = queue.canNext) {
                            queue.onNext()
                        }
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
                    abRange = abRange,
                    previewUri = item.uri,
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

    if (showTracksSheet) {
        val subtitleOptions = remember(tracks) { trackOptions(tracks, C.TRACK_TYPE_TEXT, "Subtitle") }
        val audioOptions = remember(tracks) { trackOptions(tracks, C.TRACK_TYPE_AUDIO, "Audio") }
        TracksSheet(
            subtitles = subtitleOptions,
            audio = audioOptions,
            size = subtitleSize,
            position = subtitlePosition,
            onSelectSubtitle = { option ->
                val active = player
                if (active != null) {
                    val params = active.trackSelectionParameters.buildUpon()
                    if (option == null) {
                        params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        cueText = ""
                    } else {
                        params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                            .setOverrideForType(TrackSelectionOverride(tracks.groups[option.groupIndex].mediaTrackGroup, option.trackIndex))
                    }
                    active.trackSelectionParameters = params.build()
                }
            },
            onPickSrt = { srtPicker.launch(arrayOf("*/*")) },
            onSize = { subtitleSize = it },
            onPosition = { subtitlePosition = it },
            onSelectAudio = { option ->
                player?.let { active ->
                    active.trackSelectionParameters = active.trackSelectionParameters.buildUpon()
                        .setOverrideForType(TrackSelectionOverride(tracks.groups[option.groupIndex].mediaTrackGroup, option.trackIndex))
                        .build()
                }
            },
            onDismiss = { showTracksSheet = false },
        )
    }

    if (showAbSheet) {
        AbRepeatSheet(
            range = abRange,
            positionMs = positionMs,
            onSetA = { abRange = abRange.withStart(positionMs) },
            onSetB = { abRange = abRange.withEnd(positionMs) },
            onClear = { abRange = AbRange() },
            onDismiss = { showAbSheet = false },
        )
    }

    if (showBookmarks) {
        val bookmarks by remember(item.id) { container.bookmarks.observe(item.id) }
            .collectAsStateWithLifecycle(initialValue = emptyList())
        BookmarksSheet(
            bookmarks = bookmarks,
            positionMs = positionMs,
            onAdd = { name -> scope.launch { container.bookmarks.add(item.id, positionMs, name) } },
            onJump = { bookmark ->
                player?.seekTo(bookmark.positionMs)
                positionMs = bookmark.positionMs
                ended = false
                showBookmarks = false
                onInteraction()
            },
            onDelete = { bookmark -> scope.launch { container.bookmarks.delete(bookmark.id) } },
            onDismiss = { showBookmarks = false },
        )
    }

    if (showSpeedSheet) {
        PlaybackSpeedSheet(
            speed = speed,
            onSpeed = {
                speed = it
                onInteraction()
            },
            onDismiss = { showSpeedSheet = false },
        )
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

/** Audio or subtitle tracks of [type] from the player's current track groups. */
private fun trackOptions(tracks: Tracks, type: Int, kind: String): List<TrackOption> {
    val out = mutableListOf<TrackOption>()
    tracks.groups.forEachIndexed { groupIndex, group ->
        if (group.type != type) return@forEachIndexed
        for (trackIndex in 0 until group.length) {
            val format = group.getTrackFormat(trackIndex)
            val detail = if (type == C.TRACK_TYPE_AUDIO) {
                listOfNotNull(channelLabel(format.channelCount), format.sampleMimeType?.substringAfter('/')?.uppercase())
                    .joinToString(" · ").ifEmpty { null }
            } else {
                null
            }
            out += TrackOption(
                groupIndex = groupIndex,
                trackIndex = trackIndex,
                title = trackTitle(format.label, format.language, out.size, kind),
                detail = detail,
                selected = group.isTrackSelected(trackIndex),
                supported = group.isTrackSupported(trackIndex),
            )
        }
    }
    return out
}

private fun displayNameOf(context: Context, uri: Uri): String =
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: uri.lastPathSegment.orEmpty()

private enum class LevelKind { Brightness, Volume }

private data class LevelIndicator(val kind: LevelKind, val level: Float)

@Composable
private fun LevelPill(indicator: LevelIndicator, modifier: Modifier = Modifier) {
    val icon = when {
        indicator.kind == LevelKind.Brightness -> Icons.Rounded.BrightnessMedium
        indicator.level <= 0f -> Icons.Rounded.VolumeOff
        else -> Icons.Rounded.VolumeUp
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(ScrimColor)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, contentDescription = if (indicator.kind == LevelKind.Brightness) "Brightness" else "Volume", tint = Color.White, modifier = Modifier.size(20.dp))
        Box(Modifier.width(110.dp).height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f))) {
            Box(Modifier.fillMaxWidth(indicator.level).fillMaxHeight().background(Color.White))
        }
        Text("${(indicator.level * 100).roundToInt()}%", color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}

/** Brightness the window is showing now, as 0..1 (falls back to the system setting). */
private fun currentBrightness(context: Context): Float {
    val window = context.findActivity()?.window
    val own = window?.attributes?.screenBrightness ?: -1f
    if (own >= 0f) return own
    val system = runCatching { Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS) }
        .getOrDefault(128)
    return (system / 255f).coerceIn(0.01f, 1f)
}

/** Window-only brightness override; the viewer restores the previous value when it closes. */
private fun setBrightness(context: Context, level: Float) {
    val window = context.findActivity()?.window ?: return
    window.attributes = window.attributes.also { it.screenBrightness = level.coerceIn(0.01f, 1f) }
}

@Composable
private fun TransportButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    size: Dp,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(size).clip(CircleShape).background(ScrimColor),
    ) {
        Icon(icon, contentDescription = description, tint = Color.White.copy(alpha = if (enabled) 1f else 0.35f), modifier = Modifier.size(size * 0.55f))
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
    abRange: AbRange,
    previewUri: String,
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .displayCutoutPadding()
            .padding(bottom = bottomInset),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
    if (dragging && durationMs > 0) {
        // Frame preview while scrubbing: one request per second of video.
        val context = LocalContext.current
        val seconds = (dragFraction * durationMs / 1000f).toLong()
        val request = remember(previewUri, seconds) {
            ImageRequest.Builder(context)
                .data(previewUri)
                .videoFrameMillis(seconds * 1000L)
                .size(320, 180)
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = "Seek preview at ${formatDuration(shownPosition)}",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(bottom = 6.dp)
                .size(width = 128.dp, height = 72.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black),
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
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
                    val a = abRange.startMs
                    if (a != null && durationMs > 0) {
                        val start = (a.toFloat() / durationMs).coerceIn(0f, 1f)
                        val end = ((abRange.endMs ?: a).toFloat() / durationMs).coerceIn(start, 1f)
                        Row(Modifier.fillMaxSize()) {
                            Spacer(Modifier.weight(maxOf(start, 0.0001f)))
                            Box(Modifier.weight(maxOf(end - start, 0.006f)).fillMaxHeight().background(Color(0xFFFFC857)))
                            Spacer(Modifier.weight(maxOf(1f - end, 0.0001f)))
                        }
                    }
                    Box(
                        Modifier
                            .fillMaxWidth(shown)
                            .fillMaxHeight()
                            .background(Color.White.copy(alpha = if (abRange.startMs != null) 0.6f else 1f)),
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
