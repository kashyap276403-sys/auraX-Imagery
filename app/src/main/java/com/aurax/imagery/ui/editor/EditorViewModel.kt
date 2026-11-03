package com.aurax.imagery.ui.editor

import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aurax.imagery.AppContainer
import com.aurax.imagery.data.media.BitmapEdit
import com.aurax.imagery.data.media.EditedImageSaver
import com.aurax.imagery.domain.edit.CropAspect
import com.aurax.imagery.domain.edit.CropRect
import com.aurax.imagery.domain.edit.EditState
import com.aurax.imagery.domain.edit.ExportFormat
import com.aurax.imagery.domain.edit.PhotoFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class EditTool(val label: String) { Crop("Crop"), Rotate("Rotate"), Adjust("Adjust"), Filters("Filters") }

data class EditorUiState(
    val loading: Boolean = true,
    val error: String? = null,
    /** The picture with rotation/flip applied, at preview resolution. Colour is applied by the UI. */
    val preview: ImageBitmap? = null,
    val edit: EditState = EditState(),
    val tool: EditTool = EditTool.Crop,
    val saving: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    /** While true the UI shows the untouched original (hold the compare button). */
    val comparing: Boolean = false,
    val original: ImageBitmap? = null,
    val format: ExportFormat = ExportFormat.Auto,
)

sealed interface EditorEvent {
    data object Saved : EditorEvent
    data class Failed(val message: String) : EditorEvent
}

/**
 * Holds the edit state and the preview bitmap. Colour changes only touch [EditState] (the UI
 * draws them through a colour filter), so sliders stay smooth; only rotate/flip rebuild a bitmap.
 */
class EditorViewModel(
    private val container: AppContainer,
    mediaId: Long,
) : ViewModel() {

    private val sourceUri: Uri = ContentUris.withAppendedId(
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
        mediaId,
    )
    private val saver = EditedImageSaver(container.appContext)

    private val _state = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _state.asStateFlow()

    private val _events = Channel<EditorEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var source: android.graphics.Bitmap? = null
    private var orientJob: Job? = null

    init {
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    BitmapEdit.decode(container.appContext.contentResolver, sourceUri, PREVIEW_EDGE)
                }
                source = bitmap
                val image = bitmap.asImageBitmap()
                _state.update { it.copy(loading = false, preview = image, original = image) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = "This photo couldn't be opened for editing.") }
            }
        }
    }

    private val undoStack = ArrayDeque<EditState>()
    private val redoStack = ArrayDeque<EditState>()
    private var lastKind: String? = null
    private var lastEditAt = 0L

    /**
     * Applies an edit and records the previous state for undo. Edits of the same [kind] made within
     * a short time (a slider drag, a crop drag) merge into one undo step.
     */
    private fun updateEdit(kind: String? = null, transform: (EditState) -> EditState) {
        val now = android.os.SystemClock.elapsedRealtime()
        val before = _state.value.edit
        val after = transform(before)
        if (after == before) return
        val merge = kind != null && kind == lastKind && now - lastEditAt < MERGE_WINDOW_MS
        if (!merge) {
            undoStack.addLast(before)
            if (undoStack.size > MAX_HISTORY) undoStack.removeFirst()
        }
        redoStack.clear()
        lastKind = kind
        lastEditAt = now
        _state.update { it.copy(edit = after, canUndo = undoStack.isNotEmpty(), canRedo = false) }
    }

    fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(_state.value.edit)
        lastKind = null
        _state.update { it.copy(edit = previous, canUndo = undoStack.isNotEmpty(), canRedo = true) }
        rebuildPreview()
    }

    fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(_state.value.edit)
        lastKind = null
        _state.update { it.copy(edit = next, canUndo = true, canRedo = redoStack.isNotEmpty()) }
        rebuildPreview()
    }

    fun setComparing(on: Boolean) = _state.update { it.copy(comparing = on) }

    fun setFormat(format: ExportFormat) = _state.update { it.copy(format = format) }

    /** Rebuilds the oriented preview after a rotate or flip; later taps cancel earlier, slower rebuilds. */
    private fun rebuildPreview() {
        val base = source ?: return
        orientJob?.cancel()
        orientJob = viewModelScope.launch {
            val edit = _state.value.edit
            kotlinx.coroutines.delay(if (edit.sharpness > 0f) SHARPEN_DEBOUNCE_MS else 0L)
            val shown = withContext(Dispatchers.Default) {
                val oriented = BitmapEdit.orient(base, edit)
                BitmapEdit.sharpen(oriented, edit.sharpness)
            }
            _state.update { it.copy(preview = shown.asImageBitmap()) }
        }
    }

    fun selectTool(tool: EditTool) = _state.update { it.copy(tool = tool) }

    fun rotate(clockwise: Boolean) {
        updateEdit { it.rotated(clockwise) }
        rebuildPreview()
    }

    fun flipHorizontal() {
        updateEdit { it.mirroredHorizontally() }
        rebuildPreview()
    }

    fun flipVertical() {
        updateEdit { it.mirroredVertically() }
        rebuildPreview()
    }

    fun setCrop(crop: CropRect) = updateEdit("crop") { it.copy(crop = crop) }

    fun setAspect(aspect: CropAspect) {
        val preview = _state.value.preview ?: return
        val imageAspect = preview.width.toFloat() / preview.height
        updateEdit { edit ->
            val ratio = aspect.resolve(imageAspect)
            edit.copy(
                cropAspect = aspect,
                crop = if (ratio != null) CropRect.fitAspect(ratio, imageAspect) else edit.crop,
            )
        }
    }

    fun setBrightness(value: Float) = updateEdit("brightness") { it.copy(brightness = value) }
    fun setContrast(value: Float) = updateEdit("contrast") { it.copy(contrast = value) }
    fun setSaturation(value: Float) = updateEdit("saturation") { it.copy(saturation = value) }
    fun setExposure(value: Float) = updateEdit("exposure") { it.copy(exposure = value) }
    fun setSharpness(value: Float) {
        updateEdit("sharpness") { it.copy(sharpness = value) }
        rebuildPreview()
    }
    fun setFilter(filter: PhotoFilter) = updateEdit { it.copy(filter = filter) }

    fun resetAll() {
        updateEdit { EditState() }
        rebuildPreview()
    }

    fun save() {
        val current = _state.value
        if (current.saving || current.loading || current.error != null) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val result = saver.save(sourceUri, current.edit, current.format)
            _state.update { it.copy(saving = false) }
            result.fold(
                onSuccess = { _events.send(EditorEvent.Saved) },
                onFailure = { _events.send(EditorEvent.Failed(it.message ?: "Couldn't save the photo")) },
            )
        }
    }

    companion object {
        /** Longest side of the on-screen preview; large enough to look sharp, small enough to stay fast. */
        private const val PREVIEW_EDGE = 2048
        private const val MAX_HISTORY = 50
        private const val MERGE_WINDOW_MS = 700L
        private const val SHARPEN_DEBOUNCE_MS = 120L

        fun factory(container: AppContainer, mediaId: Long) = viewModelFactory {
            initializer { EditorViewModel(container, mediaId) }
        }
    }
}
