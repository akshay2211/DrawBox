package io.ak1.drawboxsample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import drawboxsample.shared.generated.resources.Res
import drawboxsample.shared.generated.resources.bg_graph_paper
import io.ak1.drawbox.DrawBox
import io.ak1.drawbox.domain.model.Event
import io.ak1.drawbox.presentation.viewmodel.DrawBoxController
import io.ak1.drawbox.presentation.viewmodel.rememberDrawBoxController
import io.ak1.drawboxsample.save.rememberImageSaver
import io.ak1.drawboxsample.ui.components.ControlsBar
import io.ak1.drawboxsample.ui.icons.DrawBoxIcons
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth

@Composable
fun HomeScreen() {
    val colorBarVisibility = remember { mutableStateOf(false) }
    val colorIsBg = remember { mutableStateOf(false) }
    val showGrid = remember { mutableStateOf(true) }
    val imageSaver = rememberImageSaver()

    // Use DrawBoxController
    val viewModel = rememberDrawBoxController()
        /*.apply {
        this.setBackgroundPattern(
            painter = painterResource(Res.drawable.bg_graph_paper),   // any Painter, including SVG
            tint    = Color.LightGray.copy(alpha = 0.2f)                           // optional, null keeps original colors
        )
    }*/
    val state by viewModel.state.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is Event.PngSaved -> {
                    if (event.bitmap != null) imageSaver.savePng(event.bitmap!!) else println("error ${event.throwable}")
                }

                is Event.SvgExported -> {
                    imageSaver.saveSvg(event.svg)
                }

                is Event.JsonExported -> {
                    imageSaver.saveJson(event.json)
                }

                else -> {}
            }
        }
    }



    DrawBox(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = Modifier.fillMaxSize().clipToBounds(),
        showGrid = showGrid.value,
    )

    if (state.selectedIds.isNotEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            SelectionToolbar(viewModel = viewModel)
        }
    }

    // Bottom-left zoom controls. Use a viewport-center focal point so
    // toolbar-driven zoom doesn't snap to a random corner.
    Box(modifier = Modifier.fillMaxSize()) {
        ZoomToolbar(
            scalePercent = state.viewport.scalePercent,
            gridOn = showGrid.value,
            onZoomIn = { viewModel.zoomBy(1.25f, ScreenCenter) },
            onZoomOut = { viewModel.zoomBy(0.8f, ScreenCenter) },
            onReset = { viewModel.resetCamera() },
            onToggleGrid = { showGrid.value = !showGrid.value },
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 96.dp),
        )
    }

    /*   ColorPalette(
            visible = colorBarVisibility.value,
            selectedColor = if (colorIsBg.value) state.bgColor else state.strokeColor,
        ) {
            if (colorIsBg.value) {
                viewModel.setBgColor(it)
            } else {
                viewModel.setColor(it)
            }
        }*/

    ControlsBar(
        viewModel = viewModel,
        canUndo = canUndo,
        canRedo = canRedo,
        currentColor = state.strokeColor,
        currentBgColor = state.bgColor,
        currentMode = state.mode,
        currentStrokeWidth = state.strokeWidth,
        onColorClick = {
            colorBarVisibility.value = when (colorBarVisibility.value) {
                false -> true
                colorIsBg.value -> true
                else -> false
            }
            colorIsBg.value = false
        },
        onBgColorClick = {
            colorBarVisibility.value = when (colorBarVisibility.value) {
                false -> true
                !colorIsBg.value -> true
                else -> false
            }
            colorIsBg.value = true
        },
        onSizeClick = {
            colorBarVisibility.value = false
        },
        onModeSelected = { mode ->
            viewModel.setMode(mode)
        },
        onSizeSelected = { size ->
            viewModel.setStrokeWidth(size)
        },
        onImportJson = {
            imageSaver.loadJson { json -> viewModel.importPath(json) }
        },
    )
}

// Reasonable default focal point for toolbar-driven zoom. We don't know the
// canvas dimensions here, so pick a fixed-ish screen point; pinch and scroll
// gestures supply real focal points.
private val ScreenCenter = Offset(540f, 960f)

@Composable
private fun ZoomToolbar(
    scalePercent: Int,
    gridOn: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onReset: () -> Unit,
    onToggleGrid: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            IconButton(onClick = onZoomOut, modifier = Modifier.size(36.dp)) {
                Text("−", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .clickable(onClick = onReset)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$scalePercent%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onZoomIn, modifier = Modifier.size(36.dp)) {
                Text("+", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onToggleGrid, modifier = Modifier.size(36.dp)) {
                Text(
                    text = "#",
                    fontSize = 16.sp,
                    color = if (gridOn) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SelectionToolbar(viewModel: DrawBoxController) {
    Card(
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth(0.6f),
        shape = RoundedCornerShape(28.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { viewModel.bringSelectionToFront() }) {
                Icon(
                    painter = painterResource(DrawBoxIcons.Redo),
                    contentDescription = "Bring to front",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = { viewModel.sendSelectionToBack() }) {
                Icon(
                    painter = painterResource(DrawBoxIcons.Undo),
                    contentDescription = "Send to back",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = { viewModel.deleteSelected() }) {
                Icon(
                    painter = painterResource(DrawBoxIcons.Refresh),
                    contentDescription = "Delete selection",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
            IconButton(onClick = { viewModel.clearSelection() }) {
                Icon(
                    painter = painterResource(DrawBoxIcons.Settings),
                    contentDescription = "Clear selection",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
