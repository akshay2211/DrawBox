@file:OptIn(ExperimentalUuidApi::class)

package io.ak1.drawbox.domain.model

import androidx.compose.ui.geometry.Offset
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
    data class Path(
        override val id: String = Uuid.random().toString(),
        override val type: String = "Path",
        val points: List<Offset>,
        val strokeColor: Color,
        val strokeWidth: Float,
        val alpha: Float,
        override val zIndex: Int = 0,
        override val rotation: Float = 0f,
        /** Stroke pattern for the freehand path. SOLID keeps the natural pencil feel. */
        val strokeStyle: StrokeStyle = StrokeStyle.SOLID,
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
    data class Shape(
        override val id: String = Uuid.random().toString(),
        override val type: String = "Shape",
        val shapeType: ShapeType,
        val points: List<Offset>,
        val strokeColor: Color,
        val fillColor: Color? = null,
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
    ) : Element()
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
