package io.ak1.drawbox.domain.usecase

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import io.ak1.drawbox.domain.model.Element
import io.ak1.drawbox.domain.model.ResizeHandle
import io.ak1.drawbox.domain.model.ShapeType
import io.ak1.drawbox.domain.model.bounds
import io.ak1.drawbox.domain.model.resizeBounds
import io.ak1.drawbox.domain.model.topmostHit
import io.ak1.drawbox.domain.model.translate
import io.ak1.drawbox.domain.model.withBounds
import io.ak1.drawbox.domain.model.withRotation

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
    fun insertNewPath(offset: Offset, color: Color, width: Float, alpha: Float): Element.Path {
        return Element.Path(
            points = listOf(offset),
            strokeColor = color,
            strokeWidth = width,
            alpha = alpha,
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
    fun insertNewShape(shapeType: ShapeType, offset: Offset, color: Color, width: Float): Element.Shape {
        return Element.Shape(
            shapeType = shapeType,
            points = listOf(offset),
            strokeColor = color,
            strokeWidth = width,
        )
    }

    fun updateLatestShape(newPoint: Offset, currentElements: List<Element>): List<Element> {
        if (currentElements.isEmpty()) return currentElements

        val lastElement = currentElements.last()
        return if (lastElement is Element.Shape) {
            val updatedPoints = lastElement.points + newPoint
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
}
