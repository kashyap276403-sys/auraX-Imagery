package com.aurax.imagery.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aurax.imagery.ui.theme.LocalGlassStyle

/** The dark, glass-free chip behind every transient player overlay (levels, seek flash, resume). */
@Composable
fun PlayerPill(
    modifier: Modifier = Modifier,
    announce: String? = null,
    content: @Composable RowScope.() -> Unit,
) {
    // A little more opaque when the user's glass opacity is high, so text stays readable on bright video.
    val alpha = (0.34f + LocalGlassStyle.current.opacity * 0.4f).coerceIn(0.34f, 0.6f)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = alpha))
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .then(
                if (announce != null) {
                    Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = announce
                    }
                } else {
                    Modifier
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}
