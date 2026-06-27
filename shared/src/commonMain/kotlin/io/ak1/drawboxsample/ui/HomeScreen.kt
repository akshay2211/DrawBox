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
import io.ak1.drawboxsample.save.rememberImageSaver
import io.ak1.drawboxsample.ui.components.BgPatternPreset
import io.ak1.drawboxsample.ui.components.ColorPickerDialog
import io.ak1.drawboxsample.ui.components.ContextBar
import io.ak1.drawboxsample.ui.components.ControlsBar
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
    val showShapeStroke = isShapeMode || selectedHasStrokeable
    val showCornerRadius = state.mode == Mode.RECTANGLE || state.mode == Mode.TRIANGLE || selectedRoundable.isNotEmpty()
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

    // Text controls — surfaced in TEXT mode (pre-configure the next insert)
    // OR when a single Text element is selected (edit existing). Multi-text
    // editing is out of scope for v1 — the ContextBar would have to merge
    // possibly-conflicting style values.
    val selectedTexts = selectedDrawables.filterIsInstance<Element.Text>()
    val singleSelectedText = selectedTexts.singleOrNull()?.takeIf { selectedDrawables.size == 1 }
    val showTextControls = isTextMode || singleSelectedText != null
    val showEditText = singleSelectedText != null
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

    // True while the user is actively touching/dragging on the canvas. Drives
    // auto-collapse of floating bars so they get out of the way during drawing.
    var isDrawingActive by remember { mutableStateOf(false) }
    val barsExpanded = !isDrawingActive

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
                // Peek pointer events on the Initial pass so we can flip
                // isDrawingActive without consuming events from DrawBox's
                // own gesture handlers.
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            when (event.type) {
                                PointerEventType.Press -> isDrawingActive = true
                                PointerEventType.Release -> isDrawingActive = false
                                else -> Unit
                            }
                        }
                    }
                },
            showGrid = showGrid.value,
            // While the inline editor is open over a text element, tell the
            // SDK to skip both the element render and its selection chrome
            // so the editor's frame is the only one visible (no ghosting).
            hiddenElementIds = editingTextId?.let { setOf(it) } ?: emptySet(),
        )
        BoxWithConstraints(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            val isNarrow = maxWidth < 600.dp


            // Top-right unified context bar — merges shape config + selection
            // actions into one pill. Rendered only when there's something to show.
            val barVisible = showShapeStroke || showCornerRadius || showFill ||
                showStrokeToggle || showTextControls || hasSelection
            if (barVisible) {
                ContextBar(
                    isShapeMode = isShapeMode,
                    hasSelection = hasSelection,
                    showShapeStroke = showShapeStroke,
                    showCornerRadius = showCornerRadius,
                    showFill = showFill,
                    showStrokeToggle = showStrokeToggle,
                    showTextControls = showTextControls,
                    showEditText = showEditText,
                    currentColor = currentShapeColor,
                    currentFillColor = currentFillColor,
                    currentStrokeEnabled = currentStrokeEnabled,
                    currentStrokeStyle = currentStrokeStyle,
                    currentStrokeWidth = currentStrokeWidth,
                    currentCornerRadius = currentRadius,
                    currentFontSize = currentFontSize,
                    currentTextAlignment = currentTextAlignment,
                    currentFontFamilyKey = currentFontFamilyKey,
                    fontFamilyKeys = fontFamilyKeys,
                    expanded = barsExpanded,
                    onColorChange = { color ->
                        if (hasSelection) viewModel.setSelectionColor(color)
                        else viewModel.setColor(color)
                    },
                    onFillColorChange = { color -> viewModel.setSelectionFillColor(color) },
                    onStrokeEnabledChange = { enabled -> viewModel.setSelectionStrokeEnabled(enabled) },
                    onStrokeStyleChange = { style ->
                        if (hasSelection) viewModel.setSelectionStrokeStyle(style)
                        else viewModel.setStrokeStyle(style)
                    },
                    onStrokeWidthChange = { w ->
                        if (hasSelection) viewModel.setSelectionStrokeWidth(w)
                        else viewModel.setStrokeWidth(w)
                    },
                    onCornerRadiusChange = { r ->
                        if (selectedRoundable.isNotEmpty()) viewModel.setSelectionCornerRadius(r)
                        else viewModel.setCornerRadius(r)
                    },
                    onFontSizeChange = { size ->
                        // Selected text → mutate that element. TEXT mode with
                        // no selection → mutate the State default so the next
                        // insert picks it up.
                        if (singleSelectedText != null) viewModel.setSelectionFontSize(size)
                        else viewModel.setFontSize(size)
                    },
                    onTextAlignmentChange = { align ->
                        if (singleSelectedText != null) viewModel.setSelectionTextAlignment(align)
                        else viewModel.setTextAlignment(align)
                    },
                    onFontFamilyChange = { key ->
                        if (singleSelectedText != null) viewModel.setSelectionFontFamily(key)
                        else viewModel.setFontFamily(key)
                    },
                    onEditText = {
                        singleSelectedText?.let { target ->
                            editingTextId = target.id
                            editDraft = target.text
                        }
                    },
                    onBringToFront = { viewModel.bringSelectionToFront() },
                    onSendToBack = { viewModel.sendSelectionToBack() },
                    onDelete = { viewModel.deleteSelected() },
                    onClear = { viewModel.clearSelection() },
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 72.dp, end = 12.dp),
                )
            }

            // Top-right cluster: zoom (only narrow), theme toggle, settings.
            TopRightControls(
                isNarrow = isNarrow,
                scalePercent = state.viewport.scalePercent,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                onZoomIn = { viewModel.zoomBy(1.25f, ScreenCenter) },
                onZoomOut = { viewModel.zoomBy(0.8f, ScreenCenter) },
                onZoomReset = { viewModel.resetCamera() },
                onSettingsClick = { drawerOpen = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp),
                expanded = barsExpanded,
            )

            // Bottom-left zoom (wide only). Narrow folds zoom into the top-right cluster.
            if (!isNarrow) {
                ZoomToolbar(
                    scalePercent = state.viewport.scalePercent,
                    onZoomIn = { viewModel.zoomBy(1.25f, ScreenCenter) },
                    onZoomOut = { viewModel.zoomBy(0.8f, ScreenCenter) },
                    onZoomReset = { viewModel.resetCamera() },
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 24.dp),
                    expanded = barsExpanded,
                )
            }

            // Bottom-center main NavBar.
            ControlsBar(
                canUndo = canUndo,
                canRedo = canRedo,
                currentMode = state.mode,
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onModeSelected = { viewModel.setMode(it) },
                expanded = barsExpanded,
            )



            if (colorPickerForBg) {
                ColorPickerDialog(
                    initialColor = state.bgColor,
                    onDismiss = { colorPickerForBg = false },
                    onColorSelected = { viewModel.setBgColor(it) },
                )
            }

            if (showColorDialog.value) {
                ColorPickerDialog(
                    initialColor = state.strokeColor,
                    onDismiss = { showColorDialog.value = false },
                    onColorSelected = { color -> viewModel.setColor(color) },
                )
            }
        }
        // Right-side modal settings drawer.
        SettingsDrawer(
            visible = drawerOpen,
            showGrid = showGrid.value,
            currentBgColor = state.bgColor,
            currentBgPattern = currentBgPattern,
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
