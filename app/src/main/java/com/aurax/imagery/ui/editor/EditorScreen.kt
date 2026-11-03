package com.aurax.imagery.ui.editor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RotateLeft
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material.icons.rounded.Tune
import com.aurax.imagery.ui.components.GlassAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.aurax.imagery.domain.edit.ExportFormat
import com.aurax.imagery.ui.components.GlassDropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aurax.imagery.AuraXApp
import com.aurax.imagery.domain.edit.ColorMath
import com.aurax.imagery.domain.edit.CropAspect
import com.aurax.imagery.domain.edit.EditState
import com.aurax.imagery.domain.edit.PhotoFilter
import com.aurax.imagery.ui.components.EmptyState
import com.aurax.imagery.ui.components.GlassSurface
import com.aurax.imagery.ui.components.GlassTopBar
import com.aurax.imagery.ui.components.LoadingState
import kotlin.math.roundToInt

/**
 * Non-destructive photo editor: crop, rotate/flip, brightness/contrast/saturation and filters.
 * Edits are previewed live and written only on Save, always as a new copy; the original file is
 * never modified.
 */
@Composable
fun EditorScreen(mediaId: Long, onClose: () -> Unit) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as AuraXApp).container }
    val viewModel: EditorViewModel = viewModel(
        key = "editor-$mediaId",
        factory = EditorViewModel.factory(container, mediaId),
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmDiscard by remember { mutableStateOf(false) }

    val requestClose = {
        if (state.edit.isEdited && !state.saving) confirmDiscard = true else onClose()
    }
    BackHandler(onBack = requestClose)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                EditorEvent.Saved -> {
                    Toast.makeText(
                        context,
                        "Saved as a new copy in Pictures/AuraX Imagery. Your original is untouched.",
                        Toast.LENGTH_LONG,
                    ).show()
                    container.indexer.enqueue() // so the copy shows up in the gallery
                    onClose()
                }
                is EditorEvent.Failed -> Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        GlassTopBar(
            title = "Edit",
            subtitle = "Saved as a copy",
            navigationIcon = Icons.Rounded.Close,
            navigationDescription = "Close editor",
            onNavigationClick = requestClose,
            actions = {
                IconButton(onClick = viewModel::undo, enabled = state.canUndo && !state.saving) {
                    Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Undo")
                }
                IconButton(onClick = viewModel::redo, enabled = state.canRedo && !state.saving) {
                    Icon(Icons.AutoMirrored.Rounded.Redo, contentDescription = "Redo")
                }
                // Hold to see the photo as it was before the edits.
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                viewModel.setComparing(true)
                                waitForUpOrCancellation()
                                viewModel.setComparing(false)
                            }
                        }
                        .semantics { contentDescription = "Hold to compare with the original" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Compare,
                        contentDescription = null,
                        tint = if (state.comparing) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                    )
                }
                var formatMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { formatMenu = true }, enabled = !state.saving) {
                        Icon(Icons.Rounded.Image, contentDescription = "Save format: ${state.format.label}")
                    }
                    GlassDropdownMenu(expanded = formatMenu, onDismissRequest = { formatMenu = false }) {
                        ExportFormat.entries.forEach { f ->
                            DropdownMenuItem(
                                text = { Text(if (f == state.format) "${f.label}  ✓" else f.label) },
                                onClick = {
                                    viewModel.setFormat(f)
                                    formatMenu = false
                                },
                            )
                        }
                    }
                }
                IconButton(onClick = viewModel::resetAll, enabled = state.edit.isEdited && !state.saving) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset all edits")
                }
                if (state.saving) {
                    CircularProgressIndicator(modifier = Modifier.padding(12.dp).size(24.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(
                        onClick = viewModel::save,
                        enabled = state.edit.isEdited && state.preview != null,
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = "Save as a new copy")
                    }
                }
            },
        )

        val preview = state.preview
        BoxWithConstraints(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.loading -> LoadingState()
                state.error != null || preview == null -> EmptyState(
                    icon = Icons.Rounded.Close,
                    title = "Can't edit this photo",
                    message = state.error ?: "Something went wrong.",
                )
                else -> {
                    val imageAspect = preview.width.toFloat() / preview.height
                    val fitWidth: Dp = if (imageAspect > maxWidth / maxHeight) maxWidth else maxHeight * imageAspect
                    val fitHeight: Dp = if (imageAspect > maxWidth / maxHeight) maxWidth / imageAspect else maxHeight
                    val colorFilter = rememberColorFilter(state.edit)
                    val showOriginal = state.comparing && state.original != null
                    Box(modifier = Modifier.size(fitWidth, fitHeight)) {
                        Image(
                            bitmap = if (showOriginal) state.original!! else preview,
                            contentDescription = "Photo being edited",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds,
                            colorFilter = if (showOriginal) null else colorFilter,
                        )
                        // The frame is always visible; it is only draggable while the Crop tool is open.
                        if (!showOriginal) CropOverlay(
                            crop = state.edit.crop,
                            aspect = state.edit.cropAspect,
                            imageAspect = imageAspect,
                            interactive = state.tool == EditTool.Crop,
                            onCropChange = viewModel::setCrop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }

        if (preview != null) {
            ToolPanel(state = state, viewModel = viewModel)
            ToolTabs(selected = state.tool, onSelect = viewModel::selectTool)
        }
    }

    if (confirmDiscard) {
        GlassAlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard edits?") },
            text = { Text("Your changes haven't been saved. The original photo stays as it is.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onClose() }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") }
            },
        )
    }
}

@Composable
private fun rememberColorFilter(edit: EditState): ColorFilter? {
    val values = remember(edit.filter, edit.brightness, edit.contrast, edit.saturation, edit.exposure) { ColorMath.forEdit(edit) }
    return remember(values) {
        if (values.contentEquals(ColorMath.Identity)) null else ColorFilter.colorMatrix(ColorMatrix(values))
    }
}

@Composable
private fun ToolPanel(state: EditorUiState, viewModel: EditorViewModel) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(132.dp).padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
            when (state.tool) {
                EditTool.Crop -> LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items(CropAspect.entries) { aspect ->
                        FilterChip(
                            selected = state.edit.cropAspect == aspect,
                            onClick = { viewModel.setAspect(aspect) },
                            label = { Text(aspect.label) },
                        )
                    }
                }

                EditTool.Rotate -> Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ToolAction(Icons.Rounded.RotateLeft, "Rotate left") { viewModel.rotate(clockwise = false) }
                    ToolAction(Icons.Rounded.RotateRight, "Rotate right") { viewModel.rotate(clockwise = true) }
                    ToolAction(Icons.Rounded.Flip, "Flip horizontal", active = state.edit.flipHorizontal, onClick = viewModel::flipHorizontal)
                    ToolAction(
                        Icons.Rounded.Flip, "Flip vertical",
                        active = state.edit.flipVertical,
                        iconRotation = 90f,
                        onClick = viewModel::flipVertical,
                    )
                }

                EditTool.Adjust -> Column(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    AdjustSlider("Exposure", state.edit.exposure, viewModel::setExposure)
                    AdjustSlider("Brightness", state.edit.brightness, viewModel::setBrightness)
                    AdjustSlider("Contrast", state.edit.contrast, viewModel::setContrast)
                    AdjustSlider("Saturation", state.edit.saturation, viewModel::setSaturation)
                    AdjustSlider("Sharpness", state.edit.sharpness, viewModel::setSharpness, 0f..1f)
                }

                EditTool.Filters -> LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items(PhotoFilter.entries) { filter ->
                        FilterThumb(
                            filter = filter,
                            preview = state.preview,
                            selected = state.edit.filter == filter,
                            onClick = { viewModel.setFilter(filter) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolAction(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    iconRotation: Float = 0f,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(28.dp).graphicsLayer { rotationZ = iconRotation },
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A -100..100 slider; tapping the label snaps it back to 0. */
@Composable
private fun AdjustSlider(label: String, value: Float, onChange: (Float) -> Unit, range: ClosedFloatingPointRange<Float> = -1f..1f) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(84.dp).clickable { onChange(0f) },
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.weight(1f).height(32.dp),
        )
        Text(
            text = (value * 100).roundToInt().toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp),
        )
    }
}

@Composable
private fun FilterThumb(
    filter: PhotoFilter,
    preview: androidx.compose.ui.graphics.ImageBitmap?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colorFilter = remember(filter) {
        if (filter == PhotoFilter.None) null else ColorFilter.colorMatrix(ColorMatrix(filter.matrix))
    }
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    BorderStroke(if (selected) 2.dp else 0.dp, MaterialTheme.colorScheme.primary),
                    RoundedCornerShape(12.dp),
                ),
        ) {
            if (preview != null) {
                Image(
                    bitmap = preview,
                    contentDescription = filter.label,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    colorFilter = colorFilter,
                )
            }
        }
        Text(
            text = filter.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ToolTabs(selected: EditTool, onSelect: (EditTool) -> Unit) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            EditTool.entries.forEach { tool ->
                val icon = when (tool) {
                    EditTool.Crop -> Icons.Rounded.Crop
                    EditTool.Rotate -> Icons.Rounded.RotateRight
                    EditTool.Adjust -> Icons.Rounded.Tune
                    EditTool.Filters -> Icons.Rounded.AutoAwesome
                }
                ToolAction(icon, tool.label, active = tool == selected) { onSelect(tool) }
            }
        }
    }
}
