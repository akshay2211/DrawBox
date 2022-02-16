package io.ak1.drawbox

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Created by akshay on 10/12/21
 * https://ak1.io
 */


@Composable
fun DrawBox(
    drawController: DrawController,
    modifier: Modifier = Modifier.fillMaxSize(),
    bitmapCallback: (ImageBitmap?, Throwable?) -> Unit,
    trackHistory: (undoCount: Int, redoCount: Int) -> Unit = { _, _ -> }
) = AndroidView(
    factory = {
        ComposeView(it).apply {
            setContent {
                LaunchedEffect(drawController) {
                    drawController.trackBitmaps(this@apply, this, bitmapCallback)
                    drawController.trackHistory(this, trackHistory)
                }
                Canvas(modifier = modifier
                    .background(Color.Transparent)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                drawController.insertNewPath(offset)
                            }
                        ) { change, _ ->
                            val newPoint = change.position
                            drawController.updateLatestPath(newPoint)
                        }
                    }) {

                    drawController.pathList.forEach { pw ->
                        drawPath(
                            createPath(pw.points),
                            color = pw.strokeColor,
                            alpha = pw.alpha,
                            style = Stroke(
                                width = pw.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }
        }
    },
    modifier = modifier
)





