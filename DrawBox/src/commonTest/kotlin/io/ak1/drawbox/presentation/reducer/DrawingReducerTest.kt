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
            samples = listOf(Element.PathSample(Offset(0f, 0f), 10f)),
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

        assertEquals(2, (newState.elements[0] as Element.Path).samples.size)
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

    // ===== Eraser =====

    private fun erasablePath(): Element.Path = Element.Path(
        samples = listOf(
            Element.PathSample(Offset(0f, 0f), 4f),
            Element.PathSample(Offset(100f, 0f), 4f),
        ),
        strokeColor = Color.Red,
        strokeWidth = 4f,
        alpha = 1f,
    )

    @Test
    fun testEraseAtOnEmptySpaceLeavesHistoryAlone() {
        val path = erasablePath()
        val initial = State(elements = listOf(path))

        var s = reducer.reduce(initial, Intent.BeginErase)
        // Tap far away from the path: no element is hit.
        s = reducer.reduce(s, Intent.EraseAt(Offset(500f, 500f), radius = 5f))
        s = reducer.reduce(s, Intent.EndErase)

        assertEquals(1, s.elements.size)
        assertTrue(s.history.isEmpty(), "no element hit → no history entry")
        assertTrue(!s.erasingSessionDirty, "session should be closed cleanly")
    }

    @Test
    fun testEraseAtFirstHitSnapshotsHistoryOnce() {
        val a = erasablePath().copy(id = "a")
        val b = erasablePath().copy(
            id = "b",
            samples = listOf(
                Element.PathSample(Offset(0f, 200f), 4f),
                Element.PathSample(Offset(100f, 200f), 4f),
            ),
        )
        val initial = State(elements = listOf(a, b))

        var s = reducer.reduce(initial, Intent.BeginErase)
        // Three EraseAt ticks within one gesture, each landing on a different
        // element after a miss. Only the first hit should snapshot.
        s = reducer.reduce(s, Intent.EraseAt(Offset(500f, 500f), 5f))   // miss
        s = reducer.reduce(s, Intent.EraseAt(Offset(50f, 0f), 5f))      // hit a
        s = reducer.reduce(s, Intent.EraseAt(Offset(50f, 200f), 5f))    // hit b
        s = reducer.reduce(s, Intent.EndErase)

        assertEquals(0, s.elements.size)
        assertEquals(1, s.history.size, "single snapshot covers the whole sweep")
        // Undoing the gesture restores both elements as one operation.
        val undone = reducer.reduce(s, Intent.Undo)
        assertEquals(2, undone.elements.size)
    }

    @Test
    fun testEraseAtSetsSessionDirtyOnlyAfterHit() {
        val initial = State(elements = listOf(erasablePath()))
        val opened = reducer.reduce(initial, Intent.BeginErase)
        val missed = reducer.reduce(opened, Intent.EraseAt(Offset(500f, 500f), 5f))
        assertTrue(!missed.erasingSessionDirty)

        val hit = reducer.reduce(missed, Intent.EraseAt(Offset(50f, 0f), 5f))
        assertTrue(hit.erasingSessionDirty)
    }
}
