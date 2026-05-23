package io.ak1.drawboxsample.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import io.ak1.drawbox.DrawBox
import io.ak1.drawbox.domain.model.Event
import io.ak1.drawbox.presentation.viewmodel.rememberDrawBoxController
import io.ak1.drawboxsample.ui.components.ColorPalette
import io.ak1.drawboxsample.ui.components.ControlsBar
import io.ak1.drawboxsample.ui.components.ShapeSelector
import io.ak1.drawboxsample.ui.components.StrokeSizeSlider

@Composable
fun HomeScreen(onSave: (ImageBitmap) -> Unit) {
    val colorBarVisibility = remember { mutableStateOf(false) }
    val sizeBarVisibility = remember { mutableStateOf(false) }
    val colorIsBg = remember { mutableStateOf(false) }
    var json by remember { mutableStateOf("") }

    // Use DrawBoxController
    val viewModel = rememberDrawBoxController()
    val state by viewModel.state.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            // Handle events if needed
            if (event is Event.PngSaved){
                if (event.bitmap!=null)  onSave.invoke(event.bitmap!!) else println("error ${event.throwable}")
            }
        }
    }

    Box {
        Column {
            // Drawing canvas with full shape support
            ShapeSelector(state.mode){
                viewModel.setMode(it)
            }
            DrawBox(
                state = state,
                onIntent = viewModel::onIntent,
                modifier = Modifier.fillMaxSize().weight(1f, fill = false).clipToBounds(),
            )

            Row {
                Button({
                    json = viewModel.exportPath()

                }){
                    Text("Export")
                }

                Button({
                    viewModel.importPath(json)
                }){
                    Text("Import")
                }
            }

            ControlsBar(
                viewModel = viewModel,
                canUndo = canUndo,
                canRedo = canRedo,
                currentColor = state.strokeColor,
                currentBgColor = state.bgColor,
                onColorClick = {
                    colorBarVisibility.value = when (colorBarVisibility.value) {
                        false -> true
                        colorIsBg.value -> true
                        else -> false
                    }
                    colorIsBg.value = false
                    sizeBarVisibility.value = false
                },
                onBgColorClick = {
                    colorBarVisibility.value = when (colorBarVisibility.value) {
                        false -> true
                        !colorIsBg.value -> true
                        else -> false
                    }
                    colorIsBg.value = true
                    sizeBarVisibility.value = false
                },
                onSizeClick = {
                    sizeBarVisibility.value = !sizeBarVisibility.value
                    colorBarVisibility.value = false
                },
            )

            ColorPalette(
                visible = colorBarVisibility.value,
                selectedColor = if (colorIsBg.value) state.bgColor else state.strokeColor,
            ) {
                if (colorIsBg.value) {
                    viewModel.setBgColor(it)
                } else {
                    viewModel.setColor(it)
                }
            }

            StrokeSizeSlider(
                isVisible = sizeBarVisibility.value,
                progress = state.strokeWidth.toInt(),
                color = state.strokeColor,
            ) {
                viewModel.setStrokeWidth(it.toFloat())
            }
        }
    }
}
