package io.ak1.drawbox.domain.usecase

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.ak1.drawbox.domain.model.Element
import io.ak1.drawbox.domain.model.ShapeType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
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
    fun testEraseAtRemovesPathInRadius() {
        val path = Element.Path(
            points = listOf(Offset(0f, 0f), Offset(100f, 0f)),
            strokeColor = Color.Red,
            strokeWidth = 4f,
            alpha = 1f,
        )
        val elements = useCase.addElement(path, emptyList())

        val result = useCase.eraseAt(elements, Offset(50f, 0f), radius = 5f)

        assertEquals(0, result.size)
    }

    @Test
    fun testEraseAtMissReturnsSameInstance() {
        val path = Element.Path(
            points = listOf(Offset(0f, 0f), Offset(100f, 0f)),
            strokeColor = Color.Red,
            strokeWidth = 1f,
            alpha = 1f,
        )
        val elements = useCase.addElement(path, emptyList())

        // Far away from the path: nothing hit. Reducer relies on this
        // identity contract to skip no-op state copies.
        val result = useCase.eraseAt(elements, Offset(500f, 500f), radius = 5f)

        assertSame(elements, result)
    }

    @Test
    fun testEraseAtPreservesUnhitElements() {
        val hit = Element.Path(
            points = listOf(Offset(0f, 0f), Offset(50f, 0f)),
            strokeColor = Color.Red,
            strokeWidth = 2f,
            alpha = 1f,
        )
        val miss = Element.Path(
            points = listOf(Offset(0f, 200f), Offset(50f, 200f)),
            strokeColor = Color.Blue,
            strokeWidth = 2f,
            alpha = 1f,
        )
        val elements = useCase.addElement(miss, useCase.addElement(hit, emptyList()))

        val result = useCase.eraseAt(elements, Offset(25f, 0f), radius = 5f)

        assertEquals(1, result.size)
        assertEquals(miss.id, result[0].id)
    }

}
