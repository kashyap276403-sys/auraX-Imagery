package com.aurax.imagery.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aurax.imagery.domain.model.GalleryPreferences
import com.aurax.imagery.domain.model.Grouping
import com.aurax.imagery.domain.model.SortField
import com.aurax.imagery.domain.model.SortOrder
import com.aurax.imagery.domain.player.CinematicMode
import com.aurax.imagery.domain.player.OrientationLock
import com.aurax.imagery.domain.player.ScreenMode
import com.aurax.imagery.domain.player.VideoGestureSettings
import com.aurax.imagery.domain.player.VideoPlayerSettings
import com.aurax.imagery.domain.repository.PreferencesRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.galleryDataStore: DataStore<Preferences> by preferencesDataStore(name = "gallery_prefs")

/** Sort and grouping choices, persisted so the gallery opens the way the user left it. */
class DataStorePreferencesRepository(context: Context) : PreferencesRepository {
    private val store = context.applicationContext.galleryDataStore

    override val gallery: Flow<GalleryPreferences> = store.data
        .catch { e ->
            // An unreadable file falls back to defaults instead of crashing the gallery.
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            val defaults = GalleryPreferences()
            GalleryPreferences(
                sort = SortOrder(
                    field = prefs[SORT_FIELD].toEnumOrNull<SortField>() ?: defaults.sort.field,
                    ascending = prefs[SORT_ASCENDING] ?: defaults.sort.ascending,
                ),
                grouping = prefs[GROUPING].toEnumOrNull<Grouping>() ?: defaults.grouping,
            )
        }

    override suspend fun setSort(sort: SortOrder) {
        store.edit {
            it[SORT_FIELD] = sort.field.name
            it[SORT_ASCENDING] = sort.ascending
        }
    }

    override suspend fun setGrouping(grouping: Grouping) {
        store.edit { it[GROUPING] = grouping.name }
    }

    private val safeData: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    override val glassStyle: Flow<String?> = safeData.map { it[GLASS_STYLE] }

    override suspend fun setGlassStyle(encoded: String) {
        store.edit { it[GLASS_STYLE] = encoded }
    }

    override val glassPresets: Flow<String?> = safeData.map { it[GLASS_PRESETS] }

    override suspend fun setGlassPresets(encoded: String) {
        store.edit { it[GLASS_PRESETS] = encoded }
    }

    override val videoScreenMode: Flow<String?> = safeData.map { it[VIDEO_SCREEN_MODE] }
    override val videoAspectPreset: Flow<String?> = safeData.map { it[VIDEO_ASPECT_PRESET] }

    override suspend fun setVideoDisplay(screenMode: String, aspectPreset: String) {
        store.edit {
            it[VIDEO_SCREEN_MODE] = screenMode
            it[VIDEO_ASPECT_PRESET] = aspectPreset
        }
    }

    override val videoGestures: Flow<VideoGestureSettings> = safeData.map { prefs ->
        VideoGestureSettings(
            doubleTapSeek = prefs[GESTURE_DOUBLE_TAP] ?: true,
            brightness = prefs[GESTURE_BRIGHTNESS] ?: true,
            volume = prefs[GESTURE_VOLUME] ?: true,
        )
    }

    override suspend fun setVideoGestures(settings: VideoGestureSettings) {
        store.edit {
            it[GESTURE_DOUBLE_TAP] = settings.doubleTapSeek
            it[GESTURE_BRIGHTNESS] = settings.brightness
            it[GESTURE_VOLUME] = settings.volume
        }
    }

    override val videoResume: Flow<Boolean> = safeData.map { it[VIDEO_RESUME] ?: true }

    override suspend fun setVideoResume(enabled: Boolean) {
        store.edit { it[VIDEO_RESUME] = enabled }
    }

    override val videoPlayer: Flow<VideoPlayerSettings> = safeData.map { prefs ->
        val d = VideoPlayerSettings()
        VideoPlayerSettings(
            defaultSpeed = prefs[VP_SPEED] ?: d.defaultSpeed,
            defaultScreenMode = prefs[VP_SCREEN].toEnumOrNull<ScreenMode>() ?: d.defaultScreenMode,
            rememberScreenMode = prefs[VP_REMEMBER_SCREEN] ?: d.rememberScreenMode,
            seekIntervalSec = prefs[VP_SEEK] ?: d.seekIntervalSec,
            autoHideSec = prefs[VP_AUTO_HIDE] ?: d.autoHideSec,
            autoPlayNext = prefs[VP_AUTOPLAY] ?: d.autoPlayNext,
            subtitlesOnByDefault = prefs[VP_SUBTITLES] ?: d.subtitlesOnByDefault,
            orientation = prefs[VP_ORIENTATION].toEnumOrNull<OrientationLock>() ?: d.orientation,
            haptics = prefs[VP_HAPTICS] ?: d.haptics,
        ).sanitized()
    }

    override suspend fun setVideoPlayer(settings: VideoPlayerSettings) {
        val s = settings.sanitized()
        store.edit {
            it[VP_SPEED] = s.defaultSpeed
            it[VP_SCREEN] = s.defaultScreenMode.name
            it[VP_REMEMBER_SCREEN] = s.rememberScreenMode
            it[VP_SEEK] = s.seekIntervalSec
            it[VP_AUTO_HIDE] = s.autoHideSec
            it[VP_AUTOPLAY] = s.autoPlayNext
            it[VP_SUBTITLES] = s.subtitlesOnByDefault
            it[VP_ORIENTATION] = s.orientation.name
            it[VP_HAPTICS] = s.haptics
        }
    }

    override val videoCinematic: Flow<CinematicMode> =
        safeData.map { it[VIDEO_CINEMATIC].toEnumOrNull<CinematicMode>() ?: CinematicMode.Original }

    override suspend fun setVideoCinematic(mode: CinematicMode) {
        store.edit { it[VIDEO_CINEMATIC] = mode.name }
    }

    override val movieModeIds: Flow<Set<Long>> =
        safeData.map { prefs -> prefs[MOVIE_MODE_IDS].orEmpty().mapNotNull { it.toLongOrNull() }.toSet() }

    override suspend fun setMovieMode(mediaId: Long, on: Boolean) {
        store.edit {
            val current = it[MOVIE_MODE_IDS].orEmpty()
            val key = mediaId.toString()
            it[MOVIE_MODE_IDS] = if (on) current + key else current - key
        }
    }

    override suspend fun resetVideoPlayer() {
        store.edit {
            listOf(
                VP_SPEED, VP_SCREEN, VP_REMEMBER_SCREEN, VP_SEEK, VP_AUTO_HIDE, VP_AUTOPLAY, VP_SUBTITLES,
                VP_ORIENTATION, VP_HAPTICS, VIDEO_CINEMATIC, VIDEO_SCREEN_MODE, VIDEO_ASPECT_PRESET, VIDEO_RESUME,
                GESTURE_DOUBLE_TAP, GESTURE_BRIGHTNESS, GESTURE_VOLUME,
            ).forEach { key -> it.remove(key) }
        }
    }

    private inline fun <reified E : Enum<E>> String?.toEnumOrNull(): E? =
        this?.let { name -> enumValues<E>().firstOrNull { it.name == name } }

    private companion object {
        val SORT_FIELD = stringPreferencesKey("sort_field")
        val SORT_ASCENDING = booleanPreferencesKey("sort_ascending")
        val GROUPING = stringPreferencesKey("grouping")
        val GLASS_STYLE = stringPreferencesKey("glass_style")
        val GLASS_PRESETS = stringPreferencesKey("glass_presets")
        val GESTURE_DOUBLE_TAP = booleanPreferencesKey("gesture_double_tap_seek")
        val GESTURE_BRIGHTNESS = booleanPreferencesKey("gesture_brightness")
        val GESTURE_VOLUME = booleanPreferencesKey("gesture_volume")
        val VP_SPEED = floatPreferencesKey("vp_default_speed")
        val VP_SCREEN = stringPreferencesKey("vp_default_screen")
        val VP_REMEMBER_SCREEN = booleanPreferencesKey("vp_remember_screen")
        val VP_SEEK = intPreferencesKey("vp_seek_interval")
        val VP_AUTO_HIDE = intPreferencesKey("vp_auto_hide")
        val VP_AUTOPLAY = booleanPreferencesKey("vp_autoplay_next")
        val VP_SUBTITLES = booleanPreferencesKey("vp_subtitles_default")
        val VP_ORIENTATION = stringPreferencesKey("vp_orientation")
        val VP_HAPTICS = booleanPreferencesKey("vp_haptics")
        val VIDEO_CINEMATIC = stringPreferencesKey("video_cinematic")
        val MOVIE_MODE_IDS = stringSetPreferencesKey("video_movie_mode_ids")
        val VIDEO_RESUME = booleanPreferencesKey("video_resume")
        val VIDEO_SCREEN_MODE = stringPreferencesKey("video_screen_mode")
        val VIDEO_ASPECT_PRESET = stringPreferencesKey("video_aspect_preset")
    }
}
