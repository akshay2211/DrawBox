package io.ak1.drawbox

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun DrawBox(
    drawController: DrawController,
    modifier: Modifier = Modifier.fillMaxSize(),
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    bitmapCallback: (ImageBitmap?, Throwable?) -> Unit,
    trackHistory: (undoCount: Int, redoCount: Int) -> Unit = { _, _ -> },
) {
    val graphicsLayer = rememberGraphicsLayer()

    LaunchedEffect(drawController) {
        drawController.changeBgColor(backgroundColor)
        drawController.trackHistory(this, trackHistory)
        drawController.captureRequests.collect {
            try {
                bitmapCallback(graphicsLayer.toImageBitmap(), null)
            } catch (e: Throwable) {
                bitmapCallback(null, e)
            }
        }
    }

    Box(
        modifier = modifier
            .background(drawController.bgColor)
            .drawWithContent {
                graphicsLayer.record { this@drawWithContent.drawContent() }
                drawLayer(graphicsLayer)
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        drawController.insertNewPath(offset)
                        drawController.updateLatestPath(offset)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset -> drawController.insertNewPath(offset) },
                ) { change, _ ->
                    drawController.updateLatestPath(change.position)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawController.pathList.forEach { pw ->
                drawPath(
                    createPath(pw.points),
                    color = pw.strokeColor,
                    alpha = pw.alpha,
                    style = Stroke(
                        width = pw.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }
    }
}