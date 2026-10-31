package com.aurax.imagery.ui.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aurax.imagery.ui.components.GlassButton
import com.aurax.imagery.ui.components.GlassCard
import com.aurax.imagery.ui.components.LocalFloatingBarInset
import com.aurax.imagery.utils.MediaAccess

/** Shown instead of the library while access is denied. Explains the request before making it. */
@Composable
fun MediaAccessPrompt(state: MediaAccessState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .padding(bottom = LocalFloatingBarInset.current),
        verticalArrangement = Arrangement.Center,
    ) {
        GlassCard {
            Text(
                text = "Allow access to your photos",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "auraX needs permission to show the photos and videos on this device. " +
                    "Everything stays on your phone. You can allow your whole library or choose " +
                    "only specific photos in the system dialog.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (state.askedThisSession) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Access is still off. If the system dialog no longer appears, enable it in app settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                GlassButton(text = "Allow access", onClick = state.request)
                if (state.askedThisSession) {
                    GlassButton(text = "Open settings", onClick = state.openSettings)
                }
            }
        }
    }
}

/** Slim notice for partial access, with a button that re-opens the system picker dialog. */
@Composable
fun PartialAccessBanner(state: MediaAccessState, modifier: Modifier = Modifier) {
    if (state.access != MediaAccess.Partial) return
    GlassCard(modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "Showing only the photos you selected.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            GlassButton(text = "Manage", onClick = state.request)
        }
    }
}
