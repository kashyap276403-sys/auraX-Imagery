package com.aurax.imagery.ui.viewer

import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aurax.imagery.domain.model.MediaItem
import com.aurax.imagery.domain.model.MediaType
import com.aurax.imagery.ui.components.LiquidBottomSheet
import com.aurax.imagery.utils.formatAperture
import com.aurax.imagery.utils.formatDuration
import com.aurax.imagery.utils.formatExposure
import com.aurax.imagery.utils.formatFileSize
import com.aurax.imagery.utils.formatFocalLength
import com.aurax.imagery.utils.formatMegapixels
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Camera details read from a photo's EXIF block. Any field can be missing. */
data class ExifSummary(
    val camera: String?,
    val aperture: String?,
    val exposure: String?,
    val iso: String?,
    val focalLength: String?,
) {
    val isEmpty: Boolean
        get() = camera == null && aperture == null && exposure == null && iso == null && focalLength == null
}

/**
 * Reads EXIF through the platform parser straight from the content URI, off the main thread.
 * Location tags are deliberately not read: that would need the extra media-location
 * permission, which the app does not request.
 */
private suspend fun loadExif(context: android.content.Context, uri: String): ExifSummary? =
    withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(uri))?.use { stream ->
                val exif = ExifInterface(stream)
                val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim().orEmpty()
                val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim().orEmpty()
                val camera = when {
                    model.isEmpty() -> make.ifEmpty { null }
                    make.isEmpty() || model.startsWith(make, ignoreCase = true) -> model
                    else -> "$make $model"
                }
                val fNumber = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, -1.0)
                val exposure = exif.getAttributeDouble(ExifInterface.TAG_EXPOSURE_TIME, -1.0)
                val focal = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, -1.0)
                val iso = exif.getAttributeInt("ISOSpeedRatings", -1) // literal key: stable across platform ExifInterface versions
                ExifSummary(
                    camera = camera,
                    aperture = if (fNumber > 0) formatAperture(fNumber) else null,
                    exposure = if (exposure > 0) formatExposure(exposure) else null,
                    iso = if (iso > 0) "ISO $iso" else null,
                    focalLength = if (focal > 0) formatFocalLength(focal) else null,
                ).takeUnless { it.isEmpty }
            }
        }.getOrNull()
    }

/** Bottom sheet with file details and, for photos, camera (EXIF) details. */
@Composable
fun MediaInfoSheet(item: MediaItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val exif by produceState<ExifSummary?>(initialValue = null, item.id) {
        value = if (item.type == MediaType.Image) loadExif(context, item.uri) else null
    }
    val taken = remember(item.dateTakenMillis) {
        Instant.ofEpochMilli(item.dateTakenMillis).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
    }

    LiquidBottomSheet(onDismiss = onDismiss) {
        Text(text = "Details", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        InfoRow("Name", item.displayName)
        InfoRow("Date taken", taken)
        InfoRow("Size", formatFileSize(item.sizeBytes))
        if (item.width > 0 && item.height > 0) {
            val megapixels = formatMegapixels(item.width, item.height)
            InfoRow("Dimensions", "${item.width} × ${item.height}" + if (megapixels != null) "  ·  $megapixels" else "")
        }
        if (item.type == MediaType.Video && item.durationMillis != null) {
            InfoRow("Duration", formatDuration(item.durationMillis))
        }
        InfoRow("Type", item.mimeType)
        item.albumName?.let { InfoRow("Album", it) }

        exif?.let { details ->
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text(text = "Camera", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            details.camera?.let { InfoRow("Device", it) }
            details.aperture?.let { InfoRow("Aperture", it) }
            details.exposure?.let { InfoRow("Shutter", it) }
            details.iso?.let { InfoRow("Sensitivity", it) }
            details.focalLength?.let { InfoRow("Focal length", it) }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.padding(horizontal = 8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f),
        )
    }
}
