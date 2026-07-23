@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.ak1.drawboxsample.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import io.ak1.drawbox.DrawBox
import io.ak1.drawbox.domain.model.Element
import io.ak1.drawbox.domain.model.Event
import io.ak1.drawbox.domain.model.Mode
import io.ak1.drawbox.domain.model.ShapeType
import io.ak1.drawbox.domain.model.State
import io.ak1.drawbox.domain.model.bezierMidpoint
import io.ak1.drawbox.domain.model.bounds
import io.ak1.drawbox.domain.model.controlPoint
import io.ak1.drawbox.presentation.viewmodel.rememberDrawBoxController
import io.ak1.drawbox.input.imageDragAndDropTarget
import io.ak1.drawbox.input.pasteImageFromClipboard
import io.ak1.drawboxsample.save.rememberImageSaver
import io.ak1.drawboxsample.ui.components.BgPatternPreset
import io.ak1.drawbox.ui.context.ContextBar
import io.ak1.drawbox.ui.controls.ControlsBar
import io.ak1.drawbox.ui.controls.ControlsBarIntent
import io.ak1.drawbox.ui.controls.ControlsBarState
import io.ak1.drawbox.ui.controls.defaultControlsBarItems
import io.ak1.drawbox.ui.model.ContextBarIntent
import io.ak1.drawbox.ui.model.ContextBarSlots
import io.ak1.drawbox.ui.model.ContextBarState
import io.ak1.drawbox.ui.picker.RangVikalpColorPicker
import io.ak1.drawbox.text.InlineTextEditor
import io.ak1.drawboxsample.ui.components.SettingsDrawer
import io.ak1.drawboxsample.ui.components.TopRightControls
import io.ak1.drawboxsample.ui.components.ZoomToolbar
import io.ak1.drawboxsample.ui.theme.ThemeMode
import org.jetbrains.compose.resources.painterResource

@Composable
fun HomeScreen(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {},
) {
    val showColorDialog = remember { mutableStateOf(false) }
    val showGrid = remember { mutableStateOf(true) }
    val imageSaver = rememberImageSaver()
    // Supplied to exportPng so text elements lay out with real glyphs in the
    // headless raster; without it they'd render as placeholder boxes.
    val textMeasurer = rememberTextMeasurer()
    val themedBg = MaterialTheme.colorScheme.background
    val themedBrush = MaterialTheme.colorScheme.primary

    val viewModel = rememberDrawBoxController(
        initialState = State(strokeColor = themedBrush),
    )
    val state by viewModel.state.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    // Inline-text-edit state. Hoisted above the event collector because
    // Event.TextEditRequested (double-tap / re-tap of a selected text) opens
    // the editor from there. `editDraft` is the live buffer the outside-tap
    // overlay reads on commit; `editOriginal` is the pre-edit text so Esc can
    // revert without committing (#83.5).
    var editingTextId by remember { mutableStateOf<String?>(null) }
    var editDraft by remember { mutableStateOf("") }
    var editOriginal by remember { mutableStateOf("") }

    // Canvas bg follows the active theme. Re-applies whenever the resolved
    // background color changes (theme toggle, SYSTEM follow flips, etc.).
    LaunchedEffect(themedBg) { viewModel.setBgColor(themedBg) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is Event.PngExported -> imageSaver.savePng(event.bytes)
                is Event.SvgExported -> imageSaver.saveSvg(event.svg)
                is Event.JsonExported -> imageSaver.saveJson(event.json)
                // Double-tap (#83.2) or second tap on a selected text (#83.6):
                // open the inline editor for the requested element.
                is Event.TextEditRequested -> {
                    (state.elements.firstOrNull { it.id == event.id } as? Element.Text)?.let {
                        editingTextId = it.id
                        editDraft = it.text
                        editOriginal = it.text
                    }
                }
                else -> {}
            }
        }
    }

    // Debug print: dump selected element data on every selection change so the
    // box the chrome draws can be compared to the actual element data.
    LaunchedEffect(state.selectedIds, state.elements) {
        if (state.selectedIds.isEmpty()) return@LaunchedEffect
        state.elements.filter { it.id in state.selectedIds }.forEach { el ->
                val b = el.bounds()
                println(
                    "[SELECTED] id=${el.id} type=${el::class.simpleName}" + (if (el is Element.Shape) " shapeType=${el.shapeType}" else "") + " rotation=${el.rotation}" + " bounds=(L=${b.left}, T=${b.top}, R=${b.right}, B=${b.bottom}, w=${b.width}, h=${b.height})",
                )
                if (el is Element.Shape) {
                    println(
                        "  strokeStyle=${el.strokeStyle} strokeWidth=${el.strokeWidth}" + " cornerRadius=${el.cornerRadius} fillColor=${el.fillColor}",
                    )
                    if (el.shapeType == ShapeType.LINE || el.shapeType == ShapeType.ARROW) {
                        println(
                            "  bend=${el.bend} controlPoint=${el.controlPoint()}" + " bezierMidpoint=${el.bezierMidpoint()}" + " startBinding=${el.startBinding} endBinding=${el.endBinding}",
                        )
                    }
                }
            }
    }

    // Derived selection / mode flags used by the top-right contextual config.
    val selectedRoundable = state.elements.filter {
        it.id in state.selectedIds && it is Element.Shape && (it.shapeType == ShapeType.RECTANGLE || it.shapeType == ShapeType.TRIANGLE)
    }
    val selectedDrawables = state.elements.filter { it.id in state.selectedIds }
    val isShapeMode =
        state.mode == Mode.PEN || state.mode == Mode.RECTANGLE || state.mode == Mode.CIRCLE || state.mode == Mode.TRIANGLE || state.mode == Mode.ARROW || state.mode == Mode.LINE
    val isTextMode = state.mode == Mode.TEXT
    val hasSelection = selectedDrawables.isNotEmpty()
    // Shape-stroke chips apply to shapes/paths. Hide them in TEXT mode
    // (the selection is text, or there's no selection — neither needs
    // stroke style/width).
    val selectedHasStrokeable = selectedDrawables.any {
        it is Element.Shape || it is Element.Path
    }
    val currentRadius = if (selectedRoundable.isNotEmpty()) (selectedRoundable.first() as Element.Shape).cornerRadius
    else state.currentItemCornerRadius
    val currentStrokeStyle = when (val first = selectedDrawables.firstOrNull()) {
        is Element.Shape -> first.strokeStyle
        is Element.Path -> first.strokeStyle
        else -> state.currentItemStrokeStyle
    }
    val currentShapeColor = when (val first = selectedDrawables.firstOrNull()) {
        is Element.Shape -> first.strokeColor
        is Element.Path -> first.strokeColor
        is Element.Text -> first.color
        else -> state.strokeColor
    }
    val currentStrokeWidth = when (val first = selectedDrawables.firstOrNull()) {
        is Element.Shape -> first.strokeWidth
        is Element.Path -> first.strokeWidth
        else -> state.strokeWidth
    }
    // Fill + stroke-toggle are shape-only. Both are shown whenever the
    // selection contains at least one Shape; the swatches reflect that shape's
    // current fill / stroke state.
    val selectedShapes = selectedDrawables.filterIsInstance<Element.Shape>()
    val showFill = selectedShapes.isNotEmpty()
    val showStrokeToggle = selectedShapes.isNotEmpty()
    val currentFillColor = selectedShapes.firstOrNull()?.fillColor
    val currentStrokeEnabled = selectedShapes.firstOrNull()?.strokeEnabled ?: true
    // Elements that support BOTH stroke and fill drive ControlsBar's split-disc
    // swatch + the multi-target picker. Only closed shapes qualify — LINE and
    // ARROW are stroke-only, paths and text likewise.
    val hasFillableSelection = selectedShapes.any {
        it.shapeType == ShapeType.RECTANGLE ||
            it.shapeType == ShapeType.CIRCLE ||
            it.shapeType == ShapeType.TRIANGLE
    }
    // Also let the split disc appear in fillable-shape tool modes with no
    // selection, so the user can preconfigure fill BEFORE dropping the shape.
    // Backed by State.currentItemFillColor / currentItemStrokeEnabled.
    val isFillableToolMode = state.mode == Mode.RECTANGLE ||
        state.mode == Mode.CIRCLE ||
        state.mode == Mode.TRIANGLE
    val showFillTarget = hasFillableSelection || (!hasSelection && isFillableToolMode)
    val toolOrSelectionStrokeEnabled = if (hasFillableSelection)
        currentStrokeEnabled else state.currentItemStrokeEnabled
    val toolOrSelectionFillColor = if (hasFillableSelection)
        currentFillColor else state.currentItemFillColor

    // Text controls — surfaced in TEXT mode (pre-configure the next insert)
    // OR when one or more Text elements are selected. For a multi-text
    // selection (#83.3) the chips seed from the first element and flag any
    // property whose members disagree as "mixed"; the Set* intents already
    // apply to every selected element.
    val selectedTexts = selectedDrawables.filterIsInstance<Element.Text>()
    val singleSelectedText = selectedTexts.singleOrNull()?.takeIf { selectedDrawables.size == 1 }
    val firstSelectedText = selectedTexts.firstOrNull()
    // Current values follow the selection's first element when present;
    // otherwise fall back to the State defaults the next insert will use.
    val currentFontSize = firstSelectedText?.fontSize ?: state.currentItemFontSize
    val currentTextAlignment = firstSelectedText?.alignment ?: state.currentItemTextAlignment
    val currentFontFamilyKey = firstSelectedText?.fontFamilyKey ?: state.currentItemFontFamilyKey
    // Mixed when >1 text selected and the property isn't uniform across them.
    val fontSizeMixed = selectedTexts.size > 1 && selectedTexts.distinctBy { it.fontSize }.size > 1
    val textAlignmentMixed = selectedTexts.size > 1 && selectedTexts.distinctBy { it.alignment }.size > 1
    val fontFamilyMixed = selectedTexts.size > 1 && selectedTexts.distinctBy { it.fontFamilyKey }.size > 1
    val fontFamilyKeys = io.ak1.drawbox.text.FontRegistry.keys()

    // Drawer + bg-pattern state.
    var drawerOpen by remember { mutableStateOf(false) }
    var replayOpen by remember { mutableStateOf(false) }
    var currentBgPattern by remember { mutableStateOf<BgPatternPreset?>(null) }
    var colorPickerForBg by remember { mutableStateOf(false) }

    // TEXT mode flow: DrawBox dispatches Intent.InsertText("") on tap. We
    // detect the newly inserted empty Text element here and open the inline
    // editor. The editor renders as the element-to-be (no border, no Done
    // button) and is dismissed by tapping anywhere outside, which routes
    // through a full-screen overlay below. Empty commits delete the element.
    // (State declared above the event collector.)
    LaunchedEffect(state.elements) {
        if (editingTextId != null) return@LaunchedEffect
        val last = state.elements.lastOrNull()
        if (last is Element.Text && last.text.isEmpty()) {
            editingTextId = last.id
            editDraft = ""
            editOriginal = ""
        }
    }
    // Helper: commit the current draft for the editing element. Empty drafts
    // delete the element so an invisible wrap box never lingers.
    fun commitTextEdit() {
        val id = editingTextId ?: return
        if (editDraft.isEmpty()) {
            viewModel.onIntent(io.ak1.drawbox.domain.model.Intent.DeleteElement(id))
        } else {
            viewModel.updateText(id, editDraft)
        }
        editingTextId = null
    }
    // Cancel (Esc, #83.5): abandon the edit without committing the draft. A
    // freshly inserted element (empty pre-edit text) is deleted so it doesn't
    // linger; an existing element keeps its pre-edit text, which state already
    // holds since we only mutate on commit.
    fun cancelTextEdit() {
        val id = editingTextId ?: return
        if (editOriginal.isEmpty()) {
            viewModel.onIntent(io.ak1.drawbox.domain.model.Intent.DeleteElement(id))
        }
        editingTextId = null
    }

    // Apply background pattern to the canvas. Tinted against onSurface so the
    // same drawable reads on both light and dark canvases.
    val patternPainter = currentBgPattern?.let { painterResource(it.drawable) }
    val patternTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    LaunchedEffect(patternPainter, patternTint) {
        viewModel.setBackgroundPattern(patternPainter, patternTint)
    }

    // Fade all floating chrome down to 0.35 alpha while the user's actively
    // touching the canvas — matches the Samsung / OPPO Notes idle-canvas
    // pattern. Peek at pointer events on the Initial pass so DrawBox still
    // sees them un-consumed.
    var isGesturing by remember { mutableStateOf(false) }
    val chromeAlpha by animateFloatAsState(
        targetValue = if (isGesturing) 0.35f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "chromeAlpha",
    )

    Scaffold { _ ->

        DrawBox(
            state = state,
            onIntent = viewModel::onIntent,
            modifier = Modifier.fillMaxSize().clipToBounds()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            isGesturing = event.changes.any { it.pressed }
                        }
                    }
                }
                // OS drag-drop: dragging image files from Finder /
                // Explorer onto the canvas inserts them at the drop
                // point. Each file in a multi-file drop gets a small
                // cumulative offset so they don't perfectly stack.
                // Touch platforms + web targets receive a no-op modifier;
                // JVM Desktop is the only target that wires it today.
                .imageDragAndDropTarget { drops ->
                    drops.forEachIndexed { i, drop ->
                        val world = state.viewport.screenToWorld(drop.dropPositionScreen)
                        val cascade = (i * 24f)
                        viewModel.insertImage(
                            drop.bytes,
                            drop.intrinsicSize,
                            Offset(world.x + cascade, world.y + cascade),
                        )
                    }
                }
                // Cmd/Ctrl+V → paste an image from the system clipboard at
                // the viewport center. Per-platform clipboard read happens
                // in pasteImageFromClipboard; on touch platforms the call
                // is a no-op. Hooked via onPreviewKeyEvent so the SDK's
                // Space-bar pan handler (which only consumes Space) doesn't
                // shadow us.
                .onPreviewKeyEvent { event ->
                    val isV = event.key == Key.V
                    val isPaste = event.type == KeyEventType.KeyDown &&
                        isV &&
                        (event.isMetaPressed || event.isCtrlPressed)
                    if (isPaste) {
                        pasteImageFromClipboard { bytes, intrinsicSize ->
                            val world = state.viewport.screenToWorld(ScreenCenter)
                            viewModel.insertImage(bytes, intrinsicSize, world)
                        }
                        true
                    } else false
                },
            showGrid = showGrid.value,
            // While the inline editor is open over a text element, tell the
            // SDK to skip both the element render and its selection chrome
            // so the editor's frame is the only one visible (no ghosting).
            hiddenElementIds = editingTextId?.let { setOf(it) } ?: emptySet(),
        )
        // Commit overlay (#83.1) — composed BETWEEN the canvas and the toolbar
        // layer below. It sits ABOVE the canvas (so a tap on empty canvas
        // commits the edit) but BELOW the ContextBar (so its style chips get
        // their taps and restyle the element instead of the overlay
        // committing). No zIndex on purpose: a fillMaxSize Box WITH a zIndex
        // captures pointer events (that regressed plain selection); relying on
        // composition order keeps empty-area pass-through intact.
        if (editingTextId != null) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(editingTextId) {
                        detectTapGestures(onTap = { commitTextEdit() })
                    },
            )
        }
        BoxWithConstraints(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            val isNarrow = maxWidth < 600.dp
            val density = androidx.compose.ui.platform.LocalDensity.current
            val viewportW = with(density) { maxWidth.toPx() }
            val viewportH = with(density) { maxHeight.toPx() }

            // Selection-anchor math for the selection ContextBar. Computes the
            // union of selected element bounds in world space, converts to
            // screen space, and returns an (x, y) top-left anchor to hang the
            // pill from. Falls back to null (→ fixed top-right) when the
            // selection is entirely off-screen or spans the viewport.
            val selectionAnchor: androidx.compose.ui.unit.DpOffset? = remember(
                state.selectedIds, state.elements, state.viewport, viewportW, viewportH,
            ) {
                if (!hasSelection) return@remember null
                val worldRects = state.elements
                    .filter { it.id in state.selectedIds }
                    .map { it.bounds() }
                if (worldRects.isEmpty()) return@remember null
                val worldLeft = worldRects.minOf { it.left }
                val worldTop = worldRects.minOf { it.top }
                val worldRight = worldRects.maxOf { it.right }
                val worldBottom = worldRects.maxOf { it.bottom }
                val topLeftS = state.viewport.worldToScreen(Offset(worldLeft, worldTop))
                val bottomRightS = state.viewport.worldToScreen(Offset(worldRight, worldBottom))
                val onScreen = topLeftS.x < viewportW && bottomRightS.x > 0f &&
                    topLeftS.y < viewportH && bottomRightS.y > 0f
                val spansViewport = (bottomRightS.x - topLeftS.x) > viewportW * 0.9f ||
                    (bottomRightS.y - topLeftS.y) > viewportH * 0.9f
                if (!onScreen || spansViewport) return@remember null
                // Anchor above the selection with a 12dp gap and estimate the
                // pill's own height at 48dp. Fall back to below when the top
                // slot would collide with the app-bar area (< 72dp from top).
                val barHeightPx = with(density) { 48.dp.toPx() }
                val gapPx = with(density) { 12.dp.toPx() }
                val topAbovePx = topLeftS.y - gapPx - barHeightPx
                val topBelowPx = bottomRightS.y + gapPx
                val minTopPx = with(density) { 72.dp.toPx() }
                val maxTopPx = viewportH - barHeightPx - with(density) { 12.dp.toPx() }
                val yPx = if (topAbovePx >= minTopPx) topAbovePx else topBelowPx
                // Left-anchor the pill to the selection's left. Assume ~360dp
                // max chip stack, clamp so the pill stays fully in-viewport.
                val estBarWidthPx = with(density) { 360.dp.toPx() }
                val minLeftPx = with(density) { 12.dp.toPx() }
                val maxLeftPx = (viewportW - estBarWidthPx).coerceAtLeast(minLeftPx)
                val xPx = topLeftS.x.coerceIn(minLeftPx, maxLeftPx)
                val yPxClamped = yPx.coerceIn(minTopPx, maxTopPx)
                androidx.compose.ui.unit.DpOffset(
                    x = with(density) { xPx.toDp() },
                    y = with(density) { yPxClamped.toDp() },
                )
            }


            // Config bars split by attachment target — notes-app-style discipline:
            //  - Tool bar (bottom-center, above ControlsBar): configures the
            //    active tool's NEXT stroke/insert. Shown when a configurable
            //    tool mode is active AND nothing is selected.
            //  - Selection bar (top-right): edits / acts on the current
            //    selection. Shown when there IS a selection.
            // Mutually exclusive by construction — the user's attention is on
            // one thing at a time, so only one bar is ever visible.
            val commonBarState = ContextBarState(
                strokeColor = currentShapeColor,
                strokeEnabled = currentStrokeEnabled,
                strokeStyle = currentStrokeStyle,
                strokeWidth = currentStrokeWidth,
                fillColor = currentFillColor,
                cornerRadius = currentRadius,
                fontSize = currentFontSize,
                textAlignment = currentTextAlignment,
                fontFamilyKey = currentFontFamilyKey,
                fontFamilyKeys = fontFamilyKeys,
                fontSizeMixed = fontSizeMixed,
                textAlignmentMixed = textAlignmentMixed,
                fontFamilyMixed = fontFamilyMixed,
            )

            val toolBarVisible = !hasSelection && (isShapeMode || isTextMode)
            if (toolBarVisible) {
                ContextBar(
                    state = commonBarState,
                    slots = ContextBarSlots(
                        // Color already lives on ControlsBar's swatch — no need
                        // to duplicate it on the tool config bar. This keeps the
                        // tool bar focused on options ControlsBar doesn't
                        // expose (style, width, corner, size, align, family).
                        showStroke = false,
                        strokeToggleable = false,
                        showShapeStroke = isShapeMode,
                        showFill = false,
                        showCornerRadius = state.mode == Mode.RECTANGLE ||
                            state.mode == Mode.TRIANGLE,
                        showText = isTextMode,
                        showEditText = false,
                        showSelectionActions = false,
                    ),
                    onIntent = { intent ->
                        when (intent) {
                            is ContextBarIntent.SetStrokeColor -> viewModel.setColor(intent.color)
                            is ContextBarIntent.SetStrokeStyle -> viewModel.setStrokeStyle(intent.style)
                            is ContextBarIntent.SetStrokeWidth -> viewModel.setStrokeWidth(intent.width)
                            is ContextBarIntent.SetCornerRadius -> viewModel.setCornerRadius(intent.radius)
                            is ContextBarIntent.SetFontSize -> viewModel.setFontSize(intent.size)
                            is ContextBarIntent.SetTextAlignment -> viewModel.setTextAlignment(intent.alignment)
                            is ContextBarIntent.SetFontFamily -> viewModel.setFontFamily(intent.key)
                            else -> {}
                        }
                    },
                    fontFamilyResolver = { key -> io.ak1.drawbox.text.FontRegistry.resolve(key) },
                    modifier = Modifier.align(Alignment.BottomCenter)
                        .padding(bottom = 76.dp)
                        .alpha(chromeAlpha),
                )
            }

            if (hasSelection) {
                ContextBar(
                    state = commonBarState,
                    slots = ContextBarSlots(
                        // Color affordance owned by ControlsBar's swatch (plain
                        // or split-disc based on hasFillableSelection). The
                        // selection bar focuses on style/geometry/actions.
                        showStroke = false,
                        strokeToggleable = false,
                        showShapeStroke = selectedHasStrokeable,
                        showFill = false,
                        showCornerRadius = selectedRoundable.isNotEmpty(),
                        // Text style chips for any text selection (multi-text
                        // merges via "mixed"); inline edit stays single-only.
                        showText = selectedTexts.isNotEmpty(),
                        showEditText = singleSelectedText != null,
                        showSelectionActions = true,
                    ),
                    onIntent = { intent ->
                        when (intent) {
                            is ContextBarIntent.SetStrokeColor -> viewModel.setSelectionColor(intent.color)
                            is ContextBarIntent.SetStrokeEnabled -> viewModel.setSelectionStrokeEnabled(intent.enabled)
                            is ContextBarIntent.SetStrokeStyle -> viewModel.setSelectionStrokeStyle(intent.style)
                            is ContextBarIntent.SetStrokeWidth -> viewModel.setSelectionStrokeWidth(intent.width)
                            is ContextBarIntent.SetFillColor -> viewModel.setSelectionFillColor(intent.color)
                            is ContextBarIntent.SetCornerRadius -> viewModel.setSelectionCornerRadius(intent.radius)
                            is ContextBarIntent.SetFontSize -> viewModel.setSelectionFontSize(intent.size)
                            is ContextBarIntent.SetTextAlignment -> viewModel.setSelectionTextAlignment(intent.alignment)
                            is ContextBarIntent.SetFontFamily -> viewModel.setSelectionFontFamily(intent.key)
                            ContextBarIntent.EditText -> singleSelectedText?.let { target ->
                                editingTextId = target.id
                                editDraft = target.text
                                editOriginal = target.text
                            }
                            ContextBarIntent.BringToFront -> viewModel.bringSelectionToFront()
                            ContextBarIntent.SendToBack -> viewModel.sendSelectionToBack()
                            ContextBarIntent.Delete -> viewModel.deleteSelected()
                            ContextBarIntent.ClearSelection -> viewModel.clearSelection()
                        }
                    },
                    fontFamilyResolver = { key -> io.ak1.drawbox.text.FontRegistry.resolve(key) },
                    modifier = if (selectionAnchor != null) {
                        Modifier.align(Alignment.TopStart)
                            .padding(start = selectionAnchor.x, top = selectionAnchor.y)
                            .alpha(chromeAlpha)
                    } else {
                        // Fallback: fixed top-right when selection is off-screen
                        // or spans the viewport.
                        Modifier.align(Alignment.TopEnd)
                            .padding(top = 72.dp, end = 12.dp)
                            .alpha(chromeAlpha)
                    },
                )
            }

            // Top-right cluster: zoom (only narrow) + settings. Theme moved
            // into SettingsDrawer's View section.
            TopRightControls(
                isNarrow = isNarrow,
                scalePercent = state.viewport.scalePercent,
                onZoomIn = { viewModel.zoomBy(1.25f, ScreenCenter) },
                onZoomOut = { viewModel.zoomBy(0.8f, ScreenCenter) },
                onZoomReset = { viewModel.resetCamera() },
                onSettingsClick = { drawerOpen = true },
                modifier = Modifier.align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 12.dp)
                    .alpha(chromeAlpha),
            )

            // Bottom-left zoom (wide only). Narrow folds zoom into the top-right cluster.
            if (!isNarrow) {
                ZoomToolbar(
                    scalePercent = state.viewport.scalePercent,
                    onZoomIn = { viewModel.zoomBy(1.25f, ScreenCenter) },
                    onZoomOut = { viewModel.zoomBy(0.8f, ScreenCenter) },
                    onZoomReset = { viewModel.resetCamera() },
                    modifier = Modifier.align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 24.dp)
                        .alpha(chromeAlpha),
                )
            }

            // Bottom-center main NavBar.
            val controlsBarItems = defaultControlsBarItems(
                state = ControlsBarState(
                    currentMode = state.mode,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    strokeColor = currentShapeColor,
                    // Split-disc swatch + multi-target picker light up for
                    // fillable-shape selections AND for fillable-shape tool
                    // modes with no selection (preconfigure fill before draw).
                    showFillTarget = showFillTarget,
                    strokeEnabled = toolOrSelectionStrokeEnabled,
                    fillColor = toolOrSelectionFillColor,
                ),
                dispatch = { intent ->
                    when (intent) {
                        ControlsBarIntent.Undo -> viewModel.undo()
                        ControlsBarIntent.Redo -> viewModel.redo()
                        is ControlsBarIntent.SelectMode -> viewModel.setMode(intent.mode)
                        is ControlsBarIntent.SetStrokeColor ->
                            if (hasSelection) viewModel.setSelectionColor(intent.color)
                            else viewModel.setColor(intent.color)
                        is ControlsBarIntent.SetStrokeEnabled ->
                            if (hasSelection) viewModel.setSelectionStrokeEnabled(intent.enabled)
                            else viewModel.setStrokeEnabled(intent.enabled)
                        is ControlsBarIntent.SetFillColor ->
                            if (hasSelection) viewModel.setSelectionFillColor(intent.color)
                            else viewModel.setFillColor(intent.color)
                    }
                },
            )
            ControlsBar(
                items = controlsBarItems,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .alpha(chromeAlpha),
            )



            if (colorPickerForBg) {
                RangVikalpColorPicker(
                    initial = state.bgColor,
                    onDismiss = { colorPickerForBg = false },
                    onSelected = { viewModel.setBgColor(it) },
                )
            }

            if (showColorDialog.value) {
                RangVikalpColorPicker(
                    initial = state.strokeColor,
                    onDismiss = { showColorDialog.value = false },
                    onSelected = { color -> viewModel.setColor(color) },
                )
            }
        }
        // Right-side modal settings drawer.
        SettingsDrawer(
            visible = drawerOpen,
            showGrid = showGrid.value,
            currentBgColor = state.bgColor,
            currentBgPattern = currentBgPattern,
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            onDismiss = { drawerOpen = false },
            onDownloadSvg = { viewModel.exportSvg(); drawerOpen = false },
            onDownloadPng = { viewModel.exportPng(textMeasurer = textMeasurer); drawerOpen = false },
            onExportJson = { viewModel.exportJson(); drawerOpen = false },
            onImportJson = {
                imageSaver.loadJson { json -> viewModel.importPath(json) }
                drawerOpen = false
            },
            onInsertImage = {
                drawerOpen = false
                imageSaver.loadImage { bytes, intrinsicSize ->
                    // Drop the image at the world-space center of the current
                    // viewport so it lands somewhere visible regardless of pan
                    // or zoom. The viewport screenToWorld() does the inverse
                    // transform; we use the same ScreenCenter heuristic as the
                    // zoom toolbar since we don't have the canvas dimensions
                    // available here.
                    val world = state.viewport.screenToWorld(ScreenCenter)
                    viewModel.insertImage(bytes, intrinsicSize, world)
                }
            },
            onReplay = {
                drawerOpen = false
                replayOpen = true
            },
            onPickBgColor = { colorPickerForBg = true },
            onBgPatternSelected = { currentBgPattern = it },
            onToggleGrid = { showGrid.value = it },
            onClearCanvas = { viewModel.reset(); drawerOpen = false },
        )

        if (replayOpen) {
            ReplayScreen(
                elements = state.elements,
                bgColor = state.bgColor,
                onClose = { replayOpen = false },
            )
        }

        // Inline text editor — composed LAST so it layers above the toolbar
        // (frameless: same font/size/alignment/color as the element it edits,
        // no border, no Done button). The commit overlay is composed earlier,
        // between the canvas and the toolbar layer. Empty edits delete the
        // element. Esc reverts without committing (#83.5). No focus-loss
        // auto-commit: DrawBox re-grabs canvas focus on every press, so a
        // double-tap-to-edit (#83.2) briefly bounced focus and a focus-loss
        // commit fired instantly, closing the editor before the user could
        // type. Tap-away commit is handled by the overlay.
        val editingId = editingTextId
        if (editingId != null) {
            val target = state.elements.firstOrNull { it.id == editingId } as? Element.Text
            if (target == null) {
                editingTextId = null
            } else {
                // Wrapper fillMaxSize keeps InlineTextEditor's offset origin
                // identical to the canvas. No zIndex (composition order already
                // layers it above the toolbar) and no pointerInput, so taps
                // pass through to the ContextBar; only the field captures input.
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onPreviewKeyEvent { e ->
                            if (e.type == KeyEventType.KeyDown && e.key == Key.Escape) {
                                cancelTextEdit()
                                true
                            } else {
                                false
                            }
                        },
                ) {
                    InlineTextEditor(
                        element = target,
                        viewport = state.viewport,
                        draft = editDraft,
                        onDraftChange = { editDraft = it },
                    )
                }
            }
        }
    }
}

// Reasonable default focal point for toolbar-driven zoom. We don't know the
// canvas dimensions here, so pick a fixed-ish screen point; pinch and scroll
// gestures supply real focal points.
private val ScreenCenter = Offset(540f, 960f)
