package com.aurax.imagery.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.aurax.imagery.ui.theme.AuraBlack
import com.aurax.imagery.ui.theme.AuraCyan
import com.aurax.imagery.ui.theme.AuraViolet

/**
 * AMOLED-black app background with two faint colour washes, so the glass panels have
 * something luminous behind them to catch. Drawn once at the root, not per screen.
 */
@Composable
fun AuraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuraBlack)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(AuraCyan.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.08f),
                        radius = size.maxDimension * 0.55f,
                    ),
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(AuraViolet.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width * 0.9f, size.height * 0.85f),
                        radius = size.maxDimension * 0.6f,
                    ),
                )
            },
        content = content,
    )
}
