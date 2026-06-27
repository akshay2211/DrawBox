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
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatAlignCenter
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
 * World-pixel font-size presets surfaced by the text submenu. Picked to span
 * caption (12) → display (48) at sensible intervals; intermediate values are
 * available programmatically via [Intent.SetSelectedFontSize].
 */
private val TextSizePresets = listOf(12f, 16f, 20f, 24f, 32f, 48f)

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
    showShapeStroke: Boolean,
    showCornerRadius: Boolean,
    showFill: Boolean,
    showStrokeToggle: Boolean,
    showTextControls: Boolean,
    showEditText: Boolean,
    currentColor: Color,
    currentFillColor: Color?,
    currentStrokeEnabled: Boolean,
    currentStrokeStyle: StrokeStyle,
    currentStrokeWidth: Float,
    currentCornerRadius: Float,
    currentFontSize: Float,
    currentTextAlignment: io.ak1.drawbox.domain.model.TextAlignment,
    currentFontFamilyKey: String,
    fontFamilyKeys: Set<String>,
    expanded: Boolean,
    onColorChange: (Color) -> Unit,
    onFillColorChange: (Color?) -> Unit,
    onStrokeEnabledChange: (Boolean) -> Unit,
    onStrokeStyleChange: (StrokeStyle) -> Unit,
    onStrokeWidthChange: (Float) -> Unit,
    onCornerRadiusChange: (Float) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onTextAlignmentChange: (io.ak1.drawbox.domain.model.TextAlignment) -> Unit,
    onFontFamilyChange: (String) -> Unit,
    onEditText: () -> Unit,
    onBringToFront: () -> Unit,
    onSendToBack: () -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The bar is visible whenever any of its slots have something to show.
    val anySlot = showShapeStroke || showCornerRadius || showFill ||
        showStrokeToggle || showTextControls || hasSelection
    if (!anySlot) return

    var showColorDialog by remember { mutableStateOf(false) }
    if (showColorDialog) {
        ColorPickerDialog(
            initialColor = currentColor,
            onDismiss = { showColorDialog = false },
            onColorSelected = onColorChange,
        )
    }
    var showFillDialog by remember { mutableStateOf(false) }
    if (showFillDialog) {
        ColorPickerDialog(
            // RangVikalp has no "null" state — seed with the current fill or
            // the stroke color so the picker opens on something sensible.
            initialColor = currentFillColor ?: currentColor,
            onDismiss = { showFillDialog = false },
            onColorSelected = { onFillColorChange(it) },
        )
    }

    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant
    val errorTint = MaterialTheme.colorScheme.error

    val items = buildList {
        // When a shape is selected, the stroke slot becomes a parent that
        // exposes both "no stroke" and a color picker so the user can express
        // all three states (stroke-only / fill-only / both) from one place.
        // In shape-mode without selection it stays a one-shot picker — there's
        // no per-shape stroke yet to toggle.
        if (showStrokeToggle) {
            add(
                FloatingMenuItem(
                    id = "color",
                    icon = { _ ->
                        StrokeSwatchIcon(
                            color = currentColor,
                            enabled = currentStrokeEnabled,
                        )
                    },
                    children = listOf(
                        FloatingMenuItem(
                            id = "stroke-none",
                            isActive = !currentStrokeEnabled,
                            icon = { isActive ->
                                StrokeNoneIcon(color = isActive.getActiveColor())
                            },
                            onClick = { onStrokeEnabledChange(false) },
                        ),
                        FloatingMenuItem(
                            id = "stroke-pick",
                            isActive = currentStrokeEnabled,
                            icon = { _ ->
                                StrokeSwatchIcon(
                                    color = currentColor,
                                    enabled = true,
                                )
                            },
                            onClick = {
                                // Picking a color implies "stroke on" — saves a
                                // round-trip if the user came from the no-stroke
                                // state.
                                if (!currentStrokeEnabled) onStrokeEnabledChange(true)
                                showColorDialog = true
                            },
                        ),
                    ),
                ),
            )
        } else {
            add(
                FloatingMenuItem(
                    id = "color",
                    icon = { _ -> ColorSwatchIcon(color = currentColor) },
                    onClick = { showColorDialog = true },
                ),
            )
        }

        if (showTextControls) {
            if (showEditText) {
                add(
                    FloatingMenuItem(
                        id = "text-edit",
                        icon = { _ ->
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Filled.Edit,
                                contentDescription = "Edit text",
                                tint = active,
                            )
                        },
                        onClick = onEditText,
                    ),
                )
            }
            add(
                FloatingMenuItem(
                    id = "text-size",
                    icon = { _ -> TextSizeIcon(size = currentFontSize, color = active) },
                    children = TextSizePresets.map { value ->
                        FloatingMenuItem(
                            id = "text-size-${value.toInt()}",
                            isActive = kotlin.math.abs(currentFontSize - value) < 0.5f,
                            icon = { isActive ->
                                TextSizeIcon(
                                    size = value,
                                    color = isActive.getActiveColor(),
                                )
                            },
                            onClick = { onFontSizeChange(value) },
                        )
                    },
                ),
            )
            add(
                FloatingMenuItem(
                    id = "text-align",
                    icon = { _ ->
                        TextAlignmentIcon(
                            alignment = currentTextAlignment,
                            color = active,
                        )
                    },
                    children = io.ak1.drawbox.domain.model.TextAlignment.entries.map { align ->
                        FloatingMenuItem(
                            id = "text-align-${align.name.lowercase()}",
                            isActive = align == currentTextAlignment,
                            icon = { isActive ->
                                TextAlignmentIcon(
                                    alignment = align,
                                    color = isActive.getActiveColor(),
                                )
                            },
                            onClick = { onTextAlignmentChange(align) },
                        )
                    },
                ),
            )
            add(
                FloatingMenuItem(
                    id = "text-family",
                    icon = { _ -> FontFamilyLabel(key = currentFontFamilyKey, color = active) },
                    children = fontFamilyKeys.sorted().map { key ->
                        FloatingMenuItem(
                            id = "text-family-$key",
                            isActive = key == currentFontFamilyKey,
                            icon = { isActive ->
                                FontFamilyLabel(
                                    key = key,
                                    color = isActive.getActiveColor(),
                                )
                            },
                            onClick = { onFontFamilyChange(key) },
                        )
                    },
                ),
            )
        }

        if (showFill) {
            add(
                FloatingMenuItem(
                    id = "fill",
                    icon = { _ -> FillSwatchIcon(color = currentFillColor) },
                    children = listOf(
                        FloatingMenuItem(
                            id = "fill-none",
                            isActive = currentFillColor == null,
                            icon = { isActive ->
                                FillNoneIcon(color = isActive.getActiveColor())
                            },
                            onClick = { onFillColorChange(null) },
                        ),
                        FloatingMenuItem(
                            id = "fill-pick",
                            isActive = currentFillColor != null,
                            icon = { _ ->
                                FillSwatchIcon(color = currentFillColor)
                            },
                            onClick = { showFillDialog = true },
                        ),
                    ),
                ),
            )
        }

        if (showShapeStroke) {
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

        if (showCornerRadius) {
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

/**
 * Same shape as [ColorSwatchIcon] but adds a slash overlay when the stroke is
 * disabled — visually pairs with [FillSwatchIcon]'s null-state.
 */
@Composable
private fun StrokeSwatchIcon(color: Color, enabled: Boolean) {
    val outline = MaterialTheme.colorScheme.outline
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(if (enabled) color else Color.Transparent)
            .border(1.dp, outline, CircleShape),
    ) {
        if (!enabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = outline,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun StrokeNoneIcon(color: Color) {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .border(1.dp, color, CircleShape),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = color,
                start = Offset(0f, size.height),
                end = Offset(size.width, 0f),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun FillSwatchIcon(color: Color?) {
    val outline = MaterialTheme.colorScheme.outline
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(shape)
            .background(color ?: Color.Transparent)
            .border(1.dp, outline, shape),
    ) {
        if (color == null) {
            // Diagonal slash to communicate "no fill" — same visual idiom as
            // the SettingsDrawer's transparent-bg swatch.
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawLine(
                    color = outline,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun FillNoneIcon(color: Color) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(shape)
            .border(1.dp, color, shape),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawLine(
                color = color,
                start = Offset(0f, size.height),
                end = Offset(size.width, 0f),
                strokeWidth = 2f,
                cap = StrokeCap.Round,
            )
        }
    }
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

/**
 * Glyph-on-a-chip for a font-size preset. The glyph itself scales with
 * [size] (clamped to a chip-friendly range) so the chip visually reads as
 * "small" / "medium" / "huge" without the user having to read a number.
 */
@Composable
private fun TextSizeIcon(size: Float, color: Color) {
    val display = size.coerceIn(8f, 28f)
    BasicText(
        text = "A",
        style = androidx.compose.ui.text.TextStyle(
            color = color,
            fontSize = androidx.compose.ui.unit.TextUnit(display, androidx.compose.ui.unit.TextUnitType.Sp),
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        ),
    )
}

@Composable
private fun TextAlignmentIcon(
    alignment: io.ak1.drawbox.domain.model.TextAlignment,
    color: Color,
) {
    val icon = when (alignment) {
        io.ak1.drawbox.domain.model.TextAlignment.LEFT -> Icons.AutoMirrored.Filled.FormatAlignLeft
        io.ak1.drawbox.domain.model.TextAlignment.CENTER -> Icons.Filled.FormatAlignCenter
        io.ak1.drawbox.domain.model.TextAlignment.RIGHT -> Icons.AutoMirrored.Filled.FormatAlignRight
    }
    Icon(icon, contentDescription = alignment.name, tint = color)
}

/**
 * Three-letter glyph for the family key — written in the family itself so the
 * chip previews the typeface. Falls back to the key's first three characters
 * for unknown / custom keys.
 */
@Composable
private fun FontFamilyLabel(key: String, color: Color) {
    val label = when (key) {
        "sans" -> "Sa"
        "serif" -> "Se"
        "mono" -> "Mo"
        else -> key.take(2).replaceFirstChar { it.uppercase() }
    }
    val family = io.ak1.drawbox.text.FontRegistry.resolve(key)
    BasicText(
        text = label,
        style = androidx.compose.ui.text.TextStyle(
            color = color,
            fontFamily = family,
            fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp),
            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        ),
    )
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
