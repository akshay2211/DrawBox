package io.ak1.drawbox.presentation.viewmodel

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import io.ak1.drawbox.domain.model.Event
import io.ak1.drawbox.domain.model.Intent
import io.ak1.drawbox.domain.model.State
import io.ak1.drawbox.domain.model.Mode
import io.ak1.drawbox.domain.usecase.UseCase
import io.ak1.drawbox.domain.usecase.SvgExporter
import io.ak1.drawbox.presentation.reducer.Reducer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import io.ak1.drawbox.domain.model.PayLoad
import io.ak1.drawbox.domain.model.DrawingSerializer

/**
 * ViewModel orchestrating drawing state and side effects.
 *
 * [DrawBoxController] is the main entry point for state management in DrawBox.
 * It manages three primary responsibilities:
 *
 * 1. **State Management**: Holds and exposes the current drawing state via [state] flow
 * 2. **Intent Processing**: Accepts user intents and passes them through the reducer
 * 3. **Side Effects**: Emits events and handles persistence operations
 *
 * **MVI Loop:**
 * ```
 * UI (DrawBox) → Intent → onIntent()
 *                            ↓
 *                        Reducer
 *                            ↓
 *                        New State
 *                            ↓
 *                        Update Flow
 *                            ↓
 *                        UI Recomposes
 * ```
 *
 * **Usage:**
 * ```kotlin
 * @Composable
 * fun MyDrawingScreen() {
 *     val viewModel = rememberDrawBoxController()
 *     val state by viewModel.state.collectAsState()
 *
 *     DrawBox(
 *         state = state,
 *         onIntent = viewModel::onIntent,
 *     )
 * }
 * ```
 *
 * @property state Reactive flow of the current drawing state
 * @property events Reactive flow of side effect events (save, load, error)
 * @property canUndo True when there are elements that can be undone
 * @property canRedo True when there are previously undone actions that can be redone
 *
 * @see State
 * @see Intent
 * @see Reducer
 */
class DrawBoxController(
    private val reducer: Reducer,
) : ViewModel() {

    private val _state = MutableStateFlow(State())
    /**
     * Current drawing state as a reactive flow.
     * Collect this in your UI to receive state updates:
     *
     * ```kotlin
     * val state by viewModel.state.collectAsState()
     * ```
     */
    val state: StateFlow<State> = _state.asStateFlow()

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    /**
     * Side effect events like drawing saved, drawing loaded, or errors.
     * Use LaunchedEffect to collect and respond to events:
     *
     * ```kotlin
     * LaunchedEffect(viewModel) {
     *     viewModel.events.collect { event ->
     *         when (event) {
     *             is Event.DrawingSaved -> showMessage("Saved!")
     *             is Event.Error -> showError(event.message)
     *             else -> {}
     *         }
     *     }
     * }
     * ```
     */
    val events: SharedFlow<Event> = _events.asSharedFlow()

    private val _canUndo = MutableStateFlow(false)
    /**
     * True when undo is available (elements exist on canvas).
     * Use this to enable/disable undo buttons:
     *
     * ```kotlin
     * val canUndo by viewModel.canUndo.collectAsState()
     * Button(onClick = viewModel::undo, enabled = canUndo)
     * ```
     */
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    /**
     * True when redo is available (previously undone actions exist).
     * Use this to enable/disable redo buttons.
     */
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    /**
     * Process an intent and update state accordingly.
     *
     * This is the main entry point for state mutations. All state changes
     * must go through this function. It:
     * 1. Passes the intent to the reducer
     * 2. Updates the state
     * 3. Updates undo/redo availability
     * 4. Handles side effects (events, persistence)
     *
     * @param intent The user action or system event
     *
     * @see Intent for available intent types
     */
    fun onIntent(intent: Intent) {
        val newState = reducer.reduce(_state.value, intent)
        _state.value = newState
        updateHistoryState()
        // Handle side effects
        when (intent) {
            is Intent.SaveBitmap -> emitSaveEvent(intent.bitmap,intent.throwable)
            is Intent.LoadDrawing -> emitLoadEvent()
            else -> {

            }
        }
    }

    private fun updateHistoryState() {
        _canUndo.value = _state.value.elements.isNotEmpty()
        _canRedo.value = _state.value.undoStack.isNotEmpty()
    }

    private fun emitSaveEvent(bitmap: ImageBitmap?, throwable: Throwable?) {
        // Emit event synchronously
        _events.tryEmit(Event.PngSaved(bitmap, throwable))
    }

    private fun emitLoadEvent() {
        // Emit event synchronously
        _events.tryEmit(Event.DrawingLoaded(_state.value))
    }

    // ==================== Convenience Methods ====================

    /** Change the stroke color for new drawings */
    fun setColor(color: Color) = onIntent(Intent.SetStrokeColor(color))

    /** Change the stroke width for new drawings */
    fun setStrokeWidth(width: Float) = onIntent(Intent.SetStrokeWidth(width))

    /** Change the opacity for new drawings (0.0 to 1.0) */
    fun setOpacity(opacity: Float) = onIntent(Intent.SetOpacity(opacity))

    /** Change the canvas background color */
    fun setBgColor(color: Color) = onIntent(Intent.SetBgColor(color))

    /** Switch to a different drawing mode */
    fun setMode(mode: Mode) = onIntent(Intent.SetMode(mode))

    /** Undo the last drawing action */
    fun undo() = onIntent(Intent.Undo)

    /** Redo the last undone action */
    fun redo() = onIntent(Intent.Redo)

    /** Clear the canvas and reset to empty state */
    fun reset() = onIntent(Intent.Reset)

    // ==================== Persistence Methods ====================

    /** Capture and save the current drawing as a bitmap */
    fun saveBitmap() = state.value.invokeBitmap.invoke()

    /**
     * Export the current drawing as a JSON string.
     *
     * The exported string can be saved to a file or sent over the network,
     * then later imported using [importPath].
     *
     * @return JSON string representation of the drawing
     */
    internal fun exportPath(): String {
        val payLoad = PayLoad(
            bgColor = _state.value.bgColor,
            elements = _state.value.elements
        )
        return DrawingSerializer.serialize(payLoad)
    }

    /**
     * Export the current drawing as an SVG string.
     *
     * The exported string can be saved to a file and opened in any SVG viewer.
     * Emits an Event.SvgExported event that can be collected to save the file.
     */
    fun exportSvg() {
        val svgContent = SvgExporter.exportToSvg(
            elements = _state.value.elements
        )
        _events.tryEmit(Event.SvgExported(svgContent))
    }

    /**
     * Export the current drawing as JSON.
     *
     * Emits an [Event.JsonExported] event so the platform layer can persist it.
     */
    fun exportJson() {
        _events.tryEmit(Event.JsonExported(exportPath()))
    }

    /**
     * Import a previously exported drawing from JSON string.
     *
     * Clears the canvas and loads elements from the provided JSON.
     * Emits an [Event.Error] if deserialization fails.
     *
     * @param jsonString The exported drawing as JSON
     */
    fun importPath(jsonString: String) {
        try {
            val payLoad = DrawingSerializer.deserialize(jsonString)
            reset()
            _state.value = State(
                elements = payLoad.elements,
                undoStack = emptyList(),
                strokeColor = _state.value.strokeColor,
                strokeWidth = _state.value.strokeWidth,
                opacity = _state.value.opacity,
                bgColor = payLoad.bgColor
            )
        } catch (e: Exception) {
            // Handle deserialization error
            _events.tryEmit(Event.Error("Failed to import drawing: ${e.message}"))
        }
    }
}

/**
 * Composable factory function for creating and caching a [DrawBoxController].
 *
 * Uses Compose's [viewModel] API to ensure the same controller instance
 * is retained across recompositions. Use this in your composable functions:
 *
 * ```kotlin
 * @Composable
 * fun MyScreen() {
 *     val viewModel = rememberDrawBoxController()
 *     val state by viewModel.state.collectAsState()
 *     // ... use viewModel and state
 * }
 * ```
 *
 * @param useCase Optional custom use case instance. If not provided, a default
 *                is created. Override for custom business logic or persistence.
 * @return Remembered [DrawBoxController] instance
 */
@Composable
fun rememberDrawBoxController(
    useCase: UseCase = UseCase(),
): DrawBoxController {
    // Important: Provide initializer for KMP - type reflection unavailable on non-JVM platforms
    return viewModel {
        DrawBoxController(/*useCase, */Reducer(useCase))
    }
}
