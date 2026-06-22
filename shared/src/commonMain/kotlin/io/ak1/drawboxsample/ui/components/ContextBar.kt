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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import io.ak1.drawbox.domain.model.StrokeStyle
import io.ak1.drawboxsample.ui.icons.DrawBoxIcons
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs

private const val RadiusSharp = 0f
private const val RadiusSoft = 16f
private const val RadiusRound = 40f

private val StrokeWidthOptions = listOf(5f, 10f, 15f, 20f)

/**
 * Top-right contextual pill built on [ExpandableFloatingToolbar]. Mirrors the
 * bottom NavBar pattern: each slot is a single icon, and slots that have
 * children open a vertical sub-bar below the icon when tapped.
 *
 * Slot layout (only those relevant to the current state render):
 *
 *   [●] [▭] [○] [▢] | [↑F] [↓B] [🗑] [×]
 *    │   │   │   │     └────── selection actions (when something is selected)
 *    │   │   │   └── corner radius (RECT/TRIANGLE mode or selected roundable)
 *    │   │   └── stroke width (any shape mode or selection)
 *    │   └── stroke style (any shape mode or selection)
 *    └── color swatch (always)
 *
 * The stroke / width / corner icons show the *current* value; the popout
 * sub-bar lets the user pick a different option.
 */
@Composable
fun ContextBar(
    isShapeMode: Boolean,
    hasSelection: Boolean,
    showCornerRadius: Boolean,
    currentColor: Color,
    currentStrokeStyle: StrokeStyle,
    currentStrokeWidth: Float,
    currentCornerRadius: Float,
    expanded: Boolean,
    onColorChange: (Color) -> Unit,
    onStrokeStyleChange: (StrokeStyle) -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
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
                icon = { _ -> ColorSwatchIcon(color = currentColor) },
                onClick = { showColorDialog = true },
            ),
        )

        if (showConfig) {
            add(
                FloatingMenuItem(
                    id = "stroke",
                    icon = { _ -> StrokeStyleIcon(style = currentStrokeStyle, color = active) },
                    children = StrokeStyle.entries.map { style ->
                        FloatingMenuItem(
                            id = "stroke-${style.name.lowercase()}",
                            isActive = style == currentStrokeStyle,
                            icon = { isActive ->
                                StrokeStyleIcon(
                                    style = style,
                                    color = isActive.getActiveColor(),
                                )
                            },
                            onClick = { onStrokeStyleChange(style) },
                        )
                    },
                ),
            )
            add(
                FloatingMenuItem(
                    id = "width",
                    icon = { _ -> SizeDot(size = currentStrokeWidth, isSelected = true, color = active) },
                    children = StrokeWidthOptions.map { value ->
                        val matches = (currentStrokeWidth - value).toInt() == 0
                        FloatingMenuItem(
                            id = "width-${value.toInt()}",
                            isActive = matches,
                            icon = { isActive ->
                                SizeDot(
                                    size = value,
                                    isSelected = isActive,
                                    color = isActive.getActiveColor(),
                                )
                            },
                            onClick = { onStrokeWidthChange(value) },
                        )
                    },
                ),
            )
        }

        if (showConfig && showCornerRadius) {
            add(
                FloatingMenuItem(
                    id = "corner",
                    icon = { _ ->
                        CornerRadiusIcon(radius = currentCornerRadius, color = active)
                    },
                    children = listOf(
                        cornerChild("corner-sharp", RadiusSharp, currentCornerRadius, onCornerRadiusChange),
                        cornerChild("corner-soft", RadiusSoft, currentCornerRadius, onCornerRadiusChange),
                        cornerChild("corner-round", RadiusRound, currentCornerRadius, onCornerRadiusChange),
                    ),
                ),
            )
        }

        if (hasSelection) {
            add(
                FloatingMenuItem(
                    id = "front",
                    icon = { isActive ->
                        Icon(
                            Icons.Filled.FlipToFront,
                            contentDescription = "Bring to front",
                            tint = isActive.getActiveColor(),
                        )
                    },
                    onClick = onBringToFront,
                ),
            )
            add(
                FloatingMenuItem(
                    id = "back",
                    icon = { isActive ->
                        Icon(
                            Icons.Filled.FlipToBack,
                            contentDescription = "Send to back",
                            tint = isActive.getActiveColor(),
                        )
                    },
                    onClick = onSendToBack,
                ),
            )
            add(
                FloatingMenuItem(
                    id = "delete",
                    icon = { _ ->
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
                    icon = { isActive ->
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Clear selection",
                            tint = isActive.getActiveColor(),
                        )
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
    onSelect: (Float) -> Unit,
): FloatingMenuItem {
    val isCurrent = abs(current - value) < 0.5f
    return FloatingMenuItem(
        id = id,
        isActive = isCurrent,
        icon = { isActive -> CornerRadiusIcon(radius = value, color = isActive.getActiveColor()) },
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
private fun SizeDot(size: Float, isSelected: Boolean, color: Color) {
    Box(
        modifier = Modifier.size(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        val m = Modifier
            .size(size.dp)
        Box(
            modifier = if (isSelected) m.size(size.dp).background(color, CircleShape) else m.border(1.dp, color, CircleShape)

        )
    }
}

@Composable
private fun CornerRadiusIcon(radius: Float, color: Color) {
    val cornerIcon = when {
        radius < 1f -> DrawBoxIcons.BorderSquare
        radius < 24f -> DrawBoxIcons.BorderRounded
        else -> DrawBoxIcons.BorderPill
    }

    Icon(painterResource(cornerIcon), contentDescription = "Rounded Corner Shape", tint = color)
  /*  Box(
        modifier = Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(cornerDp))
            .border(2.dp, color, RoundedCornerShape(cornerDp)),
    ) {
        Box(modifier = Modifier.fillMaxSize())
    }*/
}
