package com.aurax.imagery.ui.viewer

import android.content.Context
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aurax.imagery.domain.player.AbRange
import com.aurax.imagery.utils.formatDuration
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

private val GlassFill = Color.Black.copy(alpha = 0.38f)

/** Faint luminous rim: brighter at the top-left, fading away, like light catching a glass edge. */
private fun rim(strength: Float) = Brush.linearGradient(
    listOf(Color.White.copy(alpha = 0.34f * strength), Color.White.copy(alpha = 0.06f * strength)),
)

/**
 * Round dark-glass button used all over the player. Presses spring in slightly; [luminous] gives
 * the main play button a brighter rim. Long-press is optional.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassCircleButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    luminous: Boolean = false,
    enabled: Boolean = true,
    reduceMotion: Boolean = false,
    plain: Boolean = false,
    onLongClick: (() -> Unit)? = null,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .then(
                if (plain) {
                    Modifier
                } else {
                    Modifier
                        .background(GlassFill)
                        .border(if (luminous) 1.5.dp else 1.dp, rim(if (luminous) 1.8f else 1f), CircleShape)
                },
            )
            .combinedClickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClickLabel = description,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = if (enabled) 1f else 0.35f),
            modifier = Modifier.size(iconSize),
        )
    }
}

/** Small translucent text chip (Info, Queue, Speed). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassChip(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(GlassFill)
            .border(1.dp, rim(0.8f), CircleShape)
            .combinedClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

/** Slim translucent capsule holding a few icon buttons (the right-hand control cluster). */
@Composable
fun GlassCluster(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(GlassFill)
            .border(1.dp, rim(1f), RoundedCornerShape(28.dp))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

private fun fadeSlideIn(fadeMs: Int, slide: Boolean, dy: Int) =
    fadeIn(tween(fadeMs)) + if (slide) slideInVertically(tween(fadeMs + 60)) { it * dy / 4 } else slideInVertically(tween(0)) { 0 }

private fun fadeSlideOut(fadeMs: Int, slide: Boolean, dy: Int) =
    fadeOut(tween(fadeMs)) + if (slide) slideOutVertically(tween(fadeMs + 60)) { it * dy / 4 } else slideOutVertically(tween(0)) { 0 }

/** Top: close, title (+ small playback info), more. The menu content is supplied by the caller. */
@Composable
fun PlayerTopBar(
    visible: Boolean,
    fadeMs: Int,
    slide: Boolean,
    title: String,
    info: String?,
    onClose: () -> Unit,
    onMore: () -> Unit,
    menu: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeSlideIn(fadeMs, slide, -1),
        exit = fadeSlideOut(fadeMs, slide, -1),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent))),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .displayCutoutPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassCircleButton(Icons.Rounded.Close, "Close", onClose, reduceMotion = fadeMs == 0)
                Column(
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    if (!info.isNullOrBlank()) {
                        Text(
                            text = info,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Box {
                    GlassCircleButton(Icons.Rounded.MoreVert, "More options", onMore, reduceMotion = fadeMs == 0)
                    menu()
                }
            }
        }
    }
}

/** Centre: previous, rewind, play/pause (the highlight), forward, next. */
@Composable
fun PlayerTransport(
    visible: Boolean,
    fadeMs: Int,
    isPlaying: Boolean,
    ended: Boolean,
    rewindIcon: ImageVector,
    forwardIcon: ImageVector,
    seekSeconds: Int,
    hasQueue: Boolean,
    canPrev: Boolean,
    canNext: Boolean,
    onPrev: () -> Unit,
    onRewind: () -> Unit,
    onPlayPause: () -> Unit,
    onForward: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduce = fadeMs == 0
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(fadeMs)),
        exit = fadeOut(tween(fadeMs)),
        modifier = modifier,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hasQueue) {
                GlassCircleButton(Icons.Rounded.SkipPrevious, "Previous video", onPrev, size = 48.dp, iconSize = 22.dp, enabled = canPrev, reduceMotion = reduce)
            }
            GlassCircleButton(rewindIcon, "Back $seekSeconds seconds", onRewind, size = 56.dp, iconSize = 28.dp, reduceMotion = reduce)
            val label = when {
                ended -> "Replay"
                isPlaying -> "Pause"
                else -> "Play"
            }
            val playIcon = when {
                ended -> Icons.Rounded.Replay
                isPlaying -> Icons.Rounded.Pause
                else -> Icons.Rounded.PlayArrow
            }
            Crossfade(targetState = playIcon, animationSpec = tween(fadeMs), label = "playIcon") { shown ->
                GlassCircleButton(shown, label, onPlayPause, size = 84.dp, iconSize = 42.dp, luminous = true, reduceMotion = reduce)
            }
            GlassCircleButton(forwardIcon, "Forward $seekSeconds seconds", onForward, size = 56.dp, iconSize = 28.dp, reduceMotion = reduce)
            if (hasQueue) {
                GlassCircleButton(Icons.Rounded.SkipNext, "Next video", onNext, size = 48.dp, iconSize = 22.dp, enabled = canNext, reduceMotion = reduce)
            }
        }
    }
}

/** One thin slider, white on translucent white. [overlay] draws inside the track (A-B range). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    dimTrack: Boolean = false,
    overlay: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {},
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        enabled = enabled,
        thumb = { Box(Modifier.size(10.dp).clip(CircleShape).background(Color.White)) },
        track = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.28f)),
            ) {
                overlay()
                Box(
                    Modifier
                        .fillMaxWidth(value.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = if (dimTrack) 0.6f else 1f)),
                )
            }
        },
        modifier = modifier,
    )
}

/**
 * Frame shown above the thumb while scrubbing. Frames come from MediaMetadataRetriever (nearest
 * keyframe, so it stays quick), are fetched one at a time after a short pause in dragging, and the
 * previous frame stays on screen until the next one is ready, so the preview never flashes black.
 */
@Composable
fun rememberSeekPreview(uri: String, active: Boolean, positionMs: Long, aspect: Float): ImageBitmap? {
    val context = LocalContext.current
    val latest = rememberUpdatedState(positionMs)
    val frame by produceState<ImageBitmap?>(null, uri, active) {
        if (!active) {
            value = null
            return@produceState
        }
        val retriever = withContext(Dispatchers.IO) {
            MediaMetadataRetriever().also { r ->
                runCatching { r.setDataSource(context, Uri.parse(uri)) }.onFailure { runCatching { r.release() } }
            }
        }
        val width = 320
        val height = (width / aspect.coerceIn(0.4f, 2.5f)).roundToInt().coerceAtLeast(2)
        try {
            snapshotFlow { latest.value }.collectLatest { ms ->
                delay(50L)
                val bitmap = withContext(Dispatchers.IO) {
                    runCatching {
                        retriever.getScaledFrameAtTime(ms * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, width, height)
                    }.getOrNull()
                }
                if (bitmap != null) value = bitmap.asImageBitmap()
            }
        } finally {
            withContext(NonCancellable + Dispatchers.IO) { runCatching { retriever.release() } }
        }
    }
    return frame
}

/**
 * Bottom: caption, the floating control cluster, a thin seek bar with a scrub preview, times,
 * fullscreen, and a row of chips. A soft gradient lifts it off the video instead of a toolbar.
 */
@Composable
fun PlayerBottomPanel(
    visible: Boolean,
    fadeMs: Int,
    slide: Boolean,
    bottomInset: Dp,
    caption: String?,
    positionMs: Long,
    durationMs: Long,
    abRange: AbRange,
    previewUri: String,
    videoAspect: Float,
    isLandscape: Boolean,
    onSeek: (Long) -> Unit,
    onDragging: (Boolean) -> Unit,
    onToggleFullscreen: () -> Unit,
    cluster: @Composable RowScope.() -> Unit,
    chips: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeSlideIn(fadeMs, slide, 1),
        exit = fadeSlideOut(fadeMs, slide, 1),
        modifier = modifier,
    ) {
        var dragFraction by remember { mutableFloatStateOf(-1f) }
        var sliderWidth by remember { mutableIntStateOf(0) }
        val dragging = dragFraction >= 0f
        val fraction = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
        val shown = if (dragging) dragFraction else fraction
        val shownMs = if (dragging) (dragFraction * durationMs).toLong() else positionMs
        val timeStyle = MaterialTheme.typography.labelMedium

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f)))),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .displayCutoutPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 36.dp, bottom = bottomInset),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = caption.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 12.dp),
                    )
                    GlassCluster(content = cluster)
                }

                Box(modifier = Modifier.fillMaxWidth().onSizeChanged { sliderWidth = it.width }) {
                    ThinSlider(
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
                        dimTrack = abRange.startMs != null,
                        overlay = {
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
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { stateDescription = "${formatDuration(shownMs)} of ${formatDuration(durationMs)}" },
                    )
                    if (dragging && durationMs > 0) {
                        ScrubPreview(
                            uri = previewUri,
                            positionMs = shownMs,
                            aspect = videoAspect,
                            thumbX = (shown * sliderWidth).roundToInt(),
                            trackWidth = sliderWidth,
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDuration(shownMs), style = timeStyle, color = Color.White)
                    Spacer(Modifier.weight(1f))
                    Text(formatDuration(durationMs), style = timeStyle, color = Color.White.copy(alpha = 0.75f))
                    GlassFullscreenToggle(isLandscape, onToggleFullscreen)
                }

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = chips,
                )
            }
        }
    }
}

@Composable
private fun GlassFullscreenToggle(isLandscape: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .combinedClickableNoIndication(onClick)
            .semantics { contentDescription = if (isLandscape) "Exit full screen" else "Full screen" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isLandscape) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.combinedClickableNoIndication(onClick: () -> Unit): Modifier =
    this.combinedClickable(role = Role.Button, onClick = onClick)

@Composable
private fun ScrubPreview(uri: String, positionMs: Long, aspect: Float, thumbX: Int, trackWidth: Int) {
    val frame = rememberSeekPreview(uri, active = true, positionMs = positionMs, aspect = aspect)
    val landscape = aspect >= 1f
    val w = if (landscape) 148.dp else 84.dp
    val h = if (landscape) (148f / aspect.coerceAtLeast(0.4f)).dp else (84f / aspect.coerceAtLeast(0.4f)).coerceAtMost(160f).dp
    val density = androidx.compose.ui.platform.LocalDensity.current
    val wPx = with(density) { w.roundToPx() }
    val hPx = with(density) { h.roundToPx() }
    val gap = with(density) { 14.dp.roundToPx() }
    Box(
        modifier = Modifier
            .offset { IntOffset((thumbX - wPx / 2).coerceIn(0, (trackWidth - wPx).coerceAtLeast(0)), -(hPx + gap)) }
            .size(w, h)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black)
            .border(1.dp, rim(1.4f), RoundedCornerShape(14.dp)),
    ) {
        frame?.let {
            Image(bitmap = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().fillMaxHeight())
        }
        Text(
            text = formatDuration(positionMs),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

/** Compact volume slider that follows the system media volume. */
@Composable
fun VolumeFlyout(onInteract: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val max = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var level by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat()) }
    PlayerPill(modifier = modifier, announce = "Volume ${(level * 100).roundToInt()} percent") {
        Icon(
            imageVector = if (level <= 0f) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
        ThinSlider(
            value = level,
            onValueChange = {
                level = it
                audio.setStreamVolume(AudioManager.STREAM_MUSIC, (it * max).roundToInt(), 0)
                onInteract()
            },
            onValueChangeFinished = onInteract,
            modifier = Modifier.width(150.dp),
        )
    }
}
