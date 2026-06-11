package io.ak1.drawbox.domain.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GeometryTest {

    private fun pathOf(vararg pts: Pair<Float, Float>): Element.Path = Element.Path(
        points = pts.map { Offset(it.first, it.second) },
        strokeColor = Color.Red,
        strokeWidth = 4f,
        alpha = 1f,
    )

    private fun rectShape(
        l: Float, t: Float, r: Float, b: Float,
        filled: Boolean = false,
    ): Element.Shape = Element.Shape(
        shapeType = ShapeType.RECTANGLE,
        points = listOf(Offset(l, t), Offset(r, b)),
        strokeColor = Color.Red,
        fillColor = if (filled) Color.Blue else null,
        strokeWidth = 4f,
    )

    private fun assertRectEquals(expected: Rect, actual: Rect, eps: Float = 0.5f) {
        assertTrue(abs(expected.left - actual.left) < eps, "left ${actual.left} != ${expected.left}")
        assertTrue(abs(expected.top - actual.top) < eps, "top ${actual.top} != ${expected.top}")
        assertTrue(abs(expected.right - actual.right) < eps, "right ${actual.right} != ${expected.right}")
        assertTrue(abs(expected.bottom - actual.bottom) < eps, "bottom ${actual.bottom} != ${expected.bottom}")
    }

    @Test
    fun pathBoundsCoversAllPoints() {
        val path = pathOf(0f to 0f, 10f to 20f, -5f to 30f)
        assertRectEquals(Rect(-5f, 0f, 10f, 30f), path.bounds())
    }

    @Test
    fun rectangleShapeBoundsFromCorners() {
        val s = rectShape(10f, 20f, 110f, 220f)
        assertRectEquals(Rect(10f, 20f, 110f, 220f), s.bounds())
    }

    @Test
    fun rectangleHitTestInsideForFilled() {
        val s = rectShape(0f, 0f, 100f, 50f, filled = true)
        assertTrue(s.hitTest(Offset(50f, 25f)))
    }

    @Test
    fun strokedRectangleHitsEdgeNotInterior() {
        val s = rectShape(0f, 0f, 100f, 50f, filled = false)
        assertTrue(s.hitTest(Offset(0f, 25f)))     // left edge
        assertFalse(s.hitTest(Offset(50f, 25f)))   // center, no fill
    }

    @Test
    fun circleHitTestStroked() {
        val s = Element.Shape(
            shapeType = ShapeType.CIRCLE,
            points = listOf(Offset(0f, 0f), Offset(100f, 0f)),
            strokeColor = Color.Red,
            strokeWidth = 4f,
        )
        // Circle: center (50,0), radius 50
        assertTrue(s.hitTest(Offset(0f, 0f)))      // left edge
        assertTrue(s.hitTest(Offset(50f, 50f)))    // bottom edge
        assertFalse(s.hitTest(Offset(50f, 0f)))    // center, no fill
    }

    @Test
    fun lineHitTestRespectsTolerance() {
        val line = Element.Shape(
            shapeType = ShapeType.LINE,
            points = listOf(Offset(0f, 0f), Offset(100f, 0f)),
            strokeColor = Color.Red,
            strokeWidth = 4f,
        )
        assertTrue(line.hitTest(Offset(50f, 1f)))    // on the line
        assertTrue(line.hitTest(Offset(50f, 5f)))    // within tolerance + stroke
        assertFalse(line.hitTest(Offset(50f, 50f)))  // way off
    }

    @Test
    fun rotatedRectangleHitTestInverseRotates() {
        val s = rectShape(0f, 0f, 100f, 50f, filled = true).copy(rotation = 90f)
        // Original bbox is (0,0)..(100,50), center (50,25).
        // After +90° rotation (clockwise in screen coords), a point that was at
        // (50,25) is still the center, hence still hit.
        assertTrue(s.hitTest(Offset(50f, 25f)))
        // A point above center along the rotated long edge should still hit.
        // The rotated bbox extends roughly from y=-25 to y=75 around center x=50.
        assertTrue(s.hitTest(Offset(50f, -20f)))
        // A point well outside the rotated bbox.
        assertFalse(s.hitTest(Offset(200f, 25f)))
    }

    @Test
    fun topmostHitPicksHighestZIndex() {
        val low = rectShape(0f, 0f, 100f, 100f, filled = true).copy(zIndex = 0, id = "low")
        val high = rectShape(0f, 0f, 100f, 100f, filled = true).copy(zIndex = 5, id = "high")
        val pick = topmostHit(listOf(low, high), Offset(50f, 50f))
        assertNotNull(pick)
        assertEquals("high", pick.id)
    }

    @Test
    fun topmostHitReturnsNullOnMiss() {
        val s = rectShape(0f, 0f, 10f, 10f, filled = true)
        assertNull(topmostHit(listOf(s), Offset(500f, 500f)))
    }

    @Test
    fun translateMovesAllPoints() {
        val path = pathOf(0f to 0f, 10f to 20f)
        val moved = path.translate(Offset(5f, -5f)) as Element.Path
        assertEquals(Offset(5f, -5f), moved.points[0])
        assertEquals(Offset(15f, 15f), moved.points[1])
        assertEquals(0f, moved.rotation, "translate must not change rotation")
    }

    @Test
    fun withBoundsScalesPathPointsLinearly() {
        val path = pathOf(0f to 0f, 100f to 100f)
        val resized = path.withBounds(Rect(0f, 0f, 200f, 50f)) as Element.Path
        assertEquals(Offset(0f, 0f), resized.points[0])
        assertEquals(Offset(200f, 50f), resized.points[1])
    }

    @Test
    fun withBoundsMovesShapeCornersPreservingOrientation() {
        val s = rectShape(0f, 0f, 100f, 100f)
        val resized = s.withBounds(Rect(50f, 50f, 250f, 150f)) as Element.Shape
        assertEquals(Offset(50f, 50f), resized.points[0])
        assertEquals(Offset(250f, 150f), resized.points[1])
    }

    @Test
    fun withRotationSetsAbsoluteAngle() {
        val s = rectShape(0f, 0f, 100f, 100f)
        val rotated = s.withRotation(45f) as Element.Shape
        assertEquals(45f, rotated.rotation)
    }

    @Test
    fun resizeBoundsDragsCornerAnchorsOpposite() {
        val original = Rect(0f, 0f, 100f, 50f)
        // Drag TopLeft corner to (-10, -20): bottom-right stays anchored.
        val resized = resizeBounds(original, 0f, ResizeHandle.TopLeft, Offset(-10f, -20f))
        assertRectEquals(Rect(-10f, -20f, 100f, 50f), resized)
    }

    @Test
    fun resizeBoundsNormalizesInversion() {
        val original = Rect(0f, 0f, 100f, 50f)
        // Drag TopLeft past BottomRight → coords swap; normalized rect must remain valid.
        val resized = resizeBounds(original, 0f, ResizeHandle.TopLeft, Offset(150f, 80f))
        assertTrue(resized.right >= resized.left)
        assertTrue(resized.bottom >= resized.top)
    }

    @Test
    fun circleBoundsInscribesCircleNotChordOfPoints() {
        // Diameter from (0,0) to (100,100): center (50,50), radius 50√2 ≈ 70.71.
        // The bbox of the two points is 100×100, but the circle is wider.
        val circle = Element.Shape(
            shapeType = ShapeType.CIRCLE,
            points = listOf(Offset(0f, 0f), Offset(100f, 100f)),
            strokeColor = Color.Red,
            strokeWidth = 4f,
        )
        val r = circle.bounds()
        val expectedRadius = 50f * kotlin.math.sqrt(2f)
        assertRectEquals(
            expected = Rect(50f - expectedRadius, 50f - expectedRadius,
                            50f + expectedRadius, 50f + expectedRadius),
            actual = r,
        )
    }

    @Test
    fun circleWithBoundsStaysCircularAndCentered() {
        val circle = Element.Shape(
            shapeType = ShapeType.CIRCLE,
            points = listOf(Offset(0f, 0f), Offset(100f, 0f)),
            strokeColor = Color.Red,
            strokeWidth = 4f,
        )
        // Drag to a non-square bbox; the circle should snap to the smaller dim.
        val resized = circle.withBounds(Rect(0f, 0f, 200f, 100f))
        val newBounds = resized.bounds()
        // Width should equal height (still a circle), and equal min(200, 100) = 100.
        assertEquals(100f, newBounds.width, "width")
        assertEquals(100f, newBounds.height, "height")
        // Center should match the requested bounds center.
        assertEquals(100f, newBounds.center.x)
        assertEquals(50f, newBounds.center.y)
    }

    @Test
    fun circleResizeKeepsOppositeEdgeAnchored() {
        // Circle bbox (0,0)-(100,100). Drag Right edge to x=150. Left edge
        // must stay at x=0 after withBounds is applied.
        val circle = Element.Shape(
            shapeType = ShapeType.CIRCLE,
            points = listOf(Offset(0f, 50f), Offset(100f, 50f)),
            strokeColor = Color.Red,
            strokeWidth = 4f,
        )
        val newBounds = resizeBoundsForElement(
            element = circle,
            handle = ResizeHandle.Right,
            pointer = Offset(150f, 50f),
        )
        val resized = circle.withBounds(newBounds) as Element.Shape
        val finalBounds = resized.bounds()
        // Square (Right-edge drag keeps the dragged horizontal extent as the side).
        assertEquals(finalBounds.width, finalBounds.height, "circle must stay square")
        // Left edge anchored.
        assertTrue(kotlin.math.abs(finalBounds.left - 0f) < 0.5f,
            "left ${finalBounds.left} drifted from 0")
    }

    @Test
    fun circleResizeCornerKeepsOppositeCornerAnchored() {
        val circle = Element.Shape(
            shapeType = ShapeType.CIRCLE,
            points = listOf(Offset(0f, 50f), Offset(100f, 50f)),
            strokeColor = Color.Red,
            strokeWidth = 4f,
        )
        val newBounds = resizeBoundsForElement(
            element = circle,
            handle = ResizeHandle.BottomRight,
            pointer = Offset(150f, 130f),
        )
        val resized = circle.withBounds(newBounds) as Element.Shape
        val finalBounds = resized.bounds()
        // TopLeft must stay anchored at (0, 0).
        assertTrue(kotlin.math.abs(finalBounds.left - 0f) < 0.5f, "left drift")
        assertTrue(kotlin.math.abs(finalBounds.top - 0f) < 0.5f, "top drift")
        assertEquals(finalBounds.width, finalBounds.height)
    }

    @Test
    fun resizeBoundsKeepsOppositeCornerFixedUnderRotation() {
        // 90° rotation of a 100×100 unit at the origin. The TopLeft corner
        // in world space lands at (100, 0) — that point must not move when we
        // drag the BottomRight handle.
        val original = Rect(0f, 0f, 100f, 100f)
        val rotation = 90f
        // World-space drag of the (rotated) BottomRight handle to (-50, 150).
        val newBounds = resizeBounds(
            originalBounds = original,
            rotation = rotation,
            handle = ResizeHandle.BottomRight,
            pointer = Offset(-50f, 150f),
        )
        // After: render is rotation around newBounds.center. The TopLeft
        // local-space corner is the un-dragged anchor; it must land back at
        // (100, 0) in world space.
        val topLeftWorld = rotateAround(newBounds.topLeft, newBounds.center, rotation)
        val eps = 0.5f
        assertTrue(kotlin.math.abs(topLeftWorld.x - 100f) < eps,
            "topLeft.x ${topLeftWorld.x} drifted from 100")
        assertTrue(kotlin.math.abs(topLeftWorld.y - 0f) < eps,
            "topLeft.y ${topLeftWorld.y} drifted from 0")
    }
}
