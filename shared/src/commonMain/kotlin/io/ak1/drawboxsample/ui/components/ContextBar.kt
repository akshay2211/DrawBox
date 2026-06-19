@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.ak1.drawboxsample.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import io.ak1.drawbox.domain.model.StrokeStyle
import kotlin.math.abs

private const val RadiusSharp = 0f
private const val RadiusSoft = 16f
private const val RadiusRound = 40f

/**
 * Top-right contextual pill built on [ExpandableFloatingToolbar]. Mirrors the
 * bottom NavBar pattern: each slot is a single icon, and slots that have
 * children open a vertical sub-bar below the icon when tapped.
 *
 * Slot layout (only those relevant to the current state render):
 *
 *   [●] [▭] [▢] | [↑F] [↓B] [🗑] [×]
 *    │   │   │     └────── selection actions (when something is selected)
 *    │   │   └── corner radius (RECT/TRIANGLE mode or selected roundable)
 *    │   └── stroke style (any shape mode or selection)
 *    └── color swatch (always)
 *
 * The stroke / corner icons show the *current* value; the popout sub-bar lets
 * the user pick a different option.
 */
@Composable
fun ContextBar(
    isShapeMode: Boolean,
    hasSelection: Boolean,
    showCornerRadius: Boolean,
    currentColor: Color,
    currentStrokeStyle: StrokeStyle,
    currentCornerRadius: Float,
    expanded: Boolean,
    onColorChange: (Color) -> Unit,
    onStrokeStyleChange: (StrokeStyle) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
    onBringToFront: () -> Unit,
    onSendToBack: () -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showConfig = isShapeMode || hasSelection
    if (!showConfig && !hasSelection) return

    var showColorDialog by remember { mutableStateOf(false) }
    if (showColorDialog) {
        ColorPickerDialog(
            initialColor = currentColor,
            onDismiss = { showColorDialog = false },
            onColorSelected = onColorChange,
        )
    }

    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant
    val errorTint = MaterialTheme.colorScheme.error

    val items = buildList {
        add(
            FloatingMenuItem(
                id = "color",
                icon = { ColorSwatchIcon(color = currentColor) },
                onClick = { showColorDialog = true },
            ),
        )

        if (showConfig) {
            add(
                FloatingMenuItem(
                    id = "stroke",
                    icon = { StrokeStyleIcon(style = currentStrokeStyle, color = active) },
                    children = StrokeStyle.entries.map { style ->
                        FloatingMenuItem(
                            id = "stroke-${style.name.lowercase()}",
                            icon = {
                                StrokeStyleIcon(
                                    style = style,
                                    color = if (style == currentStrokeStyle) active else inactive,
                                )
                            },
                            onClick = { onStrokeStyleChange(style) },
                        )
                    },
                ),
            )
        }

        if (showConfig && showCornerRadius) {
            add(
                FloatingMenuItem(
                    id = "corner",
                    icon = {
                        CornerRadiusIcon(radius = currentCornerRadius, color = active)
                    },
                    children = listOf(
                        cornerChild("corner-sharp", RadiusSharp, currentCornerRadius, active, inactive, onCornerRadiusChange),
                        cornerChild("corner-soft", RadiusSoft, currentCornerRadius, active, inactive, onCornerRadiusChange),
                        cornerChild("corner-round", RadiusRound, currentCornerRadius, active, inactive, onCornerRadiusChange),
                    ),
                ),
            )
        }

        if (hasSelection) {
            add(
                FloatingMenuItem(
                    id = "front",
                    icon = {
                        Icon(Icons.Filled.FlipToFront, contentDescription = "Bring to front")
                    },
                    onClick = onBringToFront,
                ),
            )
            add(
                FloatingMenuItem(
                    id = "back",
                    icon = {
                        Icon(Icons.Filled.FlipToBack, contentDescription = "Send to back")
                    },
                    onClick = onSendToBack,
                ),
            )
            add(
                FloatingMenuItem(
                    id = "delete",
                    icon = {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete selection",
                            tint = errorTint,
                        )
                    },
                    onClick = onDelete,
                ),
            )
            add(
                FloatingMenuItem(
                    id = "clear",
                    icon = {
                        Icon(Icons.Filled.Close, contentDescription = "Clear selection")
                    },
                    onClick = onClear,
                ),
            )
        }
    }

    ExpandableFloatingToolbar(
        items = items,
        modifier = modifier,
        expanded = expanded,
        submenuPosition = SubmenuPosition.Below,
        horizontalColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
        verticalColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
        verticalMenuSpacing = 8.dp,
    )
}

private fun cornerChild(
    id: String,
    value: Float,
    current: Float,
    active: Color,
    inactive: Color,
    onSelect: (Float) -> Unit,
): FloatingMenuItem {
    val isCurrent = abs(current - value) < 0.5f
    return FloatingMenuItem(
        id = id,
        icon = { CornerRadiusIcon(radius = value, color = if (isCurrent) active else inactive) },
        onClick = { onSelect(value) },
    )
}

@Composable
private fun ColorSwatchIcon(color: Color) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
    )
}

@Composable
private fun StrokeStyleIcon(style: StrokeStyle, color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val y = size.height / 2f
        val effect = when (style) {
            StrokeStyle.SOLID -> null
            StrokeStyle.DASHED -> PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
            StrokeStyle.DOTTED -> PathEffect.dashPathEffect(floatArrayOf(1.5f, 4f))
        }
        drawLine(
            color = color,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round,
            pathEffect = effect,
        )
    }
}

@Composable
private fun CornerRadiusIcon(radius: Float, color: Color) {
    val cornerDp = when {
        radius < 1f -> 0.dp
        radius < 24f -> 4.dp
        else -> 9.dp
    }
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(cornerDp))
            .border(2.dp, color, RoundedCornerShape(cornerDp)),
    ) {
        Box(modifier = Modifier.fillMaxSize())
    }
}
