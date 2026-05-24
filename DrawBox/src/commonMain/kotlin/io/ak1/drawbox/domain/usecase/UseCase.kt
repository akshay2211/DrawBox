package io.ak1.drawbox.domain.usecase

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.ak1.drawbox.domain.model.Element
import io.ak1.drawbox.domain.model.ShapeType

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

    // History operations
    fun undo(
        elements: List<Element>,
        undoStack: List<Element>,
    ): Pair<List<Element>, List<Element>> {
        if (elements.isEmpty()) return elements to undoStack
        val lastElement = elements.last()
        return (elements.dropLast(1)) to (undoStack + lastElement)
    }

    fun redo(
        elements: List<Element>,
        undoStack: List<Element>,
    ): Pair<List<Element>, List<Element>> {
        if (undoStack.isEmpty()) return elements to undoStack
        val redoElement = undoStack.last()
        return (elements + redoElement) to undoStack.dropLast(1)
    }
}
