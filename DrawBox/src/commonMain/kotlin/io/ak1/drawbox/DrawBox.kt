package io.ak1.drawbox

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.ak1.drawbox.domain.model.BackgroundPattern
import io.ak1.drawbox.domain.model.Element
import io.ak1.drawbox.domain.model.Intent
import io.ak1.drawbox.domain.model.Mode
import io.ak1.drawbox.domain.model.ResizeHandle
import io.ak1.drawbox.domain.model.ShapeType
import io.ak1.drawbox.domain.model.State
import io.ak1.drawbox.domain.model.angleFromCenter
import io.ak1.drawbox.domain.model.bounds
import io.ak1.drawbox.domain.model.distance
import io.ak1.drawbox.domain.model.hitTest
import io.ak1.drawbox.domain.model.resizeBoundsForElement
import io.ak1.drawbox.domain.model.rotateAround
import io.ak1.drawbox.domain.model.topmostHit
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Main drawing canvas composable supporting both freehand drawing and geometric shapes.
 *
 * [DrawBox] is a high-performance drawing surface that:
 * - Renders both [Element.Path] (freehand) and [Element.Shape] (geometric) elements
 * - Handles user gestures (tap, drag) and converts them to intents
 * - Adapts gesture behavior based on the current [Mode]
 * - Supports undo/redo for all drawing operations
 * - Captures canvas as bitmap for saving
 *
 * **Drawing Modes:**
 * - **PEN** ([Mode.PEN]): Tap/drag creates [Element.Path] for freehand drawing
 * - **RECTANGLE/CIRCLE/TRIANGLE/ARROW/LINE**: Tap/drag creates [Element.Shape] of that type
 *
 * **Rendering Pipeline:**
 * 1. Solid [State.bgColor] fill
 * 2. Optional [State.bgPattern] tiled via a cached [ShaderBrush] (rasterized once
 *    per pattern change with tint baked in; per-frame cost is a single `drawRect`)
 * 3. Sort elements by [Element.zIndex] (lower values render first)
 * 4. Render each element using [renderElement]
 * 5. Paths are rendered with strokes, shapes with their specific geometry
 *
 * **Architecture:**
 * ```
 * DrawBox (UI Layer)
 *   ↓ gesture input
 * Intent dispatch
 *   ↓
 * ViewMode/Reducer (State Layer)
 *   ↓ new state
 * DrawBox recomposes
 *   ↓ reads state.mode
 * Changes gesture behavior
 * ```
 *
 * **Performance Notes:**
 * - Uses [rememberGraphicsLayer] to render elements efficiently
 * - Pointer input is keyed by [State.mode] to update on mode changes
 * - [State.bgPattern] is converted to a tiled [ShaderBrush] via [remember];
 *   per-frame work is a single `drawRect` rather than an N×M painter tile loop
 * - Large element lists may impact frame rate; consider implementing virtualization
 * - On-screen canvas changes are tracked via state hashCode
 *
 * @param state Current drawing state containing elements, colors, and current mode
 * @param onIntent Callback to dispatch intents (user actions)
 * @param modifier Layout modifier for the canvas (defaults to fillMaxSize)
 *
 * @see State
 * @see Intent
 * @see Mode
 * @see Element
 * @see BackgroundPattern
 * @see renderElement for element-specific rendering
 */
@Composable
fun DrawBox(
    state: State,
    onIntent: (Intent) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
) {
    val graphicsLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.hashCode()){
        state.invokeBitmap = {
            scope.launch {
                try {
                    onIntent(Intent.SaveBitmap(graphicsLayer.toImageBitmap(), null))
                } catch (e: Throwable) {
                    onIntent(Intent.SaveBitmap(null, e))
                }
            }
        }
    }

    // Rasterize + tile the pattern once per [bgPattern] / density / layoutDirection
    // change. Without remember, every drag tick recomposes DrawBox, rebuilds the
    // modifier chain, and re-tiles N×M painter draws per frame; with remember the
    // per-frame work in `.drawBehind` collapses to a single shader-backed drawRect.
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val patternBrush: ShaderBrush? = remember(state.bgPattern, density, layoutDirection) {
        state.bgPattern?.toTiledBrush(density, layoutDirection)
    }

    val handleHitPx = with(density) { 14.dp.toPx() }
    val rotationOffsetPx = with(density) { 28.dp.toPx() }
    val handleSizePx = with(density) { 8.dp.toPx() }
    val pickTolerancePx = with(density) { 8.dp.toPx() }

    // The pointerInput coroutines are long-lived; reading state/onIntent through
    // rememberUpdatedState lets gesture callbacks see the current value without
    // re-keying (and re-allocating) the pointerInput block on every state change.
    val latestState by rememberUpdatedState(state)
    val latestOnIntent by rememberUpdatedState(onIntent)

    Box(
        modifier = modifier
            // Layering: solid bgColor → tiled bgPattern (if any) → graphicsLayer of strokes/shapes.
            .background(state.bgColor)
            .drawBehind {
                patternBrush?.let { drawRect(it) }
            }
            .drawWithContent {
                graphicsLayer.record { this@drawWithContent.drawContent() }
                drawLayer(graphicsLayer)
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        when (latestState.mode) {
                            Mode.SELECT -> latestOnIntent(Intent.SelectAt(offset, pickTolerancePx))
                            Mode.PEN -> {
                                latestOnIntent(Intent.InsertNewPath(offset))
                                latestOnIntent(Intent.UpdateLatestPath(offset))
                            }
                            else -> modeShapeType(latestState.mode)?.let { st ->
                                latestOnIntent(Intent.InsertNewShape(st, offset))
                                latestOnIntent(Intent.UpdateLatestShape(offset))
                            }
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                var interaction: SelectionInteraction? = null
                var lastPos = Offset.Zero

                detectDragGestures(
                    onDragStart = { offset ->
                        lastPos = offset
                        val s = latestState
                        when (s.mode) {
                            Mode.SELECT -> {
                                val classified = classifySelection(
                                    state = s,
                                    pointer = offset,
                                    handleHitPx = handleHitPx,
                                    rotationOffsetPx = rotationOffsetPx,
                                    pickTolerancePx = pickTolerancePx,
                                )
                                interaction = when (classified) {
                                    is SelectionInteraction.SelectAndMove -> {
                                        latestOnIntent(Intent.SelectAt(offset, pickTolerancePx))
                                        latestOnIntent(Intent.BeginTransform)
                                        SelectionInteraction.Move
                                    }
                                    is SelectionInteraction.Move,
                                    is SelectionInteraction.Resize,
                                    is SelectionInteraction.Rotate -> {
                                        latestOnIntent(Intent.BeginTransform)
                                        classified
                                    }
                                    is SelectionInteraction.Marquee -> {
                                        latestOnIntent(Intent.SetMarqueeRect(Rect(offset, offset)))
                                        classified
                                    }
                                }
                            }
                            Mode.PEN -> latestOnIntent(Intent.InsertNewPath(offset))
                            else -> modeShapeType(s.mode)?.let { st ->
                                latestOnIntent(Intent.InsertNewShape(st, offset))
                            }
                        }
                    },
                    onDragEnd = {
                        if (latestState.mode == Mode.SELECT) {
                            val rect = latestState.marqueeRect
                            if (interaction is SelectionInteraction.Marquee && rect != null) {
                                latestOnIntent(Intent.CommitMarquee(rect))
                            } else if (interaction is SelectionInteraction.Marquee) {
                                latestOnIntent(Intent.SetMarqueeRect(null))
                            }
                        }
                        interaction = null
                    },
                    onDragCancel = {
                        if (latestState.mode == Mode.SELECT &&
                            interaction is SelectionInteraction.Marquee
                        ) {
                            latestOnIntent(Intent.SetMarqueeRect(null))
                        }
                        interaction = null
                    },
                ) { change, _ ->
                    val pos = change.position
                    when (latestState.mode) {
                        Mode.SELECT -> when (val i = interaction) {
                            is SelectionInteraction.Move -> {
                                latestOnIntent(Intent.MoveSelected(pos - lastPos))
                                lastPos = pos
                            }
                            is SelectionInteraction.Resize -> {
                                val newBounds = resizeBoundsForElement(
                                    i.originalElement, i.handle, pos,
                                )
                                latestOnIntent(Intent.SetElementBounds(i.elementId, newBounds))
                            }
                            is SelectionInteraction.Rotate -> {
                                val current = angleFromCenter(i.center, pos)
                                val delta = current - i.initialAngle
                                latestOnIntent(Intent.SetElementRotation(
                                    i.elementId, i.originalRotation + delta,
                                ))
                            }
                            is SelectionInteraction.Marquee -> {
                                latestOnIntent(Intent.SetMarqueeRect(Rect(i.anchor, pos)))
                            }
                            else -> {}
                        }
                        Mode.PEN -> latestOnIntent(Intent.UpdateLatestPath(pos))
                        else -> if (modeShapeType(latestState.mode) != null) {
                            latestOnIntent(Intent.UpdateLatestShape(pos))
                        }
                    }
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            state.elements
                .sortedBy { it.zIndex }
                .forEach { element ->
                    renderElement(element)
                }
            drawSelectionChrome(
                state = state,
                handleSizePx = handleSizePx,
                rotationOffsetPx = rotationOffsetPx,
            )
        }
    }
}

private fun modeShapeType(mode: Mode): ShapeType? = when (mode) {
    Mode.RECTANGLE -> ShapeType.RECTANGLE
    Mode.CIRCLE -> ShapeType.CIRCLE
    Mode.TRIANGLE -> ShapeType.TRIANGLE
    Mode.ARROW -> ShapeType.ARROW
    Mode.LINE -> ShapeType.LINE
    Mode.PEN, Mode.SELECT -> null
}

// ==================== Selection gesture support ====================

/** Active interaction inferred from a drag-start in SELECT mode. */
private sealed class SelectionInteraction {
    data object Move : SelectionInteraction()
    data object SelectAndMove : SelectionInteraction()
    data class Resize(
        val elementId: String,
        val handle: ResizeHandle,
        val originalElement: Element,
    ) : SelectionInteraction()
    data class Rotate(
        val elementId: String,
        val center: Offset,
        val initialAngle: Float,
        val originalRotation: Float,
    ) : SelectionInteraction()
    data class Marquee(val anchor: Offset) : SelectionInteraction()
}

private fun classifySelection(
    state: State,
    pointer: Offset,
    handleHitPx: Float,
    rotationOffsetPx: Float,
    pickTolerancePx: Float,
): SelectionInteraction {
    // Single-selection: check rotation + resize handles first.
    if (state.selectedIds.size == 1) {
        val element = state.elements.firstOrNull { it.id in state.selectedIds }
        if (element != null) {
            val b = element.bounds()
            val rotHandle = rotationHandleWorld(b, element.rotation, rotationOffsetPx)
            if (distance(pointer, rotHandle) <= handleHitPx) {
                return SelectionInteraction.Rotate(
                    elementId = element.id,
                    center = b.center,
                    initialAngle = angleFromCenter(b.center, pointer),
                    originalRotation = element.rotation,
                )
            }
            for ((h, p) in resizeHandlesWorld(b, element.rotation)) {
                if (distance(pointer, p) <= handleHitPx) {
                    return SelectionInteraction.Resize(
                        elementId = element.id,
                        handle = h,
                        originalElement = element,
                    )
                }
            }
        }
    }
    // Drag inside already-selected → move existing selection.
    if (state.elements.any { it.id in state.selectedIds && it.hitTest(pointer, pickTolerancePx) }) {
        return SelectionInteraction.Move
    }
    // Drag on unselected element → select-then-move.
    val any = topmostHit(state.elements, pointer, pickTolerancePx)
    if (any != null) return SelectionInteraction.SelectAndMove
    // Empty space → marquee.
    return SelectionInteraction.Marquee(pointer)
}

private fun resizeHandlesLocal(bounds: Rect): List<Pair<ResizeHandle, Offset>> = listOf(
    ResizeHandle.TopLeft to Offset(bounds.left, bounds.top),
    ResizeHandle.Top to Offset(bounds.center.x, bounds.top),
    ResizeHandle.TopRight to Offset(bounds.right, bounds.top),
    ResizeHandle.Right to Offset(bounds.right, bounds.center.y),
    ResizeHandle.BottomRight to Offset(bounds.right, bounds.bottom),
    ResizeHandle.Bottom to Offset(bounds.center.x, bounds.bottom),
    ResizeHandle.BottomLeft to Offset(bounds.left, bounds.bottom),
    ResizeHandle.Left to Offset(bounds.left, bounds.center.y),
)

private fun resizeHandlesWorld(
    bounds: Rect,
    rotation: Float,
): List<Pair<ResizeHandle, Offset>> {
    val local = resizeHandlesLocal(bounds)
    return if (rotation == 0f) local
    else local.map { (h, p) -> h to rotateAround(p, bounds.center, rotation) }
}

private fun rotationHandleLocal(bounds: Rect, offsetPx: Float): Offset =
    Offset(bounds.center.x, bounds.top - offsetPx)

private fun rotationHandleWorld(
    bounds: Rect,
    rotation: Float,
    offsetPx: Float,
): Offset {
    val local = rotationHandleLocal(bounds, offsetPx)
    return if (rotation == 0f) local else rotateAround(local, bounds.center, rotation)
}

// ==================== Selection chrome rendering ====================

private val SelectionAccent = Color(0xFF2196F3)
private val SelectionMarqueeFill = Color(0x222196F3)

private fun DrawScope.drawSelectionChrome(
    state: State,
    handleSizePx: Float,
    rotationOffsetPx: Float,
) {
    state.elements
        .asSequence()
        .filter { it.id in state.selectedIds }
        .forEach { el ->
            val b = el.bounds()
            if (el.rotation == 0f) {
                drawSelectionForElement(b, handleSizePx, rotationOffsetPx)
            } else {
                withTransform({ rotate(el.rotation, pivot = b.center) }) {
                    drawSelectionForElement(b, handleSizePx, rotationOffsetPx)
                }
            }
        }
    state.marqueeRect?.let { rect ->
        val r = Rect(
            left = minOf(rect.left, rect.right),
            top = minOf(rect.top, rect.bottom),
            right = maxOf(rect.left, rect.right),
            bottom = maxOf(rect.top, rect.bottom),
        )
        drawRect(
            color = SelectionMarqueeFill,
            topLeft = r.topLeft,
            size = Size(r.width, r.height),
        )
        drawRect(
            color = SelectionAccent,
            topLeft = r.topLeft,
            size = Size(r.width, r.height),
            style = Stroke(
                width = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
            ),
        )
    }
}

private fun DrawScope.drawSelectionForElement(
    bounds: Rect,
    handleSizePx: Float,
    rotationOffsetPx: Float,
) {
    // Bounding box.
    drawRect(
        color = SelectionAccent,
        topLeft = bounds.topLeft,
        size = Size(bounds.width, bounds.height),
        style = Stroke(width = 1.5f),
    )
    // Rotation handle: short line up + filled circle.
    val rotHandle = rotationHandleLocal(bounds, rotationOffsetPx)
    drawLine(
        color = SelectionAccent,
        start = Offset(bounds.center.x, bounds.top),
        end = rotHandle,
        strokeWidth = 1.5f,
    )
    drawCircle(
        color = Color.White,
        radius = handleSizePx * 0.6f,
        center = rotHandle,
    )
    drawCircle(
        color = SelectionAccent,
        radius = handleSizePx * 0.6f,
        center = rotHandle,
        style = Stroke(1.5f),
    )
    // Resize handles.
    val half = handleSizePx * 0.5f
    resizeHandlesLocal(bounds).forEach { (_, p) ->
        drawRect(
            color = Color.White,
            topLeft = Offset(p.x - half, p.y - half),
            size = Size(handleSizePx, handleSizePx),
        )
        drawRect(
            color = SelectionAccent,
            topLeft = Offset(p.x - half, p.y - half),
            size = Size(handleSizePx, handleSizePx),
            style = Stroke(1.5f),
        )
    }
}

/**
 * Build a [ShaderBrush] that tiles the pattern's painter across the canvas via a
 * GPU-cached [ImageShader] with [TileMode.Repeated]. The painter is rasterized
 * to an [ImageBitmap] at its intrinsic size (64dp square fallback), with the
 * optional [BackgroundPattern.tint] baked in as a SrcIn color filter. Built once
 * per pattern change and reused across recompositions, so per-frame work in
 * `.drawBehind` reduces to a single [androidx.compose.ui.graphics.drawscope.DrawScope.drawRect] call.
 */
private fun BackgroundPattern.toTiledBrush(
    density: Density,
    layoutDirection: LayoutDirection,
): ShaderBrush {
    val intrinsic = painter.intrinsicSize
    val tileSize = if (intrinsic.isSpecified && intrinsic.width > 0f && intrinsic.height > 0f) {
        IntSize(
            intrinsic.width.toInt().coerceAtLeast(1),
            intrinsic.height.toInt().coerceAtLeast(1),
        )
    } else {
        val fallback = with(density) { 64.dp.toPx().toInt() }.coerceAtLeast(1)
        IntSize(fallback, fallback)
    }
    val bitmap = painter.rasterize(tileSize, density, layoutDirection, tint)
    return ShaderBrush(
        ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated),
    )
}

/**
 * Render the painter into a freshly allocated [ImageBitmap] sized to [tileSize],
 * applying an optional SrcIn [tint]. Used to bake a tileable bitmap for shader-based
 * background tiling.
 */
private fun Painter.rasterize(
    tileSize: IntSize,
    density: Density,
    layoutDirection: LayoutDirection,
    tint: Color?,
): ImageBitmap {
    val bitmap = ImageBitmap(tileSize.width, tileSize.height)
    val canvas = Canvas(bitmap)
    val sizeF = Size(tileSize.width.toFloat(), tileSize.height.toFloat())
    val filter = tint?.let { ColorFilter.tint(it, BlendMode.SrcIn) }
    CanvasDrawScope().draw(
        density = density,
        layoutDirection = layoutDirection,
        canvas = canvas,
        size = sizeF,
    ) {
        with(this@rasterize) { draw(size = sizeF, colorFilter = filter) }
    }
    return bitmap
}

/**
 * Render an element on the canvas based on its type.
 *
 * Dispatches to specific rendering functions:
 * - [Element.Path]: Draws as a continuous stroke using [DrawScope.drawPath]
 * - [Element.Shape]: Delegates to [drawShape] for type-specific rendering
 *
 * @param element The element to render
 *
 * @see drawShape for shape-specific rendering
 */
private fun DrawScope.renderElement(element: Element) {
    if (element.rotation == 0f) {
        renderElementContent(element)
    } else {
        withTransform({ rotate(element.rotation, pivot = element.bounds().center) }) {
            renderElementContent(element)
        }
    }
}

private fun DrawScope.renderElementContent(element: Element) {
    when (element) {
        is Element.Path -> {
            drawPath(
                createPath(element.points),
                color = element.strokeColor,
                alpha = element.alpha,
                style = Stroke(
                    width = element.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
        is Element.Shape -> {
            drawShape(element)
        }
    }
}

/**
 * Render a geometric shape on the canvas.
 *
 * Dispatches to type-specific rendering based on [Element.Shape.shapeType].
 * All shapes are defined by two points (start and end) and render within
 * the bounding box formed by these points.
 *
 * **Bounding Box Calculation:**
 * - topLeft: minimum (x, y) of start and end points
 * - width: absolute difference in x coordinates
 * - height: absolute difference in y coordinates
 *
 * **Fill vs Stroke:**
 * - If [Element.Shape.fillColor] is set: shape is filled
 * - If null: shape is stroked with [Element.Shape.strokeColor]
 *
 * @param shape The shape element to render
 *
 * @see DrawScope.drawRect for rectangle rendering
 * @see DrawScope.drawCircle for circle rendering
 * @see drawTriangle for triangle rendering
 * @see drawArrowShape for arrow rendering
 */
private fun DrawScope.drawShape(shape: Element.Shape) {
    if (shape.points.size < 2) return

    val start = shape.points[0]
    val end = shape.points.last()
    val width = (end.x - start.x).absoluteValue
    val height = (end.y - start.y).absoluteValue
    val topLeft = Offset(
        minOf(start.x, end.x),
        minOf(start.y, end.y)
    )

    when (shape.shapeType) {
        ShapeType.RECTANGLE -> {
            drawRect(
                color = shape.fillColor ?: shape.strokeColor,
                topLeft = topLeft,
                size = Size(width, height),
                style = if (shape.fillColor != null) Fill else Stroke(shape.strokeWidth)
            )
        }
        ShapeType.CIRCLE -> {
            val center = start + Offset(
                (end.x - start.x) / 2,
                (end.y - start.y) / 2
            )
            val distance = sqrt((end.x - start.x) * (end.x - start.x) + (end.y - start.y) * (end.y - start.y))
            val radius = distance / 2
            drawCircle(
                color = shape.fillColor ?: shape.strokeColor,
                radius = radius,
                center = center,
                style = if (shape.fillColor != null) Fill else Stroke(shape.strokeWidth)
            )
        }
        ShapeType.TRIANGLE -> {
            drawTriangle(topLeft, width, height, shape)
        }
        ShapeType.ARROW -> {
            drawArrowShape(start, end, shape.strokeColor, shape.strokeWidth)
        }
        ShapeType.LINE -> {
            drawLine(
                color = shape.strokeColor,
                start = start,
                end = end,
                strokeWidth = shape.strokeWidth
            )
        }
    }
}

/**
 * Render an isosceles triangle shape.
 *
 * The triangle is drawn with the apex at the top center, and the base at the bottom.
 * The shape is filled if [Element.Shape.fillColor] is set, otherwise stroked.
 *
 * **Geometry:**
 * - Top apex: (topLeft.x + width/2, topLeft.y)
 * - Bottom right: (topLeft.x + width, topLeft.y + height)
 * - Bottom left: (topLeft.x, topLeft.y + height)
 *
 * @param topLeft Top-left corner of the bounding box
 * @param width Width of the bounding box
 * @param height Height of the bounding box
 * @param shape Shape element containing color and stroke information
 */
private fun DrawScope.drawTriangle(
    topLeft: Offset,
    width: Float,
    height: Float,
    shape: Element.Shape,
) {
    val path = Path().apply {
        moveTo(topLeft.x + width / 2, topLeft.y)
        lineTo(topLeft.x + width, topLeft.y + height)
        lineTo(topLeft.x, topLeft.y + height)
        close()
    }
    drawPath(
        path,
        color = shape.fillColor ?: shape.strokeColor,
        style = if (shape.fillColor != null) Fill else Stroke(shape.strokeWidth)
    )
}

/**
 * Render an arrow shape with intelligent head sizing.
 *
 * The arrow consists of:
 * 1. **Line**: From start to lineEnd (calculated to not overlap arrow head)
 * 2. **Arrowhead**: Equilateral triangle at the end point, filled and stroked
 *
 * **Features:**
 * - Arrow head scales with stroke width: `arrowSize = max(30f, strokeWidth * 3f)`
 * - Line length reduced by arrow depth: `arrowDepth = arrowSize * cos(π/6)`
 * - Arrowhead has both fill and stroke for visibility at any zoom level
 * - Rotates to match line direction using angle calculation
 *
 * **Geometry:**
 * ```
 *         /\  ← arrowhead (filled + stroked)
 *        /  \
 *       /____\
 *       |    |  ← line (stroked, length reduced by arrowDepth)
 *       |____|
 *      start
 * ```
 *
 * @param start Starting point of the arrow line
 * @param end Ending point (arrowhead apex)
 * @param color Color for line and arrowhead
 * @param strokeWidth Width of the line stroke (arrowhead scales with this)
 */
private fun DrawScope.drawArrowShape(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float,
) {
    val angle = atan2(end.y - start.y, end.x - start.x)
    val arrowSize = maxOf(30f, strokeWidth * 3f)
    val arrowDepth = arrowSize * cos(PI / 6).toFloat()

    val dx = end.x - start.x
    val dy = end.y - start.y
    val distance = sqrt(dx * dx + dy * dy)
    val lineEnd = if (distance > 0) {
        Offset(
            end.x - (dx / distance) * arrowDepth,
            end.y - (dy / distance) * arrowDepth
        )
    } else {
        end
    }

    drawLine(
        color = color,
        start = start,
        end = lineEnd,
        strokeWidth = strokeWidth
    )

    val arrowPoint1 = Offset(
        end.x - arrowSize * cos(angle - PI / 6).toFloat(),
        end.y - arrowSize * sin(angle - PI / 6).toFloat()
    )
    val arrowPoint2 = Offset(
        end.x - arrowSize * cos(angle + PI / 6).toFloat(),
        end.y - arrowSize * sin(angle + PI / 6).toFloat()
    )

    val arrowPath = Path().apply {
        moveTo(end.x, end.y)
        lineTo(arrowPoint1.x, arrowPoint1.y)
        lineTo(arrowPoint2.x, arrowPoint2.y)
        close()
    }
    drawPath(arrowPath, color = color, style = Fill)
    drawPath(
        arrowPath,
        color = color,
        style = Stroke(width = maxOf(1f, strokeWidth * 0.5f), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}
