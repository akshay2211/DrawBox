@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.ak1.drawboxsample.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
 * Bottom-center floating tool bar. Five slots, left → right:
 *
 *   [Undo] [Redo] [Select] [Mode▾] [Size▾]
 *
 * Color picking lives in the contextual top-right [ShapeConfigToolbar] for the
 * active drawing mode or selection. File / canvas actions and the theme toggle
 * live in the top-right [TopRightControls] cluster and the [SettingsDrawer].
 */
@Composable
fun ControlsBar(
    canUndo: Boolean,
    canRedo: Boolean,
    currentMode: Mode,
    currentStrokeWidth: Float,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onModeSelected: (Mode) -> Unit,
    onSizeSelected: (Float) -> Unit,
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

    val sizeOptions = listOf(5f, 10f, 15f, 20f)

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
        FloatingMenuItem(
            id = "size",
            icon = {
                Icon(
                    painter = painterResource(DrawBoxIcons.Ruler),
                    contentDescription = "Stroke size",
                    tint = active,
                )
            },
            children = sizeOptions.map { value ->
                FloatingMenuItem(
                    id = "size-${value.toInt()}",
                    icon = {
                        SizeDot(
                            size = value,
                            isSelected = (currentStrokeWidth - value).toInt() == 0,
                            color = active,
                        )
                    },
                    onClick = { onSizeSelected(value) },
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

@Composable
private fun SizeDot(size: Float, isSelected: Boolean, color: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier.size(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .border(if (isSelected) 2.dp else 1.dp, color, CircleShape),
        )
    }
}