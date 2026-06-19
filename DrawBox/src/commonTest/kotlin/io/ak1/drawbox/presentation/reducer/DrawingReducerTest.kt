package io.ak1.drawbox.presentation.reducer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.ak1.drawbox.domain.model.Element
import io.ak1.drawbox.domain.model.Intent
import io.ak1.drawbox.domain.model.State
import io.ak1.drawbox.domain.model.ShapeType
import io.ak1.drawbox.domain.usecase.UseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DrawingReducerTest {

    private val useCase = UseCase()
    private val reducer = Reducer(useCase)

    private fun createTestPath(): Element.Path {
        return Element.Path(
            points = listOf(Offset(0f, 0f)),
            strokeColor = Color.Red,
            strokeWidth = 10f,
            alpha = 1f,
        )
    }

    @Test
    fun testInsertNewPathIntent() {
        val initialState = State()
        val offset = Offset(0f, 0f)
        val intent = Intent.InsertNewPath(offset)

        val newState = reducer.reduce(initialState, intent)

        assertEquals(1, newState.elements.size)
        assertTrue(newState.elements[0] is Element.Path)
        assertEquals(1, newState.history.size)
        assertTrue(newState.future.isEmpty())
    }

    @Test
    fun testUpdateLatestPathIntent() {
        val path = createTestPath()
        val initialState = State(elements = listOf(path))
        val newPoint = Offset(10f, 10f)
        val intent = Intent.UpdateLatestPath(newPoint)

        val newState = reducer.reduce(initialState, intent)

        assertEquals(2, (newState.elements[0] as Element.Path).points.size)
    }

    @Test
    fun testInsertNewShapeIntent() {
        val initialState = State()
        val offset = Offset(0f, 0f)
        val intent = Intent.InsertNewShape(ShapeType.RECTANGLE, offset)

        val newState = reducer.reduce(initialState, intent)

        assertEquals(1, newState.elements.size)
        assertTrue(newState.elements[0] is Element.Shape)
    }

    @Test
    fun testUndoIntent() {
        val path = createTestPath()
        val initialState = State(
            elements = listOf(path),
            history = listOf(emptyList()),
        )
        val intent = Intent.Undo

        val newState = reducer.reduce(initialState, intent)

        assertTrue(newState.elements.isEmpty())
        assertTrue(newState.history.isEmpty())
        assertEquals(1, newState.future.size)
    }

    @Test
    fun testRedoIntent() {
        val path = createTestPath()
        val initialState = State(future = listOf(listOf(path)))
        val intent = Intent.Redo

        val newState = reducer.reduce(initialState, intent)

        assertEquals(1, newState.elements.size)
        assertTrue(newState.future.isEmpty())
        assertEquals(1, newState.history.size)
    }

    @Test
    fun testResetIntent() {
        val path = createTestPath()
        val initialState = State(
            elements = listOf(path),
            history = listOf(emptyList()),
            future = listOf(listOf(path)),
        )
        val intent = Intent.Reset

        val newState = reducer.reduce(initialState, intent)

        assertTrue(newState.elements.isEmpty())
        assertTrue(newState.history.isEmpty())
        assertTrue(newState.future.isEmpty())
    }

    @Test
    fun testSetStrokeColorIntent() {
        val initialState = State(strokeColor = Color.Red)
        val intent = Intent.SetStrokeColor(Color.Blue)

        val newState = reducer.reduce(initialState, intent)

        assertEquals(Color.Blue, newState.strokeColor)
    }

    @Test
    fun testSetStrokeWidthIntent() {
        val initialState = State(strokeWidth = 10f)
        val intent = Intent.SetStrokeWidth(20f)

        val newState = reducer.reduce(initialState, intent)

        assertEquals(20f, newState.strokeWidth)
    }

    @Test
    fun testSetOpacityIntent() {
        val initialState = State(opacity = 1f)
        val intent = Intent.SetOpacity(0.5f)

        val newState = reducer.reduce(initialState, intent)

        assertEquals(0.5f, newState.opacity)
    }

    @Test
    fun testSetBgColorIntent() {
        val initialState = State(bgColor = Color.White)
        val intent = Intent.SetBgColor(Color.Black)

        val newState = reducer.reduce(initialState, intent)

        assertEquals(Color.Black, newState.bgColor)
    }

    @Test
    fun testDeleteElementIntent() {
        val path = createTestPath()
        val initialState = State(elements = listOf(path))
        val intent = Intent.DeleteElement(path.id)

        val newState = reducer.reduce(initialState, intent)

        assertTrue(newState.elements.isEmpty())
    }

    @Test
    fun testAddElementClearsFuture() {
        val path = createTestPath()
        val undoPath = createTestPath().copy(id = "undo")
        val initialState = State(
            elements = listOf(path),
            future = listOf(listOf(undoPath)),
        )
        val newPath = createTestPath().copy(id = "new")
        val intent = Intent.AddElement(newPath)

        val newState = reducer.reduce(initialState, intent)

        assertTrue(newState.future.isEmpty())
        assertEquals(1, newState.history.size)
    }
}
