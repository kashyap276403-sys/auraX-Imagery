package com.aurax.imagery.domain.repository

import com.aurax.imagery.domain.model.GalleryPreferences
import com.aurax.imagery.domain.model.Grouping
import com.aurax.imagery.domain.model.SortOrder
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val gallery: Flow<GalleryPreferences>
    suspend fun setSort(sort: SortOrder)
    suspend fun setGrouping(grouping: Grouping)
}
