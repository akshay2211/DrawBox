@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.ak1.drawboxsample.ui

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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
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
    val themedBg = MaterialTheme.colorScheme.background
    val themedBrush = MaterialTheme.colorScheme.primary

    val viewModel = rememberDrawBoxController(
        initialState = State(strokeColor = themedBrush),
    )
    val state by viewModel.state.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    // Canvas bg follows the active theme. Re-applies whenever the resolved
    // background color changes (theme toggle, SYSTEM follow flips, etc.).
    LaunchedEffect(themedBg) { viewModel.setBgColor(themedBg) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is Event.PngSaved -> {
                    if (event.bitmap != null) imageSaver.savePng(event.bitmap!!)
                    else println("error ${event.throwable}")
                }

                is Event.SvgExported -> imageSaver.saveSvg(event.svg)
                is Event.JsonExported -> imageSaver.saveJson(event.json)
                else -> {}
            }
        }
    }

    // Debug print: dump selected element data on every selection change so the
    // bbox the chrome draws can be compared to the actual element data.
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
    // OR when a single Text element is selected (edit existing). Multi-text
    // editing is out of scope for v1 — the ContextBar would have to merge
    // possibly-conflicting style values.
    val selectedTexts = selectedDrawables.filterIsInstance<Element.Text>()
    val singleSelectedText = selectedTexts.singleOrNull()?.takeIf { selectedDrawables.size == 1 }
    // Current values follow the selected element when present; otherwise
    // fall back to the State defaults the next insert will use.
    val currentFontSize = singleSelectedText?.fontSize ?: state.currentItemFontSize
    val currentTextAlignment = singleSelectedText?.alignment ?: state.currentItemTextAlignment
    val currentFontFamilyKey = singleSelectedText?.fontFamilyKey ?: state.currentItemFontFamilyKey
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
    var editingTextId by remember { mutableStateOf<String?>(null) }
    // Draft is hoisted up here so the outside-tap overlay can read the
    // latest value when committing — no focus-event juggling needed.
    var editDraft by remember { mutableStateOf("") }
    LaunchedEffect(state.elements) {
        if (editingTextId != null) return@LaunchedEffect
        val last = state.elements.lastOrNull()
        if (last is Element.Text && last.text.isEmpty()) {
            editingTextId = last.id
            editDraft = ""
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

    // Apply background pattern to the canvas. Tinted against onSurface so the
    // same drawable reads on both light and dark canvases.
    val patternPainter = currentBgPattern?.let { painterResource(it.drawable) }
    val patternTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    LaunchedEffect(patternPainter, patternTint) {
        viewModel.setBackgroundPattern(patternPainter, patternTint)
    }


    Scaffold { _ ->

        DrawBox(
            state = state,
            onIntent = viewModel::onIntent,
            modifier = Modifier.fillMaxSize().clipToBounds()
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
        BoxWithConstraints(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            val isNarrow = maxWidth < 600.dp


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
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 76.dp),
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
                        showText = singleSelectedText != null,
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
                            }
                            ContextBarIntent.BringToFront -> viewModel.bringSelectionToFront()
                            ContextBarIntent.SendToBack -> viewModel.sendSelectionToBack()
                            ContextBarIntent.Delete -> viewModel.deleteSelected()
                            ContextBarIntent.ClearSelection -> viewModel.clearSelection()
                        }
                    },
                    fontFamilyResolver = { key -> io.ak1.drawbox.text.FontRegistry.resolve(key) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 72.dp, end = 12.dp),
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
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp),
            )

            // Bottom-left zoom (wide only). Narrow folds zoom into the top-right cluster.
            if (!isNarrow) {
                ZoomToolbar(
                    scalePercent = state.viewport.scalePercent,
                    onZoomIn = { viewModel.zoomBy(1.25f, ScreenCenter) },
                    onZoomOut = { viewModel.zoomBy(0.8f, ScreenCenter) },
                    onZoomReset = { viewModel.resetCamera() },
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 24.dp),
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
                    .padding(bottom = 24.dp),
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
            onDownloadPng = { viewModel.saveBitmap(); drawerOpen = false },
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

        // Inline text editor + outside-tap commit overlay. The editor itself
        // renders frameless — same font/size/alignment/color as the element
        // it'll become, no border, no Done button. The full-screen overlay
        // below it captures any tap outside the field and triggers commit.
        // Empty commits delete the element.
        val editingId = editingTextId
        if (editingId != null) {
            val target = state.elements.firstOrNull { it.id == editingId } as? Element.Text
            if (target == null) {
                editingTextId = null
            } else {
                // Click-catcher: positioned over the canvas + toolbars so any
                // outside tap routes through here. Transparent — the canvas
                // remains visible underneath. Blocks ContextBar interaction
                // during edit by design (no mid-edit style changes); the
                // user picks style first, then types.
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        // Layer 2: above the canvas + toolbars (which sit at
                        // the default z 0), below the editor (z 10).
                        .zIndex(9f)
                        .fillMaxSize()
                        .pointerInput(editingId) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type == PointerEventType.Press) {
                                        commitTextEdit()
                                        break
                                    }
                                }
                            }
                        },
                )
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

// Reasonable default focal point for toolbar-driven zoom. We don't know the
// canvas dimensions here, so pick a fixed-ish screen point; pinch and scroll
// gestures supply real focal points.
private val ScreenCenter = Offset(540f, 960f)
