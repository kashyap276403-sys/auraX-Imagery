package com.aurax.imagery.domain.repository

import com.aurax.imagery.domain.model.GalleryPreferences
import com.aurax.imagery.domain.model.Grouping
import com.aurax.imagery.domain.model.SortOrder
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val gallery: Flow<GalleryPreferences>
    suspend fun setSort(sort: SortOrder)
    suspend fun setGrouping(grouping: Grouping)

    /** Glass Studio's current look as an opaque string (null until the user changes something). */
    val glassStyle: Flow<String?>
    suspend fun setGlassStyle(encoded: String)

    /** Saved custom presets, one per line. */
    val glassPresets: Flow<String?>
    suspend fun setGlassPresets(encoded: String)
}
