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
import androidx.compose.ui.graphics.asAndroidBitmap
import io.ak1.drawbox.DrawBox
import io.ak1.drawbox.rememberDrawController
import io.ak1.drawboxsample.data.local.convertToOldColor
import io.ak1.drawboxsample.ui.components.ColorRow
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
    val drawController = rememberDrawController()

    Box {
        Column {
            DrawBox(
                drawController = drawController,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f, fill = false),
                bitmapCallback = { imageBitmap, error ->
                    imageBitmap?.let {
                        save(it.asAndroidBitmap())
                    }
                }
            ) { undoCount, redoCount ->
                sizeBarVisibility.value = false
                colorBarVisibility.value = false
                undoVisibility.value = undoCount != 0
                redoVisibility.value = redoCount != 0
            }


            CustomSeekbar(
                isVisible = sizeBarVisibility.value,
                progress = currentSize.value,
                progressColor = MaterialTheme.colors.primary.convertToOldColor(),
                thumbColor = currentColor.value.convertToOldColor()
            ) {
                currentSize.value = it
                drawController.changeStrokeWidth(it.toFloat())
            }
            ColorRow(colorBarVisibility.value) {
                currentColor.value = it
                drawController.changeColor(it)
            }


            ControlsBar(
                drawController = drawController,
                {
                    drawController.saveBitmap()
                },
                {
                    colorBarVisibility.value = true
                    sizeBarVisibility.value = false
                },
                {
                    colorBarVisibility.value = false
                    sizeBarVisibility.value = true
                },
                undoVisibility = undoVisibility,
                redoVisibility = redoVisibility,
                colorValue = currentColor,
                sizeValue = currentSize
            )
        }

    }
}
/*
    var path: String = ""
    val json = GsonBuilder().create()
    if(path.isNotBlank()){
       val listOfMyClassObject = object : TypeToken<ArrayList<PathWrapper>>() {}.type
       drawController.importPath(json.fromJson(path,listOfMyClassObject))
       path = ""
    }else{
       path = json.toJson(drawController.exportPath())
       Log.e("to string","${path}")
    }
*/
