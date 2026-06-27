package io.ak1.drawbox.presentation.reducer

import io.ak1.drawbox.domain.model.Element
import io.ak1.drawbox.domain.model.HISTORY_CAP
import io.ak1.drawbox.domain.model.Intent
import io.ak1.drawbox.domain.model.Mode
import io.ak1.drawbox.domain.model.State
import io.ak1.drawbox.domain.model.Viewport
import io.ak1.drawbox.domain.usecase.UseCase

/**
 * Pure state reducer implementing the MVI pattern.
 *
 * `(State, Intent) → State`. No side effects, no mutation of the input state.
 *
 * Undo model: snapshot-based. Each intent that mutates `elements` in a way the
 * user would expect to undo (insert, delete, recolor, z-order, transform-commit)
 * pushes the previous element list onto `history` and clears `future`. The
 * per-pixel update intents (`UpdateLatestPath`, `UpdateLatestShape`,
 * `MoveSelected`, `SetElementBounds`, `SetElementRotation`) do NOT push to
 * history — the caller is expected to dispatch [Intent.BeginTransform] once at
 * the start of the gesture.
 */
class Reducer(
    private val useCase: UseCase,
) {
    fun reduce(state: State, intent: Intent): State = when (intent) {
        is Intent.AddElement -> state.snapshot().copy(
            elements = useCase.addElement(intent.element, state.elements),
        )
        is Intent.UpdateElement -> state.copy(
            elements = useCase.updateElement(intent.element, state.elements),
        )
        is Intent.DeleteElement -> state.snapshot().copy(
            elements = useCase.deleteElement(intent.elementId, state.elements),
            selectedIds = state.selectedIds - intent.elementId,
        )
        is Intent.InsertNewPath -> {
            val newPath = useCase.insertNewPath(
                intent.offset,
                state.strokeColor,
                state.strokeWidth,
                state.opacity,
                state.currentItemStrokeStyle,
            )
            state.snapshot().copy(
                elements = useCase.addElement(newPath, state.elements),
            )
        }
        is Intent.UpdateLatestPath -> state.copy(
            elements = useCase.updateLatestPath(
                intent.newPoint, state.elements, intent.pressure,
            ),
        )
        is Intent.InsertText -> {
            val newText = useCase.insertText(
                intent.text,
                intent.position,
                intent.fontSize,
                intent.fontFamilyKey,
                intent.alignment,
                intent.color,
            )
            state.snapshot().copy(
                elements = useCase.addElement(newText, state.elements),
            )
        }
        is Intent.UpdateText -> state.snapshot().copy(
            elements = useCase.updateText(state.elements, intent.id, intent.text),
        )
        is Intent.SetSelectedFontSize -> {
            if (state.selectedIds.isEmpty()) state
            else state.snapshot().copy(
                elements = useCase.setSelectedFontSize(
                    state.elements, state.selectedIds, intent.size,
                ),
            )
        }
        is Intent.SetSelectedTextAlignment -> {
            if (state.selectedIds.isEmpty()) state
            else state.snapshot().copy(
                elements = useCase.setSelectedTextAlignment(
                    state.elements, state.selectedIds, intent.alignment,
                ),
            )
        }
        is Intent.SetSelectedFontFamily -> {
            if (state.selectedIds.isEmpty()) state
            else state.snapshot().copy(
                elements = useCase.setSelectedFontFamily(
                    state.elements, state.selectedIds, intent.fontFamilyKey,
                ),
            )
        }
        is Intent.SyncTextMeasuredHeight -> {
            // Short-circuit on no-op so identical layouts in successive
            // frames don't generate State copies (recompositions, listener
            // wakeups, sync-layer chatter). The 0.5px threshold matches the
            // renderer's drift gate.
            val target = state.elements.firstOrNull { it.id == intent.id } as? Element.Text
            if (target == null ||
                kotlin.math.abs(target.measuredHeight - intent.height) < 0.5f
            ) state
            else state.copy(
                elements = useCase.syncTextMeasuredHeight(
                    state.elements, intent.id, intent.height,
                ),
            )
        }
        is Intent.InsertImage -> {
            val newImage = useCase.insertImage(
                intent.bytes,
                intent.position,
                intent.intrinsicSize,
            )
            state.snapshot().copy(
                elements = useCase.addElement(newImage, state.elements),
            )
        }
        is Intent.InsertNewShape -> {
            val newShape = useCase.insertNewShape(
                intent.shapeType,
                intent.offset,
                state.strokeColor,
                state.strokeWidth,
                state.currentItemCornerRadius,
                state.currentItemStrokeStyle,
            )
            state.snapshot().copy(
                elements = useCase.addElement(newShape, state.elements),
            )
        }
        is Intent.UpdateLatestShape -> state.copy(
            elements = useCase.updateLatestShape(intent.newPoint, state.elements),
        )
        is Intent.SetStrokeColor -> state.copy(strokeColor = intent.color)
        is Intent.SetStrokeWidth -> state.copy(strokeWidth = intent.width)
        is Intent.SetCornerRadius -> state.copy(currentItemCornerRadius = intent.radius)
        is Intent.SetStrokeStyle -> state.copy(currentItemStrokeStyle = intent.style)
        is Intent.SetFontSize -> state.copy(currentItemFontSize = intent.size)
        is Intent.SetFontFamily -> state.copy(currentItemFontFamilyKey = intent.fontFamilyKey)
        is Intent.SetTextAlignment -> state.copy(currentItemTextAlignment = intent.alignment)
        is Intent.SetOpacity -> state.copy(opacity = intent.opacity)
        is Intent.SetBgColor -> state.copy(bgColor = intent.bgColor)
        is Intent.SetBackgroundPattern -> state.copy(bgPattern = intent.pattern)
        is Intent.SetMode -> {
            // Selection is meaningful only in SELECT mode. Switching away clears it.
            val nextSelected = if (intent.mode == Mode.SELECT) state.selectedIds else emptySet()
            state.copy(
                mode = intent.mode,
                selectedIds = nextSelected,
                marqueeRect = null,
            )
        }

        // Selection
        is Intent.SelectAt -> {
            val hit = useCase.hitTopmost(state.elements, intent.offset, intent.tolerance)
            state.copy(selectedIds = if (hit == null) emptySet() else setOf(hit.id))
        }
        is Intent.SetMarqueeRect -> state.copy(marqueeRect = intent.rect)
        is Intent.CommitMarquee -> state.copy(
            selectedIds = useCase.selectInRect(state.elements, intent.rect),
            marqueeRect = null,
        )
        is Intent.ClearSelection -> state.copy(selectedIds = emptySet())
        is Intent.DeleteSelected -> {
            if (state.selectedIds.isEmpty()) state
            else state.snapshot().copy(
                elements = useCase.deleteSelected(state.elements, state.selectedIds),
                selectedIds = emptySet(),
            )
        }
        is Intent.BeginTransform -> state.snapshot()
        is Intent.EndTransform -> state
        is Intent.MoveSelected -> state.copy(
            elements = useCase.propagateBindings(
                useCase.translateSelected(state.elements, state.selectedIds, intent.delta),
            ),
        )
        is Intent.SetElementBounds -> state.copy(
            elements = useCase.propagateBindings(
                useCase.setElementBounds(state.elements, intent.id, intent.bounds),
            ),
        )
        is Intent.SetElementRotation -> state.copy(
            elements = useCase.propagateBindings(
                useCase.setElementRotation(state.elements, intent.id, intent.rotationDegrees),
            ),
        )
        is Intent.SetElementPoints -> state.copy(
            elements = useCase.setElementPoints(state.elements, intent.id, intent.points),
        )
        is Intent.SetLineBend -> state.copy(
            elements = useCase.setLineBend(state.elements, intent.id, intent.bend),
        )
        is Intent.FinalizeArrowBindings -> state.copy(
            // Don't snapshot here — this intent is always dispatched at the END
            // of a gesture whose START already snapshotted (InsertNewShape for
            // a freshly drawn arrow, BeginTransform for an endpoint drag). A
            // second snapshot here would split a single user action across two
            // undo entries.
            elements = useCase.propagateBindings(
                useCase.finalizeArrowBindings(state.elements, intent.id),
            ),
        )
        is Intent.SetSelectedStrokeColor -> {
            if (state.selectedIds.isEmpty()) state
            else state.snapshot().copy(
                elements = useCase.setSelectedStrokeColor(
                    state.elements, state.selectedIds, intent.color,
                ),
            )
        }
        is Intent.SetSelectedFillColor -> {
            if (state.selectedIds.isEmpty()) state
            else state.snapshot().copy(
                elements = useCase.setSelectedFillColor(
                    state.elements, state.selectedIds, intent.color,
                ),
            )
        }
        is Intent.SetSelectedStrokeEnabled -> {
            if (state.selectedIds.isEmpty()) state
            else state.snapshot().copy(
                elements = useCase.setSelectedStrokeEnabled(
                    state.elements, state.selectedIds, intent.enabled,
                ),
            )
        }
        is Intent.SetSelectedStrokeWidth -> {
            if (state.selectedIds.isEmpty()) state
            else state.snapshot().copy(
                elements = useCase.setSelectedStrokeWidth(
                    state.elements, state.selectedIds, intent.width,
                ),
            )
        }
        is Intent.SetSelectedCornerRadius -> {
            if (state.selectedIds.isEmpty()) state
            else state.snapshot().copy(
                elements = useCase.setSelectedCornerRadius(
                    state.elements, state.selectedIds, intent.radius,
                ),
            )
        }
        is Intent.SetSelectedStrokeStyle -> {
            if (state.selectedIds.isEmpty()) state
            else state.snapshot().copy(
                elements = useCase.setSelectedStrokeStyle(
                    state.elements, state.selectedIds, intent.style,
                ),
            )
        }
        // Eraser
        is Intent.BeginErase -> {
            // Defensive reset: a previous session that was cancelled mid-sweep
            // without an EndErase would leave the dirty flag stuck.
            if (state.erasingSessionDirty) state.copy(erasingSessionDirty = false)
            else state
        }
        is Intent.EraseAt -> {
            // Snapshot lazily on the FIRST removal of the current session — a
            // tap or drag through empty space pushes nothing to history. The
            // eraseAt helper returns the same list instance when no element is
            // hit, so we can detect a miss by reference and short-circuit.
            val next = useCase.eraseAt(state.elements, intent.point, intent.radius)
            if (next === state.elements) state
            else {
                val base = if (!state.erasingSessionDirty) state.snapshot() else state
                base.copy(
                    elements = next,
                    erasingSessionDirty = true,
                    selectedIds = state.selectedIds.intersect(next.map { it.id }.toSet()),
                )
            }
        }
        is Intent.EndErase -> {
            if (state.erasingSessionDirty) state.copy(erasingSessionDirty = false)
            else state
        }
        is Intent.SetEraserSize -> state.copy(eraserSize = intent.size)

        is Intent.BringSelectionToFront -> {
            if (state.selectedIds.isEmpty()) state
            else state.snapshot().copy(
                elements = useCase.bringToFront(state.elements, state.selectedIds),
            )
        }
        is Intent.SendSelectionToBack -> {
            if (state.selectedIds.isEmpty()) state
            else state.snapshot().copy(
                elements = useCase.sendToBack(state.elements, state.selectedIds),
            )
        }

        // Camera / Viewport
        is Intent.PanBy -> state.copy(viewport = state.viewport.panBy(intent.delta))
        is Intent.ZoomBy -> state.copy(
            viewport = state.viewport.zoomBy(intent.factor, intent.focalScreen),
        )
        is Intent.ZoomTo -> state.copy(
            viewport = state.viewport.zoomTo(intent.targetScale, intent.focalScreen),
        )
        is Intent.ResetCamera -> state.copy(viewport = Viewport())
        is Intent.SetTempPan -> state.copy(tempPanActive = intent.active)

        // History
        is Intent.Undo -> {
            val prev = state.history.lastOrNull() ?: return@reduce state
            state.copy(
                elements = prev,
                history = state.history.dropLast(1),
                future = (state.future + listOf(state.elements)).takeLast(HISTORY_CAP),
                selectedIds = state.selectedIds.intersect(prev.map { it.id }.toSet()),
            )
        }
        is Intent.Redo -> {
            val next = state.future.lastOrNull() ?: return@reduce state
            state.copy(
                elements = next,
                future = state.future.dropLast(1),
                history = (state.history + listOf(state.elements)).takeLast(HISTORY_CAP),
                selectedIds = state.selectedIds.intersect(next.map { it.id }.toSet()),
            )
        }
        is Intent.Reset -> State()

        else -> state
    }

    /** Push current elements onto [State.history] and clear [State.future]. */
    private fun State.snapshot(): State = copy(
        history = (history + listOf(elements)).takeLast(HISTORY_CAP),
        future = emptyList(),
    )
}
