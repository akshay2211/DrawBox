package io.ak1.drawbox.domain.usecase

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.ak1.drawbox.domain.model.Element
import io.ak1.drawbox.domain.model.State
import io.ak1.drawbox.domain.model.ShapeType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UseCaseTest {

    private val useCase = UseCase()

    private fun createTestPath(): Element.Path {
        return Element.Path(
            points = listOf(Offset(0f, 0f)),
            strokeColor = Color.Red,
            strokeWidth = 10f,
            alpha = 1f,
        )
    }

    @Test
    fun testAddElement() {
        val currentElements = emptyList<Element>()
        val newPath = createTestPath()

        val result = useCase.addElement(newPath, currentElements)

        assertEquals(1, result.size)
        assertTrue(result[0] is Element.Path)
    }

    @Test
    fun testAddMultipleElements() {
        val firstPath = createTestPath()
        val secondPath = createTestPath()

        var elements = useCase.addElement(firstPath, emptyList())
        elements = useCase.addElement(secondPath, elements)

        assertEquals(2, elements.size)
        assertEquals(0, elements[0].zIndex)
        assertEquals(1, elements[1].zIndex)
    }

    @Test
    fun testUpdateElement() {
        val path = createTestPath()
        val elements = listOf(path)
        val updatedPath = path.copy(strokeColor = Color.Blue)

        val result = useCase.updateElement(updatedPath, elements)

        assertEquals(1, result.size)
        assertEquals(Color.Blue, (result[0] as Element.Path).strokeColor)
    }

    @Test
    fun testDeleteElement() {
        val path = createTestPath()
        val elements = listOf(path)

        val result = useCase.deleteElement(path.id, elements)

        assertTrue(result.isEmpty())
    }

    @Test
    fun testInsertNewPath() {
        val offset = Offset(10f, 20f)
        val color = Color.Green
        val width = 5f
        val alpha = 0.8f

        val result = useCase.insertNewPath(offset, color, width, alpha)

        assertEquals(offset, result.points[0])
        assertEquals(color, result.strokeColor)
        assertEquals(width, result.strokeWidth)
        assertEquals(alpha, result.alpha)
    }

    @Test
    fun testUpdateLatestPath() {
        val path = createTestPath()
        val elements = listOf(path)
        val newPoint = Offset(10f, 10f)

        val result = useCase.updateLatestPath(newPoint, elements)

        assertEquals(2, (result[0] as Element.Path).points.size)
        assertEquals(newPoint, (result[0] as Element.Path).points[1])
    }

    @Test
    fun testUpdateLatestPathEmpty() {
        val elements = emptyList<Element>()
        val newPoint = Offset(10f, 10f)

        val result = useCase.updateLatestPath(newPoint, elements)

        assertTrue(result.isEmpty())
    }

    @Test
    fun testInsertNewShape() {
        val offset = Offset(0f, 0f)
        val color = Color.Red
        val width = 2f

        val result = useCase.insertNewShape(ShapeType.RECTANGLE, offset, color, width)

        assertEquals(ShapeType.RECTANGLE, result.shapeType)
        assertEquals(offset, result.points[0])
        assertEquals(color, result.strokeColor)
    }

    @Test
    fun testUndo() {
        val path = createTestPath()
        val elements = listOf(path)
        val undoStack = emptyList<Element>()

        val (newElements, newUndoStack) = useCase.undo(elements, undoStack)

        assertTrue(newElements.isEmpty())
        assertEquals(1, newUndoStack.size)
    }

    @Test
    fun testUndoEmpty() {
        val elements = emptyList<Element>()
        val undoStack = emptyList<Element>()

        val (newElements, newUndoStack) = useCase.undo(elements, undoStack)

        assertTrue(newElements.isEmpty())
        assertTrue(newUndoStack.isEmpty())
    }

    @Test
    fun testRedo() {
        val path = createTestPath()
        val elements = emptyList<Element>()
        val undoStack = listOf(path)

        val (newElements, newUndoStack) = useCase.redo(elements, undoStack)

        assertEquals(1, newElements.size)
        assertTrue(newUndoStack.isEmpty())
    }

    @Test
    fun testRedoEmpty() {
        val elements = emptyList<Element>()
        val undoStack = emptyList<Element>()

        val (newElements, newUndoStack) = useCase.redo(elements, undoStack)

        assertTrue(newElements.isEmpty())
        assertTrue(newUndoStack.isEmpty())
    }
}
