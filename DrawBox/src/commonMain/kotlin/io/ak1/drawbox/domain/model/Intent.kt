package io.ak1.drawbox.domain.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
     */
    data class UpdateLatestPath(val newPoint: Offset) : Intent()

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
