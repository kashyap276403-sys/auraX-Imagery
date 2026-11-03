package com.aurax.imagery.ui.viewer

import android.app.PictureInPictureParams
import android.content.BroadcastReceiver
import android.content.Context
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.ZoomIn
import com.aurax.imagery.domain.player.next
import com.aurax.imagery.domain.player.nextZoomPreset
import com.aurax.imagery.domain.player.zoomLabel
import androidx.compose.runtime.mutableIntStateOf
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.View
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Forward5
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Replay30
import androidx.compose.material.icons.rounded.Replay5
import androidx.compose.runtime.getValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.aurax.imagery.domain.player.CinematicMode
import com.aurax.imagery.domain.player.swipeDeltaLabel
import com.aurax.imagery.domain.player.swipeSeekTarget
import com.aurax.imagery.domain.player.VideoPlayerSettings
import com.aurax.imagery.ui.glass.rememberReducedMotion
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import android.provider.MediaStore
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.TextButton
import androidx.compose.runtime.produceState
import androidx.core.content.ContextCompat
import com.aurax.imagery.data.media.VideoFrameSaver
import com.aurax.imagery.domain.player.resumePosition
import com.aurax.imagery.ui.gallery.shareItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
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
import com.aurax.imagery.ui.components.GlassDropdownMenu
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

/** Viewer-level extras the page needs: sleep timer state. */
data class PlayerSession(
    val sleepEndsAt: Long?,
    val onShowSleepTimer: () -> Unit,
    val onSleepExpired: () -> Unit,
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
    inPip: Boolean,
    session: PlayerSession,
    onPlayingChanged: (Boolean) -> Unit,
    onBusyChanged: (Boolean) -> Unit,
    onInteraction: () -> Unit,
    onZoomChanged: (Boolean) -> Unit,
    orientationLock: OrientationLock,
    onCycleOrientation: () -> Unit,
    queue: QueueNav,
    movieMode: Boolean,
    onMovieMode: (Boolean) -> Unit,
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
    var showDetails by remember { mutableStateOf(false) }
    var hasPlayed by remember(item.id) { mutableStateOf(false) }
    var resumedAt by remember(item.id) { mutableStateOf<Long?>(null) }
    var textureRef by remember(item.id) { mutableStateOf<TextureView?>(null) }
    val activity = remember(context) { context.findActivity() }
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
    // Holding a finger on the picture plays at 2x until it is lifted (Movie mode).
    var holdFast by remember { mutableStateOf(false) }
    var indicator by remember { mutableStateOf<LevelIndicator?>(null) }
    var indicatorTick by remember { mutableIntStateOf(0) }
    var swipeSeek by remember { mutableStateOf<SwipeSeek?>(null) }
    var seekFlash by remember { mutableStateOf<TapZone?>(null) }
    var flashTick by remember { mutableIntStateOf(0) }
    val gestures by container.preferences.videoGestures
        .collectAsStateWithLifecycle(initialValue = VideoGestureSettings())
    val vs by container.preferences.videoPlayer.collectAsStateWithLifecycle(initialValue = VideoPlayerSettings())
    val haptic = LocalHapticFeedback.current
    val reduceMotion = rememberReducedMotion()
    val fadeMs = if (reduceMotion) 0 else 220
    var cinematic by remember { mutableStateOf(CinematicMode.Original) }
    var showCinematic by remember { mutableStateOf(false) }
    var sizeKnown by remember(item.id) { mutableStateOf(false) }
    var modeToast by remember { mutableStateOf<String?>(null) }
    var toastTick by remember { mutableIntStateOf(0) }
    var showVolume by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }
    var volumeTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(toastTick) {
        if (modeToast != null) {
            delay(1300L)
            modeToast = null
        }
    }
    LaunchedEffect(showVolume, volumeTick) {
        if (showVolume) {
            delay(3000L)
            showVolume = false
        }
    }
    fun toast(text: String) {
        modeToast = text
        toastTick++
    }
    LaunchedEffect(item.id) { speed = container.preferences.videoPlayer.first().defaultSpeed }
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
    LaunchedEffect(player, speed, movieMode, holdFast) {
        player?.setPlaybackParameters(PlaybackParameters(if (movieMode && holdFast) 2f else if (movieMode) speed else 1f))
    }
    // Leaving Movie mode drops its extras: zoom, A-B loop.
    LaunchedEffect(movieMode) {
        if (!movieMode) {
            zoom = 1f
            panX = 0f
            panY = 0f
            abRange = AbRange()
        }
    }
    LaunchedEffect(player, queue.repeatMode) {
        player?.repeatMode = if (queue.repeatMode == RepeatMode.One) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }
    LaunchedEffect(ended, isCurrentPage) { if (ended && isCurrentPage) latestQueue.onEnded() }

    fun selectSubtitle(option: TrackOption?) {
        val active = player ?: return
        val group = option?.let { tracks.groups.getOrNull(it.groupIndex) }
        if (option != null && group == null) return // the track list changed under the picker
        val params = active.trackSelectionParameters.buildUpon()
        if (option == null) {
            params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            cueText = ""
        } else {
            params.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setOverrideForType(TrackSelectionOverride(group!!.mediaTrackGroup, option.trackIndex))
        }
        active.trackSelectionParameters = params.build()
    }
    fun selectAudio(option: TrackOption) {
        val group = tracks.groups.getOrNull(option.groupIndex) ?: return
        val active = player ?: return
        active.trackSelectionParameters = active.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex))
            .build()
    }
    // Audio button: jump to the next usable audio track; with only one, open the picker instead.
    fun cycleAudio() {
        val usable = trackOptions(tracks, C.TRACK_TYPE_AUDIO, "Audio").filter { it.supported }
        if (usable.size < 2) {
            showTracksSheet = true
        } else {
            val next = usable[(usable.indexOfFirst { it.selected } + 1).mod(usable.size)]
            selectAudio(next)
            toast("AUDIO · ${next.title.uppercase()}")
        }
    }
    // "Subtitles on by default": switch the first usable subtitle track on once per video.
    var subtitleDefaultDone by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(tracks, vs.subtitlesOnByDefault, movieMode) {
        if (movieMode && vs.subtitlesOnByDefault && !subtitleDefaultDone) {
            val options = trackOptions(tracks, C.TRACK_TYPE_TEXT, "Subtitle")
            if (options.isNotEmpty()) {
                subtitleDefaultDone = true
                if (options.none { it.selected }) options.firstOrNull { it.supported }?.let(::selectSubtitle)
            }
        }
    }

    // Resume position: written while playing, when paused, at the end and when the page closes.
    fun saveProgress(p: ExoPlayer, finished: Boolean = false) {
        if (!hasPlayed) return
        val total = if (p.duration != C.TIME_UNSET && p.duration > 0) p.duration else durationMs
        val position = if (finished) total else p.currentPosition
        container.appScope.launch { container.history.save(item.id, position, total) }
    }
    LaunchedEffect(player) {
        val active = player ?: return@LaunchedEffect
        if (!container.preferences.videoResume.first()) return@LaunchedEffect
        val saved = container.history.get(item.id) ?: return@LaunchedEffect
        val total = if (saved.durationMs > 0) saved.durationMs else (item.durationMillis ?: 0L)
        val target = resumePosition(saved.positionMs, total) ?: return@LaunchedEffect
        active.seekTo(target)
        positionMs = target
        resumedAt = target
    }
    LaunchedEffect(resumedAt) {
        if (resumedAt != null) {
            delay(6000L)
            resumedAt = null
        }
    }

    // Sleep timer: pause when it runs out (it follows you to the next video).
    LaunchedEffect(isCurrentPage, session.sleepEndsAt, player) {
        val end = session.sleepEndsAt
        val active = player
        if (isCurrentPage && end != null && active != null) {
            delay((end - SystemClock.elapsedRealtime()).coerceAtLeast(0L))
            active.pause()
            session.onSleepExpired()
        }
    }

    // Picture-in-picture: keep the system informed so the home gesture can shrink a playing video.
    LaunchedEffect(isCurrentPage, isPlaying, videoW, videoH) {
        if (isCurrentPage && activity != null) {
            setPipParams(activity, pipParams(context, videoW, videoH, isPlaying, autoEnter = isPlaying))
        }
    }
    DisposableEffect(isCurrentPage) {
        onDispose {
            if (isCurrentPage && activity != null) {
                setPipParams(activity, PictureInPictureParams.Builder().setAutoEnterEnabled(false).build())
            }
        }
    }
    DisposableEffect(isCurrentPage, player) {
        val active = player
        if (!isCurrentPage || active == null) return@DisposableEffect onDispose { }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (active.isPlaying) active.pause() else active.play()
            }
        }
        ContextCompat.registerReceiver(context, receiver, IntentFilter(PIP_ACTION_TOGGLE), ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val settings = container.preferences.videoPlayer.first()
        cinematic = container.preferences.videoCinematic.first()
        screenMode = settings.defaultScreenMode
        if (settings.rememberScreenMode) {
            container.preferences.videoScreenMode.first()
                ?.let { saved -> ScreenMode.entries.firstOrNull { it.name == saved } }
                ?.let { screenMode = it }
            container.preferences.videoAspectPreset.first()
                ?.let { saved -> AspectPreset.entries.firstOrNull { it.name == saved } }
                ?.let { preset = it }
        }
    }
    fun applyDisplay(mode: ScreenMode, newPreset: AspectPreset) {
        screenMode = mode
        preset = newPreset
        if (vs.rememberScreenMode) scope.launch { container.preferences.setVideoDisplay(mode.name, newPreset.name) }
    }
    LaunchedEffect(zoom) { onZoomChanged(zoom > 1.001f) }
    LaunchedEffect(isCurrentPage, showRatioSheet, showSpeedSheet, showTracksSheet, showAbSheet, showBookmarks, showDetails, showCinematic, showMenu, dragging) {
        // Neighbouring pages are composed too; only the visible one may report "busy".
        if (isCurrentPage) onBusyChanged(showCinematic || showRatioSheet || showSpeedSheet || showTracksSheet || showAbSheet || showBookmarks || showDetails || showMenu || dragging)
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
            saveProgress(created)
            created.release()
            player = null
            isPlaying = false
            firstFrameShown = false
        }
    }

    if (isCurrentPage) EqualizerEffect(player)
    if (showEqualizer && isCurrentPage) EqualizerDialog(onDismiss = { showEqualizer = false })

    // Mirror player state into Compose state.
    DisposableEffect(player) {
        val active = player ?: return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) hasPlayed = true else saveProgress(active)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                ended = playbackState == Player.STATE_ENDED
                if (ended) saveProgress(active, finished = true)
                if (playbackState == Player.STATE_READY && active.duration != C.TIME_UNSET) {
                    durationMs = active.duration
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    aspect = videoSize.width * videoSize.pixelWidthHeightRatio / videoSize.height
                    sizeKnown = true
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

    val controlsNow by rememberUpdatedState(controlsVisible)
    // Poll the playback position: fast while the seek bar is on screen, slow while paused or hidden.
    LaunchedEffect(player) {
        val active = player ?: return@LaunchedEffect
        var lastSave = SystemClock.elapsedRealtime()
        while (isActive) {
            positionMs = active.currentPosition.coerceAtLeast(0L)
            if (isPlaying && SystemClock.elapsedRealtime() - lastSave > 5_000L) {
                saveProgress(active)
                lastSave = SystemClock.elapsedRealtime()
            }
            abRange.loopTarget(positionMs)?.let { target ->
                active.seekTo(target)
                positionMs = target
            }
            delay(if (abRange.isActive && isPlaying) 80L else if (isPlaying && controlsNow) 250L else if (isPlaying) 1000L else 600L)
        }
    }

    // Never keep playing in the background.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player?.pause()
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
        // Normal mode always shows the whole picture (Fit); screen mode, ratio and zoom are Movie mode features.
        val modeNow = if (movieMode) screenMode else ScreenMode.Fit
        val ratio = if (movieMode) preset.ratio.takeIf { screenMode == ScreenMode.Fit || screenMode == ScreenMode.Fill || screenMode == ScreenMode.Best } else null
        val frame = computeFrame(containerW, containerH, videoW, videoH, modeNow, ratio)
        val frameWDp by animateDpAsState(with(density) { frame.width.toDp() }, tween(if (reduceMotion) 0 else 240), label = "frameW")
        val frameHDp by animateDpAsState(with(density) { frame.height.toDp() }, tween(if (reduceMotion) 0 else 240), label = "frameH")


        // Until the player reports the real picture size, show the poster fitted to the screen. MediaStore
        // sizes can ignore rotation, so a frame built from them could flash the wrong shape.
        if (!firstFrameShown && !sizeKnown) {
            AsyncImage(
                model = item.uri,
                contentDescription = item.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }

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
            // Poster frame until the first video frame is on screen (only once the real shape is known).
            if (!firstFrameShown && sizeKnown) {
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
                        factory = { ctx -> TextureView(ctx).also { active.setVideoTextureView(it); textureRef = it } },
                        update = { view -> applyLook(view, if (movieMode) cinematic else CinematicMode.Original) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        // Gesture layer above the picture (a TextureView would swallow touches meant for a layer below it) and below the controls: taps, double-taps, swipes, pinch.
        Box(
            modifier = Modifier
                .fillMaxSize()
            .pointerInput(gestures.doubleTapSeek, movieMode) {
                detectTapGestures(
                    onPress = {
                        if (movieMode) {
                            kotlinx.coroutines.coroutineScope {
                                val timer = launch {
                                    kotlinx.coroutines.delay(viewConfiguration.longPressTimeoutMillis + 150L)
                                    holdFast = true
                                    if (vs.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                tryAwaitRelease()
                                timer.cancel()
                                holdFast = false
                            }
                        }
                    },
                    onTap = { onToggleControls() },
                    onDoubleTap = { offset ->
                        val zone = tapZone(offset.x, size.width.toFloat())
                        val active = player
                        if (movieMode && gestures.doubleTapSeek && zone != TapZone.Center && active != null) {
                            val delta = if (zone == TapZone.Right) vs.seekIntervalSec * 1000L else -vs.seekIntervalSec * 1000L
                            active.seekTo(seekTarget(active.currentPosition, delta, durationMs))
                            ended = false
                            seekFlash = zone
                            flashTick++
                            if (vs.haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
            // One finger, VLC style: swipe sideways to seek, up/down on the left half for brightness and on
            // the right half for volume. Decides the direction after a small move so taps still work.
            .pointerInput(gestures.brightness, gestures.volume, movieMode) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!movieMode) return@awaitEachGesture
                    val slop = viewConfiguration.touchSlop
                    var totalX = 0f
                    var totalY = 0f
                    var horizontal = false
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.count { it.pressed } > 1) return@awaitEachGesture // a pinch, not ours
                        val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                        if (!change.pressed) return@awaitEachGesture
                        val delta = change.positionChange()
                        totalX += delta.x
                        totalY += delta.y
                        if (kotlin.math.abs(totalX) > slop && kotlin.math.abs(totalX) > kotlin.math.abs(totalY)) {
                            horizontal = true
                            break
                        }
                        if (kotlin.math.abs(totalY) > slop) break
                    }
                    val width = size.width.toFloat()
                    if (horizontal) {
                        val p = player ?: return@awaitEachGesture
                        val startMs = p.currentPosition
                        val total = durationMs
                        var targetMs = startMs
                        dragging = true
                        var dragX = totalX
                        horizontalDrag(down.id) { change ->
                            dragX += change.positionChange().x
                            change.consume()
                            targetMs = swipeSeekTarget(startMs, dragX, width, total)
                            swipeSeek = SwipeSeek(targetMs, targetMs - startMs)
                        }
                        dragging = false
                        if (swipeSeek != null) {
                            p.seekTo(targetMs)
                            positionMs = targetMs
                            ended = false
                            swipeSeek = null
                            onInteraction()
                        }
                        return@awaitEachGesture
                    }
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
                    move(totalY)
                    verticalDrag(down.id) { change ->
                        move(change.positionChange().y)
                        change.consume()
                    }
                }
            }
            // Two fingers pinch-zoom and move the picture. One finger never moves it, so seeking and brightness/volume swipes cannot shift the frame.
            .pointerInput(containerW, containerH, frame, movieMode) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    if (!movieMode) return@awaitEachGesture
                    var active = false
                    do {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.count { it.pressed }
                        val pinching = pressed >= 2
                        if (pinching) {
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
        if (movieMode && cueText.isNotBlank()) {
            val lifted by animateDpAsState(
                targetValue = if (controlsVisible) maxOf(subtitlePosition.bottomDp, (if (actionBarVisible) 104 else 12) + 150).dp else subtitlePosition.bottomDp.dp,
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

        resumedAt?.let { at ->
            PlayerPill(
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 150.dp),
                announce = "Continuing from ${formatDuration(at)}",
            ) {
                Text("Continuing from ${formatDuration(at)}", color = Color.White, style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = {
                    player?.seekTo(0L)
                    positionMs = 0L
                    resumedAt = null
                }) { Text("Start over") }
            }
        }

        // Brightness / volume level and double-tap seek feedback.
        indicator?.let { LevelPill(it, Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 72.dp)) }
        if (holdFast) {
            PlayerPill(
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 72.dp),
                announce = "Playing at 2 times speed",
            ) {
                Text("2×  ▸▸", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }
        swipeSeek?.let { sw ->
            PlayerPill(
                modifier = Modifier.align(Alignment.Center),
                announce = "Seek ${swipeDeltaLabel(sw.deltaMs)}",
            ) {
                Text(
                    text = "${swipeDeltaLabel(sw.deltaMs)}   ${formatDuration(sw.targetMs)} / ${formatDuration(durationMs)}",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
        seekFlash?.let { zone ->
            val label = if (zone == TapZone.Right) "+${vs.seekIntervalSec}s" else "-${vs.seekIntervalSec}s"
            PlayerPill(
                modifier = Modifier
                    .align(if (zone == TapZone.Right) Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 40.dp),
                announce = if (zone == TapZone.Right) "Forward ${vs.seekIntervalSec} seconds" else "Back ${vs.seekIntervalSec} seconds",
            ) {
                Text(text = label, color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }

        val infoLine = listOfNotNull(
            if (videoW > 0f && videoH > 0f) "${videoW.toInt()}×${videoH.toInt()}" else null,
            if (speed != 1f) speedLabel(speed) else null,
            if (!cinematic.isOriginal) cinematic.label else null,
        ).joinToString(" · ").ifEmpty { null }
        PlayerTopBar(
            visible = controlsVisible,
            fadeMs = fadeMs,
            slide = !reduceMotion,
            title = item.displayName,
            info = infoLine,
            glass = movieMode,
            onClose = onClose,
            onMore = { showMenu = true },
            menu = {
                GlassDropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (movieMode) {
                    DropdownMenuItem(
                            text = { Text("Screen ratio") },
                            leadingIcon = { Icon(Icons.Rounded.AspectRatio, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showRatioSheet = true
                            },
                        )
                        if (zoom > 1f) {
                        DropdownMenuItem(
                            text = { Text("Reset zoom") },
                            leadingIcon = { Icon(Icons.Rounded.ZoomIn, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                zoom = 1f
                                panX = 0f
                                panY = 0f
                                toast("ZOOM 100%")
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Zoom · ${zoomLabel(zoom)}") },
                        leadingIcon = { Icon(Icons.Rounded.ZoomIn, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            val next = nextZoomPreset(zoom)
                            zoom = next
                            if (next <= 1f) {
                                panX = 0f
                                panY = 0f
                            }
                            toast("ZOOM ${zoomLabel(next)}")
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(if (EqState.enabled) "Equalizer · ${EqState.preset.label}" else "Equalizer") },
                        leadingIcon = { Icon(Icons.Rounded.VolumeUp, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            showEqualizer = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Volume") },
                        leadingIcon = { Icon(Icons.Rounded.VolumeUp, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            showVolume = true
                            volumeTick++
                        },
                    )
                    DropdownMenuItem(
                            text = { Text(if (cinematic.isOriginal) "Cinematic look" else "Cinematic look · ${cinematic.label}") },
                            leadingIcon = { Icon(Icons.Rounded.Movie, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showCinematic = true
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
                            text = { Text("Picture in picture") },
                            leadingIcon = { Icon(Icons.Rounded.PictureInPictureAlt, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                if (activity == null || !enterPip(activity, pipParams(context, videoW, videoH, isPlaying, isPlaying))) {
                                    Toast.makeText(context, "Picture-in-picture isn't available", Toast.LENGTH_SHORT).show()
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (session.sleepEndsAt != null) "Sleep timer · on" else "Sleep timer") },
                            leadingIcon = { Icon(Icons.Rounded.Bedtime, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                session.onShowSleepTimer()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Save frame as photo") },
                            leadingIcon = { Icon(Icons.Rounded.PhotoCamera, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                val view = textureRef
                                val w = videoW.toInt().coerceIn(2, 4096)
                                val h = videoH.toInt().coerceIn(2, 4096)
                                val frame = if (view != null && firstFrameShown) runCatching { view.getBitmap(w, h) }.getOrNull() else null
                                if (frame == null) {
                                    Toast.makeText(context, "Start the video first, then save a frame", Toast.LENGTH_SHORT).show()
                                } else {
                                    scope.launch {
                                        VideoFrameSaver(context).save(frame)
                                            .onSuccess {
                                                Toast.makeText(context, "Saved to Pictures/AuraX Imagery", Toast.LENGTH_SHORT).show()
                                                container.indexer.enqueue()
                                            }
                                            .onFailure { Toast.makeText(context, "Couldn't save the frame", Toast.LENGTH_SHORT).show() }
                                    }
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                shareItems(context, listOf(item))
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
                                showDetails = true
                            },
                        )
                    DropdownMenuItem(
                        text = { Text("Exit Movie mode") },
                        leadingIcon = { Icon(Icons.Rounded.Movie, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onMovieMode(false)
                            toast("MOVIE MODE OFF")
                        },
                    )
                    } else {
                    DropdownMenuItem(
                            text = { Text("Picture in picture") },
                            leadingIcon = { Icon(Icons.Rounded.PictureInPictureAlt, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                if (activity == null || !enterPip(activity, pipParams(context, videoW, videoH, isPlaying, isPlaying))) {
                                    Toast.makeText(context, "Picture-in-picture isn't available", Toast.LENGTH_SHORT).show()
                                }
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                shareItems(context, listOf(item))
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
                                showDetails = true
                            },
                        )
                    DropdownMenuItem(
                        text = { Text("Movie mode") },
                        leadingIcon = { Icon(Icons.Rounded.Movie, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onMovieMode(true)
                            toast("MOVIE MODE ON")
                        },
                    )
                    }
                }
            },
            modifier = Modifier.align(Alignment.TopCenter),
        )

        modeToast?.let { text ->
            PlayerPill(
                modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 84.dp),
                announce = text,
            ) {
                Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }

        // Centre transport: previous, rewind, play/pause, forward, next.
        if (active != null && ready) {
            PlayerTransport(
                visible = (controlsVisible || !isPlaying) && !inPip,
                fadeMs = fadeMs,
                isPlaying = isPlaying,
                ended = ended,
                showSeek = movieMode,
                rewindIcon = replayIcon(vs.seekIntervalSec),
                forwardIcon = forwardIcon(vs.seekIntervalSec),
                seekSeconds = vs.seekIntervalSec,
                onRewind = {
                    active.seekTo((active.currentPosition - vs.seekIntervalSec * 1000L).coerceAtLeast(0L))
                    ended = false
                    onInteraction()
                },
                onPlayPause = {
                    when {
                        ended -> {
                            active.seekTo(0L)
                            active.play()
                        }
                        isPlaying -> active.pause()
                        else -> active.play()
                    }
                    if (vs.haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onInteraction()
                },
                onForward = {
                    val limit = if (durationMs > 0) durationMs else Long.MAX_VALUE
                    active.seekTo((active.currentPosition + vs.seekIntervalSec * 1000L).coerceAtMost(limit))
                    onInteraction()
                },
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // Bottom panel: caption, control cluster, thin seek bar with scrub preview, times, chips.
        if (active != null && ready) {
            val panelInset = if (actionBarVisible) 104.dp else 12.dp
            val date = remember(item.dateTakenMillis) {
                java.time.Instant.ofEpochMilli(item.dateTakenMillis).atZone(java.time.ZoneId.systemDefault())
                    .format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))
            }
            if (showVolume && controlsVisible) {
                VolumeFlyout(
                    onInteract = {
                        volumeTick++
                        onInteraction()
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 20.dp, bottom = panelInset + 236.dp),
                )
            }
            PlayerBottomPanel(
                visible = controlsVisible,
                fadeMs = fadeMs,
                slide = !reduceMotion,
                bottomInset = panelInset,
                caption = if (movieMode) listOfNotNull(item.albumName, date).joinToString(" · ") else null,
                positionMs = positionMs,
                durationMs = durationMs,
                abRange = abRange,
                previewUri = item.uri,
                videoAspect = if (videoW > 0f && videoH > 0f) videoW / videoH else 16f / 9f,
                isLandscape = isLandscape,
                onSeek = { target ->
                    active.seekTo(target)
                    positionMs = target
                    ended = false
                    onInteraction()
                },
                onDragging = { dragging = it },
                onToggleFullscreen = onToggleFullscreen,
                cluster = if (movieMode) {
                    {
                        GlassCircleButton(Icons.Rounded.Subtitles, "Subtitles", { showTracksSheet = true }, size = 44.dp, iconSize = 22.dp, plain = true, reduceMotion = reduceMotion)
                        GlassCircleButton(Icons.Rounded.Audiotrack, "Audio track. Long press to switch to the next one", {
                            showTracksSheet = true
                        }, onLongClick = {
                            cycleAudio()
                            onInteraction()
                        }, size = 44.dp, iconSize = 22.dp, plain = true, reduceMotion = reduceMotion)
                        GlassCircleButton(
                            icon = Icons.Rounded.AspectRatio,
                            description = "Screen ratio ${screenMode.label}. Long press for presets",
                            onClick = {
                                val next = screenMode.next()
                                applyDisplay(next, preset)
                                toast(next.label.uppercase())
                                onInteraction()
                            },
                            onLongClick = { showRatioSheet = true },
                            size = 44.dp, iconSize = 22.dp, plain = true, reduceMotion = reduceMotion,
                        )
                    }
                } else {
                    null
                },
                chips = null,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (showCinematic) {
        CinematicSheet(
            mode = cinematic,
            onMode = {
                cinematic = it
                scope.launch { container.preferences.setVideoCinematic(it) }
            },
            onDismiss = { showCinematic = false },
        )
    }

    if (showDetails) {
        val tech = remember(tracks, firstFrameShown) { player?.let(::techInfo) }
        val location by produceState<String?>(null, item.id) {
            value = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.query(Uri.parse(item.uri), arrayOf(MediaStore.MediaColumns.RELATIVE_PATH), null, null, null)
                        ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
                }.getOrNull()
            }
        }
        VideoDetailsSheet(item = item, tech = tech, location = location, onDismiss = { showDetails = false })
    }

    if (showTracksSheet) {
        val subtitleOptions = remember(tracks) { trackOptions(tracks, C.TRACK_TYPE_TEXT, "Subtitle") }
        val audioOptions = remember(tracks) { trackOptions(tracks, C.TRACK_TYPE_AUDIO, "Audio") }
        TracksSheet(
            subtitles = subtitleOptions,
            audio = audioOptions,
            size = subtitleSize,
            position = subtitlePosition,
            onSelectSubtitle = { option -> selectSubtitle(option) },
            onPickSrt = { srtPicker.launch(arrayOf("*/*")) },
            onSize = { subtitleSize = it },
            onPosition = { subtitlePosition = it },
            onSelectAudio = { option ->
                val group = tracks.groups.getOrNull(option.groupIndex)
                player?.let { active ->
                    if (group != null) {
                        active.trackSelectionParameters = active.trackSelectionParameters.buildUpon()
                            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex))
                            .build()
                    }
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

/** Technical facts about the stream being decoded, if the player has started on it. */
private fun techInfo(player: ExoPlayer): VideoTechInfo? {
    val video = player.videoFormat ?: return null
    val audio = player.audioFormat
    fun bitrateLabel(bps: Int): String? = if (bps > 0) "%.1f Mbps".format(bps / 1_000_000f) else null
    return VideoTechInfo(
        resolution = if (video.width > 0 && video.height > 0) "${video.width} × ${video.height}" else null,
        frameRate = if (video.frameRate > 0f) "%.1f fps".format(video.frameRate) else null,
        videoCodec = (video.codecs ?: video.sampleMimeType?.substringAfter('/')?.uppercase()),
        videoBitrate = bitrateLabel(if (video.averageBitrate > 0) video.averageBitrate else video.bitrate),
        audio = audio?.let { a ->
            listOfNotNull(
                a.sampleMimeType?.substringAfter('/')?.uppercase(),
                channelLabel(a.channelCount),
                if (a.bitrate > 0) "${a.bitrate / 1000} kbps" else null,
            ).joinToString(" · ").ifEmpty { null }
        },
    )
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
    val name = if (indicator.kind == LevelKind.Brightness) "Brightness" else "Volume"
    val percent = (indicator.level * 100).roundToInt()
    PlayerPill(modifier = modifier, announce = "$name $percent percent") {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Box(Modifier.width(110.dp).height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f))) {
            Box(Modifier.fillMaxWidth(indicator.level).fillMaxHeight().background(Color.White))
        }
        Text("$percent%", color = Color.White, style = MaterialTheme.typography.labelMedium)
    }
}

private fun replayIcon(seconds: Int) = when (seconds) {
    5 -> Icons.Rounded.Replay5
    30 -> Icons.Rounded.Replay30
    else -> Icons.Rounded.Replay10
}

private fun forwardIcon(seconds: Int) = when (seconds) {
    5 -> Icons.Rounded.Forward5
    30 -> Icons.Rounded.Forward30
    else -> Icons.Rounded.Forward10
}

/** Colour look via the TextureView's layer paint: one GPU colour multiply, and none at all for Original. */
private fun applyLook(view: TextureView, mode: CinematicMode) {
    if (mode.isOriginal) {
        view.setLayerType(View.LAYER_TYPE_NONE, null)
    } else {
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(ColorMatrix(mode.matrix)) }
        view.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
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

/** Live state of a horizontal swipe-to-seek: where it would land and how far that is from the start. */
private data class SwipeSeek(val targetMs: Long, val deltaMs: Long)
