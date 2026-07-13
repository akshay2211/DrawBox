package io.ak1.drawbox.ui.context

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import io.ak1.drawbox.domain.model.StrokeStyle
import io.ak1.drawbox.domain.model.TextAlignment
import io.ak1.drawbox.ui.model.ContextBarDispatch
import io.ak1.drawbox.ui.model.ContextBarIntent
import io.ak1.drawbox.ui.model.ContextBarState
import io.ak1.drawbox.ui.toolbar.FloatingMenuItem
import io.ak1.drawbox.ui.toolbar.activeIconTint
import io.ak1.drawbox.ui.toolbar.separator
import kotlin.math.abs

/** Stroke-width presets surfaced by the shape stroke-width sub-menu. */
val DefaultStrokeWidths: List<Float> = listOf(5f, 10f, 15f, 20f)

/** Font-size presets (world-pixel) surfaced by [textContextItems]. */
val DefaultTextSizePresets: List<Float> = listOf(12f, 16f, 20f, 24f, 32f, 48f)

/** Corner-radius presets surfaced by [cornerRadiusContextItems]: sharp, soft, pill. */
val DefaultCornerRadii: List<Float> = listOf(0f, 16f, 40f)

/**
 * A color-swatch item that opens the caller's color picker on tap.
 *
 * When [toggleable] is true, the swatch becomes a parent slot whose children
 * are "no stroke" and "pick color" — matches the tri-state UX for shape
 * selections (stroke-only / fill-only / both). When false, a single tap opens
 * the picker directly.
 *
 * @param onPickColor invoked when the user taps the "pick" action. The caller
 *   owns dialog state; a typical implementation is `{ showDialog = true }`.
 */
fun strokeColorContextItem(
    state: ContextBarState,
    dispatch: ContextBarDispatch,
    onPickColor: () -> Unit,
    toggleable: Boolean = false,
): List<FloatingMenuItem> {
    val root = if (toggleable) {
        FloatingMenuItem(
            id = "stroke-color",
            icon = { _ ->
                StrokeSwatchIcon(color = state.strokeColor, enabled = state.strokeEnabled)
            },
            children = listOf(
                FloatingMenuItem(
                    id = "stroke-none",
                    isActive = !state.strokeEnabled,
                    icon = { isActive -> StrokeNoneIcon(color = isActive.activeIconTint()) },
                    onClick = { dispatch(ContextBarIntent.SetStrokeEnabled(false)) },
                ),
                FloatingMenuItem(
                    id = "stroke-pick",
                    isActive = state.strokeEnabled,
                    icon = { _ ->
                        StrokeSwatchIcon(color = state.strokeColor, enabled = true)
                    },
                    onClick = {
                        // Picking implies "stroke on" so the user doesn't have to
                        // round-trip from the no-stroke state.
                        if (!state.strokeEnabled) dispatch(ContextBarIntent.SetStrokeEnabled(true))
                        onPickColor()
                    },
                ),
            ),
        )
    } else {
        FloatingMenuItem(
            id = "stroke-color",
            icon = { _ -> ColorSwatchIcon(color = state.strokeColor) },
            onClick = onPickColor,
        )
    }
    return listOf(root)
}

/**
 * Combined stroke + fill "colors" slot. One parent chip whose vertical submenu
 * groups stroke and fill sections:
 *
 *   [stroke swatch → picker]
 *   [no-stroke toggle]        (only if [strokeToggleable])
 *   ─────
 *   [fill swatch → picker]    (only if [showFill])
 *   [no-fill toggle]          (only if [showFill])
 *
 * Emit this instead of separate [strokeColorContextItem] + [fillContextItems]
 * when the host wants one consolidated colors chip — same UX Samsung Notes and
 * Figma use. Falls back to a simple stroke-only chip when [showFill] is false
 * and [strokeToggleable] is false.
 */
fun colorsContextItem(
    state: ContextBarState,
    dispatch: ContextBarDispatch,
    onPickStrokeColor: () -> Unit,
    onPickFillColor: () -> Unit,
    strokeToggleable: Boolean = false,
    showFill: Boolean = false,
): List<FloatingMenuItem> {
    // Degenerate case — no toggle, no fill — just the plain stroke picker.
    if (!strokeToggleable && !showFill) {
        return strokeColorContextItem(state, dispatch, onPickStrokeColor, toggleable = false)
    }
    val children = buildList {
        add(
            FloatingMenuItem(
                id = "colors-stroke",
                isActive = state.strokeEnabled,
                icon = { _ -> StrokeSwatchIcon(color = state.strokeColor, enabled = true) },
                onClick = {
                    if (!state.strokeEnabled) dispatch(ContextBarIntent.SetStrokeEnabled(true))
                    onPickStrokeColor()
                },
            ),
        )
        // "No stroke" / "No fill" toggles only appear when the OTHER target
        // is currently visible — otherwise turning this one off would leave
        // the shape invisible (invariant: at least one of stroke / fill on).
        val strokeIsOn = state.strokeEnabled
        val fillIsOn = state.fillColor != null
        if (strokeToggleable && fillIsOn) {
            add(
                FloatingMenuItem(
                    id = "colors-stroke-none",
                    isActive = !strokeIsOn,
                    icon = { isActive -> StrokeNoneIcon(color = isActive.activeIconTint()) },
                    onClick = { dispatch(ContextBarIntent.SetStrokeEnabled(false)) },
                ),
            )
        }
        if (showFill) {
            add(separator("colors-sep"))
            add(
                FloatingMenuItem(
                    id = "colors-fill",
                    isActive = fillIsOn,
                    icon = { _ -> FillSwatchIcon(color = state.fillColor) },
                    onClick = onPickFillColor,
                ),
            )
            if (strokeIsOn) {
                add(
                    FloatingMenuItem(
                        id = "colors-fill-none",
                        isActive = !fillIsOn,
                        icon = { isActive -> FillNoneIcon(color = isActive.activeIconTint()) },
                        onClick = { dispatch(ContextBarIntent.SetFillColor(null)) },
                    ),
                )
            }
        }
    }
    return listOf(
        FloatingMenuItem(
            id = "colors",
            icon = { _ ->
                ColorsSwatchIcon(
                    strokeColor = state.strokeColor,
                    strokeEnabled = state.strokeEnabled,
                    fillColor = state.fillColor,
                )
            },
            children = children,
        ),
    )
}

/**
 * Fill-color slot. Parent item with two children: "no fill" (null) and "pick
 * fill color" (opens caller's dialog).
 */
fun fillContextItems(
    state: ContextBarState,
    dispatch: ContextBarDispatch,
    onPickFillColor: () -> Unit,
): List<FloatingMenuItem> = listOf(
    FloatingMenuItem(
        id = "fill",
        icon = { _ -> FillSwatchIcon(color = state.fillColor) },
        children = listOf(
            FloatingMenuItem(
                id = "fill-none",
                isActive = state.fillColor == null,
                icon = { isActive -> FillNoneIcon(color = isActive.activeIconTint()) },
                onClick = { dispatch(ContextBarIntent.SetFillColor(null)) },
            ),
            FloatingMenuItem(
                id = "fill-pick",
                isActive = state.fillColor != null,
                icon = { _ -> FillSwatchIcon(color = state.fillColor) },
                onClick = onPickFillColor,
            ),
        ),
    ),
)

/**
 * Two items for shape strokes: style (solid/dashed/dotted) and width. Emit
 * together for shape-mode / shape-selection contexts.
 */
@Composable
fun shapeStrokeContextItems(
    state: ContextBarState,
    dispatch: ContextBarDispatch,
    widths: List<Float> = DefaultStrokeWidths,
): List<FloatingMenuItem> {
    val active = MaterialTheme.colorScheme.primary
    return listOf(
        FloatingMenuItem(
            id = "stroke-style",
            icon = { _ -> StrokeStyleIcon(style = state.strokeStyle, color = active) },
            children = StrokeStyle.entries.map { style ->
                FloatingMenuItem(
                    id = "stroke-style-${style.name.lowercase()}",
                    isActive = style == state.strokeStyle,
                    icon = { isActive ->
                        StrokeStyleIcon(style = style, color = isActive.activeIconTint())
                    },
                    onClick = { dispatch(ContextBarIntent.SetStrokeStyle(style)) },
                )
            },
        ),
        FloatingMenuItem(
            id = "stroke-width",
            icon = { _ -> SizeDot(width = state.strokeWidth, selected = true, color = active) },
            children = widths.map { value ->
                FloatingMenuItem(
                    id = "stroke-width-${value.toInt()}",
                    isActive = (state.strokeWidth - value).toInt() == 0,
                    icon = { isActive ->
                        SizeDot(width = value, selected = isActive, color = isActive.activeIconTint())
                    },
                    onClick = { dispatch(ContextBarIntent.SetStrokeWidth(value)) },
                )
            },
        ),
    )
}

/**
 * Corner-radius slot. Parent item whose children are the [presets] (defaults
 * to [DefaultCornerRadii]: sharp / soft / pill).
 */
@Composable
fun cornerRadiusContextItems(
    state: ContextBarState,
    dispatch: ContextBarDispatch,
    presets: List<Float> = DefaultCornerRadii,
): List<FloatingMenuItem> {
    val active = MaterialTheme.colorScheme.primary
    return listOf(
        FloatingMenuItem(
            id = "corner",
            icon = { _ -> CornerRadiusIcon(radius = state.cornerRadius, color = active) },
            children = presets.mapIndexed { index, value ->
                FloatingMenuItem(
                    id = "corner-$index",
                    isActive = abs(state.cornerRadius - value) < 0.5f,
                    icon = { isActive ->
                        CornerRadiusIcon(radius = value, color = isActive.activeIconTint())
                    },
                    onClick = { dispatch(ContextBarIntent.SetCornerRadius(value)) },
                )
            },
        ),
    )
}

/**
 * Text controls: optional edit, size presets, alignment, and font family.
 *
 * @param fontFamilyResolver maps a font-family key (e.g. `"sans"`) to a
 *   [FontFamily] used to preview the family in the picker chip. Returning
 *   `null` renders the label in the default family.
 * @param showEdit when true, includes an "Edit text" slot that dispatches
 *   [ContextBarIntent.EditText]. Enable for single-text-selection contexts.
 */
@Composable
fun textContextItems(
    state: ContextBarState,
    dispatch: ContextBarDispatch,
    fontFamilyResolver: (String) -> FontFamily?,
    showEdit: Boolean = false,
    sizePresets: List<Float> = DefaultTextSizePresets,
): List<FloatingMenuItem> {
    val active = MaterialTheme.colorScheme.primary
    // Dimmed tint for a chip whose selected elements disagree ("mixed").
    val mixedTint = active.copy(alpha = 0.4f)
    return buildList {
        if (showEdit) {
            add(
                FloatingMenuItem(
                    id = "text-edit",
                    icon = { _ ->
                        Icon(Icons.Filled.Edit, contentDescription = "Edit text", tint = active)
                    },
                    onClick = { dispatch(ContextBarIntent.EditText) },
                ),
            )
        }
        add(
            FloatingMenuItem(
                id = "text-size",
                icon = { _ ->
                    TextSizeIcon(size = state.fontSize, color = if (state.fontSizeMixed) mixedTint else active)
                },
                children = sizePresets.map { value ->
                    FloatingMenuItem(
                        id = "text-size-${value.toInt()}",
                        isActive = !state.fontSizeMixed && abs(state.fontSize - value) < 0.5f,
                        icon = { isActive ->
                            TextSizeIcon(size = value, color = isActive.activeIconTint())
                        },
                        onClick = { dispatch(ContextBarIntent.SetFontSize(value)) },
                    )
                },
            ),
        )
        add(
            FloatingMenuItem(
                id = "text-align",
                icon = { _ ->
                    TextAlignmentIcon(
                        alignment = state.textAlignment,
                        color = if (state.textAlignmentMixed) mixedTint else active,
                    )
                },
                children = TextAlignment.entries.map { align ->
                    FloatingMenuItem(
                        id = "text-align-${align.name.lowercase()}",
                        isActive = !state.textAlignmentMixed && align == state.textAlignment,
                        icon = { isActive ->
                            TextAlignmentIcon(alignment = align, color = isActive.activeIconTint())
                        },
                        onClick = { dispatch(ContextBarIntent.SetTextAlignment(align)) },
                    )
                },
            ),
        )
        add(
            FloatingMenuItem(
                id = "text-family",
                icon = { _ ->
                    FontFamilyLabel(
                        key = state.fontFamilyKey,
                        color = if (state.fontFamilyMixed) mixedTint else active,
                        fontFamily = fontFamilyResolver(state.fontFamilyKey),
                    )
                },
                children = state.fontFamilyKeys.sorted().map { key ->
                    FloatingMenuItem(
                        id = "text-family-$key",
                        isActive = !state.fontFamilyMixed && key == state.fontFamilyKey,
                        icon = { isActive ->
                            FontFamilyLabel(
                                key = key,
                                color = isActive.activeIconTint(),
                                fontFamily = fontFamilyResolver(key),
                            )
                        },
                        onClick = { dispatch(ContextBarIntent.SetFontFamily(key)) },
                    )
                },
            ),
        )
    }
}

/**
 * Selection actions: bring to front, send to back, delete.
 *
 * Deselection is not surfaced as a chip — tapping empty canvas, Esc, or
 * switching tools all deselect for free. A dedicated "clear" affordance is
 * the least-used chip in the pill and the cheapest to drop.
 */
@Composable
fun selectionContextItems(dispatch: ContextBarDispatch): List<FloatingMenuItem> {
    val errorTint = MaterialTheme.colorScheme.error
    return listOf(
        FloatingMenuItem(
            id = "front",
            icon = { isActive ->
                Icon(
                    Icons.Filled.FlipToFront,
                    contentDescription = "Bring to front",
                    tint = isActive.activeIconTint(),
                )
            },
            onClick = { dispatch(ContextBarIntent.BringToFront) },
        ),
        FloatingMenuItem(
            id = "back",
            icon = { isActive ->
                Icon(
                    Icons.Filled.FlipToBack,
                    contentDescription = "Send to back",
                    tint = isActive.activeIconTint(),
                )
            },
            onClick = { dispatch(ContextBarIntent.SendToBack) },
        ),
        FloatingMenuItem(
            id = "delete",
            icon = { _ ->
                Icon(Icons.Filled.Delete, contentDescription = "Delete selection", tint = errorTint)
            },
            onClick = { dispatch(ContextBarIntent.Delete) },
        ),
    )
}
