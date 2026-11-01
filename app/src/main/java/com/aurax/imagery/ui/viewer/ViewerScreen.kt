package com.aurax.imagery.ui.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import android.content.res.Configuration
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.delay
import androidx.compose.runtime.CompositionLocalProvider
import com.aurax.imagery.ui.glass.LocalHazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aurax.imagery.AuraXApp
import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.domain.model.MediaType
import com.aurax.imagery.domain.player.OrientationLock
import com.aurax.imagery.ui.components.GlassSurface
import com.aurax.imagery.ui.components.GlassTopBar
import com.aurax.imagery.ui.components.LoadingState
import com.aurax.imagery.ui.gallery.MediaSource
import com.aurax.imagery.ui.gallery.rememberTrashAction
import com.aurax.imagery.ui.gallery.shareItems
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * Full-screen, immersive viewer.
 *
 * - swipe left/right between photos and videos (same order as the grid you came from)
 * - pinch and double-tap to zoom photos; one tap shows or hides the controls and system bars
 * - drag down (while not zoomed) to close
 * - actions: favorite, share, edit (photos), info/EXIF, delete (to the system trash)
 */
@Composable
fun ViewerScreen(
    source: MediaSource,
    startMediaId: Long,
    onClose: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val container = (LocalContext.current.applicationContext as AuraXApp).container
    val viewModel: ViewerViewModel = viewModel(
        key = "viewer-$source",
        factory = ViewerViewModel.factory(container, source),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        !state.loaded -> Box(Modifier.fillMaxSize().background(Color.Black)) { LoadingState() }

        // Everything was deleted (or the source emptied): nothing left to show.
        state.items.isEmpty() -> LaunchedEffect(Unit) { onClose() }

        else -> ViewerContent(
            items = state.items,
            initialPage = state.items.indexOfFirst { it.id == startMediaId }.coerceAtLeast(0),
            viewModel = viewModel,
            onClose = onClose,
            onEdit = onEdit,
        )
    }
}

@Composable
private fun ViewerContent(
    items: List<MediaItem>,
    initialPage: Int,
    viewModel: ViewerViewModel,
    onClose: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as AuraXApp).container }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = initialPage) { items.size }

    var chromeVisible by remember { mutableStateOf(true) }
    var zoomed by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var orientationLock by remember { mutableStateOf(OrientationLock.Auto) }
    val landscape = orientationLock == OrientationLock.Landscape
    var videoPlaying by remember { mutableStateOf(false) }
    var videoBusy by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }

    val dragY = remember { Animatable(0f) }
    val dismissDistance = with(LocalDensity.current) { 140.dp.toPx() }
    val dragProgress = (abs(dragY.value) / (dismissDistance * 3f)).coerceIn(0f, 1f)

    ImmersiveEffect(hideSystemBars = !chromeVisible)
    OrientationLockEffect(orientationLock)
    BrightnessRestoreEffect()

    val trash = rememberTrashAction { trashed ->
        // Hide at once; the index catches up after the background refresh.
        viewModel.markTrashed(trashed.map { it.id })
        container.indexer.enqueue()
    }

    val current = items.getOrNull(pagerState.currentPage)
    val isVideo = current?.type == MediaType.Video
    val orientationLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    // Photos always get the action bar. Videos only while paused in portrait, so playback stays clean.
    val actionBarAllowed = current != null && (!isVideo || (!videoPlaying && !landscape && !orientationLandscape))

    LaunchedEffect(current?.id) { videoPlaying = false; videoBusy = false }
    // While a video plays, the controls fade out after a few idle seconds; any interaction restarts it.
    LaunchedEffect(chromeVisible, videoPlaying, videoBusy, interactionTick, isVideo) {
        if (chromeVisible && isVideo && videoPlaying && !videoBusy) {
            delay(3500L)
            chromeVisible = false
        }
    }
    // The photo/video pager is the backdrop; only the chrome drawn above it blurs it.
    val backdrop = rememberHazeState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 1f - dragProgress))
            // Swipe down to close. Disabled while zoomed so panning a zoomed photo never closes it.
            .pointerInput(zoomed) {
                if (!zoomed) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (abs(dragY.value) > dismissDistance) onClose() else dragY.animateTo(0f, spring())
                            }
                        },
                        onDragCancel = { scope.launch { dragY.animateTo(0f, spring()) } },
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            scope.launch { dragY.snapTo(dragY.value + amount) }
                        },
                    )
                }
            },
    ) {
        Box(Modifier.fillMaxSize().hazeSource(backdrop)) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !zoomed,
            key = { index -> items[index].id },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = dragY.value
                    val shrink = 1f - 0.15f * dragProgress
                    scaleX = shrink
                    scaleY = shrink
                },
        ) { page ->
            val item = items[page]
            val isCurrent = pagerState.settledPage == page
            if (item.type == MediaType.Video) {
                VideoPlayerPage(
                    item = item,
                    isCurrentPage = isCurrent,
                    controlsVisible = chromeVisible,
                    isLandscape = landscape || orientationLandscape,
                    actionBarVisible = actionBarAllowed,
                    onToggleControls = { chromeVisible = !chromeVisible },
                    onToggleFullscreen = {
                        orientationLock = if (orientationLock == OrientationLock.Landscape) OrientationLock.Auto else OrientationLock.Landscape
                    },
                    orientationLock = orientationLock,
                    onCycleOrientation = { orientationLock = orientationLock.next() },
                    onClose = onClose,
                    onShowDetails = { showInfo = true },
                    onPlayingChanged = { videoPlaying = it },
                    onBusyChanged = { videoBusy = it },
                    onInteraction = { interactionTick++ },
                    onZoomChanged = { isZoomed -> if (pagerState.settledPage == page) zoomed = isZoomed },
                )
            } else {
                ZoomableImage(
                    item = item,
                    isCurrent = isCurrent,
                    onZoomChanged = { isZoomed -> if (pagerState.settledPage == page) zoomed = isZoomed },
                    onTap = { chromeVisible = !chromeVisible },
                )
            }
        }

        }

        CompositionLocalProvider(LocalHazeState provides backdrop) {
AnimatedVisibility(
            visible = chromeVisible && !isVideo,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                // Details (name, date, size, camera info...) live behind this button instead of on screen.
                if (current != null) {
                    IconButton(onClick = { showInfo = true }) {
                        Icon(Icons.Rounded.Info, contentDescription = "Details", tint = Color.White)
                    }
                }
            }
        }
}

        if (current != null) {
            CompositionLocalProvider(LocalHazeState provides backdrop) {
AnimatedVisibility(
                visible = chromeVisible && actionBarAllowed,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                GlassSurface(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .fillMaxWidth()
                        .height(72.dp),
                    shape = RoundedCornerShape(36.dp),
                    cornerRadius = 36.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { viewModel.toggleFavorite(current) }) {
                            Icon(
                                imageVector = if (current.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = if (current.isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (current.isFavorite) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            )
                        }
                        IconButton(onClick = { shareItems(context, listOf(current)) }) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share")
                        }
                        if (current.type == MediaType.Image) {
                            IconButton(onClick = { onEdit(current.id) }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                            }
                        }
                        IconButton(onClick = { trash(listOf(current)) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Move to trash")
                        }
                    }
                }
            }
}
        }
    }

    if (showInfo && current != null) {
        MediaInfoSheet(item = current, onDismiss = { showInfo = false })
    }
}
