package io.ak1.drawbox.presentation.reducer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import io.ak1.drawbox.domain.model.Element
import io.ak1.drawbox.domain.model.Intent
import io.ak1.drawbox.domain.model.Mode
import io.ak1.drawbox.domain.model.ShapeType
import io.ak1.drawbox.domain.model.State
import io.ak1.drawbox.domain.model.StrokeStyle
import io.ak1.drawbox.domain.usecase.UseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SelectionReducerTest {

    private val reducer = Reducer(UseCase())

    private fun rect(id: String, l: Float, t: Float, r: Float, b: Float, z: Int = 0): Element.Shape =
        Element.Shape(
            id = id,
            shapeType = ShapeType.RECTANGLE,
            points = listOf(Offset(l, t), Offset(r, b)),
            strokeColor = Color.Red,
            fillColor = Color.Blue,
            strokeWidth = 4f,
            zIndex = z,
        )

    @Test
    fun selectAtTopmostWinsAndOthersClear() {
        val a = rect("a", 0f, 0f, 100f, 100f, z = 0)
        val b = rect("b", 0f, 0f, 100f, 100f, z = 5)
        val state = State(elements = listOf(a, b), mode = Mode.SELECT, selectedIds = setOf("a"))
        val out = reducer.reduce(state, Intent.SelectAt(Offset(50f, 50f)))
        assertEquals(setOf("b"), out.selectedIds)
    }

    @Test
    fun selectAtMissClearsSelection() {
        val a = rect("a", 0f, 0f, 100f, 100f)
        val state = State(elements = listOf(a), mode = Mode.SELECT, selectedIds = setOf("a"))
        val out = reducer.reduce(state, Intent.SelectAt(Offset(500f, 500f)))
        assertTrue(out.selectedIds.isEmpty())
    }

    @Test
    fun moveSelectedTranslatesOnlySelected() {
        val a = rect("a", 0f, 0f, 100f, 100f)
        val b = rect("b", 200f, 0f, 300f, 100f)
        val state = State(elements = listOf(a, b), mode = Mode.SELECT, selectedIds = setOf("a"))
        val out = reducer.reduce(state, Intent.MoveSelected(Offset(10f, 20f)))
        val moved = out.elements.first { it.id == "a" } as Element.Shape
        val untouched = out.elements.first { it.id == "b" } as Element.Shape
        assertEquals(Offset(10f, 20f), moved.points[0])
        assertEquals(Offset(110f, 120f), moved.points[1])
        assertEquals(Offset(200f, 0f), untouched.points[0])
    }

    @Test
    fun deleteSelectedRemovesAndSnapshots() {
        val a = rect("a", 0f, 0f, 100f, 100f)
        val b = rect("b", 200f, 0f, 300f, 100f)
        val state = State(elements = listOf(a, b), mode = Mode.SELECT, selectedIds = setOf("a"))
        val out = reducer.reduce(state, Intent.DeleteSelected)
        assertEquals(1, out.elements.size)
        assertEquals("b", out.elements[0].id)
        assertTrue(out.selectedIds.isEmpty())
        assertEquals(1, out.history.size)
    }

    @Test
    fun commitMarqueeSelectsBoundsIntersecting() {
        val a = rect("a", 0f, 0f, 100f, 100f)
        val b = rect("b", 200f, 0f, 300f, 100f)
        val c = rect("c", 1000f, 1000f, 1100f, 1100f)
        val state = State(
            elements = listOf(a, b, c),
            mode = Mode.SELECT,
            marqueeRect = Rect(-10f, -10f, 350f, 50f),
        )
        val out = reducer.reduce(state, Intent.CommitMarquee(Rect(-10f, -10f, 350f, 50f)))
        assertEquals(setOf("a", "b"), out.selectedIds)
        assertEquals(null, out.marqueeRect)
    }

    @Test
    fun beginTransformPlusMoveRoundTripsUnderUndo() {
        val a = rect("a", 0f, 0f, 100f, 100f)
        val initial = State(elements = listOf(a), mode = Mode.SELECT, selectedIds = setOf("a"))
        val afterBegin = reducer.reduce(initial, Intent.BeginTransform)
        val afterMove = reducer.reduce(afterBegin, Intent.MoveSelected(Offset(50f, 50f)))
        val moved = afterMove.elements.first() as Element.Shape
        assertEquals(Offset(50f, 50f), moved.points[0])
        // Undo restores the pre-transform position.
        val undone = reducer.reduce(afterMove, Intent.Undo)
        val restored = undone.elements.first() as Element.Shape
        assertEquals(Offset(0f, 0f), restored.points[0])
    }

    @Test
    fun setModeAwayFromSelectClearsSelection() {
        val a = rect("a", 0f, 0f, 100f, 100f)
        val state = State(elements = listOf(a), mode = Mode.SELECT, selectedIds = setOf("a"))
        val out = reducer.reduce(state, Intent.SetMode(Mode.PEN))
        assertTrue(out.selectedIds.isEmpty())
    }

    @Test
    fun setModeIntoSelectPreservesExistingSelection() {
        val a = rect("a", 0f, 0f, 100f, 100f)
        // Selection technically can't exist before entering SELECT in normal flow,
        // but the reducer must not stomp on it during the transition.
        val state = State(elements = listOf(a), mode = Mode.PEN, selectedIds = setOf("a"))
        val out = reducer.reduce(state, Intent.SetMode(Mode.SELECT))
        assertEquals(setOf("a"), out.selectedIds)
    }

    @Test
    fun setSelectedStrokeColorRecolorsOnlySelected() {
        val a = rect("a", 0f, 0f, 100f, 100f)
        val b = rect("b", 0f, 0f, 100f, 100f)
        val state = State(elements = listOf(a, b), mode = Mode.SELECT, selectedIds = setOf("a"))
        val out = reducer.reduce(state, Intent.SetSelectedStrokeColor(Color.Green))
        val recolored = out.elements.first { it.id == "a" } as Element.Shape
        val untouched = out.elements.first { it.id == "b" } as Element.Shape
        assertEquals(Color.Green, recolored.strokeColor)
        assertEquals(Color.Red, untouched.strokeColor)
        assertEquals(1, out.history.size)
    }

    @Test
    fun bringSelectionToFrontIncreasesZIndex() {
        val a = rect("a", 0f, 0f, 10f, 10f, z = 0)
        val b = rect("b", 0f, 0f, 10f, 10f, z = 5)
        val state = State(elements = listOf(a, b), mode = Mode.SELECT, selectedIds = setOf("a"))
        val out = reducer.reduce(state, Intent.BringSelectionToFront)
        val newA = out.elements.first { it.id == "a" }
        val newB = out.elements.first { it.id == "b" }
        assertTrue(newA.zIndex > newB.zIndex)
    }

    @Test
    fun setCornerRadiusUpdatesDefaultOnly() {
        val initial = State(currentItemCornerRadius = 0f)
        val out = reducer.reduce(initial, Intent.SetCornerRadius(16f))
        assertEquals(16f, out.currentItemCornerRadius)
        // Default change must NOT enter undo history.
        assertTrue(out.history.isEmpty())
    }

    @Test
    fun insertNewShapeAppliesCurrentItemCornerRadius() {
        val initial = State(
            mode = Mode.RECTANGLE,
            currentItemCornerRadius = 24f,
        )
        val out = reducer.reduce(initial, Intent.InsertNewShape(ShapeType.RECTANGLE, Offset.Zero))
        val inserted = out.elements.first() as Element.Shape
        assertEquals(24f, inserted.cornerRadius)
    }

    @Test
    fun setSelectedCornerRadiusAppliesOnlyToRoundableShapes() {
        val rect = rect("r", 0f, 0f, 100f, 100f)
        val circle = Element.Shape(
            id = "c",
            shapeType = ShapeType.CIRCLE,
            points = listOf(Offset(0f, 0f), Offset(80f, 0f)),
            strokeColor = Color.Red,
            fillColor = null,
            strokeWidth = 4f,
        )
        val state = State(
            elements = listOf(rect, circle),
            mode = Mode.SELECT,
            selectedIds = setOf("r", "c"),
        )
        val out = reducer.reduce(state, Intent.SetSelectedCornerRadius(12f))
        val newRect = out.elements.first { it.id == "r" } as Element.Shape
        val newCircle = out.elements.first { it.id == "c" } as Element.Shape
        assertEquals(12f, newRect.cornerRadius)
        // Circle is not roundable — must be left untouched.
        assertEquals(0f, newCircle.cornerRadius)
        // Snapshots history.
        assertEquals(1, out.history.size)
    }

    @Test
    fun setStrokeStyleUpdatesDefaultOnly() {
        val initial = State()
        val out = reducer.reduce(initial, Intent.SetStrokeStyle(StrokeStyle.DASHED))
        assertEquals(StrokeStyle.DASHED, out.currentItemStrokeStyle)
        assertTrue(out.history.isEmpty())
    }

    @Test
    fun insertNewShapeAppliesCurrentItemStrokeStyle() {
        val initial = State(
            mode = Mode.CIRCLE,
            currentItemStrokeStyle = StrokeStyle.DOTTED,
        )
        val out = reducer.reduce(initial, Intent.InsertNewShape(ShapeType.CIRCLE, Offset.Zero))
        val inserted = out.elements.first() as Element.Shape
        assertEquals(StrokeStyle.DOTTED, inserted.strokeStyle)
    }

    @Test
    fun setSelectedStrokeStyleAppliesToShapesAndPaths() {
        val r = rect("r", 0f, 0f, 100f, 100f)
        val p = Element.Path(
            id = "p",
            points = listOf(Offset(0f, 0f)),
            strokeColor = Color.Red,
            strokeWidth = 4f,
            alpha = 1f,
        )
        val state = State(
            elements = listOf(r, p),
            mode = Mode.SELECT,
            selectedIds = setOf("r", "p"),
        )
        val out = reducer.reduce(state, Intent.SetSelectedStrokeStyle(StrokeStyle.DASHED))
        val newRect = out.elements.first { it.id == "r" } as Element.Shape
        val newPath = out.elements.first { it.id == "p" } as Element.Path
        assertEquals(StrokeStyle.DASHED, newRect.strokeStyle)
        assertEquals(StrokeStyle.DASHED, newPath.strokeStyle)
        assertEquals(1, out.history.size)
    }

    @Test
    fun insertNewPathAppliesCurrentItemStrokeStyle() {
        val initial = State(
            mode = Mode.PEN,
            currentItemStrokeStyle = StrokeStyle.DOTTED,
        )
        val out = reducer.reduce(initial, Intent.InsertNewPath(Offset.Zero))
        val inserted = out.elements.first() as Element.Path
        assertEquals(StrokeStyle.DOTTED, inserted.strokeStyle)
    }

    @Test
    fun redoReappliesUndoneEdit() {
        val a = rect("a", 0f, 0f, 100f, 100f)
        val initial = State(elements = listOf(a), mode = Mode.SELECT, selectedIds = setOf("a"))
        val afterDelete = reducer.reduce(initial, Intent.DeleteSelected)
        assertTrue(afterDelete.elements.isEmpty())
        val afterUndo = reducer.reduce(afterDelete, Intent.Undo)
        assertEquals(1, afterUndo.elements.size)
        val afterRedo = reducer.reduce(afterUndo, Intent.Redo)
        assertTrue(afterRedo.elements.isEmpty())
    }
}
