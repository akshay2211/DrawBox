package io.ak1.drawboxsample.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import drawboxsample.shared.generated.resources.Res
import io.ak1.drawboxsample.ui.theme.ThemeMode
import drawboxsample.shared.generated.resources.bg_graph_paper
import drawboxsample.shared.generated.resources.bg_hideout
import drawboxsample.shared.generated.resources.bg_texture
import drawboxsample.shared.generated.resources.bg_tiny_checkers
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Preset canvas background patterns sourced from compose resources. Order in
 * [BgPatternPreset.entries] is the order shown in the drawer's preset row.
 */
enum class BgPatternPreset(val drawable: DrawableResource, val label: String) {
    GraphPaper(Res.drawable.bg_graph_paper, "Graph"),
    TinyCheckers(Res.drawable.bg_tiny_checkers, "Checker"),
    Hideout(Res.drawable.bg_hideout, "Hideout"),
    Texture(Res.drawable.bg_texture, "Texture"),
}

/**
 * Right-anchored modal drawer surfacing canvas-level actions: export, import,
 * background, view options, and destructive clear. Slides in from the right
 * over a scrim; tapping the scrim, the close button, or `Esc` dismisses it.
 *
 * Implemented as a hand-rolled side sheet because Material 3 only ships the
 * start-anchored ModalNavigationDrawer on Compose Multiplatform alpha07.
 */
@Composable
fun SettingsDrawer(
    visible: Boolean,
    showGrid: Boolean,
    currentBgColor: Color,
    currentBgPattern: BgPatternPreset?,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
    onDownloadSvg: () -> Unit,
    onDownloadPng: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onInsertImage: () -> Unit,
    onReplay: () -> Unit,
    onPickBgColor: () -> Unit,
    onBgPatternSelected: (BgPatternPreset?) -> Unit,
    onToggleGrid: (Boolean) -> Unit,
    onClearCanvas: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        )
        {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onDismiss,
                    ),
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            Surface(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = {},
                    ),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize().systemBarsPadding()
                        .verticalScroll(rememberScrollState()),
                ) {
                    DrawerHeader(onDismiss = onDismiss)

                    SectionLabel("Export")
                    DrawerRow(
                        icon = Icons.Filled.Download,
                        label = "Download SVG",
                        onClick = onDownloadSvg,
                    )
                    DrawerRow(
                        icon = Icons.Filled.Download,
                        label = "Download PNG",
                        onClick = onDownloadPng,
                    )
                    DrawerRow(
                        icon = Icons.Filled.Upload,
                        label = "Export JSON",
                        onClick = onExportJson,
                    )

                    SectionLabel("Import")
                    DrawerRow(
                        icon = Icons.Filled.Upload,
                        label = "Import JSON",
                        onClick = onImportJson,
                    )
                    DrawerRow(
                        icon = Icons.Filled.Image,
                        label = "Insert image",
                        onClick = onInsertImage,
                    )

                    SectionLabel("Playback")
                    DrawerRow(
                        icon = Icons.Filled.PlayArrow,
                        label = "Replay drawing",
                        onClick = onReplay,
                    )

                    SectionLabel("Canvas")
                    BgColorRow(currentBgColor = currentBgColor, onClick = onPickBgColor)
                    BgPatternRow(
                        current = currentBgPattern,
                        onSelect = onBgPatternSelected,
                    )

                    SectionLabel("View")
                    ThemeRow(current = themeMode, onSelect = onThemeModeChange)
                    GridSwitchRow(checked = showGrid, onCheckedChange = onToggleGrid)

                    SectionLabel("Danger")
                    DrawerRow(
                        icon = Icons.Filled.DeleteSweep,
                        label = "Clear canvas",
                        tint = MaterialTheme.colorScheme.error,
                        onClick = onClearCanvas,
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        IconButton(onClick = onDismiss) {
            Icon(Icons.Filled.Close, contentDescription = "Close settings")
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 6.dp),
    )
}

@Composable
private fun DrawerRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
            modifier = Modifier.padding(end = 8.dp),
        )
        if (trailing != null) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                trailing()
            }
        }
    }
}

@Composable
private fun BgColorRow(currentBgColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            Icons.Filled.Brush,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = "Background color",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(end = 8.dp),
        )
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(currentBgColor)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
        }
    }
}

@Composable
private fun BgPatternRow(
    current: BgPatternPreset?,
    onSelect: (BgPatternPreset?) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                Icons.Filled.Pattern,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Background pattern",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, start = 34.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PatternChip(
                label = "None",
                selected = current == null,
                content = { Box(modifier = Modifier.fillMaxSize()) },
                onClick = { onSelect(null) },
            )
            BgPatternPreset.entries.forEach { preset ->
                val tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                PatternChip(
                    label = preset.label,
                    selected = current == preset,
                    content = {
                        androidx.compose.foundation.Image(
                            painter = painterResource(preset.drawable),
                            contentDescription = preset.label,
                            modifier = Modifier.fillMaxSize(),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(tint),
                        )
                    },
                    onClick = { onSelect(preset) },
                )
            }
        }
    }
}

@Composable
private fun PatternChip(
    label: String,
    selected: Boolean,
    content: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(10.dp),
                )
                .clickable(onClick = onClick),
        ) {
            content()
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Three-way theme picker rendered as icon buttons trailing a labeled row.
 * Matches the drawer's row idiom — icon+label+trailing content — instead of
 * a floating cycle button.
 */
@Composable
private fun ThemeRow(current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val icon = when (current) {
            ThemeMode.SYSTEM -> Icons.Filled.BrightnessAuto
            ThemeMode.LIGHT -> Icons.Filled.LightMode
            ThemeMode.DARK -> Icons.Filled.DarkMode
        }
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = "Theme",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(end = 8.dp),
        )
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ThemeChoice(Icons.Filled.BrightnessAuto, "System", current == ThemeMode.SYSTEM) {
                    onSelect(ThemeMode.SYSTEM)
                }
                ThemeChoice(Icons.Filled.LightMode, "Light", current == ThemeMode.LIGHT) {
                    onSelect(ThemeMode.LIGHT)
                }
                ThemeChoice(Icons.Filled.DarkMode, "Dark", current == ThemeMode.DARK) {
                    onSelect(ThemeMode.DARK)
                }
            }
        }
    }
}

@Composable
private fun ThemeChoice(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer
    else Color.Transparent
    val tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun GridSwitchRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            Icons.Filled.GridOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = "Show grid",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(end = 8.dp),
        )
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
