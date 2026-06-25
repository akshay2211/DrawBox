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
            samples = listOf(Element.PathSample(Offset(0f, 0f), 4f)),
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
    fun updateLatestShapeKeepsExactlyTwoPoints() {
        val s0 = State(mode = Mode.RECTANGLE)
        val s1 = reducer.reduce(s0, Intent.InsertNewShape(ShapeType.RECTANGLE, Offset(10f, 10f)))
        val s2 = reducer.reduce(s1, Intent.UpdateLatestShape(Offset(20f, 20f)))
        val s3 = reducer.reduce(s2, Intent.UpdateLatestShape(Offset(900f, 5f)))
        val s4 = reducer.reduce(s3, Intent.UpdateLatestShape(Offset(40f, 80f)))
        val shape = s4.elements.last() as Element.Shape
        // Exactly two points: original start + current cursor end. Drag-through
        // detours like (900, 5) must NOT leak into points or bounds.
        assertEquals(2, shape.points.size)
        assertEquals(Offset(10f, 10f), shape.points[0])
        assertEquals(Offset(40f, 80f), shape.points[1])
    }

    @Test
    fun setLineBendUpdatesBendOnLineLikeShape() {
        val line = Element.Shape(
            id = "ln",
            shapeType = ShapeType.LINE,
            points = listOf(Offset(0f, 0f), Offset(100f, 0f)),
            strokeColor = Color.Red,
            strokeWidth = 4f,
        )
        val state = State(elements = listOf(line))
        val out = reducer.reduce(state, Intent.SetLineBend("ln", Offset(0f, 30f)))
        val updated = out.elements.first() as Element.Shape
        assertEquals(Offset(0f, 30f), updated.bend)
    }

    @Test
    fun setElementPointsReplacesPoints() {
        val line = Element.Shape(
            id = "ln",
            shapeType = ShapeType.LINE,
            points = listOf(Offset(0f, 0f), Offset(100f, 0f)),
            strokeColor = Color.Red,
            strokeWidth = 4f,
        )
        val state = State(elements = listOf(line))
        val out = reducer.reduce(
            state,
            Intent.SetElementPoints("ln", listOf(Offset(10f, 10f), Offset(200f, 200f))),
        )
        val updated = out.elements.first() as Element.Shape
        assertEquals(Offset(10f, 10f), updated.points[0])
        assertEquals(Offset(200f, 200f), updated.points[1])
    }

    @Test
    fun finalizeArrowBindingsBindsAndSnapsEndpointsToBoundary() {
        val box1 = rect("a", 0f, 0f, 100f, 100f)   // center (50, 50)
        val box2 = rect("b", 200f, 200f, 300f, 300f) // center (250, 250)
        val arrow = Element.Shape(
            id = "arr",
            shapeType = ShapeType.ARROW,
            points = listOf(Offset(50f, 50f), Offset(250f, 250f)),
            strokeColor = Color.Red,
            strokeWidth = 4f,
        )
        val state = State(elements = listOf(box1, box2, arrow))
        val out = reducer.reduce(state, Intent.FinalizeArrowBindings("arr"))
        val updated = out.elements.first { it.id == "arr" } as Element.Shape
        assertEquals("a", updated.startBinding)
        assertEquals("b", updated.endBinding)
        // Start endpoint must be on box1's boundary (right edge x = 100, since
        // box2's center is to the bottom-right and box1 is axis-aligned square).
        assertEquals(100f, updated.points[0].x)
        // End endpoint must be on box2's boundary (left edge x = 200).
        assertEquals(200f, updated.points[1].x)
        // First-time dual connection seeds a non-zero perpendicular bend.
        assertTrue(updated.bend != Offset.Zero, "expected auto-curve on fresh connection")
    }

    @Test
    fun finalizeArrowBindsOnNearMissAndSnapsToBoundary() {
        // User drops the arrow end ~12 world-px outside the right edge of a
        // rectangle. That's inside the binding tolerance, so the arrow should
        // bind AND propagateBindings should pull the endpoint to the boundary.
        val box = rect("a", 0f, 0f, 100f, 100f)
        val arrow = Element.Shape(
            id = "arr",
            shapeType = ShapeType.ARROW,
            // Start far to the right; end is JUST outside the box's right edge.
            points = listOf(Offset(400f, 50f), Offset(112f, 50f)),
            strokeColor = Color.Red,
            strokeWidth = 4f,
        )
        val state = State(elements = listOf(box, arrow))
        val out = reducer.reduce(state, Intent.FinalizeArrowBindings("arr"))
        val updated = out.elements.first { it.id == "arr" } as Element.Shape
        assertEquals("a", updated.endBinding, "near-miss must bind to the box")
        // Endpoint must snap to the box's right edge (x = 100), not stay at the
        // 112 drop point.
        assertEquals(100f, updated.points[1].x)
    }

    @Test
    fun movingBoundShapeUpdatesArrowEndpointToBoundary() {
        val box = rect("b", 0f, 0f, 100f, 100f)
        val arrow = Element.Shape(
            id = "arr",
            shapeType = ShapeType.ARROW,
            // Far end is dead horizontal-right of the box's center, so the ray
            // exits the right edge cleanly at y = 50.
            points = listOf(Offset(50f, 50f), Offset(500f, 50f)),
            strokeColor = Color.Red,
            strokeWidth = 4f,
            startBinding = "b",
        )
        val state = State(
            elements = listOf(box, arrow),
            mode = Mode.SELECT,
            selectedIds = setOf("b"),
        )
        val out = reducer.reduce(state, Intent.MoveSelected(Offset(100f, 0f)))
        val updatedArrow = out.elements.first { it.id == "arr" } as Element.Shape
        // Box moved +100 on x → new bounds (100, 0)-(200, 100), center (150, 50).
        // Far endpoint is at (500, 50): horizontal ray → exit on right edge.
        assertEquals(Offset(200f, 50f), updatedArrow.points[0])
        // The unbound end shouldn't have moved.
        assertEquals(Offset(500f, 50f), updatedArrow.points[1])
    }

    @Test
    fun drawingAConnectorArrowProducesOneUndoEntry() {
        // Reproduce the "draw arrow + finalize bindings" gesture:
        //   1. InsertNewShape (snapshots the empty state)
        //   2. UpdateLatestShape (no snapshot)
        //   3. FinalizeArrowBindings (must NOT snapshot — same transaction)
        // Result: exactly one history entry; one undo removes the whole arrow.
        val box1 = rect("a", 0f, 0f, 100f, 100f)
        val box2 = rect("b", 200f, 200f, 300f, 300f)
        val start = State(
            elements = listOf(box1, box2),
            mode = Mode.ARROW,
        )
        val afterInsert = reducer.reduce(start, Intent.InsertNewShape(ShapeType.ARROW, Offset(50f, 50f)))
        val afterUpdate = reducer.reduce(afterInsert, Intent.UpdateLatestShape(Offset(250f, 250f)))
        val arrowId = afterUpdate.elements.last().id
        val afterFinalize = reducer.reduce(afterUpdate, Intent.FinalizeArrowBindings(arrowId))

        assertEquals(1, afterFinalize.history.size, "draw-and-finalize must use exactly one snapshot")
        // One Undo removes the entire arrow.
        val undone = reducer.reduce(afterFinalize, Intent.Undo)
        assertEquals(2, undone.elements.size)
        assertTrue(undone.elements.none { it.id == arrowId })
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
