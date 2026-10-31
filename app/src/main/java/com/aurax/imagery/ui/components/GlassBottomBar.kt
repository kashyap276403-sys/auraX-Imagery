package com.aurax.imagery.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aurax.imagery.ui.theme.AuraTextPrimary
import com.aurax.imagery.ui.theme.LocalGlassStyle
import kotlin.math.roundToInt

data class GlassNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

/**
 * Floating glass navigation bar. A single glass "lens" slides between tabs on a slightly
 * under-damped spring, which gives the liquid overshoot as it settles.
 */
@Composable
fun GlassBottomBar(
    items: List<GlassNavItem>,
    selectedRoute: String?,
    onSelect: (GlassNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = LocalGlassStyle.current
    val haptics = LocalHapticFeedback.current
    val selectedIndex = items.indexOfFirst { it.route == selectedRoute }.coerceAtLeast(0)

    // Animation speed from Glass Studio scales the spring stiffness.
    val indicatorPosition by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = Spring.StiffnessMediumLow * style.animationSpeed.coerceAtLeast(0.1f),
        ),
        label = "navIndicator",
    )

    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        shape = RoundedCornerShape(36.dp),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
        ) {
            val itemWidth = maxWidth / items.size
            val lensShape = RoundedCornerShape(30.dp)

            // The sliding glass lens behind the active item.
            Box(
                modifier = Modifier
                    .width(itemWidth)
                    .fillMaxHeight()
                    .offset { IntOffset((itemWidth.roundToPx() * indicatorPosition).roundToInt(), 0) }
                    .clip(lensShape)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.White.copy(alpha = 0.26f), Color.White.copy(alpha = 0.10f)),
                        ),
                    )
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)), lensShape),
            )

            Row(modifier = Modifier.fillMaxSize()) {
                items.forEachIndexed { index, item ->
                    NavBarItem(
                        item = item,
                        selected = index == selectedIndex,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (index != selectedIndex) {
                                if (style.haptics) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelect(item)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.NavBarItem(
    item: GlassNavItem,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tint by animateColorAsState(
        targetValue = if (selected) AuraTextPrimary else AuraTextPrimary.copy(alpha = 0.55f),
        label = "navItemTint",
    )
    Column(
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Icon(imageVector = item.icon, contentDescription = null, tint = tint, modifier = Modifier.height(24.dp))
        Text(text = item.label, color = tint, fontSize = 11.sp, maxLines = 1)
    }
}
