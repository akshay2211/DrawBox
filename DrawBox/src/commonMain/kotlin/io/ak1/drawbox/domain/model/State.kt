package io.ak1.drawbox.domain.model

import androidx.compose.ui.geometry.Rect
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
 * - [SELECT]: Selects existing elements. Tap to select, drag to move, drag a handle
 *   to resize or rotate, drag on empty space to marquee-select multiple elements.
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
    /** Selection mode - tap / drag to select, move, resize, rotate existing elements */
    data object SELECT : Mode()
    /** Pan mode - drag pans the camera; useful for navigating the infinite canvas */
    data object PAN : Mode()
}

/**
 * Resize handle positions around a selected element's bounding box. Eight
 * handles in total: four corners and four edge midpoints.
 */
enum class ResizeHandle {
    TopLeft, Top, TopRight, Right, BottomRight, Bottom, BottomLeft, Left,
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
 * @property history Stack of prior element snapshots for undo. Newest entry is last.
 * @property future Stack of element snapshots for redo (populated when Undo runs).
 * @property selectedIds IDs of currently selected elements. Drives selection chrome
 *           rendering and the targets of move/resize/rotate/delete intents.
 * @property marqueeRect Transient rectangle drawn while a marquee selection is in
 *           progress. Null when no marquee is active. Not undoable.
 * @property strokeColor Color for drawing lines and shape strokes
 * @property strokeWidth Width/thickness of strokes in pixels
 * @property opacity Alpha channel for drawing (0.0 = transparent, 1.0 = opaque)
 * @property bgColor Solid background color of the canvas (painted underneath [bgPattern])
 * @property bgPattern Optional repeating image tiled above [bgColor] and below all elements;
 *           runtime decoration only and not persisted by JSON/SVG export
 * @property mode Current drawing mode determining what gets created on user input
 *
 * @see Element
 * @see Intent
 * @see Mode
 * @see BackgroundPattern
 */
data class State(
    val elements: List<Element> = emptyList(),
    val history: List<List<Element>> = emptyList(),
    val future: List<List<Element>> = emptyList(),
    val selectedIds: Set<String> = emptySet(),
    val marqueeRect: Rect? = null,
    val viewport: Viewport = Viewport(),
    val tempPanActive: Boolean = false,
    val strokeColor: Color = Color.Red,
    val strokeWidth: Float = 10f,
    val currentItemCornerRadius: Float = 0f,
    val currentItemStrokeStyle: StrokeStyle = StrokeStyle.SOLID,
    val opacity: Float = 1f,
    val bgColor: Color = Color.Black,
    val bgPattern: BackgroundPattern? = null,
    val mode: Mode = Mode.PEN,
){
    internal var invokeBitmap :(() -> Unit) = {}
}

/**
 * The mode currently driving user input. Equals [State.mode] unless the user
 * is holding the space bar (or has otherwise activated a temporary pan), in
 * which case it returns [Mode.PAN]. The original [State.mode] is preserved so
 * release of the temp-pan modifier restores the previous tool.
 */
val State.effectiveMode: Mode get() = if (tempPanActive) Mode.PAN else mode

/** Maximum number of snapshots kept in [State.history] and [State.future]. */
internal const val HISTORY_CAP: Int = 100
