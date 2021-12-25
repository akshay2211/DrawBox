package io.ak1.pencil

import android.view.MotionEvent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Created by akshay on 24/12/21
 * https://ak1.io
 */

fun setStrokeColor(color: Color) {
    strokeColor = color
}

fun setStrokeWidth(width: Float) {
    strokeWidth = width
}

fun setStrokeAlpha(alpha: Float) {
    strokeAlpha = alpha
}

fun getBitmap() = bitmap

fun DrawScope.drawSomePath(
    path: Path,
    color: Color = if (strokeAlpha == 1f) strokeColor else strokeColor.copy(
        strokeAlpha
    ),
    width: Float = strokeWidth
) = drawPath(
    path,
    color,
    style = Stroke(width, miter = 0f, join = StrokeJoin.Round, cap = StrokeCap.Round),
)

fun Canvas.drawSomePath(
    path: Path,
    color: Color = if (strokeAlpha == 1f) strokeColor else strokeColor.copy(
        strokeAlpha
    ),
    width: Float = strokeWidth
) = this.drawPath(path, Paint().apply {
    this.style = PaintingStyle.Stroke
    this.isAntiAlias = true
    this.color = color
    this.strokeJoin = StrokeJoin.Round
    this.strokeCap = StrokeCap.Round
    this.strokeWidth = width
})

fun MotionEvent.getRect() = Rect(this.x - 0.5f, this.y - 0.5f, this.x + 0.5f, this.y + 0.5f)


//Model
data class PathWrapper(val path: Path, val strokeWidth: Float = 5f, val strokeColor: Color)