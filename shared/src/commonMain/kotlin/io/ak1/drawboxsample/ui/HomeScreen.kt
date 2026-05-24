package io.ak1.drawboxsample.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import io.ak1.drawbox.DrawBox
import io.ak1.drawbox.domain.model.Event
import io.ak1.drawbox.presentation.viewmodel.rememberDrawBoxController
import io.ak1.drawboxsample.save.rememberImageSaver
import io.ak1.drawboxsample.ui.components.ControlsBar

@Composable
fun HomeScreen() {
    val colorBarVisibility = remember { mutableStateOf(false) }
    val colorIsBg = remember { mutableStateOf(false) }
    val imageSaver = rememberImageSaver()

    // Use DrawBoxController
    val viewModel = rememberDrawBoxController()
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

                else -> {}
            }
        }
    }



    DrawBox(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = Modifier.fillMaxSize().clipToBounds(),
    )

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
    )
}
