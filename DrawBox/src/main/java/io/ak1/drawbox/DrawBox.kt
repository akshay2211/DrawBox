package io.ak1.drawbox

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Created by akshay on 10/12/21
 * https://ak1.io
 */


@Composable
fun DrawBox(
    drawController: DrawController,
    modifier: Modifier = Modifier.fillMaxSize(),
    ) {

    Canvas(modifier = modifier
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    drawController.insertNewPath(it)
                }
            ) { change, dragAmount ->
                val newPoint = change.position
                drawController.updateLatestPath(newPoint)
            }
        }) {
        drawController.pathList.forEach {
            drawPath(
                createPath(it.points),
                color = it.strokeColor,
                alpha = it.alpha,
                style = Stroke(
                    width = it.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
/*    drawController.getDrawBoxBitmap()?.let { bitmap ->
        drawController.pathList.forEach {
            imageBitmapCanvas?.drawSomePath(it)
        }
        this.drawIntoCanvas {
            it.nativeCanvas.drawBitmap(bitmap, 0f, 0f, null)
        }
    }*/
    }
}





