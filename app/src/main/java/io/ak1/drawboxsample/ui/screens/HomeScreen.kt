package io.ak1.drawboxsample.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.elixer.palette.Presets
import com.elixer.palette.composables.Palette
import com.elixer.palette.constraints.HorizontalAlignment
import com.elixer.palette.constraints.VerticalAlignment
import io.ak1.drawbox.DrawBox
import io.ak1.drawbox.rememberDrawController
import io.ak1.drawboxsample.data.local.convertToOldColor
import io.ak1.drawboxsample.ui.components.ControlsBar
import io.ak1.drawboxsample.ui.components.CustomSeekbar

/**
 * Created by akshay on 29/12/21
 * https://ak1.io
 */

@Composable
fun HomeScreen(save: (Bitmap) -> Unit) {
    val undoVisibility = remember { mutableStateOf(false) }
    val redoVisibility = remember { mutableStateOf(false) }
    val colorBarVisibility = remember { mutableStateOf(true) }
    val sizeBarVisibility = remember { mutableStateOf(false) }
    val currentColor = remember { mutableStateOf(Color.Red) }
    val currentSize = remember { mutableStateOf(10) }

    val drawController = rememberDrawController{ undoCount, redoCount ->
        undoVisibility.value = undoCount != 0
        redoVisibility.value = redoCount != 0
    }

    Box {
        Column {
            DrawBox(
                drawController = drawController,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f, fill = false)
            )


            CustomSeekbar(
                isVisible = sizeBarVisibility.value,
                progress = currentSize.value,
                progressColor = MaterialTheme.colors.primary.convertToOldColor(),
                thumbColor = currentColor.value.convertToOldColor()
            ) {
                currentSize.value = it
                drawController.changeStrokeWidth(it.toFloat())
                colorBarVisibility.value = false
            }

            ControlsBar(
                drawController = drawController,
                {
                    drawController.getDrawBoxBitmap()?.let { save(it) }
                },
                {
                    colorBarVisibility.value = true
                    sizeBarVisibility.value = false
                },
                {
                    sizeBarVisibility.value = true
                    colorBarVisibility.value = false
                },
                undoVisibility = undoVisibility,
                redoVisibility = redoVisibility,
                colorValue = currentColor,
                sizeValue = currentSize
            )
        }
        Palette(
            defaultColor = Color.Red,
            buttonSize = 120.dp,
            swatches = Presets.material(),
            innerRadius = 800f,
            strokeWidth = 120f,
            spacerRotation = 5f,
            spacerOutward = 2f,
            colorWheelZIndexOnWheelDisplayed = 0f,
            colorWheelZIndexOnWheelHidden = -1f,
            buttonColorChangeAnimationDuration = 1000,
            selectedArchAnimationDuration = 300,
            verticalAlignment = VerticalAlignment.Bottom,
            horizontalAlignment = HorizontalAlignment.End,
            onColorSelected = { drawController.changeColor(it) }
        )
    }
}