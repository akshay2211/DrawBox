package io.ak1.drawbox

import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path


fun createPath(points: List<Offset>) = Path().apply {
    if (points.size > 1) {
        var oldPoint: Offset? = null
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            val point = points[i]
            oldPoint?.let {
                val midPoint = calculateMidpoint(it, point)
                if (i == 1) {
                    lineTo(midPoint.x, midPoint.y)
                } else {
                    quadraticTo(it.x, it.y, midPoint.x, midPoint.y)
                }
            }
            oldPoint = point
        }
        oldPoint?.let { lineTo(it.x, it.y) }
    }
}

private fun calculateMidpoint(start: Offset, end: Offset) = Offset((start.x + end.x) / 2, (start.y + end.y) / 2)
