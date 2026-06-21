@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.ak1.drawboxsample.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.ak1.drawbox.domain.model.Mode
import io.ak1.drawboxsample.ui.icons.DrawBoxIcons
import org.jetbrains.compose.resources.painterResource

/**
 * Bottom-center floating tool bar. Four slots, left → right:
 *
 *   [Undo] [Redo] [Select] [Mode▾]
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
    val inactive = MaterialTheme.colorScheme.onSurfaceVariant
    val disabled = MaterialTheme.colorScheme.outlineVariant

    val currentModeIcon = when (currentMode) {
        Mode.PAN -> DrawBoxIcons.Import
        Mode.PEN -> DrawBoxIcons.StrokeCurved
        Mode.RECTANGLE -> DrawBoxIcons.Rectangle
        Mode.CIRCLE -> DrawBoxIcons.Circle
        Mode.TRIANGLE -> DrawBoxIcons.Triangle
        Mode.ARROW -> DrawBoxIcons.Arrow
        Mode.LINE -> DrawBoxIcons.Line
        else -> DrawBoxIcons.StrokeCurved
    }

    val modeChildren = listOf(
        ModeChild("mode-pan", DrawBoxIcons.Import, "Pan", Mode.PAN),
        ModeChild("mode-pen", DrawBoxIcons.StrokeCurved, "Pen", Mode.PEN),
        ModeChild("mode-line", DrawBoxIcons.Line, "Line", Mode.LINE),
        ModeChild("mode-rect", DrawBoxIcons.Rectangle, "Rectangle", Mode.RECTANGLE),
        ModeChild("mode-circle", DrawBoxIcons.Circle, "Circle", Mode.CIRCLE),
        ModeChild("mode-arrow", DrawBoxIcons.Arrow, "Arrow", Mode.ARROW),
        ModeChild("mode-triangle", DrawBoxIcons.Triangle, "Triangle", Mode.TRIANGLE),
    )

    val items = listOf(
        FloatingMenuItem(
            id = "undo",
            icon = {
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
            icon = {
                Icon(
                    painter = painterResource(DrawBoxIcons.Redo),
                    contentDescription = "Redo",
                    tint = if (canRedo) active else disabled,
                )
            },
            onClick = { if (canRedo) onRedo() },
        ),
        FloatingMenuItem(
            id = "select",
            icon = {
                Icon(
                    imageVector = Icons.Filled.SelectAll,
                    contentDescription = "Select",
                    tint = if (currentMode == Mode.SELECT) active else inactive,
                )
            },
            onClick = { onModeSelected(Mode.SELECT) },
        ),
        FloatingMenuItem(
            id = "mode",
            icon = {
                Icon(
                    painter = painterResource(currentModeIcon),
                    contentDescription = "Drawing mode",
                    tint = active,
                )
            },
            children = modeChildren.map { child ->
                FloatingMenuItem(
                    id = child.id,
                    icon = {
                        Icon(
                            painter = painterResource(child.icon),
                            contentDescription = child.label,
                            tint = if (currentMode == child.mode) active else inactive,
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