package com.aurax.imagery.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aurax.imagery.ui.theme.LocalGlassStyle

/** A padded [GlassSurface] for grouped content. Clickable when [onClick] is provided. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val radius = LocalGlassStyle.current.cornerRadius
    val shape = RoundedCornerShape(radius)
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = shape,
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}
