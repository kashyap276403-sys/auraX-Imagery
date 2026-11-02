package com.aurax.imagery.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Floating glass header with a title, optional subtitle, an optional leading icon button
 * (back / close) and trailing actions.
 */
@Composable
fun GlassTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: ImageVector? = null,
    navigationDescription: String? = null,
    onNavigationClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(PaddingValues(horizontal = 16.dp, vertical = 8.dp)),
        shape = RoundedCornerShape(28.dp),
        cornerRadius = 28.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = if (navigationIcon != null) 8.dp else 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (navigationIcon != null) {
                DropIconButton(onClick = onNavigationClick) {
                    Icon(imageVector = navigationIcon, contentDescription = navigationDescription)
                }
            }
            Column(modifier = Modifier.weight(1f).padding(vertical = 6.dp, horizontal = 4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleLarge, maxLines = 1)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}
