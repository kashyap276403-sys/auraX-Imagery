package com.aurax.imagery.ui.glass

import com.aurax.imagery.domain.repository.PreferencesRepository
import com.aurax.imagery.ui.theme.GlassPreset
import com.aurax.imagery.ui.theme.GlassStyle
import com.aurax.imagery.ui.theme.GlassStyleCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The single source of truth for the glass look. Changes apply to the whole app immediately
 * (the theme collects [style]) and are written to DataStore after the user pauses, so dragging
 * a slider does not hammer the disk.
 */
@OptIn(FlowPreview::class)
class GlassSettings(private val preferences: PreferencesRepository) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _style = MutableStateFlow(GlassStyle.Natural)
    val style: StateFlow<GlassStyle> = _style.asStateFlow()

    private val _presets = MutableStateFlow<List<GlassPreset>>(emptyList())
    val presets: StateFlow<List<GlassPreset>> = _presets.asStateFlow()

    /** Set once the saved values have been read, so a slow disk never overwrites them with defaults. */
    @Volatile
    private var loaded = false

    init {
        scope.launch {
            preferences.glassStyle.first()?.let { _style.value = GlassStyleCodec.decode(it) }
            _presets.value = GlassStyleCodec.decodePresets(preferences.glassPresets.first())
            loaded = true
            _style.drop(1).debounce(400).collect { preferences.setGlassStyle(GlassStyleCodec.encode(it)) }
        }
    }

    fun update(transform: (GlassStyle) -> GlassStyle) = _style.update(transform)

    fun set(style: GlassStyle) {
        _style.value = style
    }

    fun savePreset(name: String): Boolean {
        val clean = GlassStyleCodec.cleanName(name)
        if (clean.isEmpty()) return false
        _presets.update { GlassStyleCodec.withPreset(it, GlassPreset(clean, _style.value)) }
        persistPresets()
        return true
    }

    fun deletePreset(name: String) {
        _presets.update { list -> list.filterNot { it.name == name } }
        persistPresets()
    }

    private fun persistPresets() {
        val snapshot = _presets.value
        scope.launch {
            // Never write before the saved presets were read, or an early tap would erase them.
            while (!loaded) kotlinx.coroutines.delay(50)
            preferences.setGlassPresets(GlassStyleCodec.encodePresets(snapshot))
        }
    }
}
