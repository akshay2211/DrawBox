package io.ak1.drawbox.presentation.viewmodel

import androidx.compose.ui.graphics.Color
import io.ak1.drawbox.domain.model.Intent
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DrawBoxControllerTest {

    private fun newController() = DrawBoxController(Reducer(UseCase()))

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
