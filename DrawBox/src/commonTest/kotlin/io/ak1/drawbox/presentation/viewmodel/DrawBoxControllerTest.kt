package io.ak1.drawbox.presentation.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.ak1.drawbox.domain.model.Element
import io.ak1.drawbox.domain.model.Intent
import io.ak1.drawbox.domain.model.Mode
import io.ak1.drawbox.domain.usecase.UseCase
import io.ak1.drawbox.presentation.reducer.Reducer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DrawBoxControllerTest {

    private fun newController() = DrawBoxController(Reducer(UseCase()))

    // ===== End-to-end "user draws a stroke" =====
    //
    // These drive the same intent sequence the DrawBox gesture layer dispatches
    // when a user presses, drags, and lifts in PEN mode. They live in commonTest
    // so every published target — JVM, JS, WASM, iOS — proves a user can actually
    // draw, not merely that the code compiles.

    @Test
    fun userCanDrawAFreehandStroke() {
        val controller = newController()
        controller.setMode(Mode.PEN)

        // Press down, then drag through a few points and lift.
        controller.onIntent(Intent.InsertNewPath(Offset(10f, 10f)))
        controller.onIntent(Intent.UpdateLatestPath(Offset(20f, 15f)))
        controller.onIntent(Intent.UpdateLatestPath(Offset(30f, 25f)))
        controller.onIntent(Intent.UpdateLatestPath(Offset(40f, 40f)))

        val elements = controller.state.value.elements
        assertEquals(1, elements.size, "the drag should have produced exactly one stroke")
        val path = elements[0] as Element.Path
        assertEquals(4, path.samples.size, "seed point plus three drag samples")
        assertEquals(Offset(10f, 10f), path.samples.first().position)
        assertEquals(Offset(40f, 40f), path.samples.last().position)
        assertTrue(controller.canUndo.value, "a finished stroke must be undoable")
    }

    @Test
    fun undoRemovesADrawnStroke() {
        val controller = newController()
        controller.setMode(Mode.PEN)
        controller.onIntent(Intent.InsertNewPath(Offset(0f, 0f)))
        controller.onIntent(Intent.UpdateLatestPath(Offset(50f, 50f)))
        assertEquals(1, controller.state.value.elements.size)

        controller.undo()

        assertTrue(controller.state.value.elements.isEmpty(), "undo should clear the stroke")
        assertFalse(controller.canUndo.value)
        assertTrue(controller.canRedo.value, "the undone stroke should be redoable")
    }

    @Test
    fun intentsFlowEmitsEveryProcessedIntent() = runTest(StandardTestDispatcher()) {
        val controller = newController()
        val collected = mutableListOf<Intent>()
        val job = launch { controller.intents.take(2).toList(collected) }

        // Yield so the collector actually subscribes before we emit.
        testScheduler.runCurrent()

        controller.onIntent(Intent.SetStrokeColor(Color.Red))
        controller.onIntent(Intent.SetStrokeWidth(7f))

        job.join()

        assertEquals(2, collected.size)
        assertTrue(collected[0] is Intent.SetStrokeColor)
        assertTrue(collected[1] is Intent.SetStrokeWidth)
    }

    @Test
    fun intentsFlowEmitsAfterStateUpdate() = runTest(StandardTestDispatcher()) {
        val controller = newController()
        var stateAtEmission: Color? = null
        val job = launch {
            controller.intents.take(1).collect {
                // Guarantee: at emission time, state.value already reflects the
                // reduced intent.
                stateAtEmission = controller.state.value.strokeColor
            }
        }
        testScheduler.runCurrent()

        controller.onIntent(Intent.SetStrokeColor(Color.Blue))
        job.join()

        assertEquals(Color.Blue, stateAtEmission)
    }
}
