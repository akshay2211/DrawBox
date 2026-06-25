package io.ak1.drawbox.domain.usecase

import androidx.compose.ui.graphics.Color
import io.ak1.drawbox.domain.model.Element
import io.ak1.drawbox.domain.model.ShapeType
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.sqrt

object SvgExporter {

    fun exportToSvg(
        elements: List<Element>,
        width: Int = 1000,
        height: Int = 1000
    ): String {
        val svgElements = mutableListOf<String>()

        // Calculate bounds from all elements
        val bounds = calculateBounds(elements)
        val padding = 20f

        val viewBoxX = bounds.left - padding
        val viewBoxY = bounds.top - padding
        val viewBoxWidth = bounds.width + padding * 2
        val viewBoxHeight = bounds.height + padding * 2

        // Sort elements by zIndex and convert each to SVG
        elements.sortedBy { it.zIndex }.forEach { element ->
            when (element) {
                is Element.Path -> svgElements.add(pathToSvg(element))
                is Element.Shape -> svgElements.add(shapeToSvg(element))
            }
        }

        return buildSvgDocument(svgElements, viewBoxX, viewBoxY, viewBoxWidth, viewBoxHeight)
    }

    private fun calculateBounds(elements: List<Element>): Bounds {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        elements.forEach { element ->
            when (element) {
                is Element.Path -> {
                    element.samples.forEach { sample ->
                        minX = kotlin.math.min(minX, sample.position.x)
                        minY = kotlin.math.min(minY, sample.position.y)
                        maxX = kotlin.math.max(maxX, sample.position.x)
                        maxY = kotlin.math.max(maxY, sample.position.y)
                    }
                }
                is Element.Shape -> {
                    element.points.forEach { point ->
                        minX = kotlin.math.min(minX, point.x)
                        minY = kotlin.math.min(minY, point.y)
                        maxX = kotlin.math.max(maxX, point.x)
                        maxY = kotlin.math.max(maxY, point.y)
                    }
                }
            }
        }

        return if (minX == Float.MAX_VALUE) {
            Bounds(0f, 0f, 1000f, 1000f)
        } else {
            Bounds(minX, minY, maxX - minX, maxY - minY)
        }
    }

    private fun buildSvgDocument(
        elements: List<String>,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ): String {
        val content = elements.joinToString("\n  ")
        return """<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" viewBox="$x $y $width $height">
  $content
</svg>"""
    }

    private fun pathToSvg(path: Element.Path): String {
        if (path.samples.isEmpty()) return ""

        val color = colorToHex(path.strokeColor)
        val opacity = path.alpha
        val positions = path.samples.map { it.position }

        // Uniform-width fast path: single <path> with the existing smooth
        // quadratic curve and one stroke-width.
        val firstWidth = path.samples.first().width
        val uniform = path.samples.all { it.width == firstWidth }
        if (uniform) {
            val pathData = buildSmoothPathData(positions)
            return """<path d="$pathData" stroke="$color" stroke-width="$firstWidth" fill="none" stroke-linecap="round" stroke-linejoin="round" opacity="$opacity"/>"""
        }

        // Variable-width path (pen pressure). SVG `<path>` only carries one
        // stroke-width per element, so emit one `<line>` per segment with the
        // average of the two endpoint widths. Round caps merge the segments
        // visually. Larger output, lossless visual.
        val segments = StringBuilder()
        for (i in 0 until path.samples.size - 1) {
            val a = path.samples[i]
            val b = path.samples[i + 1]
            val w = (a.width + b.width) * 0.5f
            segments.append(
                """<line x1="${a.position.x}" y1="${a.position.y}" x2="${b.position.x}" y2="${b.position.y}" stroke="$color" stroke-width="$w" stroke-linecap="round" opacity="$opacity"/>""",
            )
        }
        return """<g>$segments</g>"""
    }

    private fun buildSmoothPathData(points: List<Offset>): String {
        if (points.isEmpty()) return ""
        if (points.size == 1) return "M ${points[0].x} ${points[0].y}"

        val pathParts = mutableListOf<String>()

        // Start at first point
        pathParts.add("M ${points[0].x} ${points[0].y}")

        // Use quadratic Bézier curves for smooth interpolation
        for (i in 1 until points.size) {
            val current = points[i]
            val previous = points[i - 1]

            if (i == 1) {
                // For first segment, just line to avoid control point issues
                pathParts.add("L ${current.x} ${current.y}")
            } else {
                // Use quadratic Bézier with control point at the previous point
                // This creates a smooth curve that passes through all points
                pathParts.add("Q ${previous.x} ${previous.y} ${current.x} ${current.y}")
            }
        }

        return pathParts.joinToString(" ")
    }

    private fun shapeToSvg(shape: Element.Shape): String {
        if (shape.points.size < 2) return ""

        val start = shape.points[0]
        val end = shape.points[1]
        val color = colorToHex(shape.strokeColor)
        val strokeWidth = shape.strokeWidth
        val fillColor = if (shape.fillColor != null) colorToHex(shape.fillColor) else "none"
        val fillAttr = if (shape.fillColor != null) """fill="$fillColor"""" else """fill="none""""

        return when (shape.shapeType) {
            ShapeType.RECTANGLE -> rectangleToSvg(start, end, color, strokeWidth, fillAttr)
            ShapeType.CIRCLE -> circleToSvg(start, end, color, strokeWidth, fillAttr)
            ShapeType.TRIANGLE -> triangleToSvg(start, end, color, strokeWidth, fillAttr)
            ShapeType.ARROW -> arrowToSvg(start, end, color, strokeWidth)
            ShapeType.LINE -> lineToSvg(start, end, color, strokeWidth)
        }
    }

    private fun rectangleToSvg(
        start: Offset,
        end: Offset,
        color: String,
        strokeWidth: Float,
        fillAttr: String
    ): String {
        val x = kotlin.math.min(start.x, end.x)
        val y = kotlin.math.min(start.y, end.y)
        val width = abs(end.x - start.x)
        val height = abs(end.y - start.y)

        return """<rect x="$x" y="$y" width="$width" height="$height" stroke="$color" stroke-width="$strokeWidth" $fillAttr/>"""
    }

    private fun circleToSvg(
        start: Offset,
        end: Offset,
        color: String,
        strokeWidth: Float,
        fillAttr: String
    ): String {
        val cx = (start.x + end.x) / 2
        val cy = (start.y + end.y) / 2
        val radius = sqrt(((end.x - start.x) / 2) * ((end.x - start.x) / 2) + ((end.y - start.y) / 2) * ((end.y - start.y) / 2))

        return """<circle cx="$cx" cy="$cy" r="$radius" stroke="$color" stroke-width="$strokeWidth" $fillAttr/>"""
    }

    private fun triangleToSvg(
        start: Offset,
        end: Offset,
        color: String,
        strokeWidth: Float,
        fillAttr: String
    ): String {
        val centerX = (start.x + end.x) / 2
        val baseY = kotlin.math.max(start.y, end.y)
        val topY = kotlin.math.min(start.y, end.y)
        val width = abs(end.x - start.x)

        val points = listOf(
            "$centerX,$topY",
            "${centerX - width / 2},${baseY}",
            "${centerX + width / 2},${baseY}"
        ).joinToString(" ")

        return """<polygon points="$points" stroke="$color" stroke-width="$strokeWidth" $fillAttr/>"""
    }

    private fun arrowToSvg(
        start: Offset,
        end: Offset,
        color: String,
        strokeWidth: Float
    ): String {
        val arrowHeadSize = kotlin.math.max(strokeWidth * 2, 10f)
        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = sqrt(dx * dx + dy * dy)

        if (length == 0f) return ""

        val unitX = dx / length
        val unitY = dy / length

        val arrowX1 = end.x - unitX * arrowHeadSize
        val arrowY1 = end.y - unitY * arrowHeadSize

        val perpX = -unitY * arrowHeadSize / 2
        val perpY = unitX * arrowHeadSize / 2

        val arrowHeadPoints = listOf(
            "${end.x},${end.y}",
            "${arrowX1 + perpX},${arrowY1 + perpY}",
            "${arrowX1 - perpX},${arrowY1 - perpY}"
        ).joinToString(" ")

        val lineSvg = """<line x1="${start.x}" y1="${start.y}" x2="${end.x}" y2="${end.y}" stroke="$color" stroke-width="$strokeWidth" stroke-linecap="round"/>"""
        val headSvg = """<polygon points="$arrowHeadPoints" fill="$color"/>"""

        return "$lineSvg\n  $headSvg"
    }

    private fun lineToSvg(
        start: Offset,
        end: Offset,
        color: String,
        strokeWidth: Float
    ): String {
        return """<line x1="${start.x}" y1="${start.y}" x2="${end.x}" y2="${end.y}" stroke="$color" stroke-width="$strokeWidth" stroke-linecap="round"/>"""
    }

    private fun colorToHex(color: Color): String {
        val r = (color.red * 255).toInt().toString(16).padStart(2, '0')
        val g = (color.green * 255).toInt().toString(16).padStart(2, '0')
        val b = (color.blue * 255).toInt().toString(16).padStart(2, '0')
        return "#$r$g$b"
    }

    private data class Bounds(
        val left: Float,
        val top: Float,
        val width: Float,
        val height: Float
    )
}
