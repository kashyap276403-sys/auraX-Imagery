package com.aurax.imagery.ui.viewer

import android.media.ExifInterface
import android.provider.MediaStore
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import com.aurax.imagery.domain.model.ColorPalette
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
    /** Latitude and longitude, only when the photo has them and the location permission is granted. */
    val latLong: Pair<Double, Double>? = null,
) {
    val isEmpty: Boolean
        get() = camera == null && aperture == null && exposure == null && iso == null && focalLength == null
}

/**
 * Reads EXIF through the platform parser straight from the content URI, off the main thread.
 * Location tags are deliberately not read: that would need the extra media-location
 * permission, which the app does not request.
 */
private suspend fun loadExif(context: android.content.Context, uri: String, withLocation: Boolean): ExifSummary? =
    withContext(Dispatchers.IO) {
        runCatching {
            // With the media-location permission the original (unredacted) file is opened; otherwise GPS tags are blanked by the system.
            val source = Uri.parse(uri).let { if (withLocation) MediaStore.setRequireOriginal(it) else it }
            context.contentResolver.openInputStream(source)?.use { stream ->
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
                val ll = FloatArray(2)
                val hasLocation = withLocation && exif.getLatLong(ll)
                ExifSummary(
                    latLong = if (hasLocation) ll[0].toDouble() to ll[1].toDouble() else null,
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
    var locationGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_MEDIA_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED,
        )
    }
    val askLocation = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { locationGranted = it }
    val exif by produceState<ExifSummary?>(initialValue = null, item.id, locationGranted) {
        value = if (item.type == MediaType.Image) loadExif(context, item.uri, locationGranted) else null
    }
    val palette by produceState<List<Int>>(initialValue = emptyList(), item.id) {
        value = if (item.type == MediaType.Image) loadPalette(context, item.uri) else emptyList()
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

        if (palette.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(text = "Colours", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                palette.forEach { argb ->
                    androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        androidx.compose.foundation.layout.Box(
                            Modifier
                                .size(40.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(androidx.compose.ui.graphics.Color(argb)),
                        )
                        Text(ColorPalette.hex(argb), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (item.type == MediaType.Image) {
            val ll = exif?.latLong
            if (ll != null) {
                InfoRow("Location", "%.5f, %.5f".format(ll.first, ll.second))
                androidx.compose.material3.TextButton(onClick = {
                    val geo = Uri.parse("geo:${ll.first},${ll.second}?q=${ll.first},${ll.second}(${Uri.encode(item.displayName)})")
                    runCatching { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, geo)) }
                        .onFailure { android.widget.Toast.makeText(context, "No map app found", android.widget.Toast.LENGTH_SHORT).show() }
                }) { Text("Open in a map app") }
            } else if (!locationGranted) {
                androidx.compose.material3.TextButton(onClick = { askLocation.launch(android.Manifest.permission.ACCESS_MEDIA_LOCATION) }) {
                    Text("Show where this was taken")
                }
            }
        }

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

/** A 64 px thumbnail of the photo decoded off the main thread, reduced to its dominant colours. */
private suspend fun loadPalette(context: android.content.Context, uri: String): List<Int> =
    withContext(Dispatchers.IO) {
        runCatching {
            val source = android.graphics.ImageDecoder.createSource(context.contentResolver, Uri.parse(uri))
            val bitmap = android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                val longest = maxOf(info.size.width, info.size.height)
                if (longest > 64) {
                    val k = 64f / longest
                    decoder.setTargetSize((info.size.width * k).toInt().coerceAtLeast(1), (info.size.height * k).toInt().coerceAtLeast(1))
                }
            }
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            bitmap.recycle()
            ColorPalette.dominant(pixels)
        }.getOrDefault(emptyList())
    }
