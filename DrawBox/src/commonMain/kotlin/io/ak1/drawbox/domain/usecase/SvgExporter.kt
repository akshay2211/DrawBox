@file:OptIn(ExperimentalEncodingApi::class)

package io.ak1.drawbox.domain.usecase

import androidx.compose.ui.graphics.Color
import io.ak1.drawbox.domain.model.Element
import io.ak1.drawbox.domain.model.ShapeType
import io.ak1.drawbox.domain.model.StrokeStyle
import androidx.compose.ui.geometry.Offset
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
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
                is Element.Image -> svgElements.add(imageToSvg(element))
                is Element.Text -> svgElements.add(textToSvg(element))
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
                is Element.Image -> {
                    element.points.forEach { point ->
                        minX = kotlin.math.min(minX, point.x)
                        minY = kotlin.math.min(minY, point.y)
                        maxX = kotlin.math.max(maxX, point.x)
                        maxY = kotlin.math.max(maxY, point.y)
                    }
                }
                is Element.Text -> {
                    val left = element.topLeft.x
                    val top = element.topLeft.y
                    val right = left + element.wrapWidth
                    val bottom = top + element.measuredHeight
                    minX = kotlin.math.min(minX, left)
                    minY = kotlin.math.min(minY, top)
                    maxX = kotlin.math.max(maxX, right)
                    maxY = kotlin.math.max(maxY, bottom)
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
        val strokeOn = shape.strokeEnabled && shape.strokeWidth > 0f
        val color = if (strokeOn) colorToHex(shape.strokeColor) else "none"
        val strokeWidth = if (strokeOn) shape.strokeWidth else 0f
        val fillColor = if (shape.fillColor != null) colorToHex(shape.fillColor) else "none"
        val fillAttr = if (shape.fillColor != null) """fill="$fillColor"""" else """fill="none""""
        val dashAttr = strokeDashArrayAttr(shape.strokeStyle, shape.strokeWidth)

        val inner = when (shape.shapeType) {
            ShapeType.RECTANGLE -> rectangleToSvg(
                start, end, color, strokeWidth, fillAttr,
                cornerRadius = shape.cornerRadius, dashAttr = dashAttr,
            )
            ShapeType.CIRCLE -> circleToSvg(start, end, color, strokeWidth, fillAttr, dashAttr)
            ShapeType.TRIANGLE -> triangleToSvg(
                start, end, color, strokeWidth, fillAttr,
                cornerRadius = shape.cornerRadius, dashAttr = dashAttr,
            )
            ShapeType.ARROW -> arrowToSvg(
                start, end, colorToHex(shape.strokeColor), shape.strokeWidth,
                bend = shape.bend, dashAttr = dashAttr,
            )
            ShapeType.LINE -> lineToSvg(
                start, end, colorToHex(shape.strokeColor), shape.strokeWidth,
                bend = shape.bend, dashAttr = dashAttr,
            )
        }

        return if (shape.rotation != 0f) {
            val center = shape.unrotatedCenter()
            """<g transform="rotate(${shape.rotation}, ${center.first}, ${center.second})">$inner</g>"""
        } else inner
    }

    /**
     * Center point for SVG rotation. For RECTANGLE / TRIANGLE / ARROW / LINE
     * the AABB center matches the renderer's `bounds().center`; for CIRCLE
     * the center is the midpoint of the two diameter endpoints. Both reduce
     * to `((start + end) / 2)` since the renderer's pivot is the bbox center.
     */
    private fun Element.Shape.unrotatedCenter(): Pair<Float, Float> {
        val s = points[0]; val e = points[1]
        return (s.x + e.x) * 0.5f to (s.y + e.y) * 0.5f
    }

    /**
     * SVG `stroke-dasharray` mirroring DrawBox.kt's path effect: on / off
     * pixels are multiples of [strokeWidth]. SOLID returns empty so the
     * attribute is omitted (default SVG behavior is solid).
     */
    private fun strokeDashArrayAttr(style: StrokeStyle, strokeWidth: Float): String {
        if (style == StrokeStyle.SOLID || strokeWidth <= 0f) return ""
        val on: Float
        val off: Float
        when (style) {
            StrokeStyle.DASHED -> { on = strokeWidth * 4f; off = strokeWidth * 2f }
            StrokeStyle.DOTTED -> { on = strokeWidth * 0.5f; off = strokeWidth * 2f }
            StrokeStyle.SOLID -> return ""
        }
        return """ stroke-dasharray="$on,$off""""
    }

    private fun rectangleToSvg(
        start: Offset,
        end: Offset,
        color: String,
        strokeWidth: Float,
        fillAttr: String,
        cornerRadius: Float,
        dashAttr: String,
    ): String {
        val x = min(start.x, end.x)
        val y = min(start.y, end.y)
        val width = abs(end.x - start.x)
        val height = abs(end.y - start.y)
        // Mirror DrawBox.kt's drawRoundRect clamp: corner radius can't
        // exceed half the shorter side or the corners collide.
        val r = cornerRadius.coerceAtMost(min(width, height) * 0.5f).coerceAtLeast(0f)
        val rxAttr = if (r > 0f) """ rx="$r" ry="$r"""" else ""
        return """<rect x="$x" y="$y" width="$width" height="$height"$rxAttr stroke="$color" stroke-width="$strokeWidth"$dashAttr $fillAttr/>"""
    }

    private fun circleToSvg(
        start: Offset,
        end: Offset,
        color: String,
        strokeWidth: Float,
        fillAttr: String,
        dashAttr: String,
    ): String {
        val cx = (start.x + end.x) / 2
        val cy = (start.y + end.y) / 2
        val radius = sqrt(((end.x - start.x) / 2) * ((end.x - start.x) / 2) + ((end.y - start.y) / 2) * ((end.y - start.y) / 2))

        return """<circle cx="$cx" cy="$cy" r="$radius" stroke="$color" stroke-width="$strokeWidth"$dashAttr $fillAttr/>"""
    }

    private fun triangleToSvg(
        start: Offset,
        end: Offset,
        color: String,
        strokeWidth: Float,
        fillAttr: String,
        cornerRadius: Float,
        dashAttr: String,
    ): String {
        val centerX = (start.x + end.x) / 2
        val baseY = max(start.y, end.y)
        val topY = min(start.y, end.y)
        val width = abs(end.x - start.x)

        val apex = Offset(centerX, topY)
        val br = Offset(centerX + width / 2, baseY)
        val bl = Offset(centerX - width / 2, baseY)

        if (cornerRadius <= 0f) {
            val pts = "${apex.x},${apex.y} ${bl.x},${bl.y} ${br.x},${br.y}"
            return """<polygon points="$pts" stroke="$color" stroke-width="$strokeWidth"$dashAttr $fillAttr/>"""
        }

        // Rounded triangle — replace each vertex with a quadratic-bezier
        // arc, mirroring DrawBox.kt's `roundedTrianglePath`. Clamp radius to
        // half the shortest edge so adjacent tangent points don't cross.
        val verts = listOf(apex, br, bl)
        val edges = listOf(
            distance(verts[0], verts[1]),
            distance(verts[1], verts[2]),
            distance(verts[2], verts[0]),
        )
        val r = cornerRadius.coerceAtMost((edges.minOrNull() ?: 0f) * 0.5f)
        val t1 = Array(3) { Offset.Zero }
        val t2 = Array(3) { Offset.Zero }
        for (i in 0..2) {
            val prev = verts[(i + 2) % 3]
            val curr = verts[i]
            val next = verts[(i + 1) % 3]
            val toPrev = normalize(prev - curr)
            val toNext = normalize(next - curr)
            t1[i] = curr + toPrev * r
            t2[i] = curr + toNext * r
        }
        val d = buildString {
            append("M ${t2[0].x} ${t2[0].y}")
            for (i in 0..2) {
                val ni = (i + 1) % 3
                append(" L ${t1[ni].x} ${t1[ni].y}")
                append(" Q ${verts[ni].x} ${verts[ni].y} ${t2[ni].x} ${t2[ni].y}")
            }
            append(" Z")
        }
        return """<path d="$d" stroke="$color" stroke-width="$strokeWidth"$dashAttr $fillAttr/>"""
    }

    private fun arrowToSvg(
        start: Offset,
        end: Offset,
        color: String,
        strokeWidth: Float,
        bend: Offset,
        dashAttr: String,
    ): String {
        val arrowHeadSize = max(strokeWidth * 2, 10f)

        val bodySvg: String
        val tangentX: Float
        val tangentY: Float

        if (bend == Offset.Zero) {
            val dx = end.x - start.x
            val dy = end.y - start.y
            val length = sqrt(dx * dx + dy * dy)
            if (length == 0f) return ""
            // Shorten body so it doesn't poke through the filled arrowhead.
            val arrowDepth = arrowHeadSize * cos(PI.toFloat() / 6f)
            val lineEndX = end.x - dx / length * arrowDepth
            val lineEndY = end.y - dy / length * arrowDepth
            bodySvg = """<line x1="${start.x}" y1="${start.y}" x2="$lineEndX" y2="$lineEndY" stroke="$color" stroke-width="$strokeWidth" stroke-linecap="round"$dashAttr/>"""
            tangentX = dx; tangentY = dy
        } else {
            // Quadratic bezier body. Tangent at t=1 is `2 * (end - control)`
            // (proportional — atan2 only cares about direction).
            val midX = (start.x + end.x) * 0.5f
            val midY = (start.y + end.y) * 0.5f
            val cx = midX + bend.x
            val cy = midY + bend.y
            bodySvg = """<path d="M ${start.x} ${start.y} Q $cx $cy ${end.x} ${end.y}" stroke="$color" stroke-width="$strokeWidth" stroke-linecap="round" fill="none"$dashAttr/>"""
            tangentX = end.x - cx; tangentY = end.y - cy
        }

        val angle = atan2(tangentY, tangentX)
        val p1x = end.x - arrowHeadSize * cos(angle - PI.toFloat() / 6f)
        val p1y = end.y - arrowHeadSize * sin(angle - PI.toFloat() / 6f)
        val p2x = end.x - arrowHeadSize * cos(angle + PI.toFloat() / 6f)
        val p2y = end.y - arrowHeadSize * sin(angle + PI.toFloat() / 6f)

        val headSvg = """<polygon points="${end.x},${end.y} $p1x,$p1y $p2x,$p2y" fill="$color"/>"""
        return "$bodySvg\n  $headSvg"
    }

    private fun distance(a: Offset, b: Offset): Float {
        val dx = a.x - b.x; val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun normalize(v: Offset): Offset {
        val len = sqrt(v.x * v.x + v.y * v.y)
        return if (len > 0f) Offset(v.x / len, v.y / len) else Offset.Zero
    }

    private fun textToSvg(text: Element.Text): String {
        if (text.text.isEmpty()) return ""
        val x = text.topLeft.x
        val y = text.topLeft.y
        val w = text.wrapWidth
        val h = text.measuredHeight
        val anchor = when (text.alignment) {
            io.ak1.drawbox.domain.model.TextAlignment.LEFT -> "start"
            io.ak1.drawbox.domain.model.TextAlignment.CENTER -> "middle"
            io.ak1.drawbox.domain.model.TextAlignment.RIGHT -> "end"
        }
        val anchorX = when (text.alignment) {
            io.ak1.drawbox.domain.model.TextAlignment.LEFT -> x
            io.ak1.drawbox.domain.model.TextAlignment.CENTER -> x + w * 0.5f
            io.ak1.drawbox.domain.model.TextAlignment.RIGHT -> x + w
        }
        val color = colorToHex(text.color)
        val opacityAttr = if (text.opacity != 1f) """ opacity="${text.opacity}"""" else ""
        val transformAttr = if (text.rotation != 0f) {
            val cx = x + w * 0.5f
            val cy = y + h * 0.5f
            """ transform="rotate(${text.rotation}, $cx, $cy)""""
        } else ""
        // SVG doesn't natively support box-driven wrapping. Emit one <tspan>
        // per visual line, splitting on hard `\n` and greedy word-wrap. This
        // is intentionally lossy for re-import (the wrap rect isn't
        // recoverable from the visual layout) — JSON is the lossless format.
        val lineHeight = text.fontSize * 1.25f
        val lines = wrapTextForSvg(text.text, w, text.fontSize, text.fontFamilyKey)
        val tspans = lines.mapIndexed { i, line ->
            val dy = if (i == 0) text.fontSize else lineHeight
            """<tspan x="$anchorX" dy="$dy">${escapeXml(line)}</tspan>"""
        }.joinToString("")
        return """<text x="$anchorX" y="$y" font-family="${escapeXmlAttr(text.fontFamilyKey)}" font-size="${text.fontSize}" fill="$color" text-anchor="$anchor"$opacityAttr$transformAttr>$tspans</text>"""
    }

    /**
     * Cheap line-wrap approximation for SVG export. Honors hard `\n` breaks;
     * long words break at the character limit. Lossless round-trip happens
     * through JSON, not SVG.
     *
     * Per-family multipliers — monospace glyphs are uniformly wider than
     * proportional ones (`~0.6 × em` vs `~0.55 × em` average), so a single
     * constant under-estimates mono line lengths and lets through a
     * `println("Hello, World!")` line that the renderer's `TextMeasurer`
     * actually wraps. Aligning the multiplier with family keeps the SVG
     * `<tspan>` breaks consistent with the renderer's measured wraps.
     */
    private fun wrapTextForSvg(
        text: String,
        widthWorld: Float,
        fontSize: Float,
        fontFamilyKey: String,
    ): List<String> {
        if (widthWorld <= 0f || fontSize <= 0f) return text.split('\n')
        val charWidthMul = when (fontFamilyKey) {
            io.ak1.drawbox.domain.model.BuiltinFontFamilyKeys.MONO -> 0.6f
            else -> 0.55f
        }
        val approxCharWidth = fontSize * charWidthMul
        val charsPerLine = (widthWorld / approxCharWidth).toInt().coerceAtLeast(1)
        val out = mutableListOf<String>()
        for (hardLine in text.split('\n')) {
            if (hardLine.length <= charsPerLine) {
                out += hardLine
                continue
            }
            // Preserve leading whitespace so an indented code block keeps
            // its indent on the first wrapped line — matches the renderer's
            // `TextMeasurer` which doesn't drop leading spaces. Without
            // this, `  println(...)` (26 chars) tokenizes to two empty
            // words + `println(...)` (24 chars), fits inside the limit,
            // and the SVG never wraps even though the PNG does.
            val leadingWs = hardLine.takeWhile { it == ' ' || it == '\t' }
            val rest = hardLine.drop(leadingWs.length)
            var current = StringBuilder(leadingWs)
            for (word in rest.split(' ')) {
                // No separator before the first content word (it would
                // become an extra space after the indent); space between
                // subsequent words.
                val sep = if (current.length <= leadingWs.length) "" else " "
                val prospective = current.toString() + sep + word
                if (prospective.length <= charsPerLine) {
                    current = StringBuilder(prospective)
                } else {
                    if (current.isNotEmpty()) {
                        out += current.toString()
                        current = StringBuilder()
                    }
                    var rest2 = word
                    while (rest2.length > charsPerLine) {
                        out += rest2.substring(0, charsPerLine)
                        rest2 = rest2.substring(charsPerLine)
                    }
                    current = StringBuilder(rest2)
                }
            }
            if (current.isNotEmpty()) out += current.toString()
        }
        return out
    }

    private fun escapeXml(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")

    private fun escapeXmlAttr(s: String): String = escapeXml(s)
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun imageToSvg(image: Element.Image): String {
        if (image.points.size < 2) return ""
        val topLeft = image.points[0]
        val bottomRight = image.points[1]
        val x = kotlin.math.min(topLeft.x, bottomRight.x)
        val y = kotlin.math.min(topLeft.y, bottomRight.y)
        val w = abs(bottomRight.x - topLeft.x)
        val h = abs(bottomRight.y - topLeft.y)
        // Best-effort MIME sniffing: peek at the file's magic bytes so the SVG
        // data URI declares the right type. SVG viewers tolerate "image/png"
        // for everything but Inkscape and Figma reject it for JPEG/WebP.
        val mime = sniffMime(image.bytes)
        val base64 = Base64.encode(image.bytes)
        val opacityAttr = if (image.opacity != 1f) """ opacity="${image.opacity}"""" else ""
        val transformAttr = if (image.rotation != 0f) {
            val cx = x + w * 0.5f
            val cy = y + h * 0.5f
            """ transform="rotate(${image.rotation}, $cx, $cy)""""
        } else ""
        return """<image x="$x" y="$y" width="$w" height="$h" href="data:$mime;base64,$base64"$opacityAttr$transformAttr/>"""
    }

    /**
     * Identify common image formats by their magic-byte signatures so SVG
     * data URIs carry an accurate MIME type. Defaults to PNG when the bytes
     * don't match a recognized signature — PNG is the most widely supported
     * fallback and matches what BitmapFactory / skia emit by default for
     * synthetic content.
     */
    private fun sniffMime(bytes: ByteArray): String {
        if (bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
        ) return "image/png"
        if (bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() &&
            bytes[2] == 0xFF.toByte()
        ) return "image/jpeg"
        if (bytes.size >= 4 &&
            bytes[0] == 0x47.toByte() && bytes[1] == 0x49.toByte() &&
            bytes[2] == 0x46.toByte() && bytes[3] == 0x38.toByte()
        ) return "image/gif"
        if (bytes.size >= 12 &&
            bytes[0] == 0x52.toByte() && bytes[1] == 0x49.toByte() &&
            bytes[2] == 0x46.toByte() && bytes[3] == 0x46.toByte() &&
            bytes[8] == 0x57.toByte() && bytes[9] == 0x45.toByte() &&
            bytes[10] == 0x42.toByte() && bytes[11] == 0x50.toByte()
        ) return "image/webp"
        return "image/png"
    }

    private fun lineToSvg(
        start: Offset,
        end: Offset,
        color: String,
        strokeWidth: Float,
        bend: Offset,
        dashAttr: String,
    ): String {
        return if (bend == Offset.Zero) {
            """<line x1="${start.x}" y1="${start.y}" x2="${end.x}" y2="${end.y}" stroke="$color" stroke-width="$strokeWidth" stroke-linecap="round"$dashAttr/>"""
        } else {
            // Quadratic bezier control = midpoint + bend, matching DrawBox.kt.
            val cx = (start.x + end.x) * 0.5f + bend.x
            val cy = (start.y + end.y) * 0.5f + bend.y
            """<path d="M ${start.x} ${start.y} Q $cx $cy ${end.x} ${end.y}" stroke="$color" stroke-width="$strokeWidth" stroke-linecap="round" fill="none"$dashAttr/>"""
        }
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
