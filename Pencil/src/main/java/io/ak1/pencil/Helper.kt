package io.ak1.pencil

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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

//Model
data class PathWrapper(val path: Path, val strokeWidth: Float = 5f, val strokeColor: Color)