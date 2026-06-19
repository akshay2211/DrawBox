package io.ak1.drawbox.domain.usecase

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import io.ak1.drawbox.domain.model.Element
import io.ak1.drawbox.domain.model.ResizeHandle
import io.ak1.drawbox.domain.model.ShapeType
import io.ak1.drawbox.domain.model.StrokeStyle
import io.ak1.drawbox.domain.model.boundaryPointToward
import io.ak1.drawbox.domain.model.bounds
import io.ak1.drawbox.domain.model.resizeBounds
import io.ak1.drawbox.domain.model.topmostHit
import io.ak1.drawbox.domain.model.translate
import io.ak1.drawbox.domain.model.withBounds
import io.ak1.drawbox.domain.model.withRotation
import kotlin.math.sqrt

class UseCase {
    // Element operations
    fun addElement(element: Element, currentElements: List<Element>): List<Element> {
        val newElement = when (element) {
            is Element.Path -> element.copy(zIndex = currentElements.size)
            is Element.Shape -> element.copy(zIndex = currentElements.size)
        }
        return currentElements + newElement
    }

    fun updateElement(element: Element, currentElements: List<Element>): List<Element> {
        return currentElements.map { if (it.id == element.id) element else it }
    }

    fun deleteElement(elementId: String, currentElements: List<Element>): List<Element> {
        return currentElements.filter { it.id != elementId }
    }

    // Path operations
    fun insertNewPath(
        offset: Offset,
        color: Color,
        width: Float,
        alpha: Float,
        strokeStyle: StrokeStyle = StrokeStyle.SOLID,
    ): Element.Path {
        return Element.Path(
            points = listOf(offset),
            strokeColor = color,
            strokeWidth = width,
            alpha = alpha,
            strokeStyle = strokeStyle,
        )
    }

    fun updateLatestPath(newPoint: Offset, currentElements: List<Element>): List<Element> {
        if (currentElements.isEmpty()) return currentElements

        val lastElement = currentElements.last()
        return if (lastElement is Element.Path) {
            val updatedPoints = lastElement.points + newPoint
            currentElements.dropLast(1) + lastElement.copy(points = updatedPoints)
        } else {
            currentElements
        }
    }

    // Shape operations
    fun insertNewShape(
        shapeType: ShapeType,
        offset: Offset,
        color: Color,
        width: Float,
        cornerRadius: Float = 0f,
        strokeStyle: StrokeStyle = StrokeStyle.SOLID,
    ): Element.Shape {
        return Element.Shape(
            shapeType = shapeType,
            points = listOf(offset),
            strokeColor = color,
            strokeWidth = width,
            cornerRadius = cornerRadius,
            strokeStyle = strokeStyle,
        )
    }

    fun updateLatestShape(newPoint: Offset, currentElements: List<Element>): List<Element> {
        if (currentElements.isEmpty()) return currentElements

        val lastElement = currentElements.last()
        return if (lastElement is Element.Shape) {
            // Shapes use exactly two points (start + current end). Appending on
            // every drag tick — as the original code did — bloats `points` with
            // the cursor's whole path, leaving bounds() bigger than the rendered
            // shape and ballooning serialized JSON. Replace the second slot.
            val start = lastElement.points.firstOrNull() ?: newPoint
            val updatedPoints = listOf(start, newPoint)
            currentElements.dropLast(1) + lastElement.copy(points = updatedPoints)
        } else {
            currentElements
        }
    }

    // Selection operations

    /** Topmost element at `point`, or null if nothing was hit. */
    fun hitTopmost(elements: List<Element>, point: Offset, tolerance: Float): Element? =
        topmostHit(elements, point, tolerance)

    /** Set of element IDs whose bounding box intersects `rect`. */
    fun selectInRect(elements: List<Element>, rect: Rect): Set<String> {
        val norm = Rect(
            left = minOf(rect.left, rect.right),
            top = minOf(rect.top, rect.bottom),
            right = maxOf(rect.left, rect.right),
            bottom = maxOf(rect.top, rect.bottom),
        )
        return elements
            .filter { it.bounds().overlaps(norm) }
            .map { it.id }
            .toSet()
    }

    /** Translate every element whose id is in `ids` by `delta`. */
    fun translateSelected(
        elements: List<Element>,
        ids: Set<String>,
        delta: Offset,
    ): List<Element> {
        if (ids.isEmpty()) return elements
        return elements.map { if (it.id in ids) it.translate(delta) else it }
    }

    /** Replace the bounds of a single element. */
    fun setElementBounds(
        elements: List<Element>,
        id: String,
        bounds: Rect,
    ): List<Element> = elements.map { if (it.id == id) it.withBounds(bounds) else it }

    /** Replace the rotation of a single element (degrees). */
    fun setElementRotation(
        elements: List<Element>,
        id: String,
        degrees: Float,
    ): List<Element> = elements.map { if (it.id == id) it.withRotation(degrees) else it }

    /**
     * Recompute new bounds from a resize-handle drag and apply them. Single-
     * element only; multi-selection callers should iterate or refuse.
     */
    fun resizeSingle(
        elements: List<Element>,
        id: String,
        handle: ResizeHandle,
        pointer: Offset,
    ): List<Element> {
        val target = elements.firstOrNull { it.id == id } ?: return elements
        val newBounds = resizeBounds(target.bounds(), target.rotation, handle, pointer)
        return setElementBounds(elements, id, newBounds)
    }

    /** Delete every element whose id is in `ids`. */
    fun deleteSelected(elements: List<Element>, ids: Set<String>): List<Element> =
        elements.filter { it.id !in ids }

    /** Promote selected elements to the top of the z-order. */
    fun bringToFront(elements: List<Element>, ids: Set<String>): List<Element> {
        if (ids.isEmpty()) return elements
        val maxZ = elements.maxOfOrNull { it.zIndex } ?: 0
        var next = maxZ + 1
        return elements.map { el ->
            if (el.id in ids) when (el) {
                is Element.Path -> el.copy(zIndex = next++)
                is Element.Shape -> el.copy(zIndex = next++)
            } else el
        }
    }

    /** Push selected elements to the bottom of the z-order. */
    fun sendToBack(elements: List<Element>, ids: Set<String>): List<Element> {
        if (ids.isEmpty()) return elements
        val minZ = elements.minOfOrNull { it.zIndex } ?: 0
        var next = minZ - 1
        // Walk in current draw order so relative order among selected is preserved.
        val orderedSelected = elements.filter { it.id in ids }.map { it.id }
        val assignments = orderedSelected.reversed().associateWith { next-- }
        return elements.map { el ->
            val newZ = assignments[el.id]
            if (newZ != null) when (el) {
                is Element.Path -> el.copy(zIndex = newZ)
                is Element.Shape -> el.copy(zIndex = newZ)
            } else el
        }
    }

    /** Recolor every selected element's stroke. */
    fun setSelectedStrokeColor(
        elements: List<Element>,
        ids: Set<String>,
        color: Color,
    ): List<Element> = elements.map { el ->
        if (el.id !in ids) el else when (el) {
            is Element.Path -> el.copy(strokeColor = color)
            is Element.Shape -> el.copy(strokeColor = color)
        }
    }

    /** Re-stroke every selected element. */
    fun setSelectedStrokeWidth(
        elements: List<Element>,
        ids: Set<String>,
        width: Float,
    ): List<Element> = elements.map { el ->
        if (el.id !in ids) el else when (el) {
            is Element.Path -> el.copy(strokeWidth = width)
            is Element.Shape -> el.copy(strokeWidth = width)
        }
    }

    /**
     * Set the corner radius on selected RECTANGLE / TRIANGLE shapes. Other
     * element types in the selection are left untouched.
     */
    fun setSelectedCornerRadius(
        elements: List<Element>,
        ids: Set<String>,
        radius: Float,
    ): List<Element> = elements.map { el ->
        if (el.id !in ids) return@map el
        if (el is Element.Shape && el.shapeType.supportsCornerRadius()) {
            el.copy(cornerRadius = radius)
        } else el
    }

    /** Set the stroke pattern on every selected element (shape OR freehand path). */
    fun setSelectedStrokeStyle(
        elements: List<Element>,
        ids: Set<String>,
        style: StrokeStyle,
    ): List<Element> = elements.map { el ->
        if (el.id !in ids) return@map el
        when (el) {
            is Element.Shape -> el.copy(strokeStyle = style)
            is Element.Path -> el.copy(strokeStyle = style)
        }
    }

    // ===== Line / arrow editing =====

    /** Overwrite the points list of a single element. */
    fun setElementPoints(
        elements: List<Element>,
        id: String,
        newPoints: List<Offset>,
    ): List<Element> = elements.map { el ->
        if (el.id != id) el else when (el) {
            is Element.Path -> el.copy(points = newPoints)
            is Element.Shape -> el.copy(points = newPoints)
        }
    }

    /** Update the curve deflection of a single LINE/ARROW shape. */
    fun setLineBend(
        elements: List<Element>,
        id: String,
        bend: Offset,
    ): List<Element> = elements.map { el ->
        if (el.id == id && el is Element.Shape && el.shapeType.isLineLike()) {
            el.copy(bend = bend)
        } else el
    }

    /**
     * Attach connector bindings to an arrow: each endpoint binds to the topmost
     * non-arrow/line shape it sits inside, or to null if it sits in empty space.
     * When this call is what FIRST makes the arrow connect both endpoints, it
     * also seeds a default perpendicular bend so the connector reads as a curve
     * rather than overlapping with the straight center-to-center line.
     */
    fun finalizeArrowBindings(
        elements: List<Element>,
        arrowId: String,
    ): List<Element> {
        val target = elements.firstOrNull { it.id == arrowId } as? Element.Shape ?: return elements
        if (target.shapeType != ShapeType.ARROW || target.points.size < 2) return elements
        val candidates = elements
            .asSequence()
            .filterIsInstance<Element.Shape>()
            .filter { it.id != arrowId && it.shapeType.isBindable() }
            .sortedByDescending { it.zIndex }
            .toList()
        // Strict bounds.contains() is too unforgiving: dropping an arrow
        // endpoint a few pixels outside a shape produces no binding even when
        // the user clearly meant to connect there. Inflate the AABB by a
        // tolerance so near-misses bind, then propagateBindings will snap the
        // endpoint to the actual visible boundary.
        val startId = candidates.firstOrNull {
            it.bounds().inflate(BINDING_HIT_TOLERANCE).contains(target.points[0])
        }?.id
        val endId = candidates.firstOrNull {
            it.bounds().inflate(BINDING_HIT_TOLERANCE).contains(target.points.last())
        }?.id

        val freshFullyConnected = startId != null && endId != null &&
            (target.startBinding != startId || target.endBinding != endId) &&
            target.bend == Offset.Zero
        val seededBend = if (freshFullyConnected && startId != null && endId != null) {
            // Predict where propagateBindings will snap the endpoints to and size
            // the bend relative to THAT segment. Using the raw drag points (often
            // near shape centers) overshoots once snapping moves the endpoints to
            // the boundary — the curve would bulge way past the visible line.
            val startShape = candidates.first { it.id == startId }
            val endShape = candidates.first { it.id == endId }
            val startSnapped = startShape.boundaryPointToward(endShape.bounds().center)
            val endSnapped = endShape.boundaryPointToward(startShape.bounds().center)
            defaultConnectorBend(startSnapped, endSnapped)
        } else target.bend

        return elements.map { el ->
            if (el.id != arrowId) el
            else (el as Element.Shape).copy(
                startBinding = startId,
                endBinding = endId,
                bend = seededBend,
            )
        }
    }

    /**
     * Update every arrow's endpoints so they terminate on the bound shape's
     * boundary along the line toward the other endpoint (or its bound center).
     * Idempotent — safe to call after any element mutation. Detaches bindings
     * whose target no longer exists.
     */
    fun propagateBindings(elements: List<Element>): List<Element> {
        val byId = elements.associateBy { it.id }
        return elements.map { el ->
            if (el !is Element.Shape || el.shapeType != ShapeType.ARROW) return@map el
            if (el.startBinding == null && el.endBinding == null) return@map el
            if (el.points.size < 2) return@map el
            val startTarget = el.startBinding?.let { byId[it] as? Element.Shape }
            val endTarget = el.endBinding?.let { byId[it] as? Element.Shape }

            // Direction reference for boundary clipping: the OTHER endpoint's
            // bound center if available, else its raw position. We compute the
            // boundary intersection from each bound shape's center toward that
            // reference point so the arrow visually starts/ends at the edge.
            val endRef = endTarget?.bounds()?.center ?: el.points.last()
            val startRef = startTarget?.bounds()?.center ?: el.points[0]
            val newStart = startTarget?.boundaryPointToward(endRef) ?: el.points[0]
            val newEnd = endTarget?.boundaryPointToward(startRef) ?: el.points.last()
            el.copy(
                points = listOf(newStart, newEnd),
                startBinding = if (startTarget == null) null else el.startBinding,
                endBinding = if (endTarget == null) null else el.endBinding,
            )
        }
    }
}

/**
 * Slop, in world pixels, applied to a bindable shape's AABB when deciding
 * whether an arrow endpoint counts as "dropped on" that shape. ~24 world-px is
 * enough that hand-eye inaccuracy doesn't break the connector intent but small
 * enough that distinct shapes don't compete for the same drop.
 */
private const val BINDING_HIT_TOLERANCE: Float = 24f

/** Perpendicular bend ~20% of the line length, counter-clockwise of direction. */
private fun defaultConnectorBend(start: Offset, end: Offset): Offset {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val len = sqrt(dx * dx + dy * dy)
    if (len < 0.5f) return Offset.Zero
    val ux = dx / len
    val uy = dy / len
    val mag = len * 0.2f
    // (-uy, ux) is the counter-clockwise perpendicular.
    return Offset(-uy * mag, ux * mag)
}

private fun ShapeType.isBindable(): Boolean = when (this) {
    ShapeType.RECTANGLE, ShapeType.CIRCLE, ShapeType.TRIANGLE -> true
    ShapeType.LINE, ShapeType.ARROW -> false
}

private fun ShapeType.isLineLike(): Boolean =
    this == ShapeType.LINE || this == ShapeType.ARROW

/** Shape types whose visual outline can be rounded at the corners. */
fun ShapeType.supportsCornerRadius(): Boolean =
    this == ShapeType.RECTANGLE || this == ShapeType.TRIANGLE
