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
    is Element.Path -> pointsBounds(positions)
    is Element.Image -> pointsBounds(points)
    is Element.Text -> Rect(
        left = topLeft.x,
        top = topLeft.y,
        right = topLeft.x + wrapWidth,
        bottom = topLeft.y + measuredHeight,
    )
    is Element.Shape -> when (shapeType) {
        // Circle points are diameter endpoints, so the circle extends past them.
        // The bbox is the square inscribing the circle.
        ShapeType.CIRCLE -> circleBounds(points)
        ShapeType.LINE, ShapeType.ARROW -> lineArrowBounds(this)
        // Rectangles + triangles render from points[0] to points.last(); the
        // intermediate points (if any) are dead weight from older code that
        // appended on every drag tick. Match what the renderer actually draws.
        else -> if (points.size >= 2) {
            pointsBounds(listOf(points.first(), points.last()))
        } else {
            pointsBounds(points)
        }
    }
}

private fun lineArrowBounds(shape: Element.Shape): Rect {
    if (shape.points.size < 2) return pointsBounds(shape.points)
    val start = shape.points[0]
    val end = shape.points.last()
    if (shape.bend == Offset.Zero) return pointsBounds(listOf(start, end))
    // Quadratic bezier with control = midpoint + bend stays inside the convex
    // hull of {start, end, control}, so its bbox = bbox of those three points.
    val mid = Offset((start.x + end.x) * 0.5f, (start.y + end.y) * 0.5f)
    val control = Offset(mid.x + shape.bend.x, mid.y + shape.bend.y)
    return pointsBounds(listOf(start, end, control))
}

/** Quadratic-bezier control point for a curved [ShapeType.LINE] / [ShapeType.ARROW]. */
fun Element.Shape.controlPoint(): Offset {
    val start = points[0]
    val end = points.last()
    val mid = Offset((start.x + end.x) * 0.5f, (start.y + end.y) * 0.5f)
    return Offset(mid.x + bend.x, mid.y + bend.y)
}

/** World-space point on the curve at t = 0.5 — the spot where the bend handle sits. */
fun Element.Shape.bezierMidpoint(): Offset {
    val start = points[0]
    val end = points.last()
    val mid = Offset((start.x + end.x) * 0.5f, (start.y + end.y) * 0.5f)
    return Offset(mid.x + bend.x * 0.5f, mid.y + bend.y * 0.5f)
}

/**
 * World-space attachment point for an arrow connector binding to this shape.
 *
 * Snaps to the **center of the side that faces [target]**, instead of letting
 * the connector slide along the outline:
 *
 * - **Rectangle / square / fallback**: midpoint of whichever of the four
 *   AABB edges faces [target].
 * - **Triangle**: midpoint of whichever of the three edges (right slant,
 *   bottom, left slant) the ray from the centroid toward [target] crosses.
 * - **Circle**: tangent point on the circle in [target]'s direction. Circles
 *   have no sides, so a smooth analytic point is the natural anchor.
 *
 * Rotation-aware. The shape's `rotation` is undone on [target], the anchor
 * is computed in the unrotated frame, then rotated back to world. Scale is
 * automatically handled because [bounds] reflects the post-scale points.
 *
 * Used by `propagateBindings` and `finalizeArrowBindings` so a bound arrow
 * rides the bound shape's facing side through move / resize / rotate.
 */
fun Element.Shape.connectorAnchor(target: Offset): Offset {
    val b = bounds()
    val pivot = b.center
    val localTarget = if (rotation == 0f) target
                      else rotateAround(target, pivot, -rotation)
    val localAnchor: Offset = when (shapeType) {
        ShapeType.CIRCLE -> circleBoundary(pivot, b.width * 0.5f, localTarget)
        ShapeType.TRIANGLE -> triangleSideMidpoint(b, localTarget)
        else -> rectSideMidpoint(b, localTarget)
    }
    return if (rotation == 0f) localAnchor
           else rotateAround(localAnchor, pivot, rotation)
}

/**
 * Midpoint of whichever AABB side faces [target]. The "facing" axis is the
 * one along which [target] escapes the box first — i.e. the larger of
 * `|dx|/halfW` vs `|dy|/halfH`.
 */
private fun rectSideMidpoint(bounds: Rect, target: Offset): Offset {
    val center = bounds.center
    val halfW = bounds.width * 0.5f
    val halfH = bounds.height * 0.5f
    val dx = target.x - center.x
    val dy = target.y - center.y
    if (dx == 0f && dy == 0f) return Offset(bounds.right, center.y)
    val fx = if (halfW > 0f) abs(dx) / halfW else Float.MAX_VALUE
    val fy = if (halfH > 0f) abs(dy) / halfH else Float.MAX_VALUE
    return if (fx >= fy) {
        Offset(if (dx > 0f) bounds.right else bounds.left, center.y)
    } else {
        Offset(center.x, if (dy > 0f) bounds.bottom else bounds.top)
    }
}

/**
 * Midpoint of whichever triangle edge the ray from the centroid toward
 * [target] crosses first. Uses the same edge order as [triangleBoundary]
 * and shares [raySegmentParam] for the intersection math.
 */
private fun triangleSideMidpoint(bounds: Rect, target: Offset): Offset {
    val apex = Offset(bounds.center.x, bounds.top)
    val br = Offset(bounds.right, bounds.bottom)
    val bl = Offset(bounds.left, bounds.bottom)
    val origin = Offset(bounds.center.x, (bounds.top + 2f * bounds.bottom) / 3f)
    val dx = target.x - origin.x
    val dy = target.y - origin.y
    if (dx == 0f && dy == 0f) {
        return Offset((apex.x + br.x) * 0.5f, (apex.y + br.y) * 0.5f)
    }
    val edges = arrayOf(apex to br, br to bl, bl to apex)
    var bestT = Float.MAX_VALUE
    var hitEdge = edges[0]
    for (edge in edges) {
        val t = raySegmentParam(origin, dx, dy, edge.first, edge.second) ?: continue
        if (t > 0f && t < bestT) {
            bestT = t
            hitEdge = edge
        }
    }
    return Offset(
        (hitEdge.first.x + hitEdge.second.x) * 0.5f,
        (hitEdge.first.y + hitEdge.second.y) * 0.5f,
    )
}

/**
 * Point on this shape's outline closest to the ray from the shape's center
 * toward [target]. Used by arrow connectors to terminate at the boundary
 * instead of sinking into the shape.
 *
 * - Circle: exact analytic boundary at radius.
 * - Triangle: ray intersected against the three actual edges; the slanted
 *   sides return a point on the slant, not the AABB.
 * - Rectangle / fallback: AABB boundary along the ray.
 *
 * Note: this slides along the outline. For connector binding, prefer
 * [connectorAnchor] which snaps to side midpoints and respects rotation.
 */
fun Element.Shape.boundaryPointToward(target: Offset): Offset {
    val b = bounds()
    val center = b.center
    return when (shapeType) {
        ShapeType.CIRCLE -> circleBoundary(center, b.width * 0.5f, target)
        ShapeType.TRIANGLE -> triangleBoundary(b, target)
        else -> rectBoundary(b, target)
    }
}

private fun circleBoundary(center: Offset, radius: Float, target: Offset): Offset {
    val dx = target.x - center.x
    val dy = target.y - center.y
    val len = sqrt(dx * dx + dy * dy)
    if (len < 0.5f) return Offset(center.x + radius, center.y)
    return Offset(center.x + dx / len * radius, center.y + dy / len * radius)
}

private fun rectBoundary(bounds: Rect, target: Offset): Offset {
    val center = bounds.center
    val dx = target.x - center.x
    val dy = target.y - center.y
    if (dx == 0f && dy == 0f) return Offset(bounds.right, center.y)
    // Largest t ≤ 1 with center + t*(dx, dy) still inside the box.
    val tx = if (dx != 0f) {
        val edge = if (dx > 0f) bounds.right else bounds.left
        (edge - center.x) / dx
    } else Float.MAX_VALUE
    val ty = if (dy != 0f) {
        val edge = if (dy > 0f) bounds.bottom else bounds.top
        (edge - center.y) / dy
    } else Float.MAX_VALUE
    val t = min(tx, ty).coerceAtLeast(0f)
    return Offset(center.x + t * dx, center.y + t * dy)
}

private fun triangleBoundary(bounds: Rect, target: Offset): Offset {
    // Isosceles triangle drawn with apex at top-center; vertices match DrawBox's
    // drawTriangle path. The centroid is height/6 below the AABB center.
    val apex = Offset(bounds.center.x, bounds.top)
    val br = Offset(bounds.right, bounds.bottom)
    val bl = Offset(bounds.left, bounds.bottom)
    val origin = Offset(bounds.center.x, (bounds.top + 2f * bounds.bottom) / 3f)
    val dx = target.x - origin.x
    val dy = target.y - origin.y
    if (dx == 0f && dy == 0f) return apex
    val edges = arrayOf(apex to br, br to bl, bl to apex)
    var bestT = Float.MAX_VALUE
    var hit = apex
    for ((a, b) in edges) {
        val t = raySegmentParam(origin, dx, dy, a, b) ?: continue
        if (t > 0f && t < bestT) {
            bestT = t
            hit = Offset(origin.x + t * dx, origin.y + t * dy)
        }
    }
    return hit
}

/**
 * Solve `origin + t*(dx, dy) = segA + s*(segB - segA)` for `t`. Returns `t` if
 * `s ∈ [0, 1]` and `t > 0`, else null. Used for triangle boundary intersection.
 */
private fun raySegmentParam(
    origin: Offset,
    dx: Float,
    dy: Float,
    segA: Offset,
    segB: Offset,
): Float? {
    val sx = segB.x - segA.x
    val sy = segB.y - segA.y
    val denom = dx * sy - dy * sx
    if (kotlin.math.abs(denom) < 1e-6f) return null
    val ox = segA.x - origin.x
    val oy = segA.y - origin.y
    val t = (ox * sy - oy * sx) / denom
    val s = (ox * dy - oy * dx) / denom
    if (t <= 0f) return null
    if (s !in 0f..1f) return null
    return t
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
        is Element.Image -> b.inflate(tolerance).contains(local)
        is Element.Text -> b.inflate(tolerance).contains(local)
    }
}

private fun hitTestPath(path: Element.Path, p: Offset, tolerance: Float): Boolean {
    val samples = path.samples
    if (samples.isEmpty()) return false
    if (samples.size == 1) {
        return distance(samples[0].position, p) <= tolerance + samples[0].width * 0.5f
    }
    for (i in 0 until samples.size - 1) {
        val a = samples[i]
        val b = samples[i + 1]
        // Per-segment hit radius widens with the local stroke (taking the
        // average of the two endpoint widths), so variable-pressure strokes
        // are pickable where they're visually thick.
        val hitRadius = tolerance + ((a.width + b.width) * 0.5f) * 0.5f
        if (distanceToSegment(p, a.position, b.position) <= hitRadius) return true
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
            if (shape.bend == Offset.Zero) {
                distanceToSegment(p, start, end) <= hitRadius
            } else {
                val control = shape.controlPoint()
                distanceToQuadraticBezier(p, start, control, end) <= hitRadius
            }
        }
    }
}

private fun distanceToQuadraticBezier(p: Offset, p0: Offset, p1: Offset, p2: Offset): Float {
    // Sample the curve at N points and approximate by closest sample distance.
    // 24 samples is plenty for sub-pixel hit-tests at typical view scales.
    val steps = 24
    var best = Float.MAX_VALUE
    var i = 0
    while (i <= steps) {
        val t = i.toFloat() / steps
        val u = 1f - t
        val x = u * u * p0.x + 2f * u * t * p1.x + t * t * p2.x
        val y = u * u * p0.y + 2f * u * t * p1.y + t * t * p2.y
        val dx = p.x - x
        val dy = p.y - y
        val d2 = dx * dx + dy * dy
        if (d2 < best) best = d2
        i++
    }
    return sqrt(best)
}

/** Topmost element under `point`, picked by descending zIndex. */
fun topmostHit(elements: List<Element>, point: Offset, tolerance: Float = 8f): Element? {
    return elements
        .sortedByDescending { it.zIndex }
        .firstOrNull { it.hitTest(point, tolerance) }
}

/** Translate all points by `delta`. Rotation unchanged. */
fun Element.translate(delta: Offset): Element = when (this) {
    is Element.Path -> copy(
        samples = samples.map { it.copy(position = it.position + delta) },
    ).touched()
    is Element.Shape -> copy(points = points.map { it + delta }).touched()
    is Element.Image -> copy(points = points.map { it + delta }).touched()
    is Element.Text -> copy(topLeft = topLeft + delta).touched()
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
            // Scale spatial positions to the new bbox. Sample widths are NOT
            // scaled — pen pressure is a property of how the user drew the
            // stroke, not of the canvas zoom; preserving per-sample width
            // keeps the visual character of the original stroke after resize.
            val mapped = scalePoints(positions, bounds(), safeBounds)
            copy(samples = samples.mapIndexed { i, s -> s.copy(position = mapped[i]) }).touched()
        }
        is Element.Image -> copy(
            points = listOf(safeBounds.topLeft, safeBounds.bottomRight),
        ).touched()
        // Resize for text only honors the X extents (topLeft.x + wrapWidth)
        // and the topLeft.y. Bottom-edge drags are silently ignored — the
        // renderer owns measuredHeight and will refresh it on the next
        // layout pass via Intent.SyncTextMeasuredHeight. This makes vertical
        // handles a visual no-op, matching the behavior in tldraw / Figma.
        is Element.Text -> copy(
            topLeft = safeBounds.topLeft,
            wrapWidth = safeBounds.width,
        ).touched()
        is Element.Shape -> when (shapeType) {
            ShapeType.CIRCLE -> resizeCircle(this, safeBounds).touched()
            else -> {
                if (points.size < 2) {
                    copy(points = listOf(safeBounds.topLeft, safeBounds.bottomRight)).touched()
                } else {
                    val old = bounds()
                    val start = points[0]
                    val end = points.last()
                    // Map (start, end) into the new bbox, preserving which corner
                    // each point originally landed on.
                    val newStart = mapCorner(start, old, safeBounds)
                    val newEnd = mapCorner(end, old, safeBounds)
                    copy(points = listOf(newStart, newEnd)).touched()
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
    is Element.Path -> copy(rotation = degrees).touched()
    is Element.Shape -> copy(rotation = degrees).touched()
    is Element.Image -> copy(rotation = degrees).touched()
    is Element.Text -> copy(rotation = degrees).touched()
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
