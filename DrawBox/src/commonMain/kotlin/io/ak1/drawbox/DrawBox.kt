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
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.unit.IntOffset
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
import androidx.compose.ui.text.drawText
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
import io.ak1.drawbox.domain.model.positions
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
    /**
     * IDs of elements that should be skipped entirely by the renderer and
     * the selection chrome. Used by the sample app to suppress the
     * underlying text while an inline editor overlays it; without this the
     * editing TextField and the rendered text element would double-paint
     * the same content (and ghost as the user types).
     */
    hiddenElementIds: Set<String> = emptySet(),
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
    // ImageBitmapCache needs a CoroutineScope for off-main-thread decode +
    // race-safe completion. Tied to the composable's scope so cancellation
    // and lifecycle follow the canvas. Mip-level keyed so a small placement
    // doesn't hold a source-resolution bitmap in memory.
    val imageCache = remember(scope) { ImageBitmapCache(scope) }
    // Text layout cache: lay out each text block once per (id, text, style,
    // wrap width); reuse the result on subsequent frames. TextMeasurer is
    // composition-scoped and must come from rememberTextMeasurer.
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val textCache = remember { TextLayoutCache() }

    // Pre-measure every text element at composition time (cache hit when
    // unchanged) and dispatch SyncTextMeasuredHeight when the rendered
    // height drifts from what the model has stored. This is the loop that
    // keeps bounds() / hit-test / selection chrome accurate without putting
    // a TextMeasurer in the data layer.
    //
    // The effect runs after composition completes, so the first frame after
    // an InsertText draws with the initial-guess height. The sync intent
    // fires immediately, the reducer accepts (no snapshot), and the second
    // frame has the correct geometry. The 0.5px drift gate in both the
    // dispatch and the reducer ensures fixed-point convergence in one step.
    LaunchedEffect(state.elements, textMeasurer) {
        state.elements.forEach { el ->
            if (el !is Element.Text) return@forEach
            val layout = textCache.layoutFor(
                id = el.id,
                text = el.text,
                fontFamilyKey = el.fontFamilyKey,
                fontSize = el.fontSize,
                alignment = el.alignment,
                wrapWidth = el.wrapWidth.coerceAtLeast(1f),
                measurer = textMeasurer,
            )
            val measured = layout.size.height.toFloat()
            if (kotlin.math.abs(measured - el.measuredHeight) > 0.5f) {
                onIntent(Intent.SyncTextMeasuredHeight(el.id, measured))
            }
        }
    }

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
    // modes, default otherwise. The eraser draws its own circular overlay so
    // the system cursor is hidden (Default) under it to avoid double-cursoring.
    // Picked up by Compose Desktop / Web / IDE preview; no-op on mobile.
    val cursor = when (state.effectiveMode) {
        Mode.PAN -> PointerIcon.Hand
        Mode.SELECT -> PointerIcon.Default
        Mode.ERASER -> PointerIcon.Default
        else -> PointerIcon.Crosshair
    }

    // Live pointer position in WORLD coords for the eraser cursor overlay.
    // Null when the cursor is outside the canvas or no hover events are
    // arriving (mobile/touch). The overlay collapses gracefully in that case.
    var eraserPointerWorld by remember { mutableStateOf<Offset?>(null) }

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
            // Eraser hover tracking: keep the world-space cursor position fresh
            // for the eraser overlay circle. Only fires when ERASER is the
            // active mode — otherwise every pointer Move during PEN drawing
            // would force a recomposition and tank frame rate. Touch platforms
            // emit no hover events; the overlay collapses gracefully in that
            // case.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (latestState.effectiveMode != Mode.ERASER) continue
                        when (event.type) {
                            PointerEventType.Move,
                            PointerEventType.Enter,
                            PointerEventType.Press,
                            -> {
                                val pos = event.changes.firstOrNull()?.position
                                eraserPointerWorld = pos?.let {
                                    latestState.viewport.screenToWorld(it)
                                }
                            }
                            PointerEventType.Exit -> {
                                eraserPointerWorld = null
                            }
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
                            Mode.ERASER -> {
                                // Single tap = atomic erase session. BeginErase
                                // clears the dirty flag; EraseAt snapshots only
                                // if it actually removes something. A tap on
                                // empty space therefore consumes no undo slot.
                                latestOnIntent(Intent.BeginErase)
                                latestOnIntent(Intent.EraseAt(world, s.eraserSize / s.viewport.scale))
                                latestOnIntent(Intent.EndErase)
                            }
                            Mode.TEXT -> {
                                // Drop an empty Text element at the tap, using
                                // the State defaults the user pre-configured via
                                // the ContextBar before tapping. Matches the
                                // shape-mode pattern where stroke/fill/etc. are
                                // pre-chosen before drawing.
                                latestOnIntent(Intent.InsertText(
                                    text = "",
                                    position = world,
                                    fontSize = s.currentItemFontSize,
                                    fontFamilyKey = s.currentItemFontFamilyKey,
                                    alignment = s.currentItemTextAlignment,
                                    color = s.strokeColor,
                                ))
                            }
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
                            Mode.ERASER -> {
                                // Open the erase gesture: BeginErase clears the
                                // dirty flag so EraseAt can snapshot lazily on
                                // the first actual hit. A drag that never
                                // intersects an element pushes nothing to undo.
                                latestOnIntent(Intent.BeginErase)
                                latestOnIntent(Intent.EraseAt(world, s.eraserSize / s.viewport.scale))
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
                        } else if (s.mode == Mode.ERASER) {
                            latestOnIntent(Intent.EndErase)
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
                        if (latestState.mode == Mode.ERASER) {
                            latestOnIntent(Intent.EndErase)
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
                        Mode.PEN -> latestOnIntent(
                            Intent.UpdateLatestPath(world, change.pressureMultiplier()),
                        )
                        Mode.ERASER -> {
                            // Pressure modulates eraser radius live: light press
                            // shrinks, hard press grows. Floor at strokeWidth's
                            // multiplier (0.2) so a barely-touching pen still
                            // catches something.
                            val r = s.eraserSize * change.pressureMultiplier() / s.viewport.scale
                            latestOnIntent(Intent.EraseAt(world, r))
                        }
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
                    // The eraser removes elements from the static set on each
                    // tick. Surviving elements are unchanged, so leave activeIds
                    // empty and let staticRefsMatch handle the cache rebuild
                    // when the element list shrinks.
                    Mode.ERASER -> emptySet()
                    // Text insertion is single-step (tap → InsertText with an
                    // empty content), so there's no "drag-being-mutated"
                    // element to mark active. The sample app's modal editor
                    // dispatches the eventual UpdateText separately.
                    Mode.TEXT -> emptySet()
                    Mode.PEN, Mode.RECTANGLE, Mode.CIRCLE, Mode.TRIANGLE, Mode.ARROW, Mode.LINE -> {
                        val lastId = orderedElements.lastOrNull()?.id
                        if (lastId == null) emptySet() else setOf(lastId)
                    }
                }
            }

            // Freshness check on the cached layer. Walks orderedElements once,
            // comparing non-active and non-hidden entries against
            // lastRecordedStaticRefs by reference (allocation-free). Hidden
            // ids are skipped so a freshly-hidden element invalidates the
            // cache instead of replaying from a recording made before it was
            // hidden (otherwise the inline editor would ghost over the
            // still-cached text).
            val cacheFresh = lastRecordedVp == vp &&
                staticRefsMatch(
                    orderedElements,
                    activeIds,
                    hiddenElementIds,
                    lastRecordedStaticRefs,
                )

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
                            if (el.id !in activeIds && el.id !in hiddenElementIds) {
                                renderElement(el, pathCache, imageCache, textCache, textMeasurer, vp.scale)
                            }
                        }
                    }
                }
                lastRecordedVp = vp
                lastRecordedStaticRefs = orderedElements
                    .filter { it.id !in activeIds && it.id !in hiddenElementIds }
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
                        if (el.id in activeIds && el.id !in hiddenElementIds) renderElement(el, pathCache, imageCache, textCache, textMeasurer, vp.scale)
                    }
                }
                drawSelectionChrome(
                    state = state,
                    handleSizePx = handleSizePx,
                    rotationOffsetPx = rotationOffsetPx,
                    inverseScale = 1f / vp.scale,
                    hiddenElementIds = hiddenElementIds,
                )
                // Eraser cursor overlay: outline circle at the world-space
                // pointer with the configured eraser radius. Drawn inside the
                // world transform so the radius matches the actual hit area at
                // any zoom level. Skipped on touch (no hover events → null pos)
                // and outside ERASER mode.
                if (state.effectiveMode == Mode.ERASER) {
                    eraserPointerWorld?.let { center ->
                        val r = state.eraserSize
                        val ringColor = if (state.bgColor.luminance() < 0.5f) {
                            Color.White
                        } else {
                            Color.Black
                        }
                        drawCircle(
                            color = ringColor,
                            radius = r,
                            center = center,
                            style = Stroke(width = 1.5f / vp.scale),
                        )
                    }
                }
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
                        orderedElements.forEach { renderElement(it, pathCache, imageCache, textCache, textMeasurer, vp.scale) }
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

            // Reclaim PathCache + ImageBitmapCache + TextLayoutCache entries
            // for deleted elements. Cheap (HashMap key-retain over the live id
            // set); only worth doing when the live set actually shrank.
            if (pathCache.size() + imageCache.size() + textCache.size() > state.elements.size) {
                val liveIds = state.elements.mapTo(HashSet(state.elements.size)) { it.id }
                pathCache.retainOnly(liveIds)
                imageCache.retainOnly(liveIds)
                textCache.retainOnly(liveIds)
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

/**
 * Unit-clamped pressure multiplier for the current pointer sample.
 *
 * Returns `1.0` (no-signal default — uniform stroke) when:
 *   - The pointer is a mouse. Mouse pressure isn't meaningful for drawing;
 *     pretending it is would scale every stroke by a stale, half-correct value.
 *   - The reported pressure is `0.0`. Compose forwards `0.0` for events that
 *     genuinely carry no pressure (e.g. some touch screens, hover events).
 *
 * Otherwise the reading is clamped to `[0.2, 1.0]` so a barely-touching pen
 * still produces a visible stroke instead of a zero-width segment.
 */
private fun PointerInputChange.pressureMultiplier(): Float {
    if (type == PointerType.Mouse) return 1f
    val p = pressure
    if (p == 0f || p.isNaN()) return 1f
    return p.coerceIn(0.2f, 1f)
}

private fun modeShapeType(mode: Mode): ShapeType? = when (mode) {
    Mode.RECTANGLE -> ShapeType.RECTANGLE
    Mode.CIRCLE -> ShapeType.CIRCLE
    Mode.TRIANGLE -> ShapeType.TRIANGLE
    Mode.ARROW -> ShapeType.ARROW
    Mode.LINE -> ShapeType.LINE
    Mode.PEN, Mode.SELECT, Mode.PAN, Mode.ERASER, Mode.TEXT -> null
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
    hiddenElementIds: Set<String> = emptySet(),
) {
    val rotationOffsetWorld = rotationOffsetPx * inverseScale
    state.elements
        .asSequence()
        .filter { it.id in state.selectedIds && it.id !in hiddenElementIds }
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
private fun DrawScope.renderElement(
    element: Element,
    pathCache: PathCache? = null,
    imageCache: ImageBitmapCache? = null,
    textCache: TextLayoutCache? = null,
    textMeasurer: androidx.compose.ui.text.TextMeasurer? = null,
    viewportScale: Float = 1f,
) {
    if (element.rotation == 0f) {
        renderElementContent(element, pathCache, imageCache, textCache, textMeasurer, viewportScale)
    } else {
        withTransform({ rotate(element.rotation, pivot = element.bounds().center) }) {
            renderElementContent(element, pathCache, imageCache, textCache, textMeasurer, viewportScale)
        }
    }
}

private fun DrawScope.renderElementContent(
    element: Element,
    pathCache: PathCache?,
    imageCache: ImageBitmapCache?,
    textCache: TextLayoutCache?,
    textMeasurer: androidx.compose.ui.text.TextMeasurer?,
    viewportScale: Float,
) {
    when (element) {
        is Element.Path -> {
            val samples = element.samples
            // Uniform-width fast path when every sample carries the same width
            // (mouse / capacitive touch / programmatic insertion). One drawPath
            // call with cached geometry — keeps the existing perf characteristics
            // and respects DASHED / DOTTED stroke styles.
            val firstWidth = samples.firstOrNull()?.width ?: element.strokeWidth
            val uniform = samples.all { it.width == firstWidth }
            if (uniform || samples.size < 2) {
                val positions = element.positions
                val path = pathCache?.pathFor(element.id, positions)
                    ?: createPath(positions)
                drawPath(
                    path,
                    color = element.strokeColor,
                    alpha = element.alpha,
                    style = Stroke(
                        // Round caps always for paths — keeps pencil-like ends for
                        // solid strokes and makes individual dots visible for dotted.
                        width = firstWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = element.strokeStyle.toPathEffect(firstWidth),
                    ),
                )
            } else {
                // Variable-width path (pen pressure). Compose's Stroke path
                // effects (DASHED / DOTTED) only apply to a single drawPath
                // call with one stroke width, so they can't be reused as-is.
                // Walk arc length and split the stroke into on / off intervals
                // sized by the LOCAL sample width — that way a dashed stroke
                // at light pressure has small dashes and at heavy pressure has
                // large dashes, preserving both signals.
                drawVariableWidthPath(
                    samples = samples,
                    color = element.strokeColor,
                    alpha = element.alpha,
                    style = element.strokeStyle,
                )
            }
        }
        is Element.Shape -> {
            drawShape(element)
        }
        is Element.Image -> {
            drawImageElement(element, imageCache, viewportScale)
        }
        is Element.Text -> {
            drawTextElement(element, textCache, textMeasurer)
        }
    }
}

/**
 * Render an [Element.Text] inside its wrap box. Layout is delegated to
 * [TextLayoutCache] so re-rendering an unchanged block is allocation-free.
 *
 * When the layout cache or [textMeasurer] isn't available (e.g. the
 * read-only [DrawingPreview] preview path), the element falls back to a
 * grey placeholder rectangle — preview surfaces don't need to lay out
 * text and would otherwise pay a TextMeasurer cost per frame.
 */
private fun DrawScope.drawTextElement(
    element: Element.Text,
    textCache: TextLayoutCache?,
    textMeasurer: androidx.compose.ui.text.TextMeasurer?,
) {
    val wrapWidth = element.wrapWidth.coerceAtLeast(1f)
    if (textCache == null || textMeasurer == null) {
        // Preview path: outline the wrap box so the reader can see the
        // placeholder. No layout cost.
        drawRect(
            color = Color(0x33888888),
            topLeft = element.topLeft,
            size = Size(wrapWidth, element.measuredHeight.coerceAtLeast(1f)),
            style = Stroke(width = 1f),
        )
        return
    }
    val layout = textCache.layoutFor(
        id = element.id,
        text = element.text,
        fontFamilyKey = element.fontFamilyKey,
        fontSize = element.fontSize,
        alignment = element.alignment,
        wrapWidth = wrapWidth,
        measurer = textMeasurer,
    )
    drawText(
        textLayoutResult = layout,
        color = element.color,
        topLeft = element.topLeft,
        alpha = element.opacity,
    )
}

/**
 * Render an [Element.Image] inside its current AABB. Decoding is deferred to
 * [ImageBitmapCache] so the per-frame work is just a translated + scaled
 * `drawImage` call. When the cache misses (no [imageCache] supplied, or
 * decode failed), we draw a neutral grey placeholder rectangle so the canvas
 * still reflects the element's footprint and selection chrome remains usable.
 */
private fun DrawScope.drawImageElement(
    image: Element.Image,
    imageCache: ImageBitmapCache?,
    viewportScale: Float,
) {
    if (image.points.size < 2) return
    val topLeft = image.points[0]
    val bottomRight = image.points[1]
    val targetW = bottomRight.x - topLeft.x
    val targetH = bottomRight.y - topLeft.y
    if (targetW <= 0f || targetH <= 0f) return

    // The cache picks a mip level from the longer dimension in *screen*
    // pixels — multiplying by viewport.scale gives the renderable size.
    // A 600 px placement on a HiDPI 2× display at 1.5× zoom decodes at
    // 1800 px, not 600 (or 4096 source).
    val targetDimPx = (maxOf(targetW, targetH) * viewportScale).toInt()
    val bitmap = imageCache?.bitmapFor(image.id, image.bytes, targetDimPx)
        ?: decodeImageBitmap(image.bytes)
    if (bitmap == null) {
        // Visible placeholder so a corrupt / unsupported payload doesn't render
        // as an invisible hole that's still selectable/movable.
        drawRect(
            color = Color(0xFFE0E0E0),
            topLeft = topLeft,
            size = Size(targetW, targetH),
        )
        drawRect(
            color = Color(0xFFB0B0B0),
            topLeft = topLeft,
            size = Size(targetW, targetH),
            style = Stroke(width = 1.5f),
        )
        return
    }

    val srcW = bitmap.width
    val srcH = bitmap.height
    if (srcW <= 0 || srcH <= 0) return

    // Scale the source bitmap into the placed AABB. `withTransform` keeps the
    // outer world transform intact while applying the per-image translate +
    // scale, so element rotation (applied one level up in renderElement)
    // composes correctly. The `IntOffset` / `IntSize` `drawImage` overload is
    // used because its parameter names are stable across Compose versions —
    // the `Offset`-based variant has shifted between `topLeftOffset` and
    // `topLeft` over time.
    withTransform({
        translate(topLeft.x, topLeft.y)
        scale(
            scaleX = targetW / srcW,
            scaleY = targetH / srcH,
            pivot = Offset.Zero,
        )
    }) {
        drawImage(
            image = bitmap,
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(srcW, srcH),
            alpha = image.opacity,
        )
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
 * **Fill and stroke (independent):**
 * - If [Element.Shape.fillColor] is set, a fill pass is drawn with that color.
 * - If [Element.Shape.strokeEnabled] is true AND [Element.Shape.strokeWidth] > 0,
 *   a stroke pass is drawn on top with [Element.Shape.strokeColor].
 * - Both can be on (filled with a colored border), either alone, or — for an
 *   invisible shape — neither.
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
    val hasFill = shape.fillColor != null
    val hasStroke = shape.strokeEnabled && shape.strokeWidth > 0f

    when (shape.shapeType) {
        ShapeType.RECTANGLE -> {
            val r = shape.cornerRadius.coerceAtMost(minOf(width, height) * 0.5f)
            val size = Size(width, height)
            if (hasFill) {
                if (r > 0f) {
                    drawRoundRect(
                        color = shape.fillColor!!,
                        topLeft = topLeft,
                        size = size,
                        cornerRadius = CornerRadius(r, r),
                        style = Fill,
                    )
                } else {
                    drawRect(
                        color = shape.fillColor!!,
                        topLeft = topLeft,
                        size = size,
                        style = Fill,
                    )
                }
            }
            if (hasStroke) {
                if (r > 0f) {
                    drawRoundRect(
                        color = shape.strokeColor,
                        topLeft = topLeft,
                        size = size,
                        cornerRadius = CornerRadius(r, r),
                        style = shape.strokeDrawStyle(),
                    )
                } else {
                    drawRect(
                        color = shape.strokeColor,
                        topLeft = topLeft,
                        size = size,
                        style = shape.strokeDrawStyle(),
                    )
                }
            }
        }
        ShapeType.CIRCLE -> {
            val center = start + Offset(
                (end.x - start.x) / 2,
                (end.y - start.y) / 2
            )
            val distance = sqrt((end.x - start.x) * (end.x - start.x) + (end.y - start.y) * (end.y - start.y))
            val radius = distance / 2
            if (hasFill) {
                drawCircle(
                    color = shape.fillColor!!,
                    radius = radius,
                    center = center,
                    style = Fill,
                )
            }
            if (hasStroke) {
                drawCircle(
                    color = shape.strokeColor,
                    radius = radius,
                    center = center,
                    style = shape.strokeDrawStyle(),
                )
            }
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

/**
 * The Stroke style for an [Element.Shape] respecting its [StrokeStyle]. Always
 * returns a Stroke — fill is drawn as a separate pass in [drawShape] so a
 * shape can carry both a fill color and an outline.
 */
private fun Element.Shape.strokeDrawStyle(): androidx.compose.ui.graphics.drawscope.DrawStyle =
    Stroke(
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
        floatArrayOf(strokeWidth * DASH_ON_MULTIPLIER, strokeWidth * DASH_OFF_MULTIPLIER),
    )
    StrokeStyle.DOTTED -> PathEffect.dashPathEffect(
        floatArrayOf(strokeWidth * DOT_ON_MULTIPLIER, strokeWidth * DOT_OFF_MULTIPLIER),
    )
}

// On / off interval lengths for dashed and dotted strokes, expressed as
// multipliers of the local stroke width. Shared by the uniform-width path
// effect and the variable-width arc-length walker so both renderers agree on
// rhythm.
private const val DASH_ON_MULTIPLIER: Float = 4f
private const val DASH_OFF_MULTIPLIER: Float = 2f
private const val DOT_ON_MULTIPLIER: Float = 0.5f
private const val DOT_OFF_MULTIPLIER: Float = 2f

/**
 * Width-local on-interval length (in world pixels) for [style].
 * `SOLID` returns positive infinity so the arc-length walker never flips off.
 */
private fun StrokeStyle.onLength(width: Float): Float = when (this) {
    StrokeStyle.SOLID -> Float.POSITIVE_INFINITY
    StrokeStyle.DASHED -> width * DASH_ON_MULTIPLIER
    StrokeStyle.DOTTED -> width * DOT_ON_MULTIPLIER
}

/**
 * Width-local off-interval length for [style]. `SOLID` returns zero — never
 * consulted because [onLength] is infinite, but defined for symmetry.
 */
private fun StrokeStyle.offLength(width: Float): Float = when (this) {
    StrokeStyle.SOLID -> 0f
    StrokeStyle.DASHED -> width * DASH_OFF_MULTIPLIER
    StrokeStyle.DOTTED -> width * DOT_OFF_MULTIPLIER
}

private fun lerp(a: Offset, b: Offset, t: Float): Offset =
    Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/**
 * Render a variable-width pen-pressure stroke that also respects [style].
 *
 * Walks the polyline in arc-length space, alternating between "on" (visible)
 * and "off" (gap) intervals. The length of each interval is sized by the
 * stroke width at the position where the interval STARTS — so dashes shrink
 * with light pressure and grow with heavy pressure, matching the visual
 * intuition that lighter strokes carry finer detail.
 *
 * Each on-interval is drawn as a `drawLine` between the interval endpoints
 * with the average of the two endpoint widths and round caps, mirroring the
 * solid variable-width pencil feel. Linear width interpolation between
 * adjacent samples keeps thickness coherent across sample boundaries.
 *
 * For `SOLID` styles this collapses to a single linear sweep with no flips,
 * matching the simpler segment-per-sample render.
 *
 * **Safety floors.** A naive implementation will hang on three real cases:
 *   - A `remaining` value tiny relative to `segLen` underflows the
 *     `remaining / segLen` division to `0`, leaving `t` not advancing.
 *   - A sample width of `0` (degenerate input, NaN-propagation through
 *     pressure math, or float underflow at extreme low pressure) makes
 *     `onLength` / `offLength` both `0`, so the state machine flips
 *     forever without consuming arc length.
 *   - DOTTED at a 0.5px width can spawn ~200 segments per 100px of stroke
 *     — fine for correctness, terrible for frame budget on an active drag.
 *
 * The fix enforces a minimum effective dash width and a minimum interval
 * length, plus a per-segment iteration cap as a last-resort safeguard.
 * These floors are conservatively small (≈ 0.5 px) so they don't change
 * visible output for realistic strokes.
 */
private fun DrawScope.drawVariableWidthPath(
    samples: List<Element.PathSample>,
    color: Color,
    alpha: Float,
    style: StrokeStyle,
) {
    if (samples.size < 2) return

    fun safeOn(w: Float): Float =
        style.onLength(w.coerceAtLeast(MIN_DASH_WIDTH)).coerceAtLeast(MIN_INTERVAL_LENGTH)

    fun safeOff(w: Float): Float =
        style.offLength(w.coerceAtLeast(MIN_DASH_WIDTH)).coerceAtLeast(MIN_INTERVAL_LENGTH)

    var inOn = true
    var remaining = safeOn(samples[0].width)

    for (i in 0 until samples.size - 1) {
        val a = samples[i]
        val b = samples[i + 1]
        val dx = b.position.x - a.position.x
        val dy = b.position.y - a.position.y
        val segLen = sqrt(dx * dx + dy * dy)
        if (segLen < 1e-6f || !segLen.isFinite()) continue

        var t = 0f
        var iters = 0
        while (t < 1f && iters < MAX_DASH_ITERS_PER_SEGMENT) {
            iters++
            val parameterStep = (remaining / segLen).coerceAtMost(1f - t).coerceAtLeast(0f)
            // Numerical underflow guard: if the requested step rounds to zero
            // arc length, force this segment to complete in one shot instead
            // of looping with no progress.
            val span = if (parameterStep * segLen < MIN_INTERVAL_LENGTH * 0.5f) {
                1f - t
            } else {
                parameterStep
            }
            val tNext = t + span
            if (inOn && span > 0f) {
                val startPos = lerp(a.position, b.position, t)
                val endPos = lerp(a.position, b.position, tNext)
                val startW = lerp(a.width, b.width, t)
                val endW = lerp(a.width, b.width, tNext)
                drawLine(
                    color = color,
                    start = startPos,
                    end = endPos,
                    strokeWidth = (startW + endW) * 0.5f,
                    cap = StrokeCap.Round,
                    alpha = alpha,
                )
            }
            val consumed = span * segLen
            remaining -= consumed
            t = tNext
            if (remaining <= 0f && style != StrokeStyle.SOLID) {
                inOn = !inOn
                val widthHere = lerp(a.width, b.width, t)
                remaining = if (inOn) safeOn(widthHere) else safeOff(widthHere)
            }
        }
    }
}

/**
 * Minimum effective stroke width used inside the dash/dot walker. Prevents
 * zero-length intervals when a sample reports width 0 (degenerate input,
 * float underflow at extreme low pressure, NaN-propagation).
 */
private const val MIN_DASH_WIDTH: Float = 0.5f

/**
 * Minimum on/off interval length, in world pixels. Floors the walker's
 * arc-length step so a tiny `remaining` value can never trap the loop in
 * a zero-progress iteration.
 */
private const val MIN_INTERVAL_LENGTH: Float = 0.5f

/**
 * Per-segment iteration cap. A realistic worst case (5px segment,
 * 0.5px dot period) is ~10 iterations. 1024 is a paranoid backstop that
 * absolutely cannot be reached without a math bug — if it ever is, we
 * give up gracefully on that segment instead of pinning the main thread.
 */
private const val MAX_DASH_ITERS_PER_SEGMENT: Int = 1024

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
    if (shape.fillColor != null) {
        drawPath(path, color = shape.fillColor, style = Fill)
    }
    if (shape.strokeEnabled && shape.strokeWidth > 0f) {
        drawPath(path, color = shape.strokeColor, style = shape.strokeDrawStyle())
    }
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
