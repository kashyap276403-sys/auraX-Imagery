package com.aurax.imagery.ui.glass.studio

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PhotoAlbum
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurax.imagery.AuraXApp
import com.aurax.imagery.ui.components.GlassButton
import com.aurax.imagery.ui.components.GlassCard
import com.aurax.imagery.ui.components.GlassNavItem
import com.aurax.imagery.ui.components.GlassSlider
import com.aurax.imagery.ui.components.GlassTopBar
import com.aurax.imagery.ui.glass.LiquidGlassPanel
import com.aurax.imagery.ui.glass.LiquidNavCapsule
import com.aurax.imagery.ui.glass.LocalGlassTilt
import com.aurax.imagery.ui.glass.LocalHazeState
import com.aurax.imagery.ui.glass.MAX_GLASS_OPACITY
import com.aurax.imagery.ui.glass.rememberGlassTilt
import com.aurax.imagery.ui.glass.rememberReducedMotion
import com.aurax.imagery.ui.theme.GlassMode
import com.aurax.imagery.ui.theme.GlassStyle
import com.aurax.imagery.ui.theme.LocalGlassStyle
import com.aurax.imagery.ui.theme.baseStyle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

private val TintSwatches = listOf(
    "White" to Color.White,
    "Ice" to Color(0xFFBFE6FF),
    "Cyan" to Color(0xFF3DE0FF),
    "Violet" to Color(0xFF9B7BFF),
    "Rose" to Color(0xFFFF7AA8),
    "Amber" to Color(0xFFFFC04D),
    "Mint" to Color(0xFF6FF2C0),
    "Smoke" to Color(0xFF8A8F98),
)

private val PreviewNav = listOf(
    GlassNavItem("photos", "Photos", Icons.Rounded.PhotoLibrary),
    GlassNavItem("albums", "Albums", Icons.Rounded.PhotoAlbum),
    GlassNavItem("search", "Search", Icons.Rounded.Search),
    GlassNavItem("favorites", "Favorites", Icons.Rounded.Favorite),
    GlassNavItem("settings", "Settings", Icons.Rounded.Settings),
)

/**
 * Glass Studio. Every control edits the app-wide [com.aurax.imagery.ui.theme.GlassStyle] held by
 * GlassSettings, and the preview at the top is drawn by the same engine (Haze blur + AGSL
 * shader + the nav capsule) that the rest of the app uses, so what moves here is real.
 */
@Composable
fun GlassStudioScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { (context.applicationContext as AuraXApp).container.glassSettings }
    val style by settings.style.collectAsStateWithLifecycle()
    val presets by settings.presets.collectAsStateWithLifecycle()
    val hazeState = rememberHazeState()
    val reducedMotion = rememberReducedMotion()
    val tilt = rememberGlassTilt(enabled = style.motionEffects && !reducedMotion)
    var showSaveDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
        GlassTopBar(
            title = "Glass Studio",
            subtitle = style.mode.label,
            navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
            navigationDescription = "Back",
            onNavigationClick = onBack,
        )

        CompositionLocalProvider(LocalHazeState provides hazeState, LocalGlassTilt provides tilt) {
            StudioPreview(
                hazeState = hazeState,
                animate = style.motionEffects && !reducedMotion,
                modifier = Modifier.fillMaxWidth().height(300.dp).padding(horizontal = 16.dp),
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GlassCard {
                SectionTitle("Mode")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    GlassMode.entries.forEach { mode ->
                        FilterChip(
                            selected = style.mode == mode,
                            onClick = {
                                // A mode replaces the look but keeps personal switches.
                                settings.update { current ->
                                    mode.baseStyle().copy(
                                        haptics = current.haptics,
                                        motionEffects = current.motionEffects,
                                        performanceMode = current.performanceMode,
                                    )
                                }
                            },
                            label = { Text(mode.label.removeSuffix(" Glass"), maxLines = 1) },
                        )
                    }
                }
            }

            GlassCard {
                SectionTitle("Backdrop")
                GlassSlider("Blur intensity", style.blur, { v -> settings.update { it.copy(blur = v) } })
                GlassSlider(
                    label = "Transparency",
                    value = 1f - style.opacity / MAX_GLASS_OPACITY,
                    onValueChange = { v -> settings.update { it.copy(opacity = (1f - v) * MAX_GLASS_OPACITY) } },
                )
                GlassSlider("Refraction", style.distortion, { v -> settings.update { it.copy(distortion = v) } })
                GlassSlider("Reflection", style.reflection, { v -> settings.update { it.copy(reflection = v) } })
                GlassSlider("Edge brightness", style.edgeBrightness, { v -> settings.update { it.copy(edgeBrightness = v) } })
            }

            GlassCard {
                SectionTitle("Tint")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    TintSwatches.forEach { (name, color) ->
                        TintSwatch(
                            name = name,
                            color = color,
                            selected = style.tint == color,
                            onClick = { settings.update { it.copy(tint = color) } },
                        )
                    }
                }
            }

            GlassCard {
                SectionTitle("Shape and light")
                GlassSlider("Glow", style.glow, { v -> settings.update { it.copy(glow = v) } })
                GlassSlider("Shadow", style.shadow, { v -> settings.update { it.copy(shadow = v) } })
                GlassSlider(
                    label = "Border thickness",
                    value = style.borderWidth.value,
                    onValueChange = { v -> settings.update { it.copy(borderWidth = v.dp) } },
                    valueRange = 0f..3f,
                    valueLabel = "%.1f dp".format(style.borderWidth.value),
                )
                GlassSlider(
                    label = "Corner radius",
                    value = style.cornerRadius.value,
                    onValueChange = { v -> settings.update { it.copy(cornerRadius = v.dp) } },
                    valueRange = 8f..48f,
                    valueLabel = "${style.cornerRadius.value.toInt()} dp",
                )
            }

            GlassCard {
                SectionTitle("Motion")
                GlassSlider(
                    label = "Animation speed",
                    value = style.animationSpeed,
                    onValueChange = { v -> settings.update { it.copy(animationSpeed = v) } },
                    valueRange = 0.25f..3f,
                    valueLabel = "%.2fx".format(style.animationSpeed),
                )
                SwitchRow("Motion effects", "Tilt-reactive light and stretching capsule", style.motionEffects && true) { on ->
                    settings.update { it.copy(motionEffects = on) }
                }
                if (reducedMotion) {
                    Text(
                        text = "System animations are off, so motion is disabled regardless of this switch.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SwitchRow("Haptic feedback", "Tick when switching tabs", style.haptics) { on ->
                    settings.update { it.copy(haptics = on) }
                }
                SwitchRow("Performance mode", "No shader, no shadow, cheaper blur", style.performanceMode) { on ->
                    settings.update { it.copy(performanceMode = on) }
                }
            }

            GlassCard {
                SectionTitle("Presets")
                if (presets.isEmpty()) {
                    Text(
                        text = "Save the current look as a preset to bring it back later.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                presets.forEach { preset ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { settings.set(preset.style) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(preset.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        TextButton(onClick = { settings.set(preset.style) }) { Text("Apply") }
                        IconButton(onClick = { settings.deletePreset(preset.name) }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Delete preset ${preset.name}")
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassButton(text = "Save preset", onClick = { showSaveDialog = true })
                    GlassButton(text = "Reset mode", onClick = {
                        settings.update { current ->
                            current.mode.baseStyle().copy(
                                haptics = current.haptics,
                                motionEffects = current.motionEffects,
                                performanceMode = current.performanceMode,
                            )
                        }
                    })
                    GlassButton(text = "Defaults", onClick = { settings.set(GlassStyle.Natural) })
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showSaveDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save preset") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    singleLine = true,
                    label = { Text("Name") },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        if (settings.savePreset(name)) showSaveDialog = false
                    },
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun TintSwatch(name: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(color)
            .border(if (selected) 2.5.dp else 1.dp, if (selected) Color.White else Color.White.copy(alpha = 0.3f), CircleShape)
            .clickable(onClickLabel = name, onClick = onClick),
    )
}

/**
 * A busy, colourful scene with fine detail and moving parts: exactly what makes blur,
 * refraction and the edge light visible. It is only a test card for the glass; no app data.
 */
@Composable
private fun StudioPreview(hazeState: HazeState, animate: Boolean, modifier: Modifier = Modifier) {
    var selected by remember { mutableIntStateOf(0) }
    Box(modifier = modifier.clip(RoundedCornerShape(28.dp))) {
        // The source: everything inside is captured by the glass above it.
        Box(Modifier.matchParentSize().hazeSource(hazeState)) { TestCard(animate) }

        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            LiquidGlassPanel(modifier = Modifier.fillMaxWidth().height(92.dp), contentAlignment = Alignment.CenterStart) {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text("Liquid Glass", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Blurs and bends what is behind it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            LiquidNavCapsule(items = PreviewNav, selectedIndex = selected, onSelect = { selected = it })
        }
    }
}

@Composable
private fun TestCard(animate: Boolean) {
    val transition = rememberInfiniteTransition(label = "testcard")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "phase",
    )
    val t = if (animate) phase else 0.35f
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.linearGradient(
                    listOf(Color(0xFFFF4D8D), Color(0xFFFF9F43), Color(0xFF2EC4FF), Color(0xFF7B5CFF)),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height),
                ),
            )
            // Fine diagonal lines show refraction clearly.
            var x = -size.height
            while (x < size.width) {
                drawLine(Color.White.copy(alpha = 0.22f), Offset(x, 0f), Offset(x + size.height, size.height), strokeWidth = 2f)
                x += 26f
            }
            // Moving orbs so blur and bending visibly track the content.
            listOf(0.2f to 0.30f, 0.55f to 0.75f, 0.8f to 0.25f).forEachIndexed { i, (fx, fy) ->
                val cx = size.width * (fx + (if (i % 2 == 0) t else 1f - t) * 0.18f)
                val cy = size.height * fy
                drawCircle(Color.White.copy(alpha = 0.85f), radius = 38f + i * 14f, center = Offset(cx, cy))
                drawCircle(Color.Black.copy(alpha = 0.55f), radius = 38f + i * 14f, center = Offset(cx, cy), style = Stroke(5f))
            }
        }
        Text(
            text = "AURAX",
            color = Color.White,
            fontSize = 64.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 18.dp, top = 12.dp),
        )
        Text(
            text = "EVERY MEMORY, BEAUTIFULLY.",
            color = Color.Black.copy(alpha = 0.75f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 18.dp, bottom = 14.dp),
        )
    }
}
