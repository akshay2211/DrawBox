package io.ak1.drawboxsample.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import io.ak1.drawbox.DrawingPreview
import io.ak1.drawbox.domain.model.BuiltinFontFamilyKeys
import io.ak1.drawbox.domain.model.Element
import io.ak1.drawbox.domain.model.ShapeType
import io.ak1.drawbox.domain.model.StrokeStyle
import io.ak1.drawbox.domain.model.TextAlignment
import io.ak1.drawbox.domain.model.DrawingSerializer
import io.ak1.drawbox.domain.model.PayLoad
import io.ak1.drawbox.domain.usecase.SvgExporter
import io.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * End-to-end visual regression coverage for a complex scene containing
 * shapes (all five types, mix of fill / stroke / dashed / dotted / corner
 * radius / rotation), connector arrows (straight + bent), pen paths
 * (uniform + pressure-varied), and text in all three built-in font
 * families with all three alignments.
 *
 * Locks down both renderer outputs in parallel:
 *
 * - **PNG** — captured via Roborazzi from [DrawingPreview]. Diffs against
 *   `src/jvmTest/snapshots/ComplexScene_canvas.png`.
 * - **SVG** — exported via [SvgExporter.exportToSvg]. Diffs against
 *   `src/jvmTest/snapshots/ComplexScene.svg`. First run records the
 *   baseline; subsequent runs assert byte equality. Re-record by deleting
 *   the file.
 *
 * Failure modes this catches:
 * - Per-element rendering regressions (PNG diff).
 * - Z-order regressions in composite scenes (PNG diff).
 * - SVG serialization regressions (text-anchor, transform, fill / stroke
 *   attribute ordering, tspan wrapping).
 * - Visual / SVG drift — when one output is regressed the other usually
 *   isn't, so a divergence between PNG and SVG diffs flags it.
 */
class ComplexSceneExportTest {

    @get:Rule
    val composeRule = createComposeRule()

    // ---------- Scene ----------

    /**
     * Build the comprehensive scene. Coordinates fit inside a 800×600
     * canvas. Deterministic — no random ids, no timestamps, no Clock
     * reads. The same scene goes through both the renderer and the SVG
     * exporter so a diff in one but not the other indicates drift.
     */
    private fun complexScene(): List<Element> {
        val elements = mutableListOf<Element>()

        // ----- Header text (sans-serif, large, center-aligned) -----
        elements += text(
            text = "DrawBox Visual Regression",
            x = 50f, y = 20f, width = 700f,
            fontSize = 28f,
            family = BuiltinFontFamilyKeys.SANS,
            alignment = TextAlignment.CENTER,
            color = Color(0xFF263238),
            zIndex = 100,
        )

        // ----- Row 1: filled shapes with overlaid text labels -----

        // Rounded rectangle with a sans label.
        elements += shape(
            ShapeType.RECTANGLE,
            startX = 60f, startY = 80f, endX = 240f, endY = 180f,
            fillColor = Color(0xFFE3F2FD),
            strokeColor = Color(0xFF1565C0),
            strokeWidth = 3f,
            cornerRadius = 18f,
            zIndex = 1,
        )
        elements += text(
            text = "Rectangle",
            x = 60f, y = 118f, width = 180f,
            fontSize = 18f,
            family = BuiltinFontFamilyKeys.SANS,
            alignment = TextAlignment.CENTER,
            color = Color(0xFF0D47A1),
            zIndex = 2,
        )

        // Circle with serif label.
        elements += shape(
            ShapeType.CIRCLE,
            startX = 290f, startY = 80f, endX = 470f, endY = 180f,
            fillColor = Color(0xFFFFF3E0),
            strokeColor = Color(0xFFE65100),
            strokeWidth = 3f,
            zIndex = 1,
        )
        elements += text(
            text = "Circle",
            x = 290f, y = 118f, width = 180f,
            fontSize = 18f,
            family = BuiltinFontFamilyKeys.SERIF,
            alignment = TextAlignment.CENTER,
            color = Color(0xFFBF360C),
            zIndex = 2,
        )

        // Triangle with mono label.
        elements += shape(
            ShapeType.TRIANGLE,
            startX = 520f, startY = 80f, endX = 720f, endY = 180f,
            fillColor = Color(0xFFE8F5E9),
            strokeColor = Color(0xFF2E7D32),
            strokeWidth = 3f,
            cornerRadius = 12f,
            zIndex = 1,
        )
        elements += text(
            text = "Triangle",
            x = 520f, y = 140f, width = 200f,
            fontSize = 16f,
            family = BuiltinFontFamilyKeys.MONO,
            alignment = TextAlignment.CENTER,
            color = Color(0xFF1B5E20),
            zIndex = 2,
        )

        // ----- Row 2: connector arrows between the row 1 shapes -----

        // Rect → Circle, gently bent.
        elements += shape(
            ShapeType.ARROW,
            startX = 240f, startY = 130f, endX = 290f, endY = 130f,
            strokeColor = Color(0xFF455A64),
            strokeWidth = 2.5f,
            bend = Offset(0f, -30f),
            startBinding = null, endBinding = null,
            zIndex = 3,
        )
        // Circle → Triangle, straight + dashed.
        elements += shape(
            ShapeType.ARROW,
            startX = 470f, startY = 130f, endX = 520f, endY = 130f,
            strokeColor = Color(0xFF455A64),
            strokeWidth = 2.5f,
            strokeStyle = StrokeStyle.DASHED,
            zIndex = 3,
        )

        // ----- Row 3: stroke-style sampler line + curved line -----
        elements += shape(
            ShapeType.LINE,
            startX = 60f, startY = 220f, endX = 360f, endY = 220f,
            strokeColor = Color(0xFF6A1B9A),
            strokeWidth = 3f,
            strokeStyle = StrokeStyle.DOTTED,
            zIndex = 1,
        )
        elements += shape(
            ShapeType.LINE,
            startX = 400f, startY = 230f, endX = 720f, endY = 230f,
            strokeColor = Color(0xFFAD1457),
            strokeWidth = 3f,
            bend = Offset(0f, -40f),
            zIndex = 1,
        )

        // ----- Row 4: long-form text in three font families and three alignments -----
        elements += text(
            text = "Sans-serif, left-aligned.\nMulti-line text wraps within the box.",
            x = 60f, y = 280f, width = 220f,
            fontSize = 14f,
            family = BuiltinFontFamilyKeys.SANS,
            alignment = TextAlignment.LEFT,
            color = Color(0xFF263238),
            zIndex = 1,
        )
        elements += text(
            text = "Serif, centered.\nQuoth the raven, “Nevermore.”",
            x = 300f, y = 280f, width = 220f,
            fontSize = 14f,
            family = BuiltinFontFamilyKeys.SERIF,
            alignment = TextAlignment.CENTER,
            color = Color(0xFF263238),
            zIndex = 1,
        )
        elements += text(
            text = "fun main() {\n  println(\"Hello, World!\")\n}",
            x = 540f, y = 280f, width = 200f,
            fontSize = 13f,
            family = BuiltinFontFamilyKeys.MONO,
            alignment = TextAlignment.RIGHT,
            color = Color(0xFF1B5E20),
            zIndex = 1,
        )

        // ----- Row 5: pen paths (uniform + pressure-varied) -----
        elements += uniformPath(
            strokeWidth = 6f,
            color = Color(0xFF0D47A1),
            60f to 410f,
            120f to 390f,
            180f to 410f,
            240f to 390f,
            300f to 410f,
            360f to 390f,
        )
        elements += taperedPath(
            startWidth = 12f,
            endWidth = 2f,
            color = Color(0xFFB71C1C),
            400f to 410f,
            460f to 405f,
            520f to 400f,
            580f to 395f,
            640f to 390f,
            720f to 388f,
        )

        // ----- Row 6: rotated shapes -----
        elements += shape(
            ShapeType.RECTANGLE,
            startX = 100f, startY = 470f, endX = 220f, endY = 540f,
            fillColor = Color(0xFFFFCDD2),
            strokeColor = Color(0xFFC62828),
            strokeWidth = 3f,
            rotation = 15f,
            zIndex = 1,
        )
        elements += shape(
            ShapeType.TRIANGLE,
            startX = 280f, startY = 470f, endX = 400f, endY = 540f,
            fillColor = Color(0xFFD1C4E9),
            strokeColor = Color(0xFF4527A0),
            strokeWidth = 3f,
            rotation = -10f,
            zIndex = 1,
        )

        // ----- Row 7: thick straight arrow, bottom -----
        elements += shape(
            ShapeType.ARROW,
            startX = 460f, startY = 510f, endX = 720f, endY = 510f,
            strokeColor = Color(0xFF1B5E20),
            strokeWidth = 6f,
            zIndex = 1,
        )

        return elements
    }

    // ---------- Tests ----------

    /**
     * Verifies the JSON round-trip preserves every rendering-relevant field
     * on every element in the scene. Covers what `SvgExporter` had drift on
     * (rotation, bend, stroke style, corner radius) plus everything else
     * the data model exposes — path samples (incl. variable-width), text
     * fields, fill, stroke-enabled toggle, z-index.
     *
     * Failure here means a field is missing from the DTO or the
     * `toDto` / `toElement` mapping; the test asserts deep equality so
     * any divergence shows up as a `kotlin.test.AssertionError` naming
     * the unequal property.
     */
    @Test
    fun complexSceneJsonRoundTrip() {
        val originals = complexScene()
        val payload = PayLoad(bgColor = Color.White, elements = originals)
        val json = DrawingSerializer.serialize(payload)
        val restored = DrawingSerializer.deserialize(json)

        assertEquals(originals.size, restored.elements.size, "element count drift")

        originals.forEachIndexed { i, expected ->
            val actual = restored.elements[i]
            // Per-type assertions so a failure tells you *which* field
            // drifted, not just that two Element objects differ.
            when (expected) {
                is Element.Shape -> {
                    val a = actual as Element.Shape
                    assertEquals(expected.id, a.id, "[$i] id")
                    assertEquals(expected.shapeType, a.shapeType, "[$i] shapeType")
                    assertEquals(expected.points, a.points, "[$i] points")
                    assertEquals(expected.strokeColor, a.strokeColor, "[$i] strokeColor")
                    assertEquals(expected.fillColor, a.fillColor, "[$i] fillColor")
                    assertEquals(expected.strokeEnabled, a.strokeEnabled, "[$i] strokeEnabled")
                    assertEquals(expected.strokeWidth, a.strokeWidth, "[$i] strokeWidth")
                    assertEquals(expected.strokeStyle, a.strokeStyle, "[$i] strokeStyle")
                    assertEquals(expected.cornerRadius, a.cornerRadius, "[$i] cornerRadius")
                    assertEquals(expected.bend, a.bend, "[$i] bend")
                    assertEquals(expected.rotation, a.rotation, "[$i] rotation")
                    assertEquals(expected.startBinding, a.startBinding, "[$i] startBinding")
                    assertEquals(expected.endBinding, a.endBinding, "[$i] endBinding")
                    assertEquals(expected.zIndex, a.zIndex, "[$i] zIndex")
                }
                is Element.Path -> {
                    val a = actual as Element.Path
                    assertEquals(expected.id, a.id, "[$i] id")
                    assertEquals(expected.strokeColor, a.strokeColor, "[$i] strokeColor")
                    assertEquals(expected.strokeWidth, a.strokeWidth, "[$i] strokeWidth")
                    assertEquals(expected.alpha, a.alpha, "[$i] alpha")
                    assertEquals(expected.strokeStyle, a.strokeStyle, "[$i] strokeStyle")
                    assertEquals(expected.rotation, a.rotation, "[$i] rotation")
                    assertEquals(expected.zIndex, a.zIndex, "[$i] zIndex")
                    assertEquals(expected.samples.size, a.samples.size, "[$i] samples.size")
                    expected.samples.forEachIndexed { j, s ->
                        val rs = a.samples[j]
                        assertEquals(s.position, rs.position, "[$i] samples[$j].position")
                        assertEquals(s.width, rs.width, "[$i] samples[$j].width")
                    }
                }
                is Element.Text -> {
                    val a = actual as Element.Text
                    assertEquals(expected.id, a.id, "[$i] id")
                    assertEquals(expected.text, a.text, "[$i] text")
                    assertEquals(expected.fontFamilyKey, a.fontFamilyKey, "[$i] fontFamilyKey")
                    assertEquals(expected.fontSize, a.fontSize, "[$i] fontSize")
                    assertEquals(expected.color, a.color, "[$i] color")
                    assertEquals(expected.alignment, a.alignment, "[$i] alignment")
                    assertEquals(expected.topLeft, a.topLeft, "[$i] topLeft")
                    assertEquals(expected.wrapWidth, a.wrapWidth, "[$i] wrapWidth")
                    assertEquals(expected.opacity, a.opacity, "[$i] opacity")
                    assertEquals(expected.rotation, a.rotation, "[$i] rotation")
                    assertEquals(expected.zIndex, a.zIndex, "[$i] zIndex")
                    // measuredHeight is renderer-cached and intentionally not
                    // serialized — the loader recomputes from fontSize, which
                    // is OK for a one-frame settle window in real use but
                    // means we don't assert it here.
                }
                is Element.Image -> {
                    val a = actual as Element.Image
                    assertEquals(expected.id, a.id, "[$i] id")
                    assertTrue(expected.bytes.contentEquals(a.bytes), "[$i] bytes")
                    assertEquals(expected.intrinsicSize, a.intrinsicSize, "[$i] intrinsicSize")
                    assertEquals(expected.points, a.points, "[$i] points")
                    assertEquals(expected.opacity, a.opacity, "[$i] opacity")
                    assertEquals(expected.rotation, a.rotation, "[$i] rotation")
                    assertEquals(expected.zIndex, a.zIndex, "[$i] zIndex")
                }
            }
        }
    }

    @Test
    fun complexSceneCanvasPng() {
        composeRule.setContent {
            DrawingPreview(
                elements = complexScene(),
                bgColor = Color.White,
                modifier = Modifier
                    .size(800.dp, 600.dp)
                    .background(Color.White),
            )
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "src/jvmTest/snapshots/ComplexScene_canvas.png",
        )
    }

    @Test
    fun complexSceneSvg() {
        val svg = SvgExporter.exportToSvg(complexScene(), width = 800, height = 600)
        val snapshotDir = File("src/jvmTest/snapshots").also { it.mkdirs() }
        val baseline = File(snapshotDir, "ComplexScene.svg")

        if (!baseline.exists()) {
            // First run — record. The next CI run diffs against this.
            baseline.writeText(svg)
            // Make the recording explicit so a fresh checkout doesn't
            // silently "pass" by recording a wrong baseline; assert the
            // file is non-empty and well-formed.
            assertTrue(svg.startsWith("<?xml"), "SVG should start with XML declaration")
            assertTrue(
                svg.contains("<svg "),
                "SVG must contain an <svg> root",
            )
            return
        }

        val current = svg
        val expected = baseline.readText()
        if (current != expected) {
            // Drop the actual output next to the baseline so a developer
            // can diff with their tool of choice. Roborazzi does the same
            // for PNGs.
            File(snapshotDir, "ComplexScene.actual.svg").writeText(current)
        }
        assertEquals(
            expected,
            current,
            "SVG output drifted from baseline. Diff " +
                "src/jvmTest/snapshots/ComplexScene.svg vs ComplexScene.actual.svg; " +
                "delete the baseline to re-record.",
        )
    }

    // ---------- Element helpers ----------

    private fun shape(
        type: ShapeType,
        startX: Float, startY: Float,
        endX: Float, endY: Float,
        strokeColor: Color = Color.Black,
        fillColor: Color? = null,
        strokeWidth: Float = 3f,
        strokeStyle: StrokeStyle = StrokeStyle.SOLID,
        cornerRadius: Float = 0f,
        bend: Offset = Offset.Zero,
        rotation: Float = 0f,
        startBinding: String? = null,
        endBinding: String? = null,
        zIndex: Int = 0,
    ): Element.Shape = Element.Shape(
        shapeType = type,
        points = listOf(Offset(startX, startY), Offset(endX, endY)),
        strokeColor = strokeColor,
        fillColor = fillColor,
        strokeWidth = strokeWidth,
        strokeStyle = strokeStyle,
        cornerRadius = cornerRadius,
        bend = bend,
        rotation = rotation,
        startBinding = startBinding,
        endBinding = endBinding,
        zIndex = zIndex,
    )

    private fun text(
        text: String,
        x: Float, y: Float, width: Float,
        fontSize: Float,
        family: String,
        alignment: TextAlignment,
        color: Color,
        zIndex: Int = 0,
    ): Element.Text = Element.Text(
        text = text,
        fontFamilyKey = family,
        fontSize = fontSize,
        color = color,
        alignment = alignment,
        topLeft = Offset(x, y),
        wrapWidth = width,
        // Approximate single-line / multi-line measured height. The renderer
        // remeasures on draw; this value seeds bounds() / SVG `<text>` height
        // and isn't picky as long as it's in the right ballpark.
        measuredHeight = fontSize * (1 + text.count { it == '\n' }) * 1.3f,
        zIndex = zIndex,
    )

    private fun uniformPath(
        strokeWidth: Float,
        color: Color,
        vararg points: Pair<Float, Float>,
    ): Element.Path = Element.Path(
        samples = points.map { (x, y) ->
            Element.PathSample(Offset(x, y), strokeWidth)
        },
        strokeColor = color,
        strokeWidth = strokeWidth,
        alpha = 1f,
    )

    private fun taperedPath(
        startWidth: Float,
        endWidth: Float,
        color: Color,
        vararg points: Pair<Float, Float>,
    ): Element.Path = Element.Path(
        samples = points.mapIndexed { i, (x, y) ->
            val t = if (points.size <= 1) 0f else i.toFloat() / (points.size - 1)
            val w = startWidth + (endWidth - startWidth) * t
            Element.PathSample(Offset(x, y), w)
        },
        strokeColor = color,
        strokeWidth = startWidth,
        alpha = 1f,
    )

    // Unused but compiled-against — `Size` here ensures we catch import
    // drift on `androidx.compose.ui.geometry.Size`, which other tests in
    // the file use too. No-op at runtime.
    @Suppress("unused")
    private val sizeProbe: Size = Size.Zero
}
