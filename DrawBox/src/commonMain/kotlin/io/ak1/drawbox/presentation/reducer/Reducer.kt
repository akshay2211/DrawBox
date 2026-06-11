package io.ak1.drawbox.presentation.reducer

import io.ak1.drawbox.domain.model.HISTORY_CAP
import io.ak1.drawbox.domain.model.Intent
import io.ak1.drawbox.domain.model.Mode
import io.ak1.drawbox.domain.model.State
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
            )
            state.snapshot().copy(
                elements = useCase.addElement(newPath, state.elements),
            )
        }
        is Intent.UpdateLatestPath -> state.copy(
            elements = useCase.updateLatestPath(intent.newPoint, state.elements),
        )
        is Intent.InsertNewShape -> {
            val newShape = useCase.insertNewShape(
                intent.shapeType,
                intent.offset,
                state.strokeColor,
                state.strokeWidth,
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
        is Intent.MoveSelected -> state.copy(
            elements = useCase.translateSelected(state.elements, state.selectedIds, intent.delta),
        )
        is Intent.SetElementBounds -> state.copy(
            elements = useCase.setElementBounds(state.elements, intent.id, intent.bounds),
        )
        is Intent.SetElementRotation -> state.copy(
            elements = useCase.setElementRotation(state.elements, intent.id, intent.rotationDegrees),
        )
        is Intent.SetSelectedStrokeColor -> {
            if (state.selectedIds.isEmpty()) state
            else state.snapshot().copy(
                elements = useCase.setSelectedStrokeColor(
                    state.elements, state.selectedIds, intent.color,
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
