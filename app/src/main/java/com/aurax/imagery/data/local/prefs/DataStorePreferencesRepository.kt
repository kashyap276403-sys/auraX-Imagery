package com.aurax.imagery.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aurax.imagery.domain.model.GalleryPreferences
import com.aurax.imagery.domain.model.Grouping
import com.aurax.imagery.domain.model.SortField
import com.aurax.imagery.domain.model.SortOrder
import com.aurax.imagery.domain.player.VideoGestureSettings
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
        val VIDEO_SCREEN_MODE = stringPreferencesKey("video_screen_mode")
        val VIDEO_ASPECT_PRESET = stringPreferencesKey("video_aspect_preset")
    }
}
