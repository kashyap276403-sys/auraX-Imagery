package com.aurax.imagery.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aurax.imagery.ui.theme.LocalGlassStyle

/**
 * Modal sheet with the app's dark glass look: a near-black translucent body, large rounded
 * top corners and a scroll-safe content column. The full liquid treatment (backdrop blur)
 * lands with the glass renderer in Phase 6; callers will not need to change.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiquidBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val style = LocalGlassStyle.current
    val corner = style.cornerRadius.coerceIn(24.dp, 36.dp)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier.border(
            width = style.borderWidth,
            color = Color.White.copy(alpha = 0.10f),
            shape = RoundedCornerShape(topStart = corner, topEnd = corner),
        ),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = corner, topEnd = corner),
        // Sheets are separate windows, so there is nothing to blur behind them; they stay a dense
        // near-black glass for readability, with the app's border width and corner radius.
        containerColor = Color(0xFF0B0B10).copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            content = content,
        )
    }
}
