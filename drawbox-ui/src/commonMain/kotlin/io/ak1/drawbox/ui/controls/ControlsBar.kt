@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.ak1.drawbox.ui.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.ak1.drawbox.domain.model.Mode
import io.ak1.drawbox.ui.icons.DrawBoxIcons
import io.ak1.drawbox.ui.picker.ColorPickerSlot
import io.ak1.drawbox.ui.picker.RangVikalpColorPicker
import io.ak1.drawbox.ui.toolbar.ExpandableFloatingToolbar
import io.ak1.drawbox.ui.toolbar.FloatingMenuItem
import io.ak1.drawbox.ui.toolbar.SubmenuPosition
import io.ak1.drawbox.ui.toolbar.activeIconTint
import io.ak1.drawbox.ui.toolbar.separator
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Undo/redo capability, current [Mode], current stroke color, and recent colors
 * driving [ControlsBar].
 *
 * @param recentColors up to a handful of recently-picked colors surfaced as the
 *   color slot's submenu. Empty ⇒ tapping the color slot opens the picker
 *   directly; non-empty ⇒ tapping opens a submenu of recents plus a "custom"
 *   child that opens the picker.
 */
data class ControlsBarState(
    val currentMode: Mode,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val strokeColor: Color = Color.Black,
    val recentColors: List<Color> = emptyList(),
)

/** Every user gesture a [ControlsBar] can emit. */
sealed interface ControlsBarIntent {
    data object Undo : ControlsBarIntent
    data object Redo : ControlsBarIntent
    data class SelectMode(val mode: Mode) : ControlsBarIntent
    data class SetStrokeColor(val color: Color) : ControlsBarIntent
}

typealias ControlsBarDispatch = (ControlsBarIntent) -> Unit

/**
 * Content-description strings for the default items. Override to localize.
 */
data class ControlsBarLabels(
    val undo: String = "Undo",
    val redo: String = "Redo",
    val color: String = "Color",
    val colorCustom: String = "Pick custom color",
    val select: String = "Select",
    val eraser: String = "Eraser",
    val pen: String = "Pen",
    val text: String = "Text",
    val shape: String = "Shape",
    val line: String = "Line",
    val rectangle: String = "Rectangle",
    val circle: String = "Circle",
    val arrow: String = "Arrow",
    val triangle: String = "Triangle",
) {
    companion object {
        val Default: ControlsBarLabels = ControlsBarLabels()
    }
}

/**
 * Bottom-anchored floating tool bar. Renders an [ExpandableFloatingToolbar] of
 * [leading] + [items] + [trailing] and nothing else — positioning is the
 * caller's responsibility.
 *
 * For the standard DrawBox item set (Undo, Redo, Color, Select, Eraser, Pen,
 * Text, Shape▾), call [defaultControlsBarItems] first. For custom sets
 * (Pro-only tools, extra shape kinds, replaced icons) build your own list; the
 * same [FloatingMenuItem] primitive is used throughout `drawbox-ui`.
 *
 * ```kotlin
 * val items = defaultControlsBarItems(state, dispatch)
 * ControlsBar(
 *     items = items,
 *     trailing = listOf(myProPresenceItem, myProSyncItem),
 *     modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
 * )
 * ```
 */
@Composable
fun ControlsBar(
    items: List<FloatingMenuItem>,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    submenuPosition: SubmenuPosition = SubmenuPosition.Above,
    leading: List<FloatingMenuItem> = emptyList(),
    trailing: List<FloatingMenuItem> = emptyList(),
) {
    val combined = remember(leading, items, trailing) {
        buildList {
            addAll(leading)
            addAll(items)
            addAll(trailing)
        }
    }
    ExpandableFloatingToolbar(
        items = combined,
        modifier = modifier,
        expanded = expanded,
        submenuPosition = submenuPosition,
        horizontalColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
        verticalColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
        verticalMenuSpacing = 12.dp,
    )
}

/**
 * Standard DrawBox item set for [ControlsBar]:
 *
 *   [Undo] [Redo] | [Color] [Select] [Eraser] [Pen] [Text] | [Shape▾]
 *
 * The color slot shows the current [ControlsBarState.strokeColor] as a swatch;
 * empty [ControlsBarState.recentColors] opens the picker directly, non-empty
 * opens a submenu of recents plus a "custom" child that opens the picker.
 *
 * The shape slot shows the icon of the most-recently active shape mode and
 * re-selects it on tap; tap-again opens the sub-menu of all shapes.
 *
 * @param colorPicker composable used to render the color-picker dialog when the
 *   swatch is triggered. Defaults to [RangVikalpColorPicker]; supply your own
 *   [ColorPickerSlot] to swap in a different picker.
 * @param shapes shape modes exposed in the Shape sub-menu; reorder or subset
 *   (e.g. drop [Mode.ARROW]) without forking.
 */
@Composable
fun defaultControlsBarItems(
    state: ControlsBarState,
    dispatch: ControlsBarDispatch,
    labels: ControlsBarLabels = ControlsBarLabels.Default,
    shapes: List<Mode> = DefaultShapeModes,
    colorPicker: ColorPickerSlot = { initial, onDismiss, onSelected ->
        RangVikalpColorPicker(initial, onDismiss, onSelected)
    },
): List<FloatingMenuItem> {
    var showPicker by remember { mutableStateOf(false) }
    if (showPicker) {
        colorPicker(
            state.strokeColor,
            { showPicker = false },
            { color -> dispatch(ControlsBarIntent.SetStrokeColor(color)) },
        )
    }

    var lastShape by remember { mutableStateOf(shapes.firstOrNull() ?: Mode.RECTANGLE) }
    LaunchedEffect(state.currentMode) {
        if (state.currentMode in shapes) lastShape = state.currentMode
    }

    return remember(state, labels, shapes, lastShape) {
        buildList {
            add(historyItem("undo", DrawBoxIcons.Undo, labels.undo, state.canUndo) {
                dispatch(ControlsBarIntent.Undo)
            })
            add(historyItem("redo", DrawBoxIcons.Redo, labels.redo, state.canRedo) {
                dispatch(ControlsBarIntent.Redo)
            })
            add(separator("sep-history"))
            add(colorSwatchItem(state, dispatch, labels, onPickColor = { showPicker = true }))
            add(modeItem("select", DrawBoxIcons.Pointer, labels.select, Mode.SELECT, state.currentMode, dispatch))
            add(modeItem("eraser", DrawBoxIcons.Eraser, labels.eraser, Mode.ERASER, state.currentMode, dispatch))
            add(modeItem("pen", DrawBoxIcons.StrokeCurved, labels.pen, Mode.PEN, state.currentMode, dispatch))
            add(modeItem("text", DrawBoxIcons.Text, labels.text, Mode.TEXT, state.currentMode, dispatch))
            add(separator("sep-tools"))
            add(shapeItem(state, labels, shapes, lastShape, dispatch))
        }
    }
}

/** Default DrawBox shape modes surfaced by [defaultControlsBarItems]. */
val DefaultShapeModes: List<Mode> = listOf(
    Mode.LINE,
    Mode.RECTANGLE,
    Mode.CIRCLE,
    Mode.ARROW,
    Mode.TRIANGLE,
)

private fun historyItem(
    id: String,
    icon: DrawableResource,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
): FloatingMenuItem = FloatingMenuItem(
    id = id,
    icon = { _ ->
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.alpha(if (enabled) 1f else 0.38f),
        )
    },
    onClick = { if (enabled) onClick() },
)

private fun colorSwatchItem(
    state: ControlsBarState,
    dispatch: ControlsBarDispatch,
    labels: ControlsBarLabels,
    onPickColor: () -> Unit,
): FloatingMenuItem {
    val recents = state.recentColors
    if (recents.isEmpty()) {
        return FloatingMenuItem(
            id = "color",
            icon = { _ -> ColorSwatchButton(color = state.strokeColor, contentDescription = labels.color) },
            onClick = onPickColor,
        )
    }
    val paletteChild = FloatingMenuItem(
        id = "color-custom",
        icon = { isActive ->
            Icon(
                painter = painterResource(DrawBoxIcons.Palette),
                contentDescription = labels.colorCustom,
                tint = isActive.activeIconTint(),
            )
        },
        onClick = onPickColor,
    )
    val recentChildren = recents.mapIndexed { index, color ->
        FloatingMenuItem(
            id = "color-recent-$index",
            isActive = color == state.strokeColor,
            icon = { _ -> ColorSwatchButton(color = color, contentDescription = labels.color) },
            onClick = { dispatch(ControlsBarIntent.SetStrokeColor(color)) },
        )
    }
    return FloatingMenuItem(
        id = "color",
        icon = { _ -> ColorSwatchButton(color = state.strokeColor, contentDescription = labels.color) },
        // onClick = null → tapping opens the submenu of recents + custom.
        children = listOf(paletteChild) + recentChildren,
    )
}

@Composable
private fun ColorSwatchButton(color: Color, contentDescription: String, size: Dp = 20.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
    )
}

private fun modeItem(
    id: String,
    icon: DrawableResource,
    label: String,
    mode: Mode,
    currentMode: Mode,
    dispatch: ControlsBarDispatch,
): FloatingMenuItem = FloatingMenuItem(
    id = id,
    isActive = currentMode == mode,
    icon = { isActive ->
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = isActive.activeIconTint(),
        )
    },
    onClick = { dispatch(ControlsBarIntent.SelectMode(mode)) },
)

private fun shapeItem(
    state: ControlsBarState,
    labels: ControlsBarLabels,
    shapes: List<Mode>,
    lastShape: Mode,
    dispatch: ControlsBarDispatch,
): FloatingMenuItem {
    val isShapeMode = state.currentMode in shapes
    val dropdownIcon = drawableForShape(lastShape)
    return FloatingMenuItem(
        id = "shape",
        isActive = isShapeMode,
        icon = { isActive ->
            ShapeSlotIcon(icon = dropdownIcon, label = labels.shape, isActive = isActive)
        },
        onClick = { dispatch(ControlsBarIntent.SelectMode(lastShape)) },
        children = shapes.map { mode ->
            FloatingMenuItem(
                id = "mode-${mode.toString().lowercase()}",
                isActive = state.currentMode == mode,
                icon = { isActive ->
                    Icon(
                        painter = painterResource(drawableForShape(mode)),
                        contentDescription = shapeLabel(mode, labels),
                        tint = isActive.activeIconTint(),
                    )
                },
                onClick = { dispatch(ControlsBarIntent.SelectMode(mode)) },
            )
        },
    )
}

/**
 * Shape-slot icon with a small dropdown chevron overlay at bottom-right to
 * signal that the slot expands into a sub-menu of shape kinds.
 */
@Composable
private fun ShapeSlotIcon(
    icon: DrawableResource,
    label: String,
    isActive: Boolean,
) {
    Box(modifier = Modifier.size(24.dp)) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = isActive.activeIconTint(),
            modifier = Modifier.align(Alignment.Center),
        )
        Icon(
            imageVector = Icons.Filled.ArrowDropDown,
            contentDescription = null,
            tint = isActive.activeIconTint(),
            modifier = Modifier
                .size(10.dp)
                .align(Alignment.BottomEnd)
                .alpha(0.7f),
        )
    }
}

private fun drawableForShape(mode: Mode): DrawableResource = when (mode) {
    Mode.LINE -> DrawBoxIcons.Line
    Mode.RECTANGLE -> DrawBoxIcons.Rectangle
    Mode.CIRCLE -> DrawBoxIcons.Circle
    Mode.ARROW -> DrawBoxIcons.Arrow
    Mode.TRIANGLE -> DrawBoxIcons.Triangle
    else -> DrawBoxIcons.Rectangle
}

private fun shapeLabel(mode: Mode, labels: ControlsBarLabels): String = when (mode) {
    Mode.LINE -> labels.line
    Mode.RECTANGLE -> labels.rectangle
    Mode.CIRCLE -> labels.circle
    Mode.ARROW -> labels.arrow
    Mode.TRIANGLE -> labels.triangle
    else -> labels.shape
}
