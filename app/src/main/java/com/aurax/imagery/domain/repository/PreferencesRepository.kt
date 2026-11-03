package com.aurax.imagery.domain.repository

import com.aurax.imagery.domain.model.GalleryPreferences
import com.aurax.imagery.domain.model.Grouping
import com.aurax.imagery.domain.model.SortOrder
import com.aurax.imagery.domain.player.CinematicMode
import com.aurax.imagery.domain.player.VideoGestureSettings
import com.aurax.imagery.domain.player.VideoPlayerSettings
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val gallery: Flow<GalleryPreferences>
    suspend fun setSort(sort: SortOrder)
    suspend fun setGrouping(grouping: Grouping)

    /** Media ids opened in the viewer, most recent first (kept short). */
    val recentViewedIds: Flow<List<Long>>
    suspend fun recordViewed(mediaId: Long)
    suspend fun clearRecentViewed()

    /** Glass Studio's current look as an opaque string (null until the user changes something). */
    val glassStyle: Flow<String?>
    suspend fun setGlassStyle(encoded: String)

    /** Saved custom presets, one per line. */
    val glassPresets: Flow<String?>
    suspend fun setGlassPresets(encoded: String)

    /** Last screen mode / aspect preset chosen in the video player (enum names; null = never set). */
    val videoScreenMode: Flow<String?>
    val videoAspectPreset: Flow<String?>
    suspend fun setVideoDisplay(screenMode: String, aspectPreset: String)

    val videoGestures: Flow<VideoGestureSettings>
    suspend fun setVideoGestures(settings: VideoGestureSettings)

    /** Continue from the last position when a video is opened again. */
    val videoResume: Flow<Boolean>
    suspend fun setVideoResume(enabled: Boolean)

    val videoPlayer: Flow<VideoPlayerSettings>
    suspend fun setVideoPlayer(settings: VideoPlayerSettings)

    /** Last cinematic look chosen in the player. */
    val videoCinematic: Flow<CinematicMode>
    suspend fun setVideoCinematic(mode: CinematicMode)

    /** Videos the user has put into Movie mode (advanced player features). */
    val movieModeIds: Flow<Set<Long>>
    suspend fun setMovieMode(mediaId: Long, on: Boolean)

    /** Puts every video player preference (including gestures, resume, last screen mode) back to defaults. */
    suspend fun resetVideoPlayer()
}
