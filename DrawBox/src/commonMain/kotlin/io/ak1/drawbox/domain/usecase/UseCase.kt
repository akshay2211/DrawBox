package io.ak1.drawbox.domain.usecase

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import io.ak1.drawbox.domain.model.Element
import io.ak1.drawbox.domain.model.ResizeHandle
import io.ak1.drawbox.domain.model.ShapeType
import io.ak1.drawbox.domain.model.StrokeStyle
import io.ak1.drawbox.domain.model.TextAlignment
import io.ak1.drawbox.domain.model.bounds
import io.ak1.drawbox.domain.model.connectorAnchor
import io.ak1.drawbox.domain.model.hitTest
import io.ak1.drawbox.domain.model.resizeBounds
import io.ak1.drawbox.domain.model.topmostHit
import io.ak1.drawbox.domain.model.touched
import io.ak1.drawbox.domain.model.translate
import io.ak1.drawbox.domain.model.withBounds
import io.ak1.drawbox.domain.model.withRotation
import kotlin.math.sqrt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class UseCase {
    // Element operations
    fun addElement(element: Element, currentElements: List<Element>): List<Element> {
        val newElement = when (element) {
            is Element.Path -> element.copy(zIndex = currentElements.size)
            is Element.Shape -> element.copy(zIndex = currentElements.size)
            is Element.Image -> element.copy(zIndex = currentElements.size)
            is Element.Text -> element.copy(zIndex = currentElements.size)
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
    @OptIn(ExperimentalTime::class)
    fun insertNewPath(
        offset: Offset,
        color: Color,
        width: Float,
        alpha: Float,
        strokeStyle: StrokeStyle = StrokeStyle.SOLID,
        pressure: Float = 1f,
    ): Element.Path {
        val now = Clock.System.now().toEpochMilliseconds()
        return Element.Path(
            samples = listOf(Element.PathSample(offset, width * pressure)),
            strokeColor = color,
            strokeWidth = width,
            alpha = alpha,
            strokeStyle = strokeStyle,
            createdAt = now,
            modifiedAt = now,
        )
    }

    fun updateLatestPath(
        newPoint: Offset,
        currentElements: List<Element>,
        pressure: Float = 1f,
    ): List<Element> {
        if (currentElements.isEmpty()) return currentElements

        val lastElement = currentElements.last()
        return if (lastElement is Element.Path) {
            // Decimate sub-pixel samples: a pen drag at 60 Hz easily produces points
            // with sub-world-pixel spacing. The visual difference is invisible but
            // every extra sample grows the Path we rebuild during the active stroke.
            val lastSample = lastElement.samples.lastOrNull()
            if (lastSample != null) {
                val dx = newPoint.x - lastSample.position.x
                val dy = newPoint.y - lastSample.position.y
                if (dx * dx + dy * dy < MIN_PEN_POINT_DIST_SQ) return currentElements
            }
            val newWidth = lastElement.strokeWidth * pressure
            val newSample = Element.PathSample(newPoint, newWidth)
            // The seed sample (inserted by InsertNewPath on tap / drag-start) was
            // created without a pressure reading — Compose's tap and drag-start
            // callbacks don't expose pointer pressure, only position. That seed
            // sits at full strokeWidth while the next sample arrives modulated
            // by pen pressure, which renders as a visible "bigger dot" at the
            // stroke origin. On the first update tick, retroactively conform the
            // seed's width to this tick's pressure so the start cap matches the
            // rest of the stroke.
            val updatedSamples = if (lastElement.samples.size == 1 &&
                lastElement.samples[0].width != newWidth
            ) {
                listOf(lastElement.samples[0].copy(width = newWidth), newSample)
            } else {
                lastElement.samples + newSample
            }
            currentElements.dropLast(1) + lastElement.copy(
                samples = updatedSamples,
            ).touched()
        } else {
            currentElements
        }
    }

    // Shape operations
    @OptIn(ExperimentalTime::class)
    fun insertNewShape(
        shapeType: ShapeType,
        offset: Offset,
        color: Color,
        width: Float,
        cornerRadius: Float = 0f,
        strokeStyle: StrokeStyle = StrokeStyle.SOLID,
    ): Element.Shape {
        val now = Clock.System.now().toEpochMilliseconds()
        return Element.Shape(
            shapeType = shapeType,
            points = listOf(offset),
            strokeColor = color,
            strokeWidth = width,
            cornerRadius = cornerRadius,
            strokeStyle = strokeStyle,
            createdAt = now,
            modifiedAt = now,
        )
    }

    // Text operations
    @OptIn(ExperimentalTime::class)
    fun insertText(
        text: String,
        position: Offset,
        fontSize: Float,
        fontFamilyKey: String,
        alignment: TextAlignment,
        color: Color,
    ): Element.Text {
        val now = Clock.System.now().toEpochMilliseconds()
        // Initial guess: a single-line block at fontSize*1.2 tall. The
        // renderer dispatches Intent.SyncTextMeasuredHeight after the first
        // layout pass, so this value is only ever visible for one frame.
        return Element.Text(
            text = text,
            fontFamilyKey = fontFamilyKey,
            fontSize = fontSize,
            color = color,
            alignment = alignment,
            topLeft = position,
            wrapWidth = DEFAULT_TEXT_BOX_WIDTH,
            measuredHeight = (fontSize * 1.2f).coerceAtLeast(fontSize),
            createdAt = now,
            modifiedAt = now,
        )
    }

    /**
     * Renderer-driven height sync — the canvas measures a text block, sees
     * the rendered height differs from [Element.Text.measuredHeight] and
     * dispatches [io.ak1.drawbox.domain.model.Intent.SyncTextMeasuredHeight]
     * to bring the model in line. Bypasses history so this fixed-point
     * convergence doesn't consume undo slots.
     */
    fun syncTextMeasuredHeight(
        elements: List<Element>,
        id: String,
        height: Float,
    ): List<Element> = elements.map { el ->
        if (el is Element.Text && el.id == id) el.copy(measuredHeight = height)
        else el
    }

    /** Replace the [text] field of a single [Element.Text]. Snapshots history. */
    fun updateText(
        elements: List<Element>,
        id: String,
        text: String,
    ): List<Element> = elements.map { el ->
        if (el.id == id && el is Element.Text) el.copy(text = text).touched()
        else el
    }

    /** Set font size on every selected [Element.Text]. */
    fun setSelectedFontSize(
        elements: List<Element>,
        ids: Set<String>,
        size: Float,
    ): List<Element> = elements.map { el ->
        if (el.id in ids && el is Element.Text) el.copy(fontSize = size).touched() else el
    }

    /** Set text alignment on every selected [Element.Text]. */
    fun setSelectedTextAlignment(
        elements: List<Element>,
        ids: Set<String>,
        alignment: TextAlignment,
    ): List<Element> = elements.map { el ->
        if (el.id in ids && el is Element.Text) el.copy(alignment = alignment).touched() else el
    }

    /** Set font family key on every selected [Element.Text]. */
    fun setSelectedFontFamily(
        elements: List<Element>,
        ids: Set<String>,
        fontFamilyKey: String,
    ): List<Element> = elements.map { el ->
        if (el.id in ids && el is Element.Text) el.copy(fontFamilyKey = fontFamilyKey).touched() else el
    }

    // Image operations
    @OptIn(ExperimentalTime::class)
    fun insertImage(
        bytes: ByteArray,
        position: Offset,
        intrinsicSize: Size,
    ): Element.Image {
        val now = Clock.System.now().toEpochMilliseconds()
        // Fit the placed image into a sensible default extent: clamp the longer
        // side to MAX_PLACED_EXTENT world pixels and preserve aspect ratio.
        // Hosts that want the source size verbatim can resize via
        // SetElementBounds immediately after dispatch.
        val intrinsicW = intrinsicSize.width.coerceAtLeast(1f)
        val intrinsicH = intrinsicSize.height.coerceAtLeast(1f)
        val longer = maxOf(intrinsicW, intrinsicH)
        val scale = if (longer > MAX_PLACED_EXTENT) MAX_PLACED_EXTENT / longer else 1f
        val placedW = intrinsicW * scale
        val placedH = intrinsicH * scale
        // Anchor the AABB on [position]'s center so the drop point lands in the
        // middle of the placed image — matches the user's mental model when
        // dropping or pasting at the cursor.
        val topLeft = Offset(position.x - placedW * 0.5f, position.y - placedH * 0.5f)
        val bottomRight = Offset(topLeft.x + placedW, topLeft.y + placedH)
        return Element.Image(
            bytes = bytes,
            intrinsicSize = intrinsicSize,
            points = listOf(topLeft, bottomRight),
            createdAt = now,
            modifiedAt = now,
        )
    }

    fun updateLatestShape(newPoint: Offset, currentElements: List<Element>): List<Element> {
        if (currentElements.isEmpty()) return currentElements

        val lastElement = currentElements.last()
        return if (lastElement is Element.Shape) {
            // Shapes use exactly two points (start + current end). Appending on
            // every drag tick — as the original code did — bloats `points` with
            // the cursor's whole path, leaving bounds() bigger than the rendered
            // shape and ballooning serialized JSON. Replace the second slot.
            val start = lastElement.points.firstOrNull() ?: newPoint
            val updatedPoints = listOf(start, newPoint)
            currentElements.dropLast(1) + lastElement.copy(points = updatedPoints).touched()
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

    /**
     * Remove every element whose body is within `radius` of `point`. Reuses the
     * same [hitTest] used by selection, with the eraser disk radius substituted
     * for the pick tolerance — which means stroke width is already accounted
     * for. Returns the original list instance when nothing was hit so callers
     * can skip useless state churn.
     */
    fun eraseAt(
        elements: List<Element>,
        point: Offset,
        radius: Float,
    ): List<Element> {
        if (elements.isEmpty()) return elements
        val survivors = elements.filterNot { it.hitTest(point, radius) }
        return if (survivors.size == elements.size) elements else survivors
    }

    /** Promote selected elements to the top of the z-order. */
    fun bringToFront(elements: List<Element>, ids: Set<String>): List<Element> {
        if (ids.isEmpty()) return elements
        val maxZ = elements.maxOfOrNull { it.zIndex } ?: 0
        var next = maxZ + 1
        return elements.map { el ->
            if (el.id in ids) when (el) {
                is Element.Path -> el.copy(zIndex = next++).touched()
                is Element.Shape -> el.copy(zIndex = next++).touched()
                is Element.Image -> el.copy(zIndex = next++).touched()
                is Element.Text -> el.copy(zIndex = next++).touched()
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
                is Element.Path -> el.copy(zIndex = newZ).touched()
                is Element.Shape -> el.copy(zIndex = newZ).touched()
                is Element.Image -> el.copy(zIndex = newZ).touched()
                is Element.Text -> el.copy(zIndex = newZ).touched()
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
            is Element.Path -> el.copy(strokeColor = color).touched()
            is Element.Shape -> el.copy(strokeColor = color).touched()
            // Images have no stroke — recolor is a no-op rather than an error,
            // so multi-selecting a mix of shapes and images still works.
            is Element.Image -> el
            // Text reuses the "stroke color" intent as a unified tint so the
            // existing color picker recolors text without a separate intent.
            is Element.Text -> el.copy(color = color).touched()
        }
    }

    /**
     * Set the fill color of every selected shape. `null` clears the fill so the
     * shape renders stroke-only. Paths and images have no fill concept and are
     * left untouched — multi-selecting a mix still works.
     */
    fun setSelectedFillColor(
        elements: List<Element>,
        ids: Set<String>,
        color: Color?,
    ): List<Element> = elements.map { el ->
        if (el.id !in ids) el else when (el) {
            is Element.Shape -> el.copy(fillColor = color).touched()
            is Element.Path -> el
            is Element.Image -> el
            is Element.Text -> el
        }
    }

    /**
     * Toggle the stroke pass on every selected shape. Paths are always
     * stroked, so they're left untouched.
     */
    fun setSelectedStrokeEnabled(
        elements: List<Element>,
        ids: Set<String>,
        enabled: Boolean,
    ): List<Element> = elements.map { el ->
        if (el.id !in ids) el else when (el) {
            is Element.Shape -> el.copy(strokeEnabled = enabled).touched()
            is Element.Path -> el
            is Element.Image -> el
            is Element.Text -> el
        }
    }

    /** Re-stroke every selected element. */
    fun setSelectedStrokeWidth(
        elements: List<Element>,
        ids: Set<String>,
        width: Float,
    ): List<Element> = elements.map { el ->
        if (el.id !in ids) el else when (el) {
            // For Path: rewrite every sample's width to the new value so the
            // stroke becomes uniform. The user-facing "set stroke width" intent
            // is "make this stroke this thick everywhere," not "leave the
            // pressure-driven shape but change the default for new samples."
            is Element.Path -> el.copy(
                strokeWidth = width,
                samples = el.samples.map { it.copy(width = width) },
            ).touched()
            is Element.Shape -> el.copy(strokeWidth = width).touched()
            is Element.Image -> el
            is Element.Text -> el
        }
    }

    /**
     * Set the corner radius on selected RECTANGLE / TRIANGLE shapes. Other
     * element types in the selection are left untouched.
     */
    fun setSelectedCornerRadius(
        elements: List<Element>,
        ids: Set<String>,
        radius: Float,
    ): List<Element> = elements.map { el ->
        if (el.id !in ids) return@map el
        if (el is Element.Shape && el.shapeType.supportsCornerRadius()) {
            el.copy(cornerRadius = radius).touched()
        } else el
    }

    /** Set the stroke pattern on every selected element (shape OR freehand path). */
    fun setSelectedStrokeStyle(
        elements: List<Element>,
        ids: Set<String>,
        style: StrokeStyle,
    ): List<Element> = elements.map { el ->
        if (el.id !in ids) return@map el
        when (el) {
            is Element.Shape -> el.copy(strokeStyle = style).touched()
            is Element.Path -> el.copy(strokeStyle = style).touched()
            is Element.Image -> el
            is Element.Text -> el
        }
    }

    // ===== Line / arrow editing =====

    /** Overwrite the points list of a single element. */
    fun setElementPoints(
        elements: List<Element>,
        id: String,
        newPoints: List<Offset>,
    ): List<Element> = elements.map { el ->
        if (el.id != id) el else when (el) {
            // setElementPoints today is only called by LINE/ARROW endpoint
            // dragging, but the sealed `when` forces a Path branch. Preserve
            // existing widths by index when the list length is unchanged; fall
            // back to strokeWidth for any extra slots.
            is Element.Path -> el.copy(
                samples = newPoints.mapIndexed { i, p ->
                    val w = el.samples.getOrNull(i)?.width ?: el.strokeWidth
                    Element.PathSample(p, w)
                },
            ).touched()
            is Element.Shape -> el.copy(points = newPoints).touched()
            is Element.Image -> el.copy(points = newPoints).touched()
            // Text geometry is topLeft + wrapWidth + measuredHeight; the
            // point-list intent doesn't apply. Treat as a no-op so multi-
            // selecting LINE / ARROW + Text still works.
            is Element.Text -> el
        }
    }

    /** Update the curve deflection of a single LINE/ARROW shape. */
    fun setLineBend(
        elements: List<Element>,
        id: String,
        bend: Offset,
    ): List<Element> = elements.map { el ->
        if (el.id == id && el is Element.Shape && el.shapeType.isLineLike()) {
            el.copy(bend = bend).touched()
        } else el
    }

    /**
     * Attach connector bindings to an arrow: each endpoint binds to the topmost
     * non-arrow/line shape it sits inside, or to null if it sits in empty space.
     * When this call is what FIRST makes the arrow connect both endpoints, it
     * also seeds a default perpendicular bend so the connector reads as a curve
     * rather than overlapping with the straight center-to-center line.
     */
    fun finalizeArrowBindings(
        elements: List<Element>,
        arrowId: String,
    ): List<Element> {
        val target = elements.firstOrNull { it.id == arrowId } as? Element.Shape ?: return elements
        if (target.shapeType != ShapeType.ARROW || target.points.size < 2) return elements
        val candidates = elements
            .asSequence()
            .filterIsInstance<Element.Shape>()
            .filter { it.id != arrowId && it.shapeType.isBindable() }
            .sortedByDescending { it.zIndex }
            .toList()
        // Strict bounds.contains() is too unforgiving: dropping an arrow
        // endpoint a few pixels outside a shape produces no binding even when
        // the user clearly meant to connect there. Inflate the AABB by a
        // tolerance so near-misses bind, then propagateBindings will snap the
        // endpoint to the actual visible boundary.
        val startId = candidates.firstOrNull {
            it.bounds().inflate(BINDING_HIT_TOLERANCE).contains(target.points[0])
        }?.id
        val endId = candidates.firstOrNull {
            it.bounds().inflate(BINDING_HIT_TOLERANCE).contains(target.points.last())
        }?.id

        val freshFullyConnected = startId != null && endId != null &&
            (target.startBinding != startId || target.endBinding != endId) &&
            target.bend == Offset.Zero
        val seededBend = if (freshFullyConnected && startId != null && endId != null) {
            // Predict where propagateBindings will snap the endpoints to and size
            // the bend relative to THAT segment. Using the raw drag points (often
            // near shape centers) overshoots once snapping moves the endpoints to
            // the boundary — the curve would bulge way past the visible line.
            val startShape = candidates.first { it.id == startId }
            val endShape = candidates.first { it.id == endId }
            val startSnapped = startShape.connectorAnchor(endShape.bounds().center)
            val endSnapped = endShape.connectorAnchor(startShape.bounds().center)
            defaultConnectorBend(startSnapped, endSnapped)
        } else target.bend

        return elements.map { el ->
            if (el.id != arrowId) el
            else (el as Element.Shape).copy(
                startBinding = startId,
                endBinding = endId,
                bend = seededBend,
            ).touched()
        }
    }

    /**
     * Update every arrow's endpoints so they terminate on the bound shape's
     * boundary along the line toward the other endpoint (or its bound center).
     * Idempotent — safe to call after any element mutation. Detaches bindings
     * whose target no longer exists.
     */
    fun propagateBindings(elements: List<Element>): List<Element> {
        val byId = elements.associateBy { it.id }
        return elements.map { el ->
            if (el !is Element.Shape || el.shapeType != ShapeType.ARROW) return@map el
            if (el.startBinding == null && el.endBinding == null) return@map el
            if (el.points.size < 2) return@map el
            val startTarget = el.startBinding?.let { byId[it] as? Element.Shape }
            val endTarget = el.endBinding?.let { byId[it] as? Element.Shape }

            // Direction reference for boundary clipping: the OTHER endpoint's
            // bound center if available, else its raw position. We compute the
            // boundary intersection from each bound shape's center toward that
            // reference point so the arrow visually starts/ends at the edge.
            val endRef = endTarget?.bounds()?.center ?: el.points.last()
            val startRef = startTarget?.bounds()?.center ?: el.points[0]
            val newStart = startTarget?.connectorAnchor(endRef) ?: el.points[0]
            val newEnd = endTarget?.connectorAnchor(startRef) ?: el.points.last()
            val newStartBinding = if (startTarget == null) null else el.startBinding
            val newEndBinding = if (endTarget == null) null else el.endBinding
            // Skip touching the arrow when nothing actually changed — propagateBindings
            // runs after every mutation and most calls are no-ops for any given arrow.
            val unchanged = newStart == el.points[0] && newEnd == el.points.last() &&
                newStartBinding == el.startBinding && newEndBinding == el.endBinding
            if (unchanged) el else el.copy(
                points = listOf(newStart, newEnd),
                startBinding = newStartBinding,
                endBinding = newEndBinding,
            ).touched()
        }
    }
}

/**
 * Minimum world-space distance² between consecutive freehand pen samples.
 * Samples closer than 1 world pixel are dropped — they bloat the active stroke
 * and don't change what the user sees. Squared so we can compare without sqrt.
 */
private const val MIN_PEN_POINT_DIST_SQ: Float = 1f

/**
 * World-pixel cap on the longer side of an image placed via [UseCase.insertImage].
 * Large source images (e.g. 4096×3000 screenshots) would otherwise dominate the
 * default-zoom viewport and force the user to immediately resize. 600 px feels
 * roughly notebook-size on a 1× canvas — still readable, easy to grow.
 */
private const val MAX_PLACED_EXTENT: Float = 600f

/**
 * Default world-pixel width for a freshly inserted text block. Wide enough to
 * fit a typical sentence at default font size without immediate re-wrap, and
 * narrow enough that the wrap box's edge-handle is visible inside the
 * default-zoom viewport.
 */
private const val DEFAULT_TEXT_BOX_WIDTH: Float = 240f

/**
 * Slop, in world pixels, applied to a bindable shape's AABB when deciding
 * whether an arrow endpoint counts as "dropped on" that shape. ~24 world-px is
 * enough that hand-eye inaccuracy doesn't break the connector intent but small
 * enough that distinct shapes don't compete for the same drop.
 */
private const val BINDING_HIT_TOLERANCE: Float = 24f

/** Perpendicular bend ~20% of the line length, counter-clockwise of direction. */
private fun defaultConnectorBend(start: Offset, end: Offset): Offset {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val len = sqrt(dx * dx + dy * dy)
    if (len < 0.5f) return Offset.Zero
    val ux = dx / len
    val uy = dy / len
    val mag = len * 0.2f
    // (-uy, ux) is the counter-clockwise perpendicular.
    return Offset(-uy * mag, ux * mag)
}

private fun ShapeType.isBindable(): Boolean = when (this) {
    ShapeType.RECTANGLE, ShapeType.CIRCLE, ShapeType.TRIANGLE -> true
    ShapeType.LINE, ShapeType.ARROW -> false
}

private fun ShapeType.isLineLike(): Boolean =
    this == ShapeType.LINE || this == ShapeType.ARROW

/** Shape types whose visual outline can be rounded at the corners. */
fun ShapeType.supportsCornerRadius(): Boolean =
    this == ShapeType.RECTANGLE || this == ShapeType.TRIANGLE
