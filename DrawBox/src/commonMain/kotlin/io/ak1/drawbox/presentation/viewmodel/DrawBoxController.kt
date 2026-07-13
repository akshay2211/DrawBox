package io.ak1.drawbox.presentation.viewmodel

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import io.ak1.drawbox.domain.model.Element
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import io.ak1.drawbox.domain.model.BackgroundPattern
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
    initialState: State = State(),
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    /**
     * Current drawing state as a reactive flow.
     * Collect this in your UI to receive state updates:
     *
     * ```kotlin
     * val state by viewModel.state.collectAsState()
     * ```
     */
    val state: StateFlow<State> = _state.asStateFlow()

    private val _intents = MutableSharedFlow<Intent>(extraBufferCapacity = 64)
    /**
     * Reactive stream of intents that have been processed by this controller.
     *
     * Each intent is emitted **after** the reducer has run and [state] has been
     * updated, so subscribers can read [state] at emission time and observe the
     * state that the intent produced.
     *
     * This is the extension seam for cross-cutting concerns that need to
     * observe the intent stream without owning it — collaboration transports,
     * analytics, external history / macro recording, and layered controllers
     * (e.g. brush controllers) that decorate a base [DrawBoxController].
     *
     * Buffered ([extraBufferCapacity]) so slow subscribers don't back-pressure
     * the drawing loop. If a subscriber falls behind the buffer, its emissions
     * are dropped — subscribers that require lossless delivery should collect
     * on a dedicated dispatcher and keep their handler cheap.
     */
    val intents: SharedFlow<Intent> = _intents.asSharedFlow()

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
        val prev = _state.value
        val newState = reducer.reduce(prev, intent)
        _state.value = newState
        updateHistoryState()
        // Emit AFTER state has been updated so subscribers reading state.value
        // at the moment of emission observe the post-reduce state.
        _intents.tryEmit(intent)
        // Handle side effects
        when (intent) {
            is Intent.SaveBitmap -> emitSaveEvent(intent.bitmap,intent.throwable)
            is Intent.LoadDrawing -> emitLoadEvent()
            // Double-tap in SELECT mode: open the editor for the hit text.
            is Intent.RequestTextEditAt ->
                reducer.hitTopmost(prev.elements, intent.offset, intent.tolerance)
                    .let { it as? Element.Text }
                    ?.let { _events.tryEmit(Event.TextEditRequested(it.id)) }
            // Second tap on an already-sole-selected text opens the editor too
            // (tldraw/Figma pattern). `prev` is the pre-reduce selection.
            is Intent.SelectAt -> {
                val hit = reducer.hitTopmost(prev.elements, intent.offset, intent.tolerance)
                if (hit is Element.Text && prev.selectedIds == setOf(hit.id)) {
                    _events.tryEmit(Event.TextEditRequested(hit.id))
                }
            }
            else -> {

            }
        }
    }

    private fun updateHistoryState() {
        _canUndo.value = _state.value.history.isNotEmpty()
        _canRedo.value = _state.value.future.isNotEmpty()
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

    /** Default corner radius applied to new RECTANGLE / TRIANGLE shapes. */
    fun setCornerRadius(radius: Float) = onIntent(Intent.SetCornerRadius(radius))

    /** Update the corner radius of every selected RECTANGLE / TRIANGLE shape. */
    fun setSelectionCornerRadius(radius: Float) =
        onIntent(Intent.SetSelectedCornerRadius(radius))

    /** Default stroke pattern applied to new shapes. */
    fun setStrokeStyle(style: io.ak1.drawbox.domain.model.StrokeStyle) =
        onIntent(Intent.SetStrokeStyle(style))

    /** Update the stroke pattern of every selected shape. */
    fun setSelectionStrokeStyle(style: io.ak1.drawbox.domain.model.StrokeStyle) =
        onIntent(Intent.SetSelectedStrokeStyle(style))

    /** Default fill color applied to new shapes. `null` = stroke-only. */
    fun setFillColor(color: Color?) = onIntent(Intent.SetFillColor(color))

    /** Whether new shapes render an outline pass. `false` = fill-only. */
    fun setStrokeEnabled(enabled: Boolean) = onIntent(Intent.SetStrokeEnabled(enabled))

    /** Change the opacity for new drawings (0.0 to 1.0) */
    fun setOpacity(opacity: Float) = onIntent(Intent.SetOpacity(opacity))

    /** Change the canvas background color */
    fun setBgColor(color: Color) = onIntent(Intent.SetBgColor(color))

    /**
     * Set a repeating image — typically an SVG vector drawable — painted above
     * the solid [setBgColor] background and below all strokes/shapes.
     *
     * The painter is rasterized once at its intrinsic size (64dp square fallback
     * when no intrinsic size is reported) into a tileable [ImageBitmap], wrapped
     * in a [ShaderBrush] with [TileMode.Repeated], and reused across recompositions.
     * Tile work happens once per call, not per frame.
     *
     * Pass `null` for [painter] to clear the pattern.
     *
     * [tint] recolors the painter via SrcIn blending — ideal for monochrome /
     * alpha-driven SVGs. Multi-color SVGs and raster-backed assets will be
     * collapsed to the tint color (every opaque pixel becomes [tint]); pass
     * `null` to keep the painter's original colors.
     *
     * The pattern is a runtime decoration and is intentionally excluded from
     * PNG / JSON / SVG export.
     */
    fun setBackgroundPattern(painter: Painter?, tint: Color? = null) {
        val pattern = painter?.let { BackgroundPattern(it, tint) }
        onIntent(Intent.SetBackgroundPattern(pattern))
    }

    /** Switch to a different drawing mode */
    fun setMode(mode: Mode) = onIntent(Intent.SetMode(mode))

    /**
     * Place a bitmap on the canvas. [bytes] is the raw encoded image
     * (PNG / JPEG / WebP — whatever the renderer's decoder accepts); the SDK
     * owns decoding from there. [intrinsicSize] is the source image's pixel
     * dimensions, used to preserve aspect ratio when sizing the placed
     * element. [position] is the world-space anchor that lands at the center
     * of the placed image — drop / paste at the cursor naturally puts the
     * image under the cursor.
     */
    fun insertImage(
        bytes: ByteArray,
        intrinsicSize: Size,
        position: Offset = Offset.Zero,
    ) = onIntent(Intent.InsertImage(bytes, position, intrinsicSize))

    /**
     * Place a plain-text block on the canvas. [text] may be empty — the
     * sample-app modal-editor flow inserts an empty block first and then
     * dispatches [updateText] when the user commits content.
     *
     * [fontFamilyKey] must be a key registered via [registerFont] or one of
     * the built-in keys (`sans`, `serif`, `mono`); unknown keys fall back to
     * `sans` at render time.
     */
    fun insertText(
        text: String,
        position: Offset = Offset.Zero,
        fontSize: Float = 24f,
        fontFamilyKey: String = io.ak1.drawbox.domain.model.DEFAULT_FONT_FAMILY_KEY,
        alignment: io.ak1.drawbox.domain.model.TextAlignment =
            io.ak1.drawbox.domain.model.TextAlignment.LEFT,
        color: Color = Color.Black,
    ) = onIntent(
        Intent.InsertText(text, position, fontSize, fontFamilyKey, alignment, color),
    )

    /** Replace the text content of an existing [io.ak1.drawbox.domain.model.Element.Text]. */
    fun updateText(id: String, text: String) =
        onIntent(Intent.UpdateText(id, text))

    /**
     * Register a custom font family the SDK can resolve by [key] when
     * rendering [io.ak1.drawbox.domain.model.Element.Text] elements. Built-in
     * keys (`sans`, `serif`, `mono`) ship with the SDK and don't need
     * registration; calling this is for adding host-provided typefaces.
     *
     * The registry is in-memory only — the host re-registers on app launch.
     */
    fun registerFont(
        key: String,
        family: androidx.compose.ui.text.font.FontFamily,
    ) {
        io.ak1.drawbox.text.FontRegistry.register(key, family)
    }

    /** Set the font size of every selected text element. */
    fun setSelectionFontSize(size: Float) =
        onIntent(Intent.SetSelectedFontSize(size))

    /** Set the alignment of every selected text element. */
    fun setSelectionTextAlignment(alignment: io.ak1.drawbox.domain.model.TextAlignment) =
        onIntent(Intent.SetSelectedTextAlignment(alignment))

    /** Set the font family key of every selected text element. */
    fun setSelectionFontFamily(fontFamilyKey: String) =
        onIntent(Intent.SetSelectedFontFamily(fontFamilyKey))

    /** Default font size for the next [Intent.InsertText]. */
    fun setFontSize(size: Float) = onIntent(Intent.SetFontSize(size))

    /** Default font family key for the next [Intent.InsertText]. */
    fun setFontFamily(fontFamilyKey: String) = onIntent(Intent.SetFontFamily(fontFamilyKey))

    /** Default alignment for the next [Intent.InsertText]. */
    fun setTextAlignment(alignment: io.ak1.drawbox.domain.model.TextAlignment) =
        onIntent(Intent.SetTextAlignment(alignment))

    /** Undo the last drawing action */
    fun undo() = onIntent(Intent.Undo)

    /** Redo the last undone action */
    fun redo() = onIntent(Intent.Redo)

    /** Clear the canvas and reset to empty state */
    fun reset() = onIntent(Intent.Reset)

    // ==================== Selection Methods ====================

    /** Pick the topmost element at the given canvas offset (or clear selection on miss). */
    fun selectAt(offset: Offset, tolerance: Float = 8f) =
        onIntent(Intent.SelectAt(offset, tolerance))

    /** Clear the current selection. */
    fun clearSelection() = onIntent(Intent.ClearSelection)

    /** Delete every selected element. */
    fun deleteSelected() = onIntent(Intent.DeleteSelected)

    /** Recolor every selected element's stroke. */
    fun setSelectionColor(color: Color) = onIntent(Intent.SetSelectedStrokeColor(color))

    /**
     * Set the fill color of every selected shape. Pass `null` to clear fill so
     * the shape renders stroke-only.
     */
    fun setSelectionFillColor(color: Color?) =
        onIntent(Intent.SetSelectedFillColor(color))

    /**
     * Toggle the stroke pass on every selected shape. `false` makes the shape
     * fill-only; `true` restores its outline.
     */
    fun setSelectionStrokeEnabled(enabled: Boolean) =
        onIntent(Intent.SetSelectedStrokeEnabled(enabled))

    /** Re-stroke every selected element. */
    fun setSelectionStrokeWidth(width: Float) = onIntent(Intent.SetSelectedStrokeWidth(width))

    /**
     * Erase every element whose body intersects a disk of [radius] world pixels
     * around [point]. Snapshots history at most once and only if an element is
     * actually removed, so calls that hit nothing leave the undo stack
     * untouched. Useful for programmatic eraser passes; for user-driven
     * erasing the gesture layer in [io.ak1.drawbox.DrawBox] already drives
     * this via [Intent.EraseAt] in [Mode.ERASER].
     */
    fun eraseAt(point: Offset, radius: Float) {
        onIntent(Intent.BeginErase)
        onIntent(Intent.EraseAt(point, radius))
        onIntent(Intent.EndErase)
    }

    /** Change the world-space radius used by [Mode.ERASER]. */
    fun setEraserSize(size: Float) = onIntent(Intent.SetEraserSize(size))

    /** Bring selected elements to the top of the z-order. */
    fun bringSelectionToFront() = onIntent(Intent.BringSelectionToFront)

    /** Send selected elements to the bottom of the z-order. */
    fun sendSelectionToBack() = onIntent(Intent.SendSelectionToBack)

    // ==================== Camera / Viewport Methods ====================

    /** Translate the camera by [delta] screen pixels. */
    fun panBy(delta: Offset) = onIntent(Intent.PanBy(delta))

    /** Multiplicatively zoom anchored at [focalScreen]. */
    fun zoomBy(factor: Float, focalScreen: Offset) = onIntent(Intent.ZoomBy(factor, focalScreen))

    /** Set absolute scale anchored at [focalScreen]. */
    fun zoomTo(targetScale: Float, focalScreen: Offset) =
        onIntent(Intent.ZoomTo(targetScale, focalScreen))

    /** Reset viewport to identity. */
    fun resetCamera() = onIntent(Intent.ResetCamera)

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
                strokeColor = _state.value.strokeColor,
                strokeWidth = _state.value.strokeWidth,
                opacity = _state.value.opacity,
                bgColor = payLoad.bgColor,
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
 * @param initialState State used when the controller is first created.
 * @return Remembered [DrawBoxController] instance
 */
@Composable
fun rememberDrawBoxController(
    useCase: UseCase = UseCase(),
    initialState: State = State(),
): DrawBoxController {
    // Important: Provide initializer for KMP - type reflection unavailable on non-JVM platforms
    return viewModel {
        DrawBoxController(/*useCase, */Reducer(useCase), initialState)
    }
}
