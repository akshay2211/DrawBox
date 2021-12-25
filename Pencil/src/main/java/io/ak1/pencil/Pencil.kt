package io.ak1.pencil

import android.graphics.Bitmap
import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

/**
 * Created by akshay on 10/12/21
 * https://ak1.io
 */


internal var strokeAlpha = 1f
internal var strokeWidth = 5f
internal var strokeColor = Color.Red

// TODO: 25/12/21 convert datatype to Stack
//  currently Stack is internal in 'androidx.compose.runtime'

internal val undoStack = ArrayList<PathWrapper>()
internal var bitmap: Bitmap? = null

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Pencil(modifier: Modifier = Modifier.fillMaxSize()) {
    var size = remember { mutableStateOf(IntSize.Zero) }
    var path = Path()
    val action: MutableState<Any?> = remember { mutableStateOf(null) }
    var imageBitmapCanvas: Canvas? = null

    LaunchedEffect(size) {
        Log.i("Bitmap", "created ${size.value}")
        bitmap = Bitmap.createBitmap(size.value.width, size.value.height, Bitmap.Config.ARGB_8888)
        imageBitmapCanvas = Canvas(bitmap!!.asImageBitmap())
        action.value = "-"
    }

    Canvas(modifier = modifier
        .pointerInteropFilter {
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    path.moveTo(it.x, it.y)
                    path.addOval(it.getRect())
                }
                MotionEvent.ACTION_MOVE -> path.lineTo(it.x, it.y)
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                    undoStack.add(
                        PathWrapper(
                            path, strokeWidth,
                            if (strokeAlpha == 1f) strokeColor else strokeColor.copy(strokeAlpha)
                        )
                    )
                    path = Path()
                }
                else -> false
            }
            action.value = "${it.x},${it.y}"
            true
        }
        .onSizeChanged {
            size.value = it
        }) {
        bitmap?.let { bitmap ->
            action.value?.let {
                undoStack.forEach {
                    imageBitmapCanvas?.drawSomePath(
                        path = it.path,
                        color = it.strokeColor,
                        width = it.strokeWidth
                    )
                }
                imageBitmapCanvas?.drawSomePath(path = path)
            }
            this.drawIntoCanvas {
                it.nativeCanvas.drawBitmap(bitmap, 0f, 0f, null)
            }
        }
    }
}





