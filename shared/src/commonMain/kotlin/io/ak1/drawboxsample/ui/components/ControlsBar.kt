package io.ak1.drawboxsample.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.shadow.InnerShadowPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.ak1.drawbox.domain.model.Mode
import io.ak1.drawbox.presentation.viewmodel.DrawBoxController
import io.ak1.drawboxsample.ui.icons.DrawBoxIcons
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.min

/**
 * Controls bar with dropdown menus for drawing controls and mode selection.
 *
 * Features:
 * - Tools menu: Save, Undo, Redo, Reset
 * - Mode menu: Select drawing mode (Pen, Rectangle, Circle, Triangle, Arrow, Line)
 * - Color controls: Stroke color and background color
 * - Size slider: Adjust stroke width
 */
@Composable
fun ControlsBar(
    viewModel: DrawBoxController,
    canUndo: Boolean,
    canRedo: Boolean,
    currentColor: Color,
    currentBgColor: Color,
    currentMode: Mode,
    currentStrokeWidth: Float,
    onColorClick: () -> Unit,
    onBgColorClick: () -> Unit,
    onSizeClick: () -> Unit,
    onModeSelected: (Mode) -> Unit,
    onSizeSelected: (Float) -> Unit,
) {
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.surfaceVariant

    var modeMenuExpanded by remember { mutableStateOf(false) }
    var sizeMenuExpanded by remember { mutableStateOf(false) }
    var settingsMenuExpanded by remember { mutableStateOf(false) }

    // Helper function to get icon for current mode
    val currentModeIcon = when (currentMode) {
        Mode.PEN -> DrawBoxIcons.StrokeCurved
        Mode.RECTANGLE -> DrawBoxIcons.Rectangle
        Mode.CIRCLE -> DrawBoxIcons.Circle
        Mode.TRIANGLE -> DrawBoxIcons.Triangle
        Mode.ARROW -> DrawBoxIcons.Arrow
        Mode.LINE -> DrawBoxIcons.Line
    }

    val clickable = if (modeMenuExpanded || sizeMenuExpanded) Modifier.clickable(
        indication = null,
        interactionSource = remember { MutableInteractionSource() },
        onClick = {
            modeMenuExpanded = false
            sizeMenuExpanded = false
        }
    ) else Modifier

    Box(Modifier.fillMaxSize().then(clickable)) {
        Column(Modifier.fillMaxWidth().align(Alignment.BottomCenter)) {

            AnimatedVisibility(modeMenuExpanded, Modifier.fillMaxWidth()) {
                Card(
                    Modifier
                        .padding(8.dp)
                        .requiredWidth(500.dp)
                        .align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Pen mode
                        MenuItem(
                            DrawBoxIcons.StrokeCurved,
                            "Pen",
                            if (currentMode == Mode.PEN) inactive else active
                        ) {
                            onModeSelected(Mode.PEN)
                            modeMenuExpanded = false
                        }

                        // Line mode
                        MenuItem(
                            DrawBoxIcons.Line,
                            "Line",
                            if (currentMode == Mode.LINE) inactive else active
                        ) {
                            onModeSelected(Mode.LINE)
                            modeMenuExpanded = false
                        }

                        // Rectangle mode
                        MenuItem(
                            DrawBoxIcons.Rectangle,
                            "Rectangle",
                            if (currentMode == Mode.RECTANGLE) inactive else active
                        ) {
                            onModeSelected(Mode.RECTANGLE)
                            modeMenuExpanded = false
                        }

                        // Circle mode
                        MenuItem(
                            DrawBoxIcons.Circle,
                            "Circle",
                            if (currentMode == Mode.CIRCLE) inactive else active
                        ) {
                            onModeSelected(Mode.CIRCLE)
                            modeMenuExpanded = false
                        }

                        // Arrow mode
                        MenuItem(
                            DrawBoxIcons.Arrow,
                            "Arrow",
                            if (currentMode == Mode.ARROW) inactive else active
                        ) {
                            onModeSelected(Mode.ARROW)
                            modeMenuExpanded = false
                        }

                        // Triangle mode
                        MenuItem(
                            DrawBoxIcons.Triangle,
                            "Triangle",
                            if (currentMode == Mode.TRIANGLE) inactive else active
                        ) {
                            onModeSelected(Mode.TRIANGLE)
                            modeMenuExpanded = false
                        }
                    }
                }
            }

            AnimatedVisibility(sizeMenuExpanded, Modifier.fillMaxWidth()) {
                Card(
                    Modifier
                        .padding(8.dp)
                        .requiredWidth(300.dp)
                        .align(Alignment.CenterHorizontally),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val sizes = listOf(
                            Pair(5f, "size_1"),
                            Pair(10f, "size_2"),
                            Pair(15f, "size_3"),
                            Pair(20f, "size_4"),
                        )

                        sizes.forEach { (sizeValue, label) ->
                            SizeMenuItem(
                                sizeValue = sizeValue,
                                label = label,
                                isSelected = (currentStrokeWidth - sizeValue).toInt() == 0,
                                color = if ((currentStrokeWidth - sizeValue).toInt() == 0) inactive else active
                            ) {
                                onSizeSelected(sizeValue)
                                sizeMenuExpanded = false
                            }
                        }
                    }
                }
            }

    Card(Modifier.padding(8.dp).requiredWidth(500.dp).align(Alignment.CenterHorizontally),
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        )
        {

            // Undo button
            MenuItem(
                DrawBoxIcons.Undo,
                "Undo",
                if (canUndo) active else inactive
            ) {
                if (canUndo) viewModel.undo()
            }

            // Redo button
            MenuItem(
                DrawBoxIcons.Redo,
                "Redo",
                if (canRedo) active else inactive
            ) {
                if (canRedo) viewModel.redo()
            }

            // Reset button
            MenuItem(
                DrawBoxIcons.RefreshIcon,
                "Reset",
                if (canRedo || canUndo) active else inactive,
            ) {
                viewModel.reset()
            }

            // Drawing Mode button
            MenuItem(
                currentModeIcon,
                "Drawing Mode",
                active,
            ) {
                modeMenuExpanded = !modeMenuExpanded
            }

            // Stroke color button
            MenuItem(
                DrawBoxIcons.Palette,
                "Colors",
                currentColor,
                modifier = Modifier.weight(1f)
            ) { onColorClick() }

            // Stroke size button
            MenuItem(
                DrawBoxIcons.Ruler,
                "Size",
                active,
                modifier = Modifier.weight(1f)
            ) { sizeMenuExpanded = !sizeMenuExpanded }
        }
    }}

        SettingsMenu(viewModel,settingsMenuExpanded ){
            settingsMenuExpanded = it
        }
    }


    }



@Composable
private fun RowScope.MenuItem(
    drawable: DrawableResource,
    desc: String,
    tint: Color,
    border: Boolean = false,
    modifier: Modifier = Modifier.weight(1f),
    onClick: () -> Unit,
) {
    val iconModifier = Modifier.size(20.dp)
    IconButton(onClick = onClick, modifier = modifier.size(36.dp)) {
        Icon(
            painter = painterResource(drawable),
            contentDescription = desc,
            tint = tint,
            modifier = if (border)
                iconModifier.border(0.5.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
            else
                iconModifier,
        )
    }
}

@Composable
private fun RowScope.SizeMenuItem(
    sizeValue: Float,
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .size(40.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((sizeValue).dp)
                .let {
                    if (isSelected)
                        it.border(2.dp, color, CircleShape)
                    else
                        it.border(1.dp, color, CircleShape)
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size((sizeValue * 0.6f).dp)
                    .border(1.dp, color, CircleShape)
            )
        }
    }
}
@Composable
fun BoxScope.SettingsMenu(
    viewModel: DrawBoxController,
    menuExpanded : Boolean,
    onMenuClick : (Boolean)-> Unit
){
    Box(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
        IconButton(
            onClick = { onMenuClick.invoke(!menuExpanded) }
        ) {
            Icon(
                painter = painterResource(DrawBoxIcons.Settings),
                contentDescription = "Menu",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { onMenuClick.invoke(false) }
        ) {
        DropdownMenuItem(

            text = { Text("Download SVG") },
            leadingIcon = { Icon(painterResource(DrawBoxIcons.FileSvg),"SVG download") },
            onClick = {
                onMenuClick.invoke(false)
            }
        )

        DropdownMenuItem(
            text = { Text("Download PNG") },
            leadingIcon = { Icon(painterResource(DrawBoxIcons.FilePng),"PNG download") },
            onClick = {
                viewModel.saveBitmap()
                onMenuClick.invoke(false)
            }
        )

        DropdownMenuItem(
            text = { Text("Export JSON") },
            leadingIcon = { Icon(painterResource(DrawBoxIcons.Export),"Json Export") },
            onClick = {
                // TODO: Implement JSON export
                onMenuClick.invoke(false)
            }
        )

        DropdownMenuItem(
            text = { Text("Import JSON") },
            leadingIcon = { Icon(painterResource(DrawBoxIcons.Import),"Json Import") },
            onClick = {
                // TODO: Implement JSON import
                onMenuClick.invoke(false)
            }
        )
        }
    }
}
