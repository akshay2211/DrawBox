package io.ak1.drawbox.presentation.reducer

import androidx.compose.ui.geometry.Offset
import io.ak1.drawbox.domain.model.Element
import io.ak1.drawbox.domain.model.Intent
import io.ak1.drawbox.domain.model.Mode
import io.ak1.drawbox.domain.model.State
import io.ak1.drawbox.domain.model.Viewport
import io.ak1.drawbox.domain.model.effectiveMode
import io.ak1.drawbox.domain.usecase.UseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ViewportReducerTest {

    private val reducer = Reducer(UseCase())

    @Test
    fun panByUpdatesOffsetOnly() {
        val initial = State(viewport = Viewport(offset = Offset(10f, 10f), scale = 1.5f))
        val out = reducer.reduce(initial, Intent.PanBy(Offset(5f, -3f)))
        assertEquals(Offset(15f, 7f), out.viewport.offset)
        assertEquals(1.5f, out.viewport.scale)
    }

    @Test
    fun zoomByDoesNotSnapshotHistory() {
        val initial = State(history = emptyList())
        val out = reducer.reduce(initial, Intent.ZoomBy(2f, Offset(100f, 100f)))
        assertTrue(out.history.isEmpty(), "Viewport changes must not enter undo history")
        assertEquals(2f, out.viewport.scale)
    }

    @Test
    fun resetCameraReturnsIdentity() {
        val initial = State(viewport = Viewport(offset = Offset(1000f, 1000f), scale = 4f))
        val out = reducer.reduce(initial, Intent.ResetCamera)
        assertEquals(Offset.Zero, out.viewport.offset)
        assertEquals(1f, out.viewport.scale)
    }

    @Test
    fun setTempPanFlipsFlagAndEffectiveMode() {
        val initial = State(mode = Mode.PEN)
        assertEquals(Mode.PEN, initial.effectiveMode)
        val held = reducer.reduce(initial, Intent.SetTempPan(true))
        assertTrue(held.tempPanActive)
        assertEquals(Mode.PAN, held.effectiveMode)
        val released = reducer.reduce(held, Intent.SetTempPan(false))
        assertFalse(released.tempPanActive)
        assertEquals(Mode.PEN, released.effectiveMode)
    }

    @Test
    fun insertingAPathLeavesViewportAlone() {
        val initial = State(
            mode = Mode.PEN,
            viewport = Viewport(offset = Offset(50f, 50f), scale = 2f),
        )
        val out = reducer.reduce(initial, Intent.InsertNewPath(Offset(10f, 10f)))
        assertEquals(initial.viewport, out.viewport)
        assertTrue(out.elements.size == 1 && out.elements[0] is Element.Path)
    }
}
