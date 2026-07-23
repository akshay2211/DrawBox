# DrawBox Architecture

## Overview

DrawBox uses the **MVI (Model-View-Intent)** architecture pattern, a unidirectional data flow architecture that ensures predictable state management and easy testing.

## MVI Pattern Explained

The MVI pattern follows a strict cycle:

```
User Interaction
      ↓
   Intent (Action)
      ↓
   Reducer (Business Logic)
      ↓
   New State
      ↓
    UI Update
      ↓
   (Back to User Interaction)
```

### Benefits of MVI

✅ **Predictable** - State changes follow a strict flow
✅ **Testable** - Each component is independently testable
✅ **Debuggable** - Easy to trace state changes
✅ **Immutable** - No side effects or hidden mutations
✅ **Scalable** - Works great as complexity grows

## Core Components

### 1. Model (State)

The **State** represents the complete, immutable snapshot of the drawing canvas at any point in time.

```kotlin
data class State(
    val elements: List<Element> = emptyList(),      // All drawn elements
    val undoStack: List<Element> = emptyList(),     // Redo history
    val strokeColor: Color = Color.Red,             // Current stroke color
    val strokeWidth: Float = 10f,                   // Current stroke width
    val opacity: Float = 1f,                        // Current opacity (0.0-1.0)
    val bgColor: Color = Color.Black,               // Canvas background
    val mode: Mode = Mode.PEN                       // Current drawing mode
)
```

**Key Principle**: State is immutable. Every state change produces a new `State` object.

```kotlin
// ❌ WRONG - Mutating state
state.strokeColor = Color.Blue

// ✅ CORRECT - Creating new state
val newState = state.copy(strokeColor = Color.Blue)
```

### 2. View (UI)

The **View** layer displays the current state and sends user interactions as intents.

```kotlin
@Composable
fun DrawBox(
    state: State,                      // Current state to display
    onIntent: (Intent) -> Unit,        // Send intents to business logic
    modifier: Modifier = Modifier
)
```

The View observes state changes via `collectAsState()`:

```kotlin
@Composable
fun DrawingScreen() {
    val controller = rememberDrawBoxController()
    val state by controller.state.collectAsState()  // Observe state
    
    DrawBox(
        state = state,
        onIntent = controller::onIntent
    )
}
```

### 3. Intent

**Intent** represents a user action or system event that should change the state.

Common intents include:

```kotlin
// Drawing operations
Intent.AddElement(element)              // Add a new element
Intent.UpdateElement(element)           // Update existing element
Intent.DeleteElement(elementId)         // Delete element

// Path drawing (freehand)
Intent.InsertNewPath(offset)           // Start new path
Intent.UpdateLatestPath(newPoint)       // Add point to path

// Shape drawing
Intent.InsertNewShape(shapeType, offset)  // Start new shape
Intent.UpdateLatestShape(newPoint)        // Update shape

// Style changes
Intent.SetStrokeColor(color)            // Change stroke color
Intent.SetStrokeWidth(width)            // Change stroke width
Intent.SetOpacity(opacity)              // Set transparency
Intent.SetBgColor(bgColor)              // Change background
Intent.SetMode(mode)                    // Switch drawing mode

// History
Intent.Undo()                           // Undo last action
Intent.Redo()                           // Redo undone action
Intent.Reset()                          // Clear canvas
```

### 4. Reducer

The **Reducer** is pure business logic that takes the current state and an intent, and produces a new state.

```kotlin
fun reduce(
    state: State,
    intent: Intent
): State {
    return when (intent) {
        is Intent.SetStrokeColor -> 
            state.copy(strokeColor = intent.color)
        
        is Intent.SetMode -> 
            state.copy(mode = intent.mode)
        
        is Intent.AddElement -> 
            state.copy(elements = state.elements + intent.element)
        
        is Intent.Undo -> {
            if (state.undoStack.isEmpty()) return state
            val lastElement = state.undoStack.last()
            state.copy(
                elements = state.elements.dropLast(1),
                undoStack = state.undoStack.dropLast(1)
            )
        }
        
        // ... more intents
    }
}
```

**Reducer Principles**:
- 🔒 Pure function - no side effects
- 🎯 Deterministic - same input = same output
- 📝 Readable - easy to understand state changes

## Data Flow Example

Here's how a complete user interaction flows through the system:

```
User touches canvas to start drawing
            ↓
View detects touch event
            ↓
View sends Intent.InsertNewPath(offset)
            ↓
DrawBoxController receives intent
            ↓
Reducer.reduce(currentState, intent)
            ↓
Reducer returns newState with new Element.Path
            ↓
State flow emits new state
            ↓
Composable observes state change via collectAsState()
            ↓
DrawBox recomposes and renders new element
            ↓
User sees their stroke appear on canvas
```

## Drawing Modes

DrawBox supports 6 different drawing modes:

```kotlin
sealed class Mode {
    data object PEN : Mode()           // Freehand drawing
    data object RECTANGLE : Mode()     // Rectangular shapes
    data object CIRCLE : Mode()        // Circular shapes
    data object TRIANGLE : Mode()      // Triangular shapes
    data object ARROW : Mode()         // Arrows with smart sizing
    data object LINE : Mode()          // Straight lines
}
```

Each mode determines what type of `Element` is created:

| Mode | Element Type | User Interaction |
|------|-------------|-----------------|
| PEN | Element.Path | Continuous drag creates path points |
| RECTANGLE | Element.Shape | Drag from corner creates rectangle |
| CIRCLE | Element.Shape | Drag creates circle |
| TRIANGLE | Element.Shape | Drag creates triangle |
| ARROW | Element.Shape | Drag creates arrow with smart head |
| LINE | Element.Shape | Drag creates line |

## Elements

Elements are the drawable primitives on the canvas:

```kotlin
sealed class Element {
    // Freehand path - series of connected points
    data class Path(
        val id: String,
        val points: List<Offset>,
        val color: Color,
        val width: Float,
        val opacity: Float
    ) : Element()
    
    // Shape - rectangle, circle, triangle, arrow, or line
    data class Shape(
        val id: String,
        val shapeType: ShapeType,
        val start: Offset,
        val end: Offset,
        val color: Color,
        val width: Float,
        val opacity: Float
    ) : Element()
}

enum class ShapeType {
    RECTANGLE, CIRCLE, TRIANGLE, ARROW, LINE
}
```

## State Management Flow

```
┌─────────────────────────────────────┐
│     DrawBoxController (ViewModel)    │
│  - Holds state as StateFlow          │
│  - Receives intents via onIntent()   │
│  - Calls reducer with intent         │
│  - Emits new state                   │
└──────────┬──────────────────────────┘
           │
           │ StateFlow<State>
           │
┌──────────▼──────────────────────────┐
│   Compose UI (DrawBox)               │
│  - Observes state via collectAsState │
│  - Sends user intents to controller  │
│  - Re-renders on state change        │
└─────────────────────────────────────┘
```

## Events

Beyond state, DrawBox emits **Events** for side effects that don't fit into the state model:

```kotlin
sealed class Event {
    data class ElementAdded(val element: Element) : Event()
    data class ElementUpdated(val element: Element) : Event()
    data class ElementDeleted(val elementId: String) : Event()
    data class HistoryChanged(val canUndo: Boolean, val canRedo: Boolean) : Event()
    data class SvgExported(val svg: String) : Event()
    data class PngExported(val bytes: ByteArray) : Event()
    data class DrawingLoaded(val state: State) : Event()
    data class Error(val message: String, val throwable: Throwable? = null) : Event()
}
```

Listen to events:

```kotlin
controller.events.collect { event ->
    when (event) {
        is Event.SvgExported -> saveFile(event.svg)
        is Event.PngExported -> saveFile(event.bytes)
        is Event.Error -> showErrorDialog(event.message)
        else -> {}
    }
}
```

## Why This Architecture Matters

### Before (without MVI)
```kotlin
// ❌ Hard to track state changes
canvas.drawPath(...)
canvas.setColor(...)
undoHistory.add(...)  // Oops, forgot to add before drawing!
redo()  // What happens now?
```

### After (with MVI)
```kotlin
// ✅ Clear, predictable flow
onIntent(Intent.SetColor(Color.Blue))
onIntent(Intent.InsertNewPath(offset))
onIntent(Intent.UpdateLatestPath(newPoint))
onIntent(Intent.Undo())  // Exactly reverses the last operation
```

---

**Next**: Learn about [DrawBoxController API](../api-reference/controller.md)