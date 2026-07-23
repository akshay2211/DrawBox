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
 * - [ERASER]: Tap or drag to delete elements whose body intersects the eraser
 *   radius. Whole-element (object) eraser — strokes/shapes are removed atomically.
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
    /**
     * Plain-text mode — tap on the canvas inserts an empty [Element.Text]
     * at the tap position. The sample app opens a modal text field to
     * gather the actual content; embedders can mirror that pattern or
     * provide an inline editor (out of scope for OSS v1).
     */
    data object TEXT : Mode()
    /**
     * Object eraser — tap or drag to delete any element whose body intersects
     * the eraser radius. Whole-element (not pixel-level): a single hit removes
     * the entire stroke or shape. Undoable as one gesture.
     */
    data object ERASER : Mode()
}

/**
 * Per-tool snapshot captured by [State.toolMemory]. Restored to [State]'s
 * top-level style fields when the user swaps back to a tool.
 */
data class ToolSettings(
    val strokeColor: Color,
    val strokeWidth: Float,
    val strokeStyle: StrokeStyle,
    val cornerRadius: Float,
    val fillColor: Color?,
    val strokeEnabled: Boolean,
    val fontSize: Float,
    val fontFamilyKey: String,
    val textAlignment: TextAlignment,
)

/** Modes that carry meaningful per-tool style. Utility modes are excluded. */
private val PersistedModes: Set<Mode> = setOf(
    Mode.PEN, Mode.RECTANGLE, Mode.CIRCLE, Mode.TRIANGLE,
    Mode.ARROW, Mode.LINE, Mode.TEXT,
)

/**
 * Whether [mode] participates in [State.toolMemory] save/restore on
 * [Intent.SetMode].
 */
fun Mode.persistsSettings(): Boolean = this in PersistedModes

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
    /**
     * Default fill color applied to new [Element.Shape]s (rect/circle/triangle).
     * `null` means stroke-only — the shape renders without a fill. Mutated by
     * [Intent.SetFillColor]; the selection form [Intent.SetSelectedFillColor]
     * writes to selected elements instead.
     */
    val currentItemFillColor: Color? = null,
    /**
     * Whether the outline pass is drawn on new [Element.Shape]s. When `false`,
     * new shapes render fill-only. Mutated by [Intent.SetStrokeEnabled]; the
     * selection form [Intent.SetSelectedStrokeEnabled] writes to selected
     * elements instead.
     */
    val currentItemStrokeEnabled: Boolean = true,
    /**
     * Per-tool memory: each drawing mode (PEN, RECTANGLE, CIRCLE, TRIANGLE,
     * ARROW, LINE, TEXT) snapshots its own color / width / style / fill /
     * corner / text settings. On [Intent.SetMode] the outgoing mode's current
     * top-level fields are captured here, and the incoming mode's saved
     * settings are restored to the top-level fields. Non-drawing modes
     * (SELECT, ERASER, PAN) don't participate — they pass through settings
     * untouched. Users get the "each pen remembers its own color" behavior
     * Samsung Notes / OPPO Notes use.
     */
    val toolMemory: Map<Mode, ToolSettings> = emptyMap(),
    /**
     * Default font size in world pixels applied to text elements inserted via
     * [Mode.TEXT]. Mutated by [Intent.SetFontSize] (the non-selection form);
     * the selection form [Intent.SetSelectedFontSize] writes to the selected
     * element instead and leaves this default untouched.
     */
    val currentItemFontSize: Float = 24f,
    /**
     * Default font family key applied to new text elements. See
     * [Intent.SetFontFamily]. Always resolves via the host's
     * [io.ak1.drawbox.text.FontRegistry] at render time, so an unregistered
     * key here falls back to `sans` without crashing the canvas.
     */
    val currentItemFontFamilyKey: String = DEFAULT_FONT_FAMILY_KEY,
    /**
     * Default horizontal alignment for new text elements; see
     * [Intent.SetTextAlignment].
     */
    val currentItemTextAlignment: TextAlignment = TextAlignment.LEFT,
    val opacity: Float = 1f,
    val bgColor: Color = Color.Black,
    val bgPattern: BackgroundPattern? = null,
    val mode: Mode = Mode.PEN,
    /**
     * World-space radius used by [Mode.ERASER] for hit-testing. Pressure-aware
     * platforms can modulate this at the gesture layer without persisting the
     * modulated value here.
     */
    val eraserSize: Float = 20f,
    /**
     * Within a single erase gesture: has any tick actually removed an element
     * yet? Used to guarantee at most one history snapshot per gesture, taken
     * lazily on the first hit. A tap or drag that never intersects an element
     * therefore consumes no undo slot. Cleared by [Intent.BeginErase] and
     * [Intent.EndErase]; not persisted.
     */
    val erasingSessionDirty: Boolean = false,
)

/**
 * The mode currently driving user input. Equals [State.mode] unless the user
 * is holding the space bar (or has otherwise activated a temporary pan), in
 * which case it returns [Mode.PAN]. The original [State.mode] is preserved so
 * release of the temp-pan modifier restores the previous tool.
 */
val State.effectiveMode: Mode get() = if (tempPanActive) Mode.PAN else mode

/** Maximum number of snapshots kept in [State.history] and [State.future]. */
internal const val HISTORY_CAP: Int = 100
