package io.ak1.pencil

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import io.ak1.pencil.models.PathWrapper
import java.util.*

/**
 * Created by akshay on 10/12/21
 * https://ak1.io
 */
@Composable
fun Pencil() {
    val path = remember { mutableStateOf(Path()) }
    val action: MutableState<Any?> = remember { mutableStateOf(null) }
    val stack = remember {
        mutableStateOf(Stack<PathWrapper>())
    }
    /*   LaunchedEffect("hi") {
           paintBitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
           mPaintCanvas = Canvas(paintBitmap!!)
       }*/

    Canvas(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectDragGestures({
                Log.e("detectDragGestures", "started")
                path.value.reset()
                path.value.moveTo(it.x, it.y)

            }, {
                stack.value.push(PathWrapper(android.graphics.Path(path.value.asAndroidPath()), 5f, Color.Red))
                Log.e("detectDragGestures", "ended")
            }, {
                Log.e("detectDragGestures", "canceled")
            }, { change, dragAmount ->
                path.value.lineTo(change.position.x, change.position.y)
                action.value = change.position
            })
            detectTapGestures(
                onPress = { /* Called when the gesture starts */ },
                onDoubleTap = { /* Called on Double Tap */ },
                onLongPress = { /* Called on Long Press */ },
                onTap = { /* Called on Tap */ }
            )
        }
    ) {
        stack.value.forEach {
            //Log.e("stack","${stack.value.size}  ${it.path}")
            this.drawSomePath(path = it.path.asComposePath())
        }
        action.value?.let {
            this.drawSomePath(path = path.value)
        }


        // mPaintCanvas!!.drawPath(path.value)
        //mPaintCanvas?.drawPath(path, brushStrokePaint)
        // this.drawImage(paintBitmap!!)
        //this.(paintBitmap!!, 0f, 0f, null)
    }

}


fun DrawScope.drawSomePath(path: Path) {
    Log.e("drawSomePath", "called")
    drawPath(path, Color.Red, style = Stroke(5f))
    /*val canvasWidth = size.width
    val canvasHeight = size.height
    drawCircle(
        color = Color.Blue,
        center = Offset(x = canvasWidth / 2, y = canvasHeight / 2),
        radius = size.minDimension / 4
    )*/
}