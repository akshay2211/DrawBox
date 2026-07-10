@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.ak1.drawbox.ui.context

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.ak1.drawbox.ui.model.ContextBarDispatch
import io.ak1.drawbox.ui.model.ContextBarIntent
import io.ak1.drawbox.ui.model.ContextBarSlots
import io.ak1.drawbox.ui.model.ContextBarState
import io.ak1.drawbox.ui.picker.ColorPickerSlot
import io.ak1.drawbox.ui.picker.RangVikalpColorPicker
import io.ak1.drawbox.ui.toolbar.ExpandableFloatingToolbar
import io.ak1.drawbox.ui.toolbar.FloatingMenuItem
import io.ak1.drawbox.ui.toolbar.SubmenuPosition

/**
 * Composite contextual pill for the active drawing selection or mode.
 *
 * Assembles the built-in `*ContextItems` builders into a single
 * [ExpandableFloatingToolbar] based on [slots]. Renders nothing when [slots]
 * has no visible section, so hosts can keep the composable mounted and just
 * toggle flags.
 *
 * For non-standard combinations (or Pro-only sections) skip this composable
 * and drive `ExpandableFloatingToolbar` directly with your own list of
 * [FloatingMenuItem]s — the same builder functions are public.
 *
 * @param state snapshot of the drawing model driving what the bar shows.
 * @param onIntent invoked for every user gesture. Wire to your controller /
 *   view-model.
 * @param colorPicker composable used for both stroke and fill color pickers.
 *   Defaults to [RangVikalpColorPicker]; supply your own [ColorPickerSlot] to
 *   swap in a different picker (Material dialog, custom, etc.).
 * @param fontFamilyResolver maps a font-family key to a [FontFamily] for the
 *   picker chip preview. Return `null` to fall back to the default family.
 */
@Composable
fun ContextBar(
    state: ContextBarState,
    onIntent: ContextBarDispatch,
    slots: ContextBarSlots,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    colorPicker: ColorPickerSlot = { initial, onDismiss, onSelected ->
        RangVikalpColorPicker(initial, onDismiss, onSelected)
    },
    fontFamilyResolver: (String) -> FontFamily? = { null },
) {
    val anySlot = with(slots) {
        showStroke || showShapeStroke || showFill || showCornerRadius ||
            showText || showSelectionActions
    }
    if (!anySlot) return

    var showStrokeDialog by remember { mutableStateOf(false) }
    var showFillDialog by remember { mutableStateOf(false) }

    if (showStrokeDialog) {
        colorPicker(
            state.strokeColor,
            { showStrokeDialog = false },
            { color ->
                onIntent(ContextBarIntent.SetStrokeColor(color))
            },
        )
    }
    if (showFillDialog) {
        // RangVikalp has no null state — seed with the current fill or the
        // stroke color so the picker opens on something sensible.
        colorPicker(
            state.fillColor ?: state.strokeColor,
            { showFillDialog = false },
            { color ->
                onIntent(ContextBarIntent.SetFillColor(color))
            },
        )
    }

    val items: List<FloatingMenuItem> = buildList {
        if (slots.showStroke) {
            addAll(
                strokeColorContextItem(
                    state = state,
                    dispatch = onIntent,
                    onPickColor = { showStrokeDialog = true },
                    toggleable = slots.strokeToggleable,
                ),
            )
        }
        if (slots.showText) {
            addAll(
                textContextItems(
                    state = state,
                    dispatch = onIntent,
                    fontFamilyResolver = fontFamilyResolver,
                    showEdit = slots.showEditText,
                ),
            )
        }
        if (slots.showFill) {
            addAll(
                fillContextItems(
                    state = state,
                    dispatch = onIntent,
                    onPickFillColor = { showFillDialog = true },
                ),
            )
        }
        if (slots.showShapeStroke) {
            addAll(shapeStrokeContextItems(state = state, dispatch = onIntent))
        }
        if (slots.showCornerRadius) {
            addAll(cornerRadiusContextItems(state = state, dispatch = onIntent))
        }
        if (slots.showSelectionActions) {
            addAll(selectionContextItems(dispatch = onIntent))
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
