package io.ak1.drawbox.presentation.reducer

import io.ak1.drawbox.domain.model.Intent
import io.ak1.drawbox.domain.model.State
import io.ak1.drawbox.domain.usecase.UseCase

/**
 * Pure state reducer implementing the MVI pattern.
 *
 * The reducer is the single source of truth for how [Intent] objects transform
 * the current [State] into a new [State]. It's a pure function with no side effects:
 *
 * ```
 * (State, Intent) → State
 * ```
 *
 * **Key Properties:**
 * - **Pure**: No side effects, same inputs always produce same outputs
 * - **Deterministic**: State changes are predictable and testable
 * - **Immutable**: Never modifies the input state; always returns a new instance
 * - **Complete**: Handles all possible intent types with exhaustive when expressions
 *
 * **Example:**
 * ```kotlin
 * val newState = reducer.reduce(currentState, Intent.InsertNewPath(offset))
 * // currentState is unchanged
 * // newState has a new path element
 * ```
 *
 * The reducer delegates business logic to the [UseCase] layer, which handles
 * complex operations like adding elements, updating paths, and managing history.
 *
 * @property useCase Domain layer use case for business logic
 *
 * @see Intent
 * @see State
 * @see UseCase
 */
class Reducer(
    private val useCase: UseCase,
) {
    /**
     * Transform state based on an intent.
     *
     * This is the core MVI function. Given a current state and an intent,
     * it produces a new state reflecting the desired change.
     *
     * @param state Current state before the intent
     * @param intent The user action or system event
     * @return New state after applying the intent
     *
     * @throws No exceptions - all cases are handled
     */
    fun reduce(state: State, intent: Intent): State {
        return when (intent) {
            is Intent.AddElement -> state.copy(
                elements = useCase.addElement(intent.element, state.elements),
                undoStack = emptyList(),
            )
            is Intent.UpdateElement -> state.copy(
                elements = useCase.updateElement(intent.element, state.elements),
            )
            is Intent.DeleteElement -> state.copy(
                elements = useCase.deleteElement(intent.elementId, state.elements),
            )
            is Intent.InsertNewPath -> {
                val newPath = useCase.insertNewPath(
                    intent.offset,
                    state.strokeColor,
                    state.strokeWidth,
                    state.opacity,
                )
                state.copy(
                    elements = useCase.addElement(newPath, state.elements),
                    undoStack = emptyList(),
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
                state.copy(
                    elements = useCase.addElement(newShape, state.elements),
                    undoStack = emptyList(),
                )
            }
            is Intent.UpdateLatestShape -> state.copy(
                elements = useCase.updateLatestShape(intent.newPoint, state.elements),
            )
            is Intent.SetStrokeColor -> state.copy(strokeColor = intent.color)
            is Intent.SetStrokeWidth -> state.copy(strokeWidth = intent.width)
            is Intent.SetOpacity -> state.copy(opacity = intent.opacity)
            is Intent.SetBgColor -> state.copy(bgColor = intent.bgColor)
            // Replaces any existing pattern; null clears it. Layered above bgColor at render time.
            is Intent.SetBackgroundPattern -> state.copy(bgPattern = intent.pattern)
            is Intent.SetMode -> state.copy(mode = intent.mode)
            is Intent.Undo -> {
                val (newElements, newUndoStack) = useCase.undo(state.elements, state.undoStack)
                state.copy(elements = newElements, undoStack = newUndoStack)
            }
            is Intent.Redo -> {
                val (newElements, newUndoStack) = useCase.redo(state.elements, state.undoStack)
                state.copy(elements = newElements, undoStack = newUndoStack)
            }
            is Intent.Reset -> State()
            else -> { state }
        }
    }
}
