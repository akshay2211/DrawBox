package io.ak1.drawbox.domain.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Represents a user action or system event that should change the drawing state.
 *
 * Intent is the input to the MVI (Model-View-Intent) pattern. All state changes
 * flow through this sealed class:
 *
 * ```
 * User Action → Intent → Reducer → New State → UI Update
 * ```
 *
 * Intent instances are immutable data classes that capture the parameters of
 * what the user wants to do. The [io.ak1.drawbox.presentation.reducer.Reducer] interprets these intents and produces
 * new state.
 *
 * @see io.ak1.drawbox.presentation.reducer.Reducer.reduce for how intents are processed
 * @see State for the resulting state after intent handling
 */
sealed class Intent {
    // ==================== Element Operations ====================

    /** Add a new element to the canvas */
    data class AddElement(val element: Element) : Intent()

    /** Update an existing element in place */
    data class UpdateElement(val element: Element) : Intent()

    /** Delete an element from the canvas */
    data class DeleteElement(val elementId: String) : Intent()

    // ==================== Path Operations ====================

    /**
     * Start a new freehand path at the specified offset.
     *
     * This intent is dispatched when the user begins drawing in PEN mode.
     * It creates a new [Element.Path] with a single starting point.
     */
    data class InsertNewPath(val offset: Offset) : Intent()

    /**
     * Add a new point to the current path being drawn.
     *
     * This intent is dispatched continuously during drag operations in PEN mode.
     * It updates the latest path element with the new point.
     *
     * [pressure] is a unit-clamped multiplier applied to the active
     * [State.strokeWidth] for *this sample only*. `1.0` is the no-signal /
     * mouse / capacitive-touch default and produces uniform-width strokes
     * (back-compat with pre-pressure builds). Values that differ from `1.0`
     * promote the path's `pointWidths` from null to a per-sample list on the
     * first non-unit reading.
     */
    data class UpdateLatestPath(
        val newPoint: Offset,
        val pressure: Float = 1f,
    ) : Intent()

    // ==================== Image Operations ====================

    /**
     * Place an [Element.Image] at [position] in world space, sized from
     * [intrinsicSize] so the placed bitmap preserves its source aspect ratio
     * by default. Hosts that want different placement can dispatch
     * [SetElementBounds] right after.
     *
     * Snapshots history once.
     */
    data class InsertImage(
        val bytes: ByteArray,
        val position: Offset,
        val intrinsicSize: Size,
    ) : Intent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is InsertImage) return false
            return position == other.position &&
                intrinsicSize == other.intrinsicSize &&
                bytes.contentEquals(other.bytes)
        }
        override fun hashCode(): Int {
            var r = position.hashCode()
            r = 31 * r + intrinsicSize.hashCode()
            r = 31 * r + bytes.size
            return r
        }
    }

    // ==================== Shape Operations ====================

    /**
     * Start a new shape at the specified offset.
     *
     * This intent is dispatched when the user begins drawing in a shape mode
     * (RECTANGLE, CIRCLE, TRIANGLE, ARROW, or LINE). It creates a new [Element.Shape]
     * with the shape type and starting point.
     *
     * @param shapeType The type of shape being drawn
     * @param offset The starting point (usually where user began the drag)
     */
    data class InsertNewShape(val shapeType: ShapeType, val offset: Offset) : Intent()

    /**
     * Update the current shape's dimensions by providing the end point.
     *
     * This intent is dispatched continuously during drag operations in shape modes.
     * The shape's geometry is calculated from the start point and this end point.
     */
    data class UpdateLatestShape(val newPoint: Offset) : Intent()

    // ==================== Style Operations ====================

    /** Change the stroke color for new drawings */
    data class SetStrokeColor(val color: Color) : Intent()

    /** Change the stroke width for new drawings */
    data class SetStrokeWidth(val width: Float) : Intent()

    /**
     * Default corner radius applied to new RECTANGLE / TRIANGLE shapes. Has
     * no effect on shapes that ignore it (circle, line, arrow) and on shapes
     * already on the canvas.
     */
    data class SetCornerRadius(val radius: Float) : Intent()

    /**
     * Default stroke pattern applied to new shapes. Has no effect on existing
     * shapes on the canvas; use [SetSelectedStrokeStyle] to retro-edit those.
     */
    data class SetStrokeStyle(val style: StrokeStyle) : Intent()

    /** Change the opacity/alpha for new drawings (0.0 to 1.0) */
    data class SetOpacity(val opacity: Float) : Intent()

    /** Change the canvas background color */
    data class SetBgColor(val bgColor: Color) : Intent()

    /**
     * Set or clear the repeating background pattern painted above [SetBgColor]
     * and below all drawing elements.
     *
     * Pass `null` to remove an existing pattern. The pattern is purely a runtime
     * decoration — it is not captured by PNG/JSON/SVG export.
     */
    data class SetBackgroundPattern(val pattern: BackgroundPattern?) : Intent()

    /**
     * Switch to a different drawing mode.
     *
     * Changes what type of element is created on the next user interaction.
     * For example, switching from [Mode.PEN] to [Mode.RECTANGLE] causes the
     * next drag to create a rectangle instead of a path.
     */
    data class SetMode(val mode: Mode) : Intent()

    // ==================== Selection Operations ====================

    /**
     * Pick the topmost element at [offset] and make it the sole selection. If no
     * element is hit, clears selection. Tolerance is in canvas pixels.
     */
    data class SelectAt(val offset: Offset, val tolerance: Float = 8f) : Intent()

    /** Replace the current marquee rectangle (or clear with null). Not undoable. */
    data class SetMarqueeRect(val rect: Rect?) : Intent()

    /**
     * Finalize a marquee selection: select every element whose bounding box
     * intersects [rect]. Also clears [State.marqueeRect].
     */
    data class CommitMarquee(val rect: Rect) : Intent()

    /** Clear the current selection. */
    data object ClearSelection : Intent()

    /** Delete every element in [State.selectedIds]. */
    data object DeleteSelected : Intent()

    /**
     * Push the current elements onto the undo history. Use at the start of a
     * gesture that mutates the selection across many tiny updates (drag move,
     * resize, rotate) so a single undo reverts the whole gesture.
     */
    data object BeginTransform : Intent()

    /**
     * Marks the end of a drag-driven mutation. State is already up to date by
     * this point; the reducer treats this as a no-op. Provided as a *hint* for
     * transaction-aware observers (sync layers, autosave debouncers,
     * analytics): a coherent gesture just ended, commit/flush now.
     *
     * **Best-effort.** May be missed on app crash, force-quit, OS interrupt,
     * or unusual gesture-detector teardown. Observers must NOT rely on this
     * for correctness — treat it as an optimization signal layered on top of
     * timeout/idle-based commit policies.
     */
    data object EndTransform : Intent()

    /** Translate every selected element by [delta]. Does not snapshot history. */
    data class MoveSelected(val delta: Offset) : Intent()

    /**
     * Set the absolute bounding box of a single element. Used during resize.
     * Does not snapshot history (caller should dispatch [BeginTransform] first).
     */
    data class SetElementBounds(val id: String, val bounds: Rect) : Intent()

    /**
     * Set the absolute rotation in degrees of a single element. Used during
     * rotate. Does not snapshot history.
     */
    data class SetElementRotation(val id: String, val rotationDegrees: Float) : Intent()

    /**
     * Replace the points list of a single element. Used when dragging the
     * endpoint of a LINE/ARROW. Does not snapshot history.
     */
    data class SetElementPoints(val id: String, val points: List<Offset>) : Intent()

    /**
     * Update the curve deflection of a LINE/ARROW. Does not snapshot history.
     */
    data class SetLineBend(val id: String, val bend: Offset) : Intent()

    /**
     * Recompute connector bindings for an ARROW based on where its endpoints
     * currently sit. Called at drag-end after creating or moving the arrow.
     *
     * Does NOT snapshot history — every gesture that dispatches this already
     * snapshotted at its start (InsertNewShape for a fresh arrow,
     * BeginTransform for an endpoint drag), so this intent is the tail of an
     * existing transaction and an extra snapshot would double-count the undo.
     */
    data class FinalizeArrowBindings(val id: String) : Intent()

    /** Replace the stroke color of every selected element. Snapshots history. */
    data class SetSelectedStrokeColor(val color: Color) : Intent()

    /** Replace the stroke width of every selected element. Snapshots history. */
    data class SetSelectedStrokeWidth(val width: Float) : Intent()

    /**
     * Set the corner radius of every selected RECTANGLE / TRIANGLE shape.
     * Snapshots history. Other shape types in the selection are left alone.
     */
    data class SetSelectedCornerRadius(val radius: Float) : Intent()

    /**
     * Set the stroke pattern of every selected [Element.Shape]. Snapshots
     * history. Freehand paths in the selection are left alone.
     */
    data class SetSelectedStrokeStyle(val style: StrokeStyle) : Intent()

    // ==================== Eraser Operations ====================

    /**
     * Open an erase session. Resets [State.erasingSessionDirty] so the next
     * [EraseAt] that actually removes an element will snapshot history once.
     * A session that never lands on an element snapshots nothing — taps and
     * drags through empty space consume no undo slot.
     *
     * Not undoable, not history-relevant on its own.
     */
    data object BeginErase : Intent()

    /**
     * Delete every element whose body intersects a disk of [radius] around
     * [point] (world space). Snapshots history lazily on the first hit within
     * the current erase session (see [BeginErase]). Subsequent hits inside the
     * same session mutate elements in place without snapshotting, so a single
     * undo reverts the entire sweep. No-op when no element is hit — no
     * snapshot is pushed and no [State] copy is emitted.
     */
    data class EraseAt(val point: Offset, val radius: Float) : Intent()

    /**
     * Close an erase session. Clears [State.erasingSessionDirty] so the next
     * session starts fresh. Also serves as a transaction-end signal for sync /
     * autosave observers (see [EndTransform]).
     */
    data object EndErase : Intent()

    /** Change the world-space eraser radius used by [Mode.ERASER]. */
    data class SetEraserSize(val size: Float) : Intent()

    /** Move selected elements to the top of the z-order. Snapshots history. */
    data object BringSelectionToFront : Intent()

    /** Move selected elements to the bottom of the z-order. Snapshots history. */
    data object SendSelectionToBack : Intent()

    // ==================== Camera / Viewport Operations ====================

    /** Translate the camera by [delta] screen pixels. Not undoable (session state). */
    data class PanBy(val delta: Offset) : Intent()

    /**
     * Multiplicatively zoom the camera by [factor] anchored at [focalScreen]
     * (a point in screen pixels that stays put). Not undoable.
     */
    data class ZoomBy(val factor: Float, val focalScreen: Offset) : Intent()

    /** Set absolute scale to [targetScale] anchored at [focalScreen]. Not undoable. */
    data class ZoomTo(val targetScale: Float, val focalScreen: Offset) : Intent()

    /** Reset the viewport to identity (offset = Zero, scale = 1). Not undoable. */
    data object ResetCamera : Intent()

    /** Toggle the transient pan-while-held flag (e.g. for space bar). Not undoable. */
    data class SetTempPan(val active: Boolean) : Intent()

    // ==================== History Operations ====================

    /** Undo the last drawing action */
    object Undo : Intent()

    /** Redo the last undone action */
    object Redo : Intent()

    /** Clear all elements and reset to empty canvas */
    object Reset : Intent()

    // ==================== Persistence Operations ====================

    /**
     * Save the current drawing as a bitmap.
     *
     * This is typically emitted internally by the ViewModel after capturing
     * a bitmap from the graphics layer.
     *
     * @param bitmap The captured bitmap, or null if capture failed
     * @param throwable The error if bitmap capture failed, null on success
     */
    data class SaveBitmap(val bitmap: ImageBitmap?, val throwable: Throwable?) : Intent()

    /** Load a previously saved drawing from storage */
    object LoadDrawing : Intent()
}
