package io.ak1.drawbox.domain.model

import androidx.compose.ui.geometry.Offset
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

    /** Change the opacity/alpha for new drawings (0.0 to 1.0) */
    data class SetOpacity(val opacity: Float) : Intent()

    /** Change the canvas background color */
    data class SetBgColor(val bgColor: Color) : Intent()

    /**
     * Switch to a different drawing mode.
     *
     * Changes what type of element is created on the next user interaction.
     * For example, switching from [Mode.PEN] to [Mode.RECTANGLE] causes the
     * next drag to create a rectangle instead of a path.
     */
    data class SetMode(val mode: Mode) : Intent()

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
