package io.ak1.drawboxsample.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun icon(name: String, build: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(build).build()

private fun ImageVector.Builder.stroke(block: PathBuilder.() -> Unit) {
    path(
        stroke = SolidColor(Color.White),
        strokeLineWidth = 1f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = block,
    )
}

object DrawBoxIcons {

    val Download: ImageVector = icon("Download") {
        stroke {
            moveTo(21f, 15f); verticalLineToRelative(4f)
            arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, 2f)
            horizontalLineTo(5f)
            arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, -2f)
            verticalLineToRelative(-4f)
        }
        stroke {
            moveTo(7f, 10f); lineToRelative(5f, 5f); lineToRelative(5f, -5f)
        }
        stroke {
            moveTo(12f, 15f); lineTo(12f, 3f)
        }
    }

    val Undo: ImageVector = icon("Undo") {
        stroke {
            moveTo(9f, 14f); lineToRelative(-5f, -5f); lineToRelative(5f, -5f)
        }
        stroke {
            moveTo(20f, 20f); verticalLineToRelative(-7f)
            arcToRelative(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = false, -4f, -4f)
            horizontalLineTo(4f)
        }
    }

    val Redo: ImageVector = icon("Redo") {
        stroke {
            moveTo(15f, 14f); lineToRelative(5f, -5f); lineToRelative(-5f, -5f)
        }
        stroke {
            moveTo(4f, 20f); verticalLineToRelative(-7f)
            arcToRelative(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 4f, -4f)
            horizontalLineTo(20f)
        }
    }

    val Refresh: ImageVector = icon("Refresh") {
        stroke {
            moveTo(1f, 4f); lineTo(1f, 10f); lineTo(7f, 10f)
        }
        stroke {
            moveTo(23f, 20f); lineTo(23f, 14f); lineTo(17f, 14f)
        }
        stroke {
            moveTo(20.49f, 9f)
            arcToRelative(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = false, -14.85f, -3.36f)
            lineTo(1f, 10f)
            moveToRelative(22f, 4f)
            lineToRelative(-4.64f, 4.36f)
            arcToRelative(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, -14.85f, -3.36f)
        }
    }

    val Size: ImageVector = icon("Size") {
        stroke {
            moveTo(12f, 20f); lineTo(12f, 10f)
        }
        stroke {
            moveTo(18f, 20f); lineTo(18f, 4f)
        }
        stroke {
            moveTo(6f, 20f); lineTo(6f, 16f)
        }
    }

    val Color: ImageVector = icon("Color") {
        path(
            fill = SolidColor(androidx.compose.ui.graphics.Color.White),
            stroke = SolidColor(androidx.compose.ui.graphics.Color.White),
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(12f, 2f)
            arcToRelative(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 0f, 20f)
            arcToRelative(10f, 10f, 0f, isMoreThanHalf = true, isPositiveArc = true, 0f, -20f)
            close()
        }
    }
}