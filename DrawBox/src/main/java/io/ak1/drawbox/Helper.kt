package io.ak1.drawbox

import android.view.MotionEvent
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Created by akshay on 24/12/21
 * https://ak1.io
 */


internal fun MotionEvent.getRect() =
    Rect(this.x - 0.5f, this.y - 0.5f, this.x + 0.5f, this.y + 0.5f)

internal fun DrawScope.drawSomePath(
    path: Path,
    color: Color,
    width: Float
) = drawPath(
    path,
    color,
    style = Stroke(width, miter = 0f, join = StrokeJoin.Round, cap = StrokeCap.Round),
)


//Model
data class PathWrapper(
    var points: SnapshotStateList<Offset>,
    val strokeWidth: Float = 5f,
    val strokeColor: Color,
    val alpha: Float = 1f
)


fun createPath(points: List<Offset>) = Path().apply {
    if (points.size > 1) {
        var oldPoint: Offset? = null
        this.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val point: Offset = points[i]
            oldPoint?.let {
                val midPoint = calculateMidpoint(it, point)
                if (i == 1) {
                    this.lineTo(midPoint.x, midPoint.y)
                } else {
                    this.quadraticBezierTo(it.x, it.y, midPoint.x, midPoint.y)
                }
            }
            oldPoint = point
        }
        oldPoint?.let { this.lineTo(it.x, oldPoint.y) }
    }
}

private fun calculateMidpoint(start: Offset, end: Offset) =
    Offset((start.x + end.x) / 2, (start.y + end.y) / 2)
