@file:OptIn(ExperimentalUuidApi::class)

package io.ak1.drawbox.domain.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Base sealed class for all drawable elements on the canvas.
 *
 * The [Element] type system allows the renderer to handle different drawing
 * primitives uniformly. Use pattern matching or `when` expressions to handle
 * different element types.
 *
 * @property id Unique identifier for this element
 * @property zIndex Layering order (higher values appear on top)
 * @property type String identifier of the element type for serialization
 *
 * @see Element.Path
 * @see Element.Shape
 */
sealed class Element {
    abstract val id: String
    abstract val zIndex: Int
    abstract val type: String

    /**
     * Rotation in degrees, applied around the element's axis-aligned bounding box
     * center at render time. Points stored on the element are in the unrotated
     * (logical) coordinate space.
     *
     * Default 0f preserves back-compat with drawings created before rotation
     * existed.
     */
    abstract val rotation: Float

    /**
     * Epoch milliseconds at which this element was first created. Used for
     * ordered replay (and later as a tie-break source for collab clocks). 0L
     * means "unknown" — element predates the timestamp field; consumers that
     * sort by createdAt should treat 0 as "earliest".
     */
    abstract val createdAt: Long

    /**
     * Epoch milliseconds at which any property of this element last changed.
     * On creation, equals [createdAt]. Bumped via [touched] on every
     * mutation path so sync layers can resolve concurrent edits with LWW
     * semantics.
     */
    abstract val modifiedAt: Long

    /**
     * Represents a freehand drawn path created in PEN mode.
     *
     * A path is a series of connected points drawn by the user. It's rendered
     * as a continuous curve with the specified stroke properties.
     *
     * @property points List of (x, y) coordinates along the path
     * @property strokeColor Color of the path stroke
     * @property strokeWidth Width/thickness of the stroke
     * @property alpha Opacity of the path (0.0 = transparent, 1.0 = opaque)
     * @property zIndex Layer ordering - higher values render on top
     *
     * @see androidx.compose.ui.graphics.drawscope.DrawScope.drawPath with Stroke style
     */
    /**
     * One sample along a freehand pen stroke.
     *
     * The required fields ([position], [width]) are what the OSS renderer
     * consumes. The optional fields are nullable so unit-pressure / mouse /
     * touch strokes carry zero overhead and serialize cleanly:
     *
     * - [tilt] — pen altitude angle in degrees, `0.0` = perpendicular to the
     *   screen, `90.0` = parallel. Reported by Apple Pencil and S Pen.
     *   Consumed by tilt-aware brush renderers (Pro). OSS renders without it.
     * - [azimuth] — pen rotation around its own axis in degrees, `0.0..360.0`.
     *   Reported by Apple Pencil. Drives calligraphy-style brush orientation
     *   in tilt-aware renderers. OSS renders without it.
     *
     * Velocity is intentionally NOT stored — it is derivable from position
     * deltas between successive samples and [Element.modifiedAt]; storing it
     * would duplicate data the renderer can compute on demand.
     */
    data class PathSample(
        val position: Offset,
        val width: Float,
        val tilt: Float? = null,
        val azimuth: Float? = null,
    )

    data class Path(
        override val id: String = Uuid.random().toString(),
        override val type: String = "Path",
        /**
         * Ordered samples along the stroke. Each sample carries both its
         * world-space position and its width, so pressure-modulated strokes
         * are represented natively without a parallel widths list. Uniform
         * strokes simply set every sample's width to [strokeWidth].
         */
        val samples: List<PathSample>,
        val strokeColor: Color,
        /**
         * Default width applied to new samples appended during the active
         * stroke. Existing samples carry their own width; setters that
         * mutate width across the whole stroke (`SetSelectedStrokeWidth`)
         * overwrite every sample's width to keep the stroke uniform.
         */
        val strokeWidth: Float,
        val alpha: Float,
        override val zIndex: Int = 0,
        override val rotation: Float = 0f,
        /** Stroke pattern for the freehand path. SOLID keeps the natural pencil feel. */
        val strokeStyle: StrokeStyle = StrokeStyle.SOLID,
        override val createdAt: Long = 0L,
        override val modifiedAt: Long = 0L,
    ) : Element()

    /**
     * Represents a geometric shape created in shape modes (RECTANGLE, CIRCLE, etc).
     *
     * A shape is defined by two points (start and end) and a shape type that
     * determines how the space between them is rendered. Shapes can be filled
     * or stroked based on the [fillColor] property.
     *
     * @property shapeType Type of shape to render (RECTANGLE, CIRCLE, etc)
     * @property points Two-element list containing start and end points:
     *   - points[0] = start point
     *   - points[1] = end point (last point)
     * @property strokeColor Color for shape outline/stroke
     * @property fillColor Optional fill color. If null, shape is stroked only
     * @property strokeWidth Width/thickness of the stroke
     * @property zIndex Layer ordering - higher values render on top
     *
     * @see ShapeType for available shape types
     * @see androidx.compose.ui.graphics.drawscope.DrawScope.drawRect, drawCircle, drawPath for rendering details
     */
    /**
     * Bitmap element placed on the canvas. `bytes` is the raw encoded payload
     * (PNG / JPEG / WebP) and the source of truth — the renderer decodes it
     * into an [androidx.compose.ui.graphics.ImageBitmap] on demand and caches
     * the result keyed by [id]. Resize / rotate / move / select reuse the
     * standard two-point AABB infrastructure.
     *
     * @property bytes Raw encoded image bytes; never decoded until rendered.
     * @property intrinsicSize Pixel dimensions of the source image, used to
     *   preserve aspect ratio on initial placement and to drive SVG export's
     *   `width` / `height` attributes.
     * @property points Two-corner AABB in world space: `[topLeft, bottomRight]`.
     * @property opacity Alpha applied at render time, independent of any
     *   alpha already baked into the source pixels.
     */
    data class Image(
        override val id: String = Uuid.random().toString(),
        override val type: String = "Image",
        val bytes: ByteArray,
        val intrinsicSize: Size,
        val points: List<Offset>,
        val opacity: Float = 1f,
        override val zIndex: Int = 0,
        override val rotation: Float = 0f,
        override val createdAt: Long = 0L,
        override val modifiedAt: Long = 0L,
    ) : Element() {
        // ByteArray defaults to reference equality, which would make two
        // identical-bytes copies of the same image compare unequal. Override
        // so undo/redo snapshots that re-decode the same payload behave
        // correctly. Keep id out of equality so element-level identity is
        // already determined by id elsewhere; here we want structural
        // equality for diffing.
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Image) return false
            if (id != other.id) return false
            if (intrinsicSize != other.intrinsicSize) return false
            if (points != other.points) return false
            if (opacity != other.opacity) return false
            if (zIndex != other.zIndex) return false
            if (rotation != other.rotation) return false
            if (createdAt != other.createdAt) return false
            if (modifiedAt != other.modifiedAt) return false
            return bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + intrinsicSize.hashCode()
            result = 31 * result + points.hashCode()
            result = 31 * result + opacity.hashCode()
            result = 31 * result + zIndex
            result = 31 * result + rotation.hashCode()
            result = 31 * result + createdAt.hashCode()
            result = 31 * result + modifiedAt.hashCode()
            result = 31 * result + bytes.size
            return result
        }
    }

    data class Shape(
        override val id: String = Uuid.random().toString(),
        override val type: String = "Shape",
        val shapeType: ShapeType,
        val points: List<Offset>,
        val strokeColor: Color,
        val fillColor: Color? = null,
        val strokeEnabled: Boolean = true,
        val strokeWidth: Float,
        override val zIndex: Int = 0,
        override val rotation: Float = 0f,
        /**
         * Corner radius in world pixels for [ShapeType.RECTANGLE] and
         * [ShapeType.TRIANGLE]. Ignored for circle, line, arrow. 0 = sharp.
         * Auto-clamped at render time so it never exceeds half the shortest
         * adjacent edge.
         */
        val cornerRadius: Float = 0f,
        /**
         * Stroke pattern for non-filled rendering. Solid by default. For arrows
         * the body line follows this style; the head stays solid for clarity.
         */
        val strokeStyle: StrokeStyle = StrokeStyle.SOLID,
        /**
         * Offset of the curve's mid-point from the straight-line midpoint of
         * `points[0]` → `points.last()`. Only meaningful for [ShapeType.LINE]
         * and [ShapeType.ARROW]; ignored otherwise. `Offset.Zero` (default) =
         * straight. Non-zero = the line/arrow renders as a quadratic bezier
         * arcing through `midpoint + bend`.
         */
        val bend: Offset = Offset.Zero,
        /**
         * ID of the element this arrow's start endpoint is bound to. When set
         * and the bound element moves/resizes/rotates, the arrow's start point
         * follows. Only honored for [ShapeType.ARROW].
         */
        val startBinding: String? = null,
        /**
         * ID of the element this arrow's end endpoint is bound to. Behaves like
         * [startBinding] for the end point. Only honored for [ShapeType.ARROW].
         */
        val endBinding: String? = null,
        override val createdAt: Long = 0L,
        override val modifiedAt: Long = 0L,
    ) : Element()
}

/**
 * The spatial positions of every sample in a freehand stroke, in order.
 * Use this when the caller only cares about geometry (bounds, hit testing,
 * path tessellation); reach for [Element.Path.samples] directly when widths
 * are also needed.
 */
val Element.Path.positions: List<Offset> get() = samples.map { it.position }

/**
 * Stamp [Element.modifiedAt] with [now] (defaults to current epoch ms).
 * Call this on every mutation site so sync layers see a fresh LWW timestamp.
 */
@OptIn(kotlin.time.ExperimentalTime::class)
fun Element.touched(now: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()): Element =
    when (this) {
        is Element.Path -> copy(modifiedAt = now)
        is Element.Shape -> copy(modifiedAt = now)
        is Element.Image -> copy(modifiedAt = now)
    }

/**
 * Defines how [Element.Shape] is rendered on the canvas.
 *
 * Each shape type has specific rendering rules and geometry calculations:
 * - RECTANGLE: Axis-aligned rectangle using top-left and dimensions
 * - CIRCLE: Circle with center calculated from start/end points
 * - TRIANGLE: Isosceles triangle pointing upward
 * - ARROW: Arrow with line and scaled arrowhead
 * - LINE: Straight line between two points
 *
 * @see Element.Shape
 * @see io.ak1.drawbox.DrawBox rendering functions (drawRect, drawCircle, drawTriangle, etc)
 */
sealed class ShapeType {
    /** Axis-aligned rectangle shape */
    data object RECTANGLE : ShapeType()
    /** Circular shape */
    data object CIRCLE : ShapeType()
    /** Triangle shape */
    data object TRIANGLE : ShapeType()
    /** Arrow shape with intelligent head sizing */
    data object ARROW : ShapeType()
    /** Straight line shape */
    data object LINE : ShapeType()
}

/**
 * Stroke pattern applied when rendering a stroked (non-filled) [Element.Shape].
 *
 * Has no effect on filled shapes, and is intentionally not applied to
 * [Element.Path] (freehand strokes stay continuous to preserve pencil feel).
 */
enum class StrokeStyle { SOLID, DASHED, DOTTED }
