package io.ak1.drawbox.domain.model

import androidx.compose.ui.graphics.Color

/**
 * Represents the current drawing mode, determining what type of element is created
 * when the user interacts with the canvas.
 *
 * Each mode maps to a specific [Element] type and rendering behavior:
 * - [PEN]: Creates [Element.Path] for freehand drawing
 * - [RECTANGLE]: Creates [Element.Shape] with [ShapeType.RECTANGLE]
 * - [CIRCLE]: Creates [Element.Shape] with [ShapeType.CIRCLE]
 * - [TRIANGLE]: Creates [Element.Shape] with [ShapeType.TRIANGLE]
 * - [ARROW]: Creates [Element.Shape] with [ShapeType.ARROW]
 * - [LINE]: Creates [Element.Shape] with [ShapeType.LINE]
 *
 * The mode is read from [State.mode] and passed to [DrawBox], which uses it to
 * determine how to interpret user gestures.
 */
sealed class Mode {
    /** Freehand drawing mode - creates continuous paths from user input */
    data object PEN : Mode()
    /** Rectangle shape mode */
    data object RECTANGLE : Mode()
    /** Circle shape mode */
    data object CIRCLE : Mode()
    /** Triangle shape mode */
    data object TRIANGLE : Mode()
    /** Arrow shape mode with intelligent head sizing */
    data object ARROW : Mode()
    /** Line shape mode */
    data object LINE : Mode()
}

/**
 * Represents the complete immutable state of the drawing canvas.
 *
 * All drawing operations produce a new [State] instance rather than mutating
 * the existing one, following functional programming principles. This ensures:
 * - Clear state history for undo/redo
 * - Predictable state changes
 * - Easy debugging and testing
 *
 * @property elements List of all drawable elements currently on canvas
 * @property undoStack Stack of previous states for redo functionality
 * @property strokeColor Color for drawing lines and shape strokes
 * @property strokeWidth Width/thickness of strokes in pixels
 * @property opacity Alpha channel for drawing (0.0 = transparent, 1.0 = opaque)
 * @property bgColor Background color of the canvas
 * @property mode Current drawing mode determining what gets created on user input
 *
 * @see Element
 * @see Intent
 * @see Mode
 */
data class State(
    val elements: List<Element> = emptyList(),
    val undoStack: List<Element> = emptyList(),
    val strokeColor: Color = Color.Red,
    val strokeWidth: Float = 10f,
    val opacity: Float = 1f,
    val bgColor: Color = Color.Black,
    val mode: Mode = Mode.PEN,
){
    internal var invokeBitmap :(() -> Unit) = {}
}
