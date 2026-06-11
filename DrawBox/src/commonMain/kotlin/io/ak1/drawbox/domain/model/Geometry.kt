package io.ak1.drawbox.domain.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Per-element geometry primitives used by selection, hit testing, and resize/
 * rotate transforms. Pure functions — no Compose UI state, no side effects.
 *
 * Coordinate model:
 *   - Element `points` are stored in unrotated (logical) space.
 *   - `bounds()` returns the axis-aligned bounding box of those points.
 *   - Rendering and hit testing apply `rotation` around `bounds().center`.
 */

/** Axis-aligned bounding box of the element in unrotated logical space. */
fun Element.bounds(): Rect = when (this) {
    is Element.Path -> pointsBounds(points)
    is Element.Shape -> when (shapeType) {
        // Circle points are diameter endpoints, so the circle extends past them.
        // The bbox is the square inscribing the circle.
        ShapeType.CIRCLE -> circleBounds(points)
        else -> pointsBounds(points)
    }
}

private fun circleBounds(points: List<Offset>): Rect {
    if (points.size < 2) return pointsBounds(points)
    val start = points[0]
    val end = points.last()
    val cx = (start.x + end.x) * 0.5f
    val cy = (start.y + end.y) * 0.5f
    val dx = end.x - start.x
    val dy = end.y - start.y
    val radius = sqrt(dx * dx + dy * dy) * 0.5f
    return Rect(cx - radius, cy - radius, cx + radius, cy + radius)
}

private fun pointsBounds(points: List<Offset>): Rect {
    if (points.isEmpty()) return Rect(0f, 0f, 0f, 0f)
    var minX = points[0].x
    var minY = points[0].y
    var maxX = minX
    var maxY = minY
    for (i in 1 until points.size) {
        val p = points[i]
        if (p.x < minX) minX = p.x
        if (p.y < minY) minY = p.y
        if (p.x > maxX) maxX = p.x
        if (p.y > maxY) maxY = p.y
    }
    return Rect(minX, minY, maxX, maxY)
}

/**
 * Hit test against a world-space point. Inverse-rotates the point into the
 * element's logical space, then applies a per-type test that accounts for
 * stroke width and the provided pick tolerance.
 */
fun Element.hitTest(point: Offset, tolerance: Float = 8f): Boolean {
    val b = bounds()
    val local = if (rotation == 0f) point else rotateAround(point, b.center, -rotation)
    return when (this) {
        is Element.Path -> hitTestPath(this, local, tolerance)
        is Element.Shape -> hitTestShape(this, local, b, tolerance)
    }
}

private fun hitTestPath(path: Element.Path, p: Offset, tolerance: Float): Boolean {
    val hitRadius = tolerance + path.strokeWidth * 0.5f
    val pts = path.points
    if (pts.isEmpty()) return false
    if (pts.size == 1) return distance(pts[0], p) <= hitRadius
    for (i in 0 until pts.size - 1) {
        if (distanceToSegment(p, pts[i], pts[i + 1]) <= hitRadius) return true
    }
    return false
}

private fun hitTestShape(
    shape: Element.Shape,
    p: Offset,
    bounds: Rect,
    tolerance: Float,
): Boolean {
    val hitRadius = tolerance + shape.strokeWidth * 0.5f
    if (shape.points.size < 2) return false
    val start = shape.points[0]
    val end = shape.points.last()
    return when (shape.shapeType) {
        ShapeType.RECTANGLE -> {
            if (shape.fillColor != null) {
                bounds.inflate(hitRadius).contains(p)
            } else {
                val outer = bounds.inflate(hitRadius)
                val inner = bounds.deflate(hitRadius)
                outer.contains(p) && !inner.contains(p)
            }
        }
        ShapeType.CIRCLE -> {
            val center = Offset(
                (start.x + end.x) * 0.5f,
                (start.y + end.y) * 0.5f,
            )
            val radius = distance(start, end) * 0.5f
            val d = distance(p, center)
            if (shape.fillColor != null) {
                d <= radius + hitRadius
            } else {
                abs(d - radius) <= hitRadius
            }
        }
        ShapeType.TRIANGLE -> {
            val w = abs(end.x - start.x)
            val h = abs(end.y - start.y)
            val tl = Offset(min(start.x, end.x), min(start.y, end.y))
            val apex = Offset(tl.x + w * 0.5f, tl.y)
            val br = Offset(tl.x + w, tl.y + h)
            val bl = Offset(tl.x, tl.y + h)
            if (shape.fillColor != null) {
                pointInTriangle(p, apex, br, bl)
            } else {
                distanceToSegment(p, apex, br) <= hitRadius ||
                    distanceToSegment(p, br, bl) <= hitRadius ||
                    distanceToSegment(p, bl, apex) <= hitRadius
            }
        }
        ShapeType.LINE, ShapeType.ARROW -> {
            distanceToSegment(p, start, end) <= hitRadius
        }
    }
}

/** Topmost element under `point`, picked by descending zIndex. */
fun topmostHit(elements: List<Element>, point: Offset, tolerance: Float = 8f): Element? {
    return elements
        .sortedByDescending { it.zIndex }
        .firstOrNull { it.hitTest(point, tolerance) }
}

/** Translate all points by `delta`. Rotation unchanged. */
fun Element.translate(delta: Offset): Element = when (this) {
    is Element.Path -> copy(points = points.map { it + delta })
    is Element.Shape -> copy(points = points.map { it + delta })
}

/**
 * Scale points so the bounding box matches `newBounds`. For two-point shapes
 * this snaps start/end to the new bbox corners (preserving orientation of the
 * original drag direction). For freedraw paths every point is scaled linearly.
 * Rotation unchanged.
 */
fun Element.withBounds(newBounds: Rect): Element {
    val safeBounds = newBounds.normalized().minSize(1f)
    return when (this) {
        is Element.Path -> {
            val mapped = scalePoints(points, bounds(), safeBounds)
            copy(points = mapped)
        }
        is Element.Shape -> when (shapeType) {
            ShapeType.CIRCLE -> resizeCircle(this, safeBounds)
            else -> {
                if (points.size < 2) {
                    copy(points = listOf(safeBounds.topLeft, safeBounds.bottomRight))
                } else {
                    val old = bounds()
                    val start = points[0]
                    val end = points.last()
                    // Map (start, end) into the new bbox, preserving which corner
                    // each point originally landed on.
                    val newStart = mapCorner(start, old, safeBounds)
                    val newEnd = mapCorner(end, old, safeBounds)
                    copy(points = listOf(newStart, newEnd))
                }
            }
        }
    }
}

private fun resizeCircle(shape: Element.Shape, newBounds: Rect): Element.Shape {
    // Circles stay circular even when newBounds is not square — radius snaps to
    // the smaller dimension. Diameter direction is preserved so visual orientation
    // (which a circle encodes via its two-point representation) survives a resize.
    val newCenter = newBounds.center
    val newRadius = min(newBounds.width, newBounds.height) * 0.5f
    val start = shape.points[0]
    val end = shape.points.last()
    val dx = end.x - start.x
    val dy = end.y - start.y
    val len = sqrt(dx * dx + dy * dy)
    val dirX = if (len > 0f) dx / len else 1f
    val dirY = if (len > 0f) dy / len else 0f
    val hx = dirX * newRadius
    val hy = dirY * newRadius
    return shape.copy(
        points = listOf(
            Offset(newCenter.x - hx, newCenter.y - hy),
            Offset(newCenter.x + hx, newCenter.y + hy),
        ),
    )
}

private fun mapCorner(p: Offset, from: Rect, to: Rect): Offset {
    val tx = if (from.width <= 0f) 0f else (p.x - from.left) / from.width
    val ty = if (from.height <= 0f) 0f else (p.y - from.top) / from.height
    return Offset(to.left + tx * to.width, to.top + ty * to.height)
}

private fun scalePoints(points: List<Offset>, from: Rect, to: Rect): List<Offset> {
    if (points.isEmpty()) return points
    return points.map { mapCorner(it, from, to) }
}

/** Absolute rotation in degrees. */
fun Element.withRotation(degrees: Float): Element = when (this) {
    is Element.Path -> copy(rotation = degrees)
    is Element.Shape -> copy(rotation = degrees)
}

/**
 * Compute new bounds when dragging [handle] of [originalBounds] to world-space
 * pointer [pointer]. The opposite corner / edge midpoint stays anchored in
 * **world** space, even when the element is rotated.
 *
 * The math: with a rotation θ around the bbox center, applying the local-space
 * handle drag shifts the bbox center from C_old to C_new. Without correction
 * the opposite anchor (fixed in local space) drifts in world space by
 * `R_θ(C_new − C_old) − (C_new − C_old)`. We translate the new bounds by
 * `(I − R_θ)(C_old − C_new)` to cancel that drift.
 */
fun resizeBounds(
    originalBounds: Rect,
    rotation: Float,
    handle: ResizeHandle,
    pointer: Offset,
): Rect {
    val centerOld = originalBounds.center
    val localPointer = if (rotation == 0f) pointer
                       else rotateAround(pointer, centerOld, -rotation)
    val newBoundsLocal = applyHandleDrag(originalBounds, handle, localPointer)
        .normalized()
        .minSize(1f)
    if (rotation == 0f) return newBoundsLocal

    val centerNew = newBoundsLocal.center
    val deltaCenter = Offset(centerOld.x - centerNew.x, centerOld.y - centerNew.y)
    val rotatedDelta = rotateAround(deltaCenter, Offset.Zero, rotation)
    val shift = Offset(deltaCenter.x - rotatedDelta.x, deltaCenter.y - rotatedDelta.y)
    return Rect(
        left = newBoundsLocal.left + shift.x,
        top = newBoundsLocal.top + shift.y,
        right = newBoundsLocal.right + shift.x,
        bottom = newBoundsLocal.bottom + shift.y,
    )
}

private fun applyHandleDrag(rect: Rect, handle: ResizeHandle, p: Offset): Rect = when (handle) {
    ResizeHandle.TopLeft     -> Rect(p.x, p.y, rect.right, rect.bottom)
    ResizeHandle.Top         -> Rect(rect.left, p.y, rect.right, rect.bottom)
    ResizeHandle.TopRight    -> Rect(rect.left, p.y, p.x, rect.bottom)
    ResizeHandle.Right       -> Rect(rect.left, rect.top, p.x, rect.bottom)
    ResizeHandle.BottomRight -> Rect(rect.left, rect.top, p.x, p.y)
    ResizeHandle.Bottom      -> Rect(rect.left, rect.top, rect.right, p.y)
    ResizeHandle.BottomLeft  -> Rect(p.x, rect.top, rect.right, p.y)
    ResizeHandle.Left        -> Rect(p.x, rect.top, rect.right, rect.bottom)
}

/**
 * Type-aware variant of [resizeBounds]. The returned bounds is sized so that
 * `element.withBounds(returnedBounds).bounds() == returnedBounds` — i.e., no
 * second-pass snapping inside [withBounds] can shift the result.
 *
 * For circles, the result is constrained to a square anchored at the un-dragged
 * corner / edge midpoint in local space. The rotation shift is then applied to
 * keep that anchor's WORLD position fixed.
 */
fun resizeBoundsForElement(
    element: Element,
    handle: ResizeHandle,
    pointer: Offset,
): Rect {
    val originalBounds = element.bounds()
    val rotation = element.rotation
    val centerOld = originalBounds.center
    val localPointer = if (rotation == 0f) pointer
                       else rotateAround(pointer, centerOld, -rotation)
    val raw = applyHandleDrag(originalBounds, handle, localPointer).normalized().minSize(1f)
    val constrained = constrainBoundsForType(element, handle, raw, originalBounds)
    if (rotation == 0f) return constrained

    val centerNew = constrained.center
    val deltaCenter = Offset(centerOld.x - centerNew.x, centerOld.y - centerNew.y)
    val rotatedDelta = rotateAround(deltaCenter, Offset.Zero, rotation)
    val shift = Offset(deltaCenter.x - rotatedDelta.x, deltaCenter.y - rotatedDelta.y)
    return Rect(
        left = constrained.left + shift.x,
        top = constrained.top + shift.y,
        right = constrained.right + shift.x,
        bottom = constrained.bottom + shift.y,
    )
}

private fun constrainBoundsForType(
    element: Element,
    handle: ResizeHandle,
    raw: Rect,
    original: Rect,
): Rect {
    if (element !is Element.Shape || element.shapeType != ShapeType.CIRCLE) return raw
    // Circle must stay a square AND keep the un-dragged anchor at its local-space
    // position. Side is the raw dragged dimension for edge handles, max of the
    // two for corner handles (grows to encompass the drag).
    val anchor = anchorLocalForHandle(handle, original)
    val side = when (handle) {
        ResizeHandle.Top, ResizeHandle.Bottom -> raw.height
        ResizeHandle.Left, ResizeHandle.Right -> raw.width
        else -> max(raw.width, raw.height)
    }.coerceAtLeast(1f)
    return when (handle) {
        ResizeHandle.TopLeft -> Rect(anchor.x - side, anchor.y - side, anchor.x, anchor.y)
        ResizeHandle.TopRight -> Rect(anchor.x, anchor.y - side, anchor.x + side, anchor.y)
        ResizeHandle.BottomLeft -> Rect(anchor.x - side, anchor.y, anchor.x, anchor.y + side)
        ResizeHandle.BottomRight -> Rect(anchor.x, anchor.y, anchor.x + side, anchor.y + side)
        ResizeHandle.Top -> Rect(
            anchor.x - side * 0.5f, anchor.y - side,
            anchor.x + side * 0.5f, anchor.y,
        )
        ResizeHandle.Bottom -> Rect(
            anchor.x - side * 0.5f, anchor.y,
            anchor.x + side * 0.5f, anchor.y + side,
        )
        ResizeHandle.Left -> Rect(
            anchor.x - side, anchor.y - side * 0.5f,
            anchor.x, anchor.y + side * 0.5f,
        )
        ResizeHandle.Right -> Rect(
            anchor.x, anchor.y - side * 0.5f,
            anchor.x + side, anchor.y + side * 0.5f,
        )
    }
}

/** Local-space anchor point that must stay fixed when [handle] is dragged. */
private fun anchorLocalForHandle(handle: ResizeHandle, bounds: Rect): Offset = when (handle) {
    ResizeHandle.TopLeft -> Offset(bounds.right, bounds.bottom)
    ResizeHandle.Top -> Offset(bounds.center.x, bounds.bottom)
    ResizeHandle.TopRight -> Offset(bounds.left, bounds.bottom)
    ResizeHandle.Right -> Offset(bounds.left, bounds.center.y)
    ResizeHandle.BottomRight -> Offset(bounds.left, bounds.top)
    ResizeHandle.Bottom -> Offset(bounds.center.x, bounds.top)
    ResizeHandle.BottomLeft -> Offset(bounds.right, bounds.top)
    ResizeHandle.Left -> Offset(bounds.right, bounds.center.y)
}

/**
 * Angle in degrees from `center` to `pointer` measured from the positive x-axis,
 * clockwise positive (matching Compose's rotation direction).
 */
fun angleFromCenter(center: Offset, pointer: Offset): Float {
    val rad = atan2(pointer.y - center.y, pointer.x - center.x)
    return rad * (180f / PI.toFloat())
}

// ---------- Geometry helpers ----------

internal fun distance(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}

private fun distanceToSegment(p: Offset, a: Offset, b: Offset): Float {
    val dx = b.x - a.x
    val dy = b.y - a.y
    val lengthSq = dx * dx + dy * dy
    if (lengthSq == 0f) return distance(p, a)
    val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / lengthSq).coerceIn(0f, 1f)
    val projX = a.x + t * dx
    val projY = a.y + t * dy
    val ex = p.x - projX
    val ey = p.y - projY
    return sqrt(ex * ex + ey * ey)
}

private fun pointInTriangle(p: Offset, a: Offset, b: Offset, c: Offset): Boolean {
    val s1 = sign(p, a, b)
    val s2 = sign(p, b, c)
    val s3 = sign(p, c, a)
    val hasNeg = s1 < 0 || s2 < 0 || s3 < 0
    val hasPos = s1 > 0 || s2 > 0 || s3 > 0
    return !(hasNeg && hasPos)
}

private fun sign(p: Offset, a: Offset, b: Offset): Float =
    (p.x - b.x) * (a.y - b.y) - (a.x - b.x) * (p.y - b.y)

/** Rotate `point` around `pivot` by `degrees` (counter-clockwise positive). */
internal fun rotateAround(point: Offset, pivot: Offset, degrees: Float): Offset {
    if (degrees == 0f) return point
    val rad = degrees * (PI.toFloat() / 180f)
    val s = sin(rad)
    val c = cos(rad)
    val dx = point.x - pivot.x
    val dy = point.y - pivot.y
    return Offset(
        pivot.x + dx * c - dy * s,
        pivot.y + dx * s + dy * c,
    )
}

internal fun Rect.normalized(): Rect = Rect(
    left = min(left, right),
    top = min(top, bottom),
    right = max(left, right),
    bottom = max(top, bottom),
)

internal fun Rect.minSize(min: Float): Rect {
    val w = max(width, min)
    val h = max(height, min)
    return Rect(left, top, left + w, top + h)
}
