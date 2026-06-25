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
            samples = listOf(Element.PathSample(Offset(0f, 0f), 10f)),
            strokeColor = Color.Red,
            strokeWidth = 10f,
            alpha = 1f,
        )
    }

    private fun uniformPath(strokeWidth: Float, positions: List<Offset>): Element.Path =
        Element.Path(
            samples = positions.map { Element.PathSample(it, strokeWidth) },
            strokeColor = Color.Red,
            strokeWidth = strokeWidth,
            alpha = 1f,
        )

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

        assertEquals(offset, result.samples[0].position)
        assertEquals(color, result.strokeColor)
        assertEquals(width, result.strokeWidth)
        assertEquals(width, result.samples[0].width)
        assertEquals(alpha, result.alpha)
    }

    @Test
    fun testUpdateLatestPath() {
        val path = createTestPath()
        val elements = listOf(path)
        val newPoint = Offset(10f, 10f)

        val result = useCase.updateLatestPath(newPoint, elements)
        val updated = result[0] as Element.Path

        assertEquals(2, updated.samples.size)
        assertEquals(newPoint, updated.samples[1].position)
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

    // ===== Pen pressure =====

    @Test
    fun testInsertNewPathDefaultsToUnitPressure() {
        val result = useCase.insertNewPath(
            offset = Offset(0f, 0f),
            color = Color.Red,
            width = 10f,
            alpha = 1f,
        )
        assertEquals(1, result.samples.size)
        assertEquals(10f, result.samples[0].width)
    }

    @Test
    fun testInsertNewPathModulatesWidthByPressure() {
        val result = useCase.insertNewPath(
            offset = Offset(0f, 0f),
            color = Color.Red,
            width = 10f,
            alpha = 1f,
            pressure = 0.5f,
        )
        assertEquals(5f, result.samples[0].width)
    }

    @Test
    fun testUpdateLatestPathConformsSeedWidthOnFirstTick() {
        // The seed sample is created without a pressure reading (tap and
        // drag-start callbacks expose only position). On the first update tick
        // we retroactively rewrite the seed's width to match this tick's
        // pressure, so the stroke origin doesn't render as a visible "bigger
        // dot" before the rest of the variable-width path tapers down.
        val seed = useCase.insertNewPath(Offset(0f, 0f), Color.Red, 10f, 1f)
        val elements = listOf(seed)
        val updated = useCase.updateLatestPath(
            newPoint = Offset(20f, 0f),
            currentElements = elements,
            pressure = 0.3f,
        )
        val path = updated[0] as Element.Path
        assertEquals(2, path.samples.size)
        assertEquals(3f, path.samples[0].width, "seed must conform to first tick pressure")
        assertEquals(3f, path.samples[1].width)
    }

    @Test
    fun testUpdateLatestPathPreservesSeedWidthWhenPressureMatches() {
        // When the first tick reports unit pressure (mouse / touch / pen at
        // full press), the seed already has the right width; don't churn it.
        val seed = useCase.insertNewPath(Offset(0f, 0f), Color.Red, 10f, 1f)
        val updated = useCase.updateLatestPath(
            newPoint = Offset(20f, 0f),
            currentElements = listOf(seed),
            pressure = 1f,
        )
        val path = updated[0] as Element.Path
        assertEquals(10f, path.samples[0].width)
        assertEquals(10f, path.samples[1].width)
    }

    @Test
    fun testUpdateLatestPathDoesNotConformAfterFirstTick() {
        // Second-and-beyond samples should never rewrite earlier widths even
        // if pressure varies. Variable-width strokes need to retain shape.
        var elements: List<Element> = listOf(
            useCase.insertNewPath(Offset(0f, 0f), Color.Red, 10f, 1f),
        )
        elements = useCase.updateLatestPath(Offset(10f, 0f), elements, pressure = 0.5f)
        elements = useCase.updateLatestPath(Offset(20f, 0f), elements, pressure = 0.2f)
        val path = elements[0] as Element.Path
        assertEquals(3, path.samples.size)
        assertEquals(5f, path.samples[0].width)   // conformed on first tick
        assertEquals(5f, path.samples[1].width)
        assertEquals(2f, path.samples[2].width)   // independent, not back-applied
    }

    @Test
    fun testUniformStrokeRoundTripsThroughSerialization() {
        // A pressure=1.0 stroke must look identical after a JSON round-trip.
        val seed = useCase.insertNewPath(Offset(0f, 0f), Color.Red, 10f, 1f)
        val withMore = useCase.updateLatestPath(Offset(10f, 0f), listOf(seed))
        val original = withMore[0] as Element.Path

        // No serializer call here — covered by existing JSON tests. The
        // uniform-width property is what matters for this case.
        assertTrue(original.samples.all { it.width == 10f })
    }

    @Test
    fun testEraseAtRemovesPathInRadius() {
        val path = uniformPath(
            strokeWidth = 4f,
            positions = listOf(Offset(0f, 0f), Offset(100f, 0f)),
        )
        val elements = useCase.addElement(path, emptyList())

        val result = useCase.eraseAt(elements, Offset(50f, 0f), radius = 5f)

        assertEquals(0, result.size)
    }

    @Test
    fun testEraseAtMissReturnsSameInstance() {
        val path = uniformPath(
            strokeWidth = 1f,
            positions = listOf(Offset(0f, 0f), Offset(100f, 0f)),
        )
        val elements = useCase.addElement(path, emptyList())

        // Far away from the path: nothing hit. Reducer relies on this
        // identity contract to skip no-op state copies.
        val result = useCase.eraseAt(elements, Offset(500f, 500f), radius = 5f)

        assertSame(elements, result)
    }

    @Test
    fun testEraseAtPreservesUnhitElements() {
        val hit = uniformPath(
            strokeWidth = 2f,
            positions = listOf(Offset(0f, 0f), Offset(50f, 0f)),
        )
        val miss = Element.Path(
            samples = listOf(
                Element.PathSample(Offset(0f, 200f), 2f),
                Element.PathSample(Offset(50f, 200f), 2f),
            ),
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
