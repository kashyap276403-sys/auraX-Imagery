package com.aurax.imagery.ui.gallery

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aurax.imagery.domain.model.AlbumNames
import com.aurax.imagery.ui.components.GlassAlertDialog

/**
 * Chooses the album a batch Move or Copy goes to: an existing folder, or a new album typed in. New
 * albums are real folders (Pictures/Name or Movies/Name), so they show up in other gallery apps too.
 */
@Composable
fun FolderPickerDialog(
    title: String,
    existing: List<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newName by remember { mutableStateOf("") }
    val clean = AlbumNames.clean(newName)
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it.take(AlbumNames.MAX_LENGTH) },
                    singleLine = true,
                    label = { Text("New album name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (existing.isNotEmpty()) {
                    Text("Or pick an album", style = androidx.compose.material3.MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                        items(existing) { name ->
                            TextButton(onClick = { onPick(name) }, modifier = Modifier.fillMaxWidth()) {
                                Text(name, maxLines = 1)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = clean.isNotEmpty(), onClick = { onPick(clean) }) { Text("Use new album") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
