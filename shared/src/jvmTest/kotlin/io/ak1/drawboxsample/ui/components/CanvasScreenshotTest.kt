package io.ak1.drawboxsample.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import io.ak1.drawbox.DrawingPreview
import io.ak1.drawbox.domain.model.Element
import io.ak1.drawbox.domain.model.ShapeType
import io.ak1.drawbox.domain.model.StrokeStyle
import io.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test

/**
 * Visual regression coverage for the DrawBox canvas renderer.
 *
 * Uses [DrawingPreview] — a gesture-free composable that renders elements
 * straight through the same per-element draw paths the live canvas uses.
 * Tests seed deterministic element lists, capture via Roborazzi, and compare
 * against `src/jvmTest/snapshots/Canvas_*.png`.
 *
 * Failure modes this catches:
 * - Pen-pressure variable-width strokes regressing to uniform width.
 * - Shape outline/fill rendering regressions.
 * - Stroke style (DASHED/DOTTED) regressions.
 * - Bend / curve regressions on arrows.
 * - Z-order regressions in composite scenes.
 */
class CanvasScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ---------- Helpers ----------

    private fun uniformPath(
        strokeWidth: Float,
        color: Color = Color.Black,
        vararg points: Pair<Float, Float>,
    ): Element.Path = Element.Path(
        samples = points.map { (x, y) ->
            Element.PathSample(Offset(x, y), strokeWidth)
        },
        strokeColor = color,
        strokeWidth = strokeWidth,
        alpha = 1f,
    )

    /**
     * Build a path whose width tapers from [startWidth] at the first sample to
     * [endWidth] at the last, linearly. Simulates a pressure-varied stroke.
     */
    private fun taperedPath(
        startWidth: Float,
        endWidth: Float,
        color: Color = Color.Black,
        vararg points: Pair<Float, Float>,
    ): Element.Path = Element.Path(
        samples = points.mapIndexed { i, (x, y) ->
            val t = if (points.size <= 1) 0f else i.toFloat() / (points.size - 1)
            val w = startWidth + (endWidth - startWidth) * t
            Element.PathSample(Offset(x, y), w)
        },
        strokeColor = color,
        strokeWidth = startWidth,
        alpha = 1f,
    )

    private fun shape(
        type: ShapeType,
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        strokeColor: Color = Color.Black,
        fillColor: Color? = null,
        strokeWidth: Float = 4f,
        strokeStyle: StrokeStyle = StrokeStyle.SOLID,
        cornerRadius: Float = 0f,
        bend: Offset = Offset.Zero,
        rotation: Float = 0f,
    ): Element.Shape = Element.Shape(
        shapeType = type,
        points = listOf(Offset(startX, startY), Offset(endX, endY)),
        strokeColor = strokeColor,
        fillColor = fillColor,
        strokeWidth = strokeWidth,
        strokeStyle = strokeStyle,
        cornerRadius = cornerRadius,
        bend = bend,
        rotation = rotation,
    )

    @Composable
    private fun Canvas(elements: List<Element>) {
        // White canvas at a fixed pixel size keeps snapshots deterministic
        // across machines with different DPI scaling.
        DrawingPreview(
            elements = elements,
            bgColor = Color.White,
            modifier = Modifier
                .size(400.dp, 300.dp)
                .background(Color.White),
        )
    }

    private fun capture(name: String) {
        composeRule.onRoot().captureRoboImage(
            filePath = "src/jvmTest/snapshots/Canvas_$name.png",
        )
    }

    // ---------- Tests ----------

    @Test
    fun emptyCanvas() {
        composeRule.setContent { Canvas(elements = emptyList()) }
        capture("empty")
    }

    @Test
    fun uniformPenStroke() {
        // A simple sweep: 6 points, constant 8px width. Should render as a
        // smooth single-width curve.
        val path = uniformPath(
            strokeWidth = 8f,
            color = Color.Black,
            50f to 200f,
            100f to 150f,
            150f to 100f,
            200f to 80f,
            260f to 100f,
            330f to 150f,
        )
        composeRule.setContent { Canvas(elements = listOf(path)) }
        capture("uniform_pen_stroke")
    }

    @Test
    fun variableWidthPenStroke() {
        // Heavy-to-light taper: 12px at the start, 2px at the end. This is the
        // primary pen-pressure visual; locking it down here so a regression in
        // the per-segment width interpolation or the seed-conform fix gets
        // caught immediately.
        val path = taperedPath(
            startWidth = 12f,
            endWidth = 2f,
            color = Color(0xFF1565C0),
            60f to 220f,
            110f to 180f,
            160f to 140f,
            210f to 110f,
            260f to 90f,
            310f to 80f,
            350f to 80f,
        )
        composeRule.setContent { Canvas(elements = listOf(path)) }
        capture("variable_width_pen_stroke")
    }

    @Test
    fun variableWidthDashedPenStroke() {
        // Locks in dash + pen pressure composition: dash on/off lengths must
        // scale with the LOCAL stroke width — small dashes at the light end,
        // large dashes at the heavy end. Regression here would mean either
        // uniform-width dashes (lost pressure) or solid (lost dash style).
        val path = taperedPath(
            startWidth = 14f,
            endWidth = 3f,
            color = Color(0xFF1B5E20),
            60f to 220f,
            120f to 180f,
            180f to 140f,
            240f to 110f,
            300f to 90f,
            350f to 80f,
        ).copy(strokeStyle = StrokeStyle.DASHED)
        composeRule.setContent { Canvas(elements = listOf(path)) }
        capture("variable_width_dashed_pen_stroke")
    }

    @Test
    fun dashedStrokeWithZeroWidthSampleDoesNotHang() {
        // Pathological input: a sample with width 0 sitting between non-zero
        // samples. Without the safety floor in drawVariableWidthPath, the
        // dash-length math degenerates to 0 here and the walker spins
        // indefinitely. This test only needs to RETURN — capturing a snapshot
        // is enough proof the renderer didn't hang the test thread.
        val degenerate = Element.Path(
            samples = listOf(
                Element.PathSample(Offset(60f, 150f), 10f),
                Element.PathSample(Offset(150f, 150f), 0f),
                Element.PathSample(Offset(240f, 150f), 10f),
                Element.PathSample(Offset(330f, 150f), 6f),
            ),
            strokeColor = Color(0xFF0D47A1),
            strokeWidth = 10f,
            alpha = 1f,
            strokeStyle = StrokeStyle.DASHED,
        )
        composeRule.setContent { Canvas(elements = listOf(degenerate)) }
        capture("dashed_zero_width_safety")
    }

    @Test
    fun variableWidthDottedPenStroke() {
        // Same idea for dots: small dots at the light end, large dots at the
        // heavy end. With round caps the "on" intervals collapse to circles.
        val path = taperedPath(
            startWidth = 14f,
            endWidth = 3f,
            color = Color(0xFF4A148C),
            60f to 80f,
            120f to 110f,
            180f to 140f,
            240f to 170f,
            300f to 200f,
            350f to 220f,
        ).copy(strokeStyle = StrokeStyle.DOTTED)
        composeRule.setContent { Canvas(elements = listOf(path)) }
        capture("variable_width_dotted_pen_stroke")
    }

    @Test
    fun lightToHeavyPenStroke() {
        // Inverse taper: light start, heavy end. Catches regressions where
        // taper direction is silently reversed.
        val path = taperedPath(
            startWidth = 2f,
            endWidth = 12f,
            color = Color(0xFFB71C1C),
            50f to 80f,
            100f to 110f,
            150f to 150f,
            200f to 190f,
            260f to 210f,
            330f to 220f,
        )
        composeRule.setContent { Canvas(elements = listOf(path)) }
        capture("light_to_heavy_pen_stroke")
    }

    @Test
    fun strokedRectangle() {
        val rect = shape(
            type = ShapeType.RECTANGLE,
            startX = 60f, startY = 60f, endX = 340f, endY = 240f,
            strokeColor = Color.Black, strokeWidth = 4f,
        )
        composeRule.setContent { Canvas(elements = listOf(rect)) }
        capture("rectangle_stroked")
    }

    @Test
    fun filledRoundedRectangle() {
        val rect = shape(
            type = ShapeType.RECTANGLE,
            startX = 60f, startY = 60f, endX = 340f, endY = 240f,
            fillColor = Color(0xFFE57373),
            cornerRadius = 24f,
        )
        composeRule.setContent { Canvas(elements = listOf(rect)) }
        capture("rectangle_filled_rounded")
    }

    @Test
    fun strokedCircle() {
        val circle = shape(
            type = ShapeType.CIRCLE,
            startX = 100f, startY = 100f, endX = 300f, endY = 200f,
            strokeColor = Color.Black, strokeWidth = 4f,
        )
        composeRule.setContent { Canvas(elements = listOf(circle)) }
        capture("circle_stroked")
    }

    @Test
    fun strokedTriangle() {
        val tri = shape(
            type = ShapeType.TRIANGLE,
            startX = 80f, startY = 60f, endX = 320f, endY = 240f,
            strokeColor = Color.Black, strokeWidth = 4f,
        )
        composeRule.setContent { Canvas(elements = listOf(tri)) }
        capture("triangle_stroked")
    }

    @Test
    fun arrowWithBend() {
        // Curved connector. Bend lifts the midpoint above the straight line.
        val arrow = shape(
            type = ShapeType.ARROW,
            startX = 80f, startY = 220f, endX = 320f, endY = 220f,
            strokeColor = Color.Black, strokeWidth = 4f,
            bend = Offset(0f, -80f),
        )
        composeRule.setContent { Canvas(elements = listOf(arrow)) }
        capture("arrow_with_bend")
    }

    @Test
    fun dashedLine() {
        val line = shape(
            type = ShapeType.LINE,
            startX = 60f, startY = 60f, endX = 340f, endY = 240f,
            strokeColor = Color.Black, strokeWidth = 4f,
            strokeStyle = StrokeStyle.DASHED,
        )
        composeRule.setContent { Canvas(elements = listOf(line)) }
        capture("line_dashed")
    }

    // =================================================================
    // Shape variations — one element per snapshot for clear diff signal.
    // Covers the cartesian-ish product of: shape type × stroke style ×
    // fill/stroke × corner radius × rotation × stroke width × bend.
    // =================================================================

    // ----- Rectangle -----

    @Test
    fun rectangleStrokedDashed() {
        val r = shape(
            type = ShapeType.RECTANGLE,
            startX = 60f, startY = 60f, endX = 340f, endY = 240f,
            strokeColor = Color.Black, strokeWidth = 4f,
            strokeStyle = StrokeStyle.DASHED,
        )
        composeRule.setContent { Canvas(elements = listOf(r)) }
        capture("rectangle_stroked_dashed")
    }

    @Test
    fun rectangleStrokedDotted() {
        val r = shape(
            type = ShapeType.RECTANGLE,
            startX = 60f, startY = 60f, endX = 340f, endY = 240f,
            strokeColor = Color.Black, strokeWidth = 4f,
            strokeStyle = StrokeStyle.DOTTED,
        )
        composeRule.setContent { Canvas(elements = listOf(r)) }
        capture("rectangle_stroked_dotted")
    }

    @Test
    fun rectangleStrokedThick() {
        val r = shape(
            type = ShapeType.RECTANGLE,
            startX = 80f, startY = 80f, endX = 320f, endY = 220f,
            strokeColor = Color(0xFF1565C0), strokeWidth = 16f,
        )
        composeRule.setContent { Canvas(elements = listOf(r)) }
        capture("rectangle_stroked_thick")
    }

    @Test
    fun rectangleFilledSharp() {
        val r = shape(
            type = ShapeType.RECTANGLE,
            startX = 60f, startY = 60f, endX = 340f, endY = 240f,
            fillColor = Color(0xFF388E3C),
        )
        composeRule.setContent { Canvas(elements = listOf(r)) }
        capture("rectangle_filled_sharp")
    }

    @Test
    fun rectangleStrokedRounded() {
        val r = shape(
            type = ShapeType.RECTANGLE,
            startX = 60f, startY = 60f, endX = 340f, endY = 240f,
            strokeColor = Color.Black, strokeWidth = 4f,
            cornerRadius = 32f,
        )
        composeRule.setContent { Canvas(elements = listOf(r)) }
        capture("rectangle_stroked_rounded")
    }

    @Test
    fun rectangleRotated45Stroked() {
        val r = shape(
            type = ShapeType.RECTANGLE,
            startX = 100f, startY = 100f, endX = 300f, endY = 200f,
            strokeColor = Color.Black, strokeWidth = 4f,
            rotation = 45f,
        )
        composeRule.setContent { Canvas(elements = listOf(r)) }
        capture("rectangle_rotated_45_stroked")
    }

    @Test
    fun rectangleRotated45Filled() {
        val r = shape(
            type = ShapeType.RECTANGLE,
            startX = 100f, startY = 100f, endX = 300f, endY = 200f,
            fillColor = Color(0xFFAD1457),
            rotation = 45f,
        )
        composeRule.setContent { Canvas(elements = listOf(r)) }
        capture("rectangle_rotated_45_filled")
    }

    // ----- Circle -----

    @Test
    fun circleStrokedDashed() {
        val c = shape(
            type = ShapeType.CIRCLE,
            startX = 100f, startY = 100f, endX = 300f, endY = 200f,
            strokeColor = Color.Black, strokeWidth = 4f,
            strokeStyle = StrokeStyle.DASHED,
        )
        composeRule.setContent { Canvas(elements = listOf(c)) }
        capture("circle_stroked_dashed")
    }

    @Test
    fun circleStrokedDotted() {
        val c = shape(
            type = ShapeType.CIRCLE,
            startX = 100f, startY = 100f, endX = 300f, endY = 200f,
            strokeColor = Color.Black, strokeWidth = 4f,
            strokeStyle = StrokeStyle.DOTTED,
        )
        composeRule.setContent { Canvas(elements = listOf(c)) }
        capture("circle_stroked_dotted")
    }

    @Test
    fun circleStrokedThick() {
        val c = shape(
            type = ShapeType.CIRCLE,
            startX = 100f, startY = 100f, endX = 300f, endY = 200f,
            strokeColor = Color(0xFFEF6C00), strokeWidth = 14f,
        )
        composeRule.setContent { Canvas(elements = listOf(c)) }
        capture("circle_stroked_thick")
    }

    @Test
    fun circleFilled() {
        val c = shape(
            type = ShapeType.CIRCLE,
            startX = 100f, startY = 100f, endX = 300f, endY = 200f,
            fillColor = Color(0xFF6A1B9A),
        )
        composeRule.setContent { Canvas(elements = listOf(c)) }
        capture("circle_filled")
    }

    @Test
    fun circleDiagonalDiameter() {
        // Diameter endpoints along a diagonal; the inscribed circle's bbox
        // is larger than the points' AABB.
        val c = shape(
            type = ShapeType.CIRCLE,
            startX = 80f, startY = 80f, endX = 280f, endY = 220f,
            strokeColor = Color.Black, strokeWidth = 4f,
        )
        composeRule.setContent { Canvas(elements = listOf(c)) }
        capture("circle_diagonal_diameter")
    }

    // ----- Triangle -----

    @Test
    fun triangleStrokedDashed() {
        val t = shape(
            type = ShapeType.TRIANGLE,
            startX = 80f, startY = 60f, endX = 320f, endY = 240f,
            strokeColor = Color.Black, strokeWidth = 4f,
            strokeStyle = StrokeStyle.DASHED,
        )
        composeRule.setContent { Canvas(elements = listOf(t)) }
        capture("triangle_stroked_dashed")
    }

    @Test
    fun triangleStrokedDotted() {
        val t = shape(
            type = ShapeType.TRIANGLE,
            startX = 80f, startY = 60f, endX = 320f, endY = 240f,
            strokeColor = Color.Black, strokeWidth = 4f,
            strokeStyle = StrokeStyle.DOTTED,
        )
        composeRule.setContent { Canvas(elements = listOf(t)) }
        capture("triangle_stroked_dotted")
    }

    @Test
    fun triangleFilled() {
        val t = shape(
            type = ShapeType.TRIANGLE,
            startX = 80f, startY = 60f, endX = 320f, endY = 240f,
            fillColor = Color(0xFF2E7D32),
        )
        composeRule.setContent { Canvas(elements = listOf(t)) }
        capture("triangle_filled")
    }

    @Test
    fun triangleStrokedRounded() {
        val t = shape(
            type = ShapeType.TRIANGLE,
            startX = 80f, startY = 60f, endX = 320f, endY = 240f,
            strokeColor = Color.Black, strokeWidth = 4f,
            cornerRadius = 24f,
        )
        composeRule.setContent { Canvas(elements = listOf(t)) }
        capture("triangle_stroked_rounded")
    }

    @Test
    fun triangleFilledRounded() {
        val t = shape(
            type = ShapeType.TRIANGLE,
            startX = 80f, startY = 60f, endX = 320f, endY = 240f,
            fillColor = Color(0xFFD84315),
            cornerRadius = 24f,
        )
        composeRule.setContent { Canvas(elements = listOf(t)) }
        capture("triangle_filled_rounded")
    }

    @Test
    fun triangleRotated180Filled() {
        // Inverted triangle (apex at bottom).
        val t = shape(
            type = ShapeType.TRIANGLE,
            startX = 80f, startY = 60f, endX = 320f, endY = 240f,
            fillColor = Color(0xFF512DA8),
            rotation = 180f,
        )
        composeRule.setContent { Canvas(elements = listOf(t)) }
        capture("triangle_rotated_180_filled")
    }

    // ----- Arrow -----

    @Test
    fun arrowStraightThin() {
        val a = shape(
            type = ShapeType.ARROW,
            startX = 60f, startY = 150f, endX = 340f, endY = 150f,
            strokeColor = Color.Black, strokeWidth = 2f,
        )
        composeRule.setContent { Canvas(elements = listOf(a)) }
        capture("arrow_straight_thin")
    }

    @Test
    fun arrowStraightThick() {
        val a = shape(
            type = ShapeType.ARROW,
            startX = 60f, startY = 150f, endX = 340f, endY = 150f,
            strokeColor = Color(0xFFC62828), strokeWidth = 10f,
        )
        composeRule.setContent { Canvas(elements = listOf(a)) }
        capture("arrow_straight_thick")
    }

    @Test
    fun arrowDashedStraight() {
        val a = shape(
            type = ShapeType.ARROW,
            startX = 60f, startY = 150f, endX = 340f, endY = 150f,
            strokeColor = Color.Black, strokeWidth = 4f,
            strokeStyle = StrokeStyle.DASHED,
        )
        composeRule.setContent { Canvas(elements = listOf(a)) }
        capture("arrow_dashed_straight")
    }

    @Test
    fun arrowDottedStraight() {
        val a = shape(
            type = ShapeType.ARROW,
            startX = 60f, startY = 150f, endX = 340f, endY = 150f,
            strokeColor = Color.Black, strokeWidth = 4f,
            strokeStyle = StrokeStyle.DOTTED,
        )
        composeRule.setContent { Canvas(elements = listOf(a)) }
        capture("arrow_dotted_straight")
    }

    @Test
    fun arrowMildBend() {
        val a = shape(
            type = ShapeType.ARROW,
            startX = 60f, startY = 220f, endX = 340f, endY = 220f,
            strokeColor = Color.Black, strokeWidth = 4f,
            bend = Offset(0f, -40f),
        )
        composeRule.setContent { Canvas(elements = listOf(a)) }
        capture("arrow_mild_bend")
    }

    @Test
    fun arrowStrongBendDown() {
        val a = shape(
            type = ShapeType.ARROW,
            startX = 60f, startY = 80f, endX = 340f, endY = 80f,
            strokeColor = Color(0xFF1565C0), strokeWidth = 4f,
            bend = Offset(0f, 120f),
        )
        composeRule.setContent { Canvas(elements = listOf(a)) }
        capture("arrow_strong_bend_down")
    }

    @Test
    fun arrowVerticalUpward() {
        val a = shape(
            type = ShapeType.ARROW,
            startX = 200f, startY = 240f, endX = 200f, endY = 60f,
            strokeColor = Color.Black, strokeWidth = 4f,
        )
        composeRule.setContent { Canvas(elements = listOf(a)) }
        capture("arrow_vertical_upward")
    }

    @Test
    fun arrowDiagonal() {
        val a = shape(
            type = ShapeType.ARROW,
            startX = 60f, startY = 240f, endX = 340f, endY = 60f,
            strokeColor = Color.Black, strokeWidth = 4f,
        )
        composeRule.setContent { Canvas(elements = listOf(a)) }
        capture("arrow_diagonal")
    }

    // ----- Line -----

    @Test
    fun lineSolidThick() {
        val l = shape(
            type = ShapeType.LINE,
            startX = 60f, startY = 150f, endX = 340f, endY = 150f,
            strokeColor = Color.Black, strokeWidth = 10f,
        )
        composeRule.setContent { Canvas(elements = listOf(l)) }
        capture("line_solid_thick")
    }

    @Test
    fun lineDotted() {
        val l = shape(
            type = ShapeType.LINE,
            startX = 60f, startY = 150f, endX = 340f, endY = 150f,
            strokeColor = Color.Black, strokeWidth = 4f,
            strokeStyle = StrokeStyle.DOTTED,
        )
        composeRule.setContent { Canvas(elements = listOf(l)) }
        capture("line_dotted")
    }

    @Test
    fun lineCurved() {
        val l = shape(
            type = ShapeType.LINE,
            startX = 60f, startY = 200f, endX = 340f, endY = 200f,
            strokeColor = Color.Black, strokeWidth = 4f,
            bend = Offset(0f, -80f),
        )
        composeRule.setContent { Canvas(elements = listOf(l)) }
        capture("line_curved")
    }

    @Test
    fun lineCurvedDashed() {
        val l = shape(
            type = ShapeType.LINE,
            startX = 60f, startY = 200f, endX = 340f, endY = 200f,
            strokeColor = Color(0xFF6A1B9A), strokeWidth = 4f,
            strokeStyle = StrokeStyle.DASHED,
            bend = Offset(0f, -80f),
        )
        composeRule.setContent { Canvas(elements = listOf(l)) }
        capture("line_curved_dashed")
    }

    @Test
    fun lineDiagonal() {
        val l = shape(
            type = ShapeType.LINE,
            startX = 60f, startY = 240f, endX = 340f, endY = 60f,
            strokeColor = Color.Black, strokeWidth = 4f,
        )
        composeRule.setContent { Canvas(elements = listOf(l)) }
        capture("line_diagonal")
    }

    @Test
    fun compositeScene() {
        // Z-ordered scene combining every primitive. Catches z-order
        // regressions and cross-element rendering interference.
        val elements = listOf<Element>(
            shape(
                type = ShapeType.RECTANGLE,
                startX = 40f, startY = 40f, endX = 360f, endY = 260f,
                fillColor = Color(0xFFFFF59D),
                strokeWidth = 0f,
            ).copy(zIndex = 0),
            shape(
                type = ShapeType.CIRCLE,
                startX = 90f, startY = 90f, endX = 210f, endY = 210f,
                strokeColor = Color(0xFF1976D2),
                strokeWidth = 5f,
            ).copy(zIndex = 1),
            shape(
                type = ShapeType.TRIANGLE,
                startX = 220f, startY = 90f, endX = 330f, endY = 210f,
                strokeColor = Color(0xFF388E3C),
                strokeWidth = 5f,
            ).copy(zIndex = 2),
            uniformPath(
                strokeWidth = 6f,
                color = Color.Black,
                70f to 230f,
                130f to 220f,
                200f to 240f,
                270f to 215f,
                330f to 235f,
            ).copy(zIndex = 3),
            taperedPath(
                startWidth = 10f,
                endWidth = 2f,
                color = Color(0xFFB71C1C),
                60f to 60f,
                110f to 80f,
                160f to 100f,
                210f to 110f,
                260f to 105f,
            ).copy(zIndex = 4),
            shape(
                type = ShapeType.ARROW,
                startX = 60f, startY = 270f, endX = 340f, endY = 270f,
                strokeColor = Color(0xFF6A1B9A),
                strokeWidth = 3f,
                bend = Offset(0f, -20f),
            ).copy(zIndex = 5),
        )
        composeRule.setContent { Canvas(elements = elements) }
        capture("composite_scene")
    }
}
