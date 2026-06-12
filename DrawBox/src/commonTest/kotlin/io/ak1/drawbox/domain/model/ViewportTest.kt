package io.ak1.drawbox.domain.model

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ViewportTest {

    private fun assertOffsetClose(expected: Offset, actual: Offset, eps: Float = 0.001f) {
        assertTrue(abs(expected.x - actual.x) < eps, "x ${actual.x} != ${expected.x}")
        assertTrue(abs(expected.y - actual.y) < eps, "y ${actual.y} != ${expected.y}")
    }

    @Test
    fun identityRoundTrip() {
        val vp = Viewport()
        val world = Offset(42f, -17f)
        val screen = vp.worldToScreen(world)
        assertOffsetClose(world, screen) // identity
        assertOffsetClose(world, vp.screenToWorld(screen))
    }

    @Test
    fun roundTripAtArbitraryViewport() {
        val vp = Viewport(offset = Offset(120f, -40f), scale = 1.5f)
        val world = Offset(50f, 80f)
        val screen = vp.worldToScreen(world)
        assertOffsetClose(world, vp.screenToWorld(screen))
    }

    @Test
    fun panShiftsOnlyOffset() {
        val vp = Viewport(offset = Offset(10f, 20f), scale = 2f)
        val out = vp.panBy(Offset(5f, -3f))
        assertEquals(Offset(15f, 17f), out.offset)
        assertEquals(2f, out.scale)
    }

    @Test
    fun zoomByPreservesFocalPoint() {
        // The world point under the focal pixel must remain there after zoom.
        val vp = Viewport(offset = Offset(100f, 50f), scale = 1f)
        val focal = Offset(300f, 200f)
        val focalWorldBefore = vp.screenToWorld(focal)
        val out = vp.zoomBy(2f, focal)
        val focalWorldAfter = out.screenToWorld(focal)
        assertOffsetClose(focalWorldBefore, focalWorldAfter)
    }

    @Test
    fun zoomByRespectsMinMax() {
        val vp = Viewport(scale = 1f)
        assertEquals(Viewport.MAX_SCALE, vp.zoomBy(1000f, Offset.Zero).scale)
        assertEquals(Viewport.MIN_SCALE, vp.zoomBy(0.0001f, Offset.Zero).scale)
    }

    @Test
    fun zoomToSetsAbsoluteScalePreservingFocal() {
        val vp = Viewport(offset = Offset(10f, 10f), scale = 1.3f)
        val focal = Offset(200f, 100f)
        val focalWorldBefore = vp.screenToWorld(focal)
        val out = vp.zoomTo(3f, focal)
        assertEquals(3f, out.scale)
        assertOffsetClose(focalWorldBefore, out.screenToWorld(focal))
    }

    @Test
    fun scalePercentMatchesScale() {
        assertEquals(100, Viewport(scale = 1f).scalePercent)
        assertEquals(250, Viewport(scale = 2.5f).scalePercent)
    }
}
