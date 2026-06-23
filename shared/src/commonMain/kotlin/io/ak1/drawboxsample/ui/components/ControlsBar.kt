@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.ak1.drawboxsample.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.ak1.drawbox.domain.model.Mode
import io.ak1.drawboxsample.ui.icons.DrawBoxIcons
import org.jetbrains.compose.resources.painterResource

/**
 * Bottom-center floating tool bar. Six slots, left → right:
 *
 *   [Undo] [Redo] [Select] [Eraser] [Pen] [Shape▾]
 *
 * Pen (freehand) and Eraser get their own slots so the dropdown is purely
 * shape tools. The currently active tool — whether Select, Eraser, Pen, or a
 * shape — is tinted with the primary color so the user can tell at a glance
 * which mode they're in.
 *
 * Pan is intentionally absent from the toolbar: panning is still always
 * available via Space-bar (hold to temp-pan), middle-mouse drag, two-finger
 * touch, and the scroll wheel. Removing it from the bar reclaims the slot for
 * the eraser, which is a far more common drawing-tool action.
 *
 * Color / stroke width / stroke style / corner radius live in the contextual
 * top-right [ContextBar] for the active drawing mode or selection. File /
 * canvas actions and the theme toggle live in the top-right [TopRightControls]
 * cluster and the [SettingsDrawer].
 */
@Composable
fun ControlsBar(
    canUndo: Boolean,
    canRedo: Boolean,
    currentMode: Mode,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onModeSelected: (Mode) -> Unit,
    expanded: Boolean = true,
) {
    val active = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant
    val disabled = MaterialTheme.colorScheme.outlineVariant

    val shapeChildren = listOf(
        ModeChild("mode-line", DrawBoxIcons.Line, "Line", Mode.LINE),
        ModeChild("mode-rect", DrawBoxIcons.Rectangle, "Rectangle", Mode.RECTANGLE),
        ModeChild("mode-circle", DrawBoxIcons.Circle, "Circle", Mode.CIRCLE),
        ModeChild("mode-arrow", DrawBoxIcons.Arrow, "Arrow", Mode.ARROW),
        ModeChild("mode-triangle", DrawBoxIcons.Triangle, "Triangle", Mode.TRIANGLE),
    )

    val isShapeMode = shapeChildren.any { it.mode == currentMode }
    // Remember the most recently active shape mode so the Shape slot can both
    // (a) show that shape's icon as its trigger, and (b) re-select it on tap
    // when the user is currently in a non-shape mode. Defaults to Rectangle
    // until the user picks something.
    var lastShape by remember { mutableStateOf(Mode.RECTANGLE as Mode) }
    LaunchedEffect(currentMode) {
        if (shapeChildren.any { it.mode == currentMode }) lastShape = currentMode
    }
    val shapeDropdownIcon = when (lastShape) {
        Mode.LINE -> DrawBoxIcons.Line
        Mode.RECTANGLE -> DrawBoxIcons.Rectangle
        Mode.CIRCLE -> DrawBoxIcons.Circle
        Mode.ARROW -> DrawBoxIcons.Arrow
        Mode.TRIANGLE -> DrawBoxIcons.Triangle
        else -> DrawBoxIcons.Rectangle
    }

    val items = listOf(
        FloatingMenuItem(
            id = "undo",
            icon = { _ ->
                Icon(
                    painter = painterResource(DrawBoxIcons.Undo),
                    contentDescription = "Undo",
                    tint = if (canUndo) active else disabled,
                )
            },
            onClick = { if (canUndo) onUndo() },
        ),
        FloatingMenuItem(
            id = "redo",
            icon = { _ ->
                Icon(
                    painter = painterResource(DrawBoxIcons.Redo),
                    contentDescription = "Redo",
                    tint = if (canRedo) active else disabled,
                )
            },
            onClick = { if (canRedo) onRedo() },
        ),
        separator("sep-history"),
        FloatingMenuItem(
            id = "select",
            isActive = currentMode == Mode.SELECT,
            icon = { isActive ->
                Icon(
                    painter = painterResource(DrawBoxIcons.Pointer),
                    contentDescription = "Select",
                    tint = isActive.getActiveColor(),
                )
            },
            onClick = { onModeSelected(Mode.SELECT) },
        ),
        FloatingMenuItem(
            id = "eraser",
            isActive = currentMode == Mode.ERASER,
            icon = { isActive ->
                Icon(
                    painter = painterResource(DrawBoxIcons.Eraser),
                    contentDescription = "Eraser",
                    tint = isActive.getActiveColor(),
                )
            },
            onClick = { onModeSelected(Mode.ERASER) },
        ),
        FloatingMenuItem(
            id = "pen",
            isActive = currentMode == Mode.PEN,
            icon = { isActive ->
                Icon(
                    painter = painterResource(DrawBoxIcons.StrokeCurved),
                    contentDescription = "Pen",
                    tint = isActive.getActiveColor(),
                )
            },
            onClick = { onModeSelected(Mode.PEN) },
        ),
        separator("sep-tools"),
        FloatingMenuItem(
            id = "shape",
            isActive = isShapeMode,
            icon = { isActive ->
                Icon(
                    painter = painterResource(shapeDropdownIcon),
                    contentDescription = "Shape",
                    tint = isActive.getActiveColor(),
                )
            },
            onClick = { onModeSelected(lastShape) },
            children = shapeChildren.map { child ->
                FloatingMenuItem(
                    id = child.id,
                    isActive = currentMode == child.mode,
                    icon = { isActive ->
                        Icon(
                            painter = painterResource(child.icon),
                            contentDescription = child.label,
                            tint = isActive.getActiveColor(),
                        )
                    },
                    onClick = { onModeSelected(child.mode) },
                )
            },
        ),
    )

    Box(modifier = Modifier.fillMaxSize()) {
        ExpandableFloatingToolbar(
            items = items,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            expanded = expanded,
            horizontalColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
            verticalColors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
            verticalMenuSpacing = 12.dp,
        )
    }
}

private data class ModeChild(
    val id: String,
    val icon: org.jetbrains.compose.resources.DrawableResource,
    val label: String,
    val mode: Mode,
)
