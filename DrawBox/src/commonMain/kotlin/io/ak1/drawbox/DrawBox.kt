package io.ak1.drawbox

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.geometry.CornerRadius
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
import androidx.compose.ui.graphics.luminance
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
import io.ak1.drawbox.domain.model.StrokeStyle
import io.ak1.drawbox.domain.model.Viewport
import io.ak1.drawbox.domain.model.angleFromCenter
import io.ak1.drawbox.domain.model.bezierMidpoint
import io.ak1.drawbox.domain.model.bounds
import io.ak1.drawbox.domain.model.controlPoint
import io.ak1.drawbox.domain.model.distance
import io.ak1.drawbox.domain.model.effectiveMode
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
    showGrid: Boolean = true,
) {
    // Two-layer split:
    //   - finalizedLayer: cached display list of "static" elements (everything not
    //     currently being mutated). Re-recorded only when the static set OR the
    //     viewport changes — so a 500-element scene replays for free during an
    //     active drag, since nothing changes outside the one element being drawn.
    //   - captureLayer: one-shot recording used only when bitmap export is
    //     requested. Records the full scene at world-space, dispatches the
    //     resulting ImageBitmap, then sits idle. Keeps the per-frame fast path
    //     free of capture concerns.
    val finalizedLayer = rememberGraphicsLayer()
    val captureLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    // Survives recompositions; rebuilt only when an Element.Path's points list
    // reference actually changes, so finalized strokes don't allocate a new
    // Path every frame during pan/zoom or active drag.
    val pathCache = remember { PathCache() }

    // Drag-in-progress signal lifted out of the gesture coroutine so the render
    // path can tell when an element is being actively mutated (drawn, moved,
    // resized, rotated, line-edited). Marquee and pan drags do NOT flip this,
    // since they don't change any element's geometry.
    var dragInProgress by remember { mutableStateOf(false) }

    // Snapshot of (viewport, static element refs) at the last finalizedLayer
    // record. We use REFERENCE equality on element entries — the reducer always
    // produces new instances via `copy(...)` on mutation, so any change to a
    // static element is detected without scanning fields.
    var lastRecordedVp by remember { mutableStateOf<Viewport?>(null) }
    var lastRecordedStaticRefs by remember { mutableStateOf<List<Element>>(emptyList()) }

    // Bitmap capture is request/serve: the controller's invokeBitmap call sets
    // this; the next draw pass records the full scene into captureLayer and
    // dispatches Intent.SaveBitmap. Avoids re-recording every frame "just in
    // case" capture is invoked.
    var capturePending by remember { mutableStateOf(false) }

    LaunchedEffect(state.hashCode()) {
        state.invokeBitmap = {
            // invokeBitmap may be called off-main; bounce through scope so the
            // State write happens on the Compose-friendly dispatcher.
            scope.launch { capturePending = true }
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

    val handleHitPx = with(density) { 16.dp.toPx() }
    val rotationOffsetPx = with(density) { 28.dp.toPx() }
    val handleSizePx = with(density) { 8.dp.toPx() }
    val pickTolerancePx = with(density) { 12.dp.toPx() }

    // The pointerInput coroutines are long-lived; reading state/onIntent through
    // rememberUpdatedState lets gesture callbacks see the current value without
    // re-keying (and re-allocating) the pointerInput block on every state change.
    val latestState by rememberUpdatedState(state)
    val latestOnIntent by rememberUpdatedState(onIntent)

    // Keyboard focus for the space-bar temp-pan. On platforms without hardware
    // keyboards (mobile) this is harmless dead weight.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    // Cursor: hand in PAN (incl. temp-pan via Space), crosshair for drawing
    // modes, default otherwise. Picked up by Compose Desktop / Web / IDE
    // preview; no-op on mobile.
    val cursor = when (state.effectiveMode) {
        Mode.PAN -> PointerIcon.Hand
        Mode.SELECT -> PointerIcon.Default
        else -> PointerIcon.Crosshair
    }

    Box(
        modifier = modifier
            .pointerHoverIcon(cursor)
            // Keyboard: Space → SetTempPan(true/false). Routed before pointer
            // handlers so it always wins focus.
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.key == Key.Spacebar) {
                    when (event.type) {
                        KeyEventType.KeyDown -> {
                            if (!latestState.tempPanActive) {
                                latestOnIntent(Intent.SetTempPan(true))
                            }
                            true
                        }
                        KeyEventType.KeyUp -> {
                            if (latestState.tempPanActive) {
                                latestOnIntent(Intent.SetTempPan(false))
                            }
                            true
                        }
                        else -> false
                    }
                } else false
            }
            // Layering: solid bgColor → tiled bgPattern (panned with viewport) →
            // grid (also session-only chrome) → graphicsLayer of strokes/shapes.
            //
            // Grid lives in .drawBehind so it is NOT recorded into graphicsLayer,
            // which means saved bitmaps don't include it. SVG export iterates
            // state.elements directly, so it's already grid-free.
            .background(state.bgColor)
            .drawBehind {
                val vp = state.viewport
                patternBrush?.let { brush ->
                    withTransform({
                        translate(vp.offset.x, vp.offset.y)
                        scale(vp.scale, vp.scale, pivot = Offset.Zero)
                    }) {
                        // Cover the visible viewport in world coordinates.
                        val worldTL = vp.screenToWorld(Offset.Zero)
                        val worldBR = vp.screenToWorld(Offset(size.width, size.height))
                        drawRect(
                            brush = brush,
                            topLeft = worldTL,
                            size = Size(worldBR.x - worldTL.x, worldBR.y - worldTL.y),
                        )
                    }
                }
                if (showGrid) drawGrid(vp, state.bgColor)
            }
            // Re-acquire keyboard focus on every press. Without this, clicking a
            // Material button (mode menu, zoom toolbar, etc.) steals focus and
            // Space/key handlers stop firing on the canvas until the user
            // explicitly refocuses it.
            .pointerInput(focusRequester) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) {
                            runCatching { focusRequester.requestFocus() }
                        }
                    }
                }
            }
            // Middle-mouse-button drag → pan in any mode. Reliable desktop UX
            // that doesn't depend on Compose Desktop's horizontal-scroll bridge
            // (which is inconsistent on macOS trackpad / AWT MouseWheelEvent).
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var panning = false
                    var lastPos = Offset.Zero
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.buttons.isTertiaryPressed
                        val pos = event.changes.firstOrNull()?.position ?: continue
                        when {
                            pressed && !panning -> {
                                panning = true
                                lastPos = pos
                                event.changes.forEach { it.consume() }
                            }
                            pressed && panning -> {
                                val delta = pos - lastPos
                                if (delta.x != 0f || delta.y != 0f) {
                                    latestOnIntent(Intent.PanBy(delta))
                                    lastPos = pos
                                }
                                event.changes.forEach { it.consume() }
                            }
                            !pressed && panning -> {
                                panning = false
                            }
                        }
                    }
                }
            }
            // Multi-touch: pinch-zoom (focal-preserving) + two-finger pan.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    var prevDistance = 0f
                    var prevCentroid = Offset.Zero
                    var multi = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.size >= 2) {
                            val p1 = pressed[0].position
                            val p2 = pressed[1].position
                            val d = (p1 - p2).getDistance()
                            val centroid = Offset((p1.x + p2.x) * 0.5f, (p1.y + p2.y) * 0.5f)
                            if (!multi) {
                                multi = true
                                prevDistance = d
                                prevCentroid = centroid
                                // Cancel any in-progress marquee on the other handler.
                                if (latestState.marqueeRect != null) {
                                    latestOnIntent(Intent.SetMarqueeRect(null))
                                }
                            } else {
                                if (prevDistance > 1f && d > 1f) {
                                    latestOnIntent(Intent.ZoomBy(d / prevDistance, centroid))
                                }
                                latestOnIntent(Intent.PanBy(centroid - prevCentroid))
                                prevDistance = d
                                prevCentroid = centroid
                            }
                            event.changes.forEach { it.consume() }
                        } else if (multi) {
                            multi = false
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
            // Scroll wheel + trackpad: Ctrl=zoom, Shift=force horizontal (for
            // mice without a horizontal wheel), default=2D pan using whichever
            // axes the device supplies. Trackpad two-finger scroll sends both
            // delta.x and delta.y; classic mouse wheels send only delta.y.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type != PointerEventType.Scroll) continue
                        val change = event.changes.firstOrNull() ?: continue
                        val delta = change.scrollDelta
                        when {
                            event.keyboardModifiers.isCtrlPressed -> {
                                if (delta.y != 0f) {
                                    val factor = if (delta.y < 0) 1.1f else 1f / 1.1f
                                    latestOnIntent(Intent.ZoomBy(factor, change.position))
                                }
                            }
                            event.keyboardModifiers.isShiftPressed -> {
                                // Map vertical wheel to horizontal pan for users
                                // whose mouse has no horizontal axis.
                                latestOnIntent(Intent.PanBy(Offset(-delta.y * 50f, 0f)))
                            }
                            else -> {
                                latestOnIntent(Intent.PanBy(Offset(-delta.x * 50f, -delta.y * 50f)))
                            }
                        }
                        change.consume()
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { screenPos ->
                        val s = latestState
                        val world = s.viewport.screenToWorld(screenPos)
                        val tol = pickTolerancePx / s.viewport.scale
                        when (s.effectiveMode) {
                            Mode.SELECT -> latestOnIntent(Intent.SelectAt(world, tol))
                            Mode.PEN -> {
                                latestOnIntent(Intent.InsertNewPath(world))
                                latestOnIntent(Intent.UpdateLatestPath(world))
                            }
                            Mode.PAN -> {} // taps in pan mode are no-ops
                            else -> modeShapeType(s.effectiveMode)?.let { st ->
                                latestOnIntent(Intent.InsertNewShape(st, world))
                                latestOnIntent(Intent.UpdateLatestShape(world))
                            }
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                var interaction: SelectionInteraction? = null
                var lastWorld = Offset.Zero

                detectDragGestures(
                    onDragStart = { screenPos ->
                        val s = latestState
                        val world = s.viewport.screenToWorld(screenPos)
                        lastWorld = world
                        when (s.effectiveMode) {
                            Mode.PAN -> {
                                interaction = SelectionInteraction.Pan
                            }
                            Mode.SELECT -> {
                                val classified = classifySelection(
                                    state = s,
                                    pointerWorld = world,
                                    handleHitWorld = handleHitPx / s.viewport.scale,
                                    rotationOffsetWorld = rotationOffsetPx / s.viewport.scale,
                                    pickToleranceWorld = pickTolerancePx / s.viewport.scale,
                                )
                                interaction = when (classified) {
                                    is SelectionInteraction.SelectAndMove -> {
                                        latestOnIntent(Intent.SelectAt(world, pickTolerancePx / s.viewport.scale))
                                        latestOnIntent(Intent.BeginTransform)
                                        dragInProgress = true
                                        SelectionInteraction.Move
                                    }
                                    is SelectionInteraction.Move,
                                    is SelectionInteraction.Resize,
                                    is SelectionInteraction.Rotate,
                                    is SelectionInteraction.LineEndpoint,
                                    is SelectionInteraction.LineBend -> {
                                        latestOnIntent(Intent.BeginTransform)
                                        dragInProgress = true
                                        classified
                                    }
                                    is SelectionInteraction.Marquee -> {
                                        // Marquee doesn't mutate any element —
                                        // keep dragInProgress=false so the cached
                                        // layer keeps replaying for free.
                                        latestOnIntent(Intent.SetMarqueeRect(Rect(world, world)))
                                        classified
                                    }
                                    SelectionInteraction.Pan -> classified
                                }
                            }
                            Mode.PEN -> {
                                latestOnIntent(Intent.InsertNewPath(world))
                                dragInProgress = true
                            }
                            else -> modeShapeType(s.effectiveMode)?.let { st ->
                                latestOnIntent(Intent.InsertNewShape(st, world))
                                dragInProgress = true
                            }
                        }
                    },
                    onDragEnd = {
                        val s = latestState
                        if (s.mode == Mode.SELECT) {
                            val rect = s.marqueeRect
                            if (interaction is SelectionInteraction.Marquee && rect != null) {
                                latestOnIntent(Intent.CommitMarquee(rect))
                            } else if (interaction is SelectionInteraction.Marquee) {
                                latestOnIntent(Intent.SetMarqueeRect(null))
                            }
                            val maybeEndpoint = interaction as? SelectionInteraction.LineEndpoint
                            if (maybeEndpoint != null) {
                                val target = s.elements.firstOrNull { it.id == maybeEndpoint.elementId }
                                if (target is Element.Shape && target.shapeType == ShapeType.ARROW) {
                                    latestOnIntent(Intent.FinalizeArrowBindings(target.id))
                                }
                            }
                        } else if (s.mode == Mode.ARROW) {
                            // Just finished drawing an arrow — bind endpoints sitting
                            // over a shape so the arrow becomes a connector.
                            val justDrawn = s.elements.lastOrNull()
                            if (justDrawn is Element.Shape && justDrawn.shapeType == ShapeType.ARROW) {
                                latestOnIntent(Intent.FinalizeArrowBindings(justDrawn.id))
                            }
                        }
                        latestOnIntent(Intent.EndTransform)
                        interaction = null
                        dragInProgress = false
                    },
                    onDragCancel = {
                        if (latestState.mode == Mode.SELECT &&
                            interaction is SelectionInteraction.Marquee
                        ) {
                            latestOnIntent(Intent.SetMarqueeRect(null))
                        }
                        latestOnIntent(Intent.EndTransform)
                        interaction = null
                        dragInProgress = false
                    },
                ) { change, dragAmount ->
                    val s = latestState
                    val world = s.viewport.screenToWorld(change.position)
                    when (s.effectiveMode) {
                        Mode.PAN -> latestOnIntent(Intent.PanBy(dragAmount))
                        Mode.SELECT -> when (val i = interaction) {
                            is SelectionInteraction.Move -> {
                                latestOnIntent(Intent.MoveSelected(world - lastWorld))
                                lastWorld = world
                            }
                            is SelectionInteraction.Resize -> {
                                val newBounds = resizeBoundsForElement(
                                    i.originalElement, i.handle, world,
                                )
                                latestOnIntent(Intent.SetElementBounds(i.elementId, newBounds))
                            }
                            is SelectionInteraction.Rotate -> {
                                val current = angleFromCenter(i.center, world)
                                val delta = current - i.initialAngle
                                latestOnIntent(Intent.SetElementRotation(
                                    i.elementId, i.originalRotation + delta,
                                ))
                            }
                            is SelectionInteraction.LineEndpoint -> {
                                val target = s.elements.firstOrNull { it.id == i.elementId }
                                if (target is Element.Shape && target.points.size >= 2) {
                                    val newPoints = if (i.isStart) {
                                        listOf(world, target.points.last())
                                    } else {
                                        listOf(target.points[0], world)
                                    }
                                    latestOnIntent(Intent.SetElementPoints(i.elementId, newPoints))
                                }
                            }
                            is SelectionInteraction.LineBend -> {
                                val target = s.elements.firstOrNull { it.id == i.elementId }
                                if (target is Element.Shape && target.points.size >= 2) {
                                    val start = target.points[0]
                                    val end = target.points.last()
                                    // bend such that bezierMidpoint = world:
                                    //   bezierMidpoint = midpoint + bend/2 → bend = 2 * (world − midpoint)
                                    val midX = (start.x + end.x) * 0.5f
                                    val midY = (start.y + end.y) * 0.5f
                                    val newBend = Offset(
                                        2f * (world.x - midX),
                                        2f * (world.y - midY),
                                    )
                                    latestOnIntent(Intent.SetLineBend(i.elementId, newBend))
                                }
                            }
                            is SelectionInteraction.Marquee -> {
                                latestOnIntent(Intent.SetMarqueeRect(Rect(i.anchor, world)))
                            }
                            SelectionInteraction.Pan -> latestOnIntent(Intent.PanBy(dragAmount))
                            else -> {}
                        }
                        Mode.PEN -> latestOnIntent(Intent.UpdateLatestPath(world))
                        else -> if (modeShapeType(s.effectiveMode) != null) {
                            latestOnIntent(Intent.UpdateLatestShape(world))
                        }
                    }
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Grid stays in the parent's .drawBehind so it never enters
            // finalizedLayer — that keeps it out of bitmap export and out of the
            // cached rendering, while still painting on every frame.
            val vp = state.viewport
            val orderedElements = state.elements.sortedByZIndexIfNeeded()

            // Active id set: which elements does the current gesture mutate?
            // - PEN / shape modes: the just-inserted element (always elements.last())
            // - SELECT (Move/Resize/Rotate/LineEndpoint/LineBend): state.selectedIds
            // - Marquee / Pan / no drag: empty
            val activeIds: Set<String> = if (!dragInProgress) {
                emptySet()
            } else {
                when (state.effectiveMode) {
                    Mode.SELECT -> state.selectedIds
                    Mode.PAN -> emptySet()
                    Mode.PEN, Mode.RECTANGLE, Mode.CIRCLE, Mode.TRIANGLE, Mode.ARROW, Mode.LINE -> {
                        val lastId = orderedElements.lastOrNull()?.id
                        if (lastId == null) emptySet() else setOf(lastId)
                    }
                }
            }

            // Freshness check on the cached layer. Walks orderedElements once,
            // comparing non-active entries against lastRecordedStaticRefs by
            // reference (allocation-free).
            val cacheFresh = lastRecordedVp == vp &&
                staticRefsMatch(orderedElements, activeIds, lastRecordedStaticRefs)

            if (!cacheFresh) {
                // Re-record at screen-space (transform baked in). This means
                // pan/zoom invalidates the cache — explicitly accepted trade-off:
                // the win is during active drawing, where the viewport is stable
                // but elements churn. Pan/zoom optimization is a follow-up.
                finalizedLayer.record {
                    withTransform({
                        translate(vp.offset.x, vp.offset.y)
                        scale(vp.scale, vp.scale, pivot = Offset.Zero)
                    }) {
                        orderedElements.forEach { el ->
                            if (el.id !in activeIds) renderElement(el, pathCache)
                        }
                    }
                }
                lastRecordedVp = vp
                lastRecordedStaticRefs = orderedElements.filter { it.id !in activeIds }
            }

            // Play the cached static layer. No transform — it was baked in at
            // record time and the layer replays in screen-space.
            drawLayer(finalizedLayer)

            // Draw the active element(s) + selection chrome live, through the
            // same world→screen transform.
            withTransform({
                translate(vp.offset.x, vp.offset.y)
                scale(vp.scale, vp.scale, pivot = Offset.Zero)
            }) {
                if (activeIds.isNotEmpty()) {
                    orderedElements.forEach { el ->
                        if (el.id in activeIds) renderElement(el, pathCache)
                    }
                }
                drawSelectionChrome(
                    state = state,
                    handleSizePx = handleSizePx,
                    rotationOffsetPx = rotationOffsetPx,
                    inverseScale = 1f / vp.scale,
                )
            }

            // On-demand bitmap capture: record the full scene into captureLayer,
            // then dispatch SaveBitmap. The captureLayer is otherwise untouched,
            // so the per-frame fast path stays clean of capture concerns.
            if (capturePending) {
                captureLayer.record {
                    withTransform({
                        translate(vp.offset.x, vp.offset.y)
                        scale(vp.scale, vp.scale, pivot = Offset.Zero)
                    }) {
                        orderedElements.forEach { renderElement(it, pathCache) }
                    }
                }
                capturePending = false
                scope.launch {
                    try {
                        onIntent(Intent.SaveBitmap(captureLayer.toImageBitmap(), null))
                    } catch (e: Throwable) {
                        onIntent(Intent.SaveBitmap(null, e))
                    }
                }
            }

            // Reclaim PathCache entries for deleted elements. Cheap (HashMap
            // key-retain over the live id set); only worth doing when something
            // was actually removed.
            if (pathCache.size() > state.elements.size) {
                pathCache.retainOnly(state.elements.mapTo(HashSet(state.elements.size)) { it.id })
            }
        }
    }
}

/**
 * Render-only preview of a set of [Element]s. No gestures, no chrome, no grid.
 * Useful for replay scrubbing, thumbnails, and read-only embeds.
 *
 * Elements are drawn in [Element.zIndex] order, projected through [viewport]
 * the same way [DrawBox] does for its main canvas.
 */
@Composable
fun DrawingPreview(
    elements: List<Element>,
    bgColor: Color,
    modifier: Modifier = Modifier.fillMaxSize(),
    viewport: Viewport = Viewport(),
) {
    Canvas(modifier = modifier) {
        drawRect(color = bgColor)
        withTransform({
            translate(viewport.offset.x, viewport.offset.y)
            scale(viewport.scale, viewport.scale, pivot = Offset.Zero)
        }) {
            elements
                .sortedBy { it.zIndex }
                .forEach { renderElement(it) }
        }
    }
}

private val GridLightOnDark = Color(0x40FFFFFF)  // ~25% white over dark bgs
private val GridDarkOnLight = Color(0x33000000)  // ~20% black over light bgs
private const val GRID_BASE_STEP_WORLD = 50f

private fun DrawScope.drawGrid(vp: Viewport, bgColor: Color) {
    val step = GRID_BASE_STEP_WORLD * vp.scale
    if (step < 6f) return // collapse to avoid moiré at extreme zoom-out
    // Auto-contrast: light grid on dark bg, dark grid on light bg.
    val color = if (bgColor.luminance() < 0.5f) GridLightOnDark else GridDarkOnLight
    val offX = vp.offset.x.mod(step)
    val offY = vp.offset.y.mod(step)
    var x = offX
    while (x < size.width) {
        drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += step
    }
    var y = offY
    while (y < size.height) {
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += step
    }
}

private fun modeShapeType(mode: Mode): ShapeType? = when (mode) {
    Mode.RECTANGLE -> ShapeType.RECTANGLE
    Mode.CIRCLE -> ShapeType.CIRCLE
    Mode.TRIANGLE -> ShapeType.TRIANGLE
    Mode.ARROW -> ShapeType.ARROW
    Mode.LINE -> ShapeType.LINE
    Mode.PEN, Mode.SELECT, Mode.PAN -> null
}

// ==================== Selection gesture support ====================

/**
 * Active interaction inferred from a drag-start. SELECT-mode drags resolve to
 * one of [Move] / [SelectAndMove] / [Resize] / [Rotate] / [LineEndpoint] /
 * [LineBend] / [Marquee]; PAN-mode drags resolve to [Pan]. Stored coordinates
 * are all in world space.
 */
private sealed class SelectionInteraction {
    data object Move : SelectionInteraction()
    data object SelectAndMove : SelectionInteraction()
    data object Pan : SelectionInteraction()
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
    /** Dragging the start or end endpoint of a [ShapeType.LINE]/[ShapeType.ARROW]. */
    data class LineEndpoint(
        val elementId: String,
        val isStart: Boolean,
    ) : SelectionInteraction()
    /** Dragging the bend handle (curve midpoint) of a [ShapeType.LINE]/[ShapeType.ARROW]. */
    data class LineBend(
        val elementId: String,
    ) : SelectionInteraction()
    data class Marquee(val anchor: Offset) : SelectionInteraction()
}

private fun ShapeType.isLineLike(): Boolean = this == ShapeType.LINE || this == ShapeType.ARROW

private fun classifySelection(
    state: State,
    pointerWorld: Offset,
    handleHitWorld: Float,
    rotationOffsetWorld: Float,
    pickToleranceWorld: Float,
): SelectionInteraction {
    // Single-selection: check shape-specific handles first.
    if (state.selectedIds.size == 1) {
        val element = state.elements.firstOrNull { it.id in state.selectedIds }
        if (element != null) {
            // LINE / ARROW use a 3-dot chrome: start, end, bend midpoint.
            if (element is Element.Shape && element.shapeType.isLineLike() &&
                element.points.size >= 2
            ) {
                val start = element.points[0]
                val end = element.points.last()
                if (distance(pointerWorld, start) <= handleHitWorld) {
                    return SelectionInteraction.LineEndpoint(element.id, isStart = true)
                }
                if (distance(pointerWorld, end) <= handleHitWorld) {
                    return SelectionInteraction.LineEndpoint(element.id, isStart = false)
                }
                if (distance(pointerWorld, element.bezierMidpoint()) <= handleHitWorld) {
                    return SelectionInteraction.LineBend(element.id)
                }
                // Fall through: drag on the line body = move existing selection.
            } else {
                val b = element.bounds()
                val rotHandle = rotationHandleWorld(b, element.rotation, rotationOffsetWorld)
                if (distance(pointerWorld, rotHandle) <= handleHitWorld) {
                    return SelectionInteraction.Rotate(
                        elementId = element.id,
                        center = b.center,
                        initialAngle = angleFromCenter(b.center, pointerWorld),
                        originalRotation = element.rotation,
                    )
                }
                for ((h, p) in resizeHandlesWorld(b, element.rotation)) {
                    if (distance(pointerWorld, p) <= handleHitWorld) {
                        return SelectionInteraction.Resize(
                            elementId = element.id,
                            handle = h,
                            originalElement = element,
                        )
                    }
                }
            }
        }
    }
    // Drag inside already-selected → move existing selection.
    if (state.elements.any { it.id in state.selectedIds && it.hitTest(pointerWorld, pickToleranceWorld) }) {
        return SelectionInteraction.Move
    }
    // Drag on unselected element → select-then-move.
    val any = topmostHit(state.elements, pointerWorld, pickToleranceWorld)
    if (any != null) return SelectionInteraction.SelectAndMove
    // Empty space → marquee.
    return SelectionInteraction.Marquee(pointerWorld)
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
    inverseScale: Float,
) {
    val rotationOffsetWorld = rotationOffsetPx * inverseScale
    state.elements
        .asSequence()
        .filter { it.id in state.selectedIds }
        .forEach { el ->
            // LINE / ARROW get a minimal 3-dot chrome: start, end, and bend midpoint.
            if (el is Element.Shape && el.shapeType.isLineLike() && el.points.size >= 2) {
                drawLineSelectionChrome(el, handleSizePx, inverseScale)
                return@forEach
            }
            val b = el.bounds()
            if (el.rotation == 0f) {
                drawSelectionForElement(b, handleSizePx, rotationOffsetWorld, inverseScale)
            } else {
                withTransform({ rotate(el.rotation, pivot = b.center) }) {
                    drawSelectionForElement(b, handleSizePx, rotationOffsetWorld, inverseScale)
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
                width = 1.5f * inverseScale,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(8f * inverseScale, 6f * inverseScale),
                ),
            ),
        )
    }
}

private fun DrawScope.drawLineSelectionChrome(
    shape: Element.Shape,
    handleSizePx: Float,
    inverseScale: Float,
) {
    val start = shape.points[0]
    val end = shape.points.last()
    val mid = shape.bezierMidpoint()
    val dotRadius = handleSizePx * 0.55f * inverseScale
    val strokeWorld = 1.5f * inverseScale
    listOf(start, end, mid).forEach { p ->
        drawCircle(color = Color.White, radius = dotRadius, center = p)
        drawCircle(
            color = SelectionAccent,
            radius = dotRadius,
            center = p,
            style = Stroke(strokeWorld),
        )
    }
}

private fun DrawScope.drawSelectionForElement(
    bounds: Rect,
    handleSizePx: Float,
    rotationOffsetWorld: Float,
    inverseScale: Float,
) {
    // Bounding box.
    drawRect(
        color = SelectionAccent,
        topLeft = bounds.topLeft,
        size = Size(bounds.width, bounds.height),
        style = Stroke(width = 1.5f * inverseScale),
    )
    // Rotation handle: short line up + filled circle.
    val rotHandle = rotationHandleLocal(bounds, rotationOffsetWorld)
    val handleWorld = handleSizePx * inverseScale
    val strokeWorld = 1.5f * inverseScale
    drawLine(
        color = SelectionAccent,
        start = Offset(bounds.center.x, bounds.top),
        end = rotHandle,
        strokeWidth = strokeWorld,
    )
    drawCircle(
        color = Color.White,
        radius = handleWorld * 0.6f,
        center = rotHandle,
    )
    drawCircle(
        color = SelectionAccent,
        radius = handleWorld * 0.6f,
        center = rotHandle,
        style = Stroke(strokeWorld),
    )
    // Resize handles.
    val half = handleWorld * 0.5f
    resizeHandlesLocal(bounds).forEach { (_, p) ->
        drawRect(
            color = Color.White,
            topLeft = Offset(p.x - half, p.y - half),
            size = Size(handleWorld, handleWorld),
        )
        drawRect(
            color = SelectionAccent,
            topLeft = Offset(p.x - half, p.y - half),
            size = Size(handleWorld, handleWorld),
            style = Stroke(strokeWorld),
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
private fun DrawScope.renderElement(element: Element, pathCache: PathCache? = null) {
    if (element.rotation == 0f) {
        renderElementContent(element, pathCache)
    } else {
        withTransform({ rotate(element.rotation, pivot = element.bounds().center) }) {
            renderElementContent(element, pathCache)
        }
    }
}

private fun DrawScope.renderElementContent(element: Element, pathCache: PathCache?) {
    when (element) {
        is Element.Path -> {
            val path = pathCache?.pathFor(element.id, element.points) ?: createPath(element.points)
            drawPath(
                path,
                color = element.strokeColor,
                alpha = element.alpha,
                style = Stroke(
                    width = element.strokeWidth,
                    // Round caps always for paths — keeps pencil-like ends for
                    // solid strokes and makes individual dots visible for dotted.
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = element.strokeStyle.toPathEffect(element.strokeWidth),
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
            val r = shape.cornerRadius.coerceAtMost(minOf(width, height) * 0.5f)
            if (r > 0f) {
                drawRoundRect(
                    color = shape.fillColor ?: shape.strokeColor,
                    topLeft = topLeft,
                    size = Size(width, height),
                    cornerRadius = CornerRadius(r, r),
                    style = shape.drawStyle(),
                )
            } else {
                drawRect(
                    color = shape.fillColor ?: shape.strokeColor,
                    topLeft = topLeft,
                    size = Size(width, height),
                    style = shape.drawStyle(),
                )
            }
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
                style = shape.drawStyle(),
            )
        }
        ShapeType.TRIANGLE -> {
            drawTriangle(topLeft, width, height, shape)
        }
        ShapeType.ARROW -> {
            drawArrowShape(shape)
        }
        ShapeType.LINE -> {
            if (shape.bend == Offset.Zero) {
                drawLine(
                    color = shape.strokeColor,
                    start = start,
                    end = end,
                    strokeWidth = shape.strokeWidth,
                    cap = strokeCapFor(shape.strokeStyle),
                    pathEffect = shape.strokeStyle.toPathEffect(shape.strokeWidth),
                )
            } else {
                val control = shape.controlPoint()
                val path = Path().apply {
                    moveTo(start.x, start.y)
                    quadraticTo(control.x, control.y, end.x, end.y)
                }
                drawPath(
                    path,
                    color = shape.strokeColor,
                    style = Stroke(
                        width = shape.strokeWidth,
                        cap = strokeCapFor(shape.strokeStyle),
                        join = StrokeJoin.Round,
                        pathEffect = shape.strokeStyle.toPathEffect(shape.strokeWidth),
                    ),
                )
            }
        }
    }
}

/** Stroke/fill style for an [Element.Shape] respecting its [StrokeStyle]. */
private fun Element.Shape.drawStyle(): androidx.compose.ui.graphics.drawscope.DrawStyle =
    if (fillColor != null) Fill else Stroke(
        width = strokeWidth,
        cap = strokeCapFor(strokeStyle),
        join = StrokeJoin.Round,
        pathEffect = strokeStyle.toPathEffect(strokeWidth),
    )

private fun strokeCapFor(style: StrokeStyle): StrokeCap = when (style) {
    // Round caps + tiny dashes render as visible dots; for dashed and solid,
    // butt caps give a crisp edge so dashes don't blur into one another.
    StrokeStyle.DOTTED -> StrokeCap.Round
    else -> StrokeCap.Butt
}

private fun StrokeStyle.toPathEffect(strokeWidth: Float): PathEffect? = when (this) {
    StrokeStyle.SOLID -> null
    StrokeStyle.DASHED -> PathEffect.dashPathEffect(
        floatArrayOf(strokeWidth * 4f, strokeWidth * 2f),
    )
    StrokeStyle.DOTTED -> PathEffect.dashPathEffect(
        floatArrayOf(strokeWidth * 0.5f, strokeWidth * 2f),
    )
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
    val apex = Offset(topLeft.x + width / 2, topLeft.y)
    val br = Offset(topLeft.x + width, topLeft.y + height)
    val bl = Offset(topLeft.x, topLeft.y + height)
    val path = if (shape.cornerRadius > 0f) {
        roundedTrianglePath(apex, br, bl, shape.cornerRadius)
    } else {
        Path().apply {
            moveTo(apex.x, apex.y)
            lineTo(br.x, br.y)
            lineTo(bl.x, bl.y)
            close()
        }
    }
    drawPath(
        path,
        color = shape.fillColor ?: shape.strokeColor,
        style = shape.drawStyle(),
    )
}

/**
 * Build a triangle path with rounded corners by replacing each sharp vertex
 * with a quadratic-bezier arc. The radius is clamped to half the shortest edge
 * so adjacent tangent points never cross.
 */
private fun roundedTrianglePath(v0: Offset, v1: Offset, v2: Offset, radius: Float): Path {
    val verts = listOf(v0, v1, v2)
    val edges = listOf(
        distance(v0, v1),
        distance(v1, v2),
        distance(v2, v0),
    )
    val r = radius.coerceAtMost(edges.min() * 0.5f)
    if (r <= 0f) {
        return Path().apply {
            moveTo(v0.x, v0.y); lineTo(v1.x, v1.y); lineTo(v2.x, v2.y); close()
        }
    }
    // For each vertex i: t1 lies on edge (i-1 → i) near i; t2 lies on edge
    // (i → i+1) near i. The quadratic from t1 to t2 with the vertex as the
    // control point approximates a circular arc.
    val t1 = Array(3) { Offset.Zero }
    val t2 = Array(3) { Offset.Zero }
    for (i in 0..2) {
        val prev = verts[(i + 2) % 3]
        val curr = verts[i]
        val next = verts[(i + 1) % 3]
        val toPrev = normalizeOffset(prev - curr)
        val toNext = normalizeOffset(next - curr)
        t1[i] = curr + toPrev * r
        t2[i] = curr + toNext * r
    }
    return Path().apply {
        moveTo(t2[0].x, t2[0].y)
        for (i in 0..2) {
            val ni = (i + 1) % 3
            lineTo(t1[ni].x, t1[ni].y)
            quadraticTo(verts[ni].x, verts[ni].y, t2[ni].x, t2[ni].y)
        }
        close()
    }
}

private fun normalizeOffset(v: Offset): Offset {
    val len = sqrt(v.x * v.x + v.y * v.y)
    return if (len > 0f) Offset(v.x / len, v.y / len) else Offset.Zero
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
private fun DrawScope.drawArrowShape(shape: Element.Shape) {
    val start = shape.points[0]
    val end = shape.points.last()
    val color = shape.strokeColor
    val strokeWidth = shape.strokeWidth
    val arrowSize = maxOf(30f, strokeWidth * 3f)
    val arrowDepth = arrowSize * cos(PI / 6).toFloat()

    val angle: Float
    if (shape.bend == Offset.Zero) {
        // Straight arrow — body is shortened so the head sits cleanly at the tip.
        val dx = end.x - start.x
        val dy = end.y - start.y
        angle = atan2(dy, dx)
        val distance = sqrt(dx * dx + dy * dy)
        val lineEnd = if (distance > 0) Offset(
            end.x - (dx / distance) * arrowDepth,
            end.y - (dy / distance) * arrowDepth,
        ) else end
        drawLine(
            color = color,
            start = start,
            end = lineEnd,
            strokeWidth = strokeWidth,
            cap = strokeCapFor(shape.strokeStyle),
            pathEffect = shape.strokeStyle.toPathEffect(strokeWidth),
        )
    } else {
        // Curved arrow — quadratic bezier; head direction comes from the tangent
        // at t = 1, i.e. 2 * (end - control), simplified to (end - control).
        val control = shape.controlPoint()
        val tx = end.x - control.x
        val ty = end.y - control.y
        angle = atan2(ty, tx)
        val path = Path().apply {
            moveTo(start.x, start.y)
            quadraticTo(control.x, control.y, end.x, end.y)
        }
        drawPath(
            path,
            color = color,
            style = Stroke(
                width = strokeWidth,
                cap = strokeCapFor(shape.strokeStyle),
                join = StrokeJoin.Round,
                pathEffect = shape.strokeStyle.toPathEffect(strokeWidth),
            ),
        )
    }

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
