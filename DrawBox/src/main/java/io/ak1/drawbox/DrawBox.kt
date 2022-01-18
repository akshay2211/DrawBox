package io.ak1.drawbox

import android.util.Log
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.collect
import java.util.*

/**
 * Created by akshay on 10/12/21
 * https://ak1.io
 */


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawBox(
    drawController: DrawController,
    modifier: Modifier = Modifier.fillMaxSize(),
    trackHistory: (undoCount: Int, redoCount: Int) -> Unit = { _, _ -> }
) {
    val refreshState = UUID.randomUUID().toString()
    var size = remember { mutableStateOf(IntSize.Zero) }
    var path = Path()
    val action: MutableState<Any?> = remember { mutableStateOf(null) }
    var imageBitmapCanvas: Canvas? = null

    LaunchedEffect(refreshState) {
        imageBitmapCanvas = drawController.generateCanvas(size.value)
        action.value = UUID.randomUUID().toString()
        drawController.changeRequests.collect {
            action.value = it
            trackHistory(drawController.undoStack.size, drawController.redoStack.size)
        }
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
                    drawController.redoStack.clear()
                    drawController.undoStack.add(
                        PathWrapper(
                            path,
                            drawController.strokeWidth,
                            drawController.strokeColor
                        )
                    )
                    trackHistory(drawController.undoStack.size, drawController.redoStack.size)
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
        Log.i("actions", "${action.value}")
        drawController.getDrawBoxBitmap()?.let { bitmap ->
            action.value?.let {
                drawController.undoStack.forEach {
                    imageBitmapCanvas?.drawSomePath(
                        path = it.path,
                        color = it.strokeColor,
                        width = it.strokeWidth
                    )
                }
                imageBitmapCanvas?.drawSomePath(
                    path = path,
                    color = drawController.strokeColor,
                    width = drawController.strokeWidth
                )
            }
            this.drawIntoCanvas {
                it.nativeCanvas.drawBitmap(bitmap, 0f, 0f, null)
            }
        }
    }
}





