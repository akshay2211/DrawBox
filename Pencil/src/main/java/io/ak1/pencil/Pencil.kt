package io.ak1.pencil

import android.graphics.Bitmap
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection

/**
 * Created by akshay on 10/12/21
 * https://ak1.io
 */


internal var strokeAlpha = 1f
internal var strokeWidth = 5f
internal var strokeColor = Color.Red
val undoStack = ArrayList<PathWrapper>()
internal var bitmap: Bitmap? = null

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Pencil(modifier: Modifier = Modifier.fillMaxSize()) {
    var size = remember { mutableStateOf(IntSize.Zero) }
    var path = Path()
    val action: MutableState<Any?> = remember { mutableStateOf(null) }
    /*LaunchedEffect(size) {
        bitmap =
            Bitmap.createBitmap(size.value.width, size.value.height, Bitmap.Config.ARGB_8888)
                val imageBitmapCanvas = Canvas(bitmap!!.asImageBitmap())
                val scope = CanvasDrawScope().draw(
                    Density(1.0f), LayoutDirection.Ltr, imageBitmapCanvas,
                    Size(size.value.width.toFloat(), size.value.height.toFloat()),
                ) {
                    action.value?.let {
                        undoStack.forEach {
                            this.drawSomePath(
                                path = it.path,
                                color = it.strokeColor,
                                width = it.strokeWidth
                            )
                        }
                        this.drawSomePath(path = path)
                    }
                }

    }*/
    Canvas(modifier = modifier
        .pointerInteropFilter {
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    path.moveTo(it.x, it.y)
                    path.addOval(Rect(it.x - 0.5f, it.y - 0.5f, it.x + 0.5f, it.y + 0.5f))
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
        }){
        action.value?.let {
            undoStack.forEach {
                this.drawSomePath(
                    path = it.path,
                    color = it.strokeColor,
                    width = it.strokeWidth
                )
            }
            this.drawSomePath(path = path)
        }
    }
}





