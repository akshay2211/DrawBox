# DrawBox Architecture

DrawBox is built with **Clean Architecture + MVI (Model-View-Intent)** pattern for maximum testability, maintainability, and extensibility.

## 🏗️ Architecture Layers

### **1. Domain Layer** (Pure Business Logic)
- **Location**: `domain/`
- **Dependencies**: None (zero external dependencies)
- **Responsibility**: Core business rules and use cases

**Key Files:**
- `model/` - Data models (Element, State, Intent, Event, Mode, ShapeType)
- `repository/` - Repository interfaces (contracts)
- `usecase/` - Business logic (UseCase)

**Why Separate**: Ensures the core logic is testable and framework-agnostic.

```kotlin
// Example: Domain is pure Kotlin
val newElements = useCase.addElement(element, currentElements)
// No Compose, no Android, no framework dependencies
```

### **2. Data Layer** (Persistence)
- **Location**: `data/`
- **Dependencies**: Domain layer only
- **Responsibility**: Serialization, storage, repository implementation

**Key Files:**
- `serialization/` - JSON serialization for drawings
- `repository/` - Repository implementations

**Why Separate**: Isolates persistence concerns, easy to switch storage backends.

### **3. Presentation Layer** (UI State Management)
- **Location**: `presentation/`
- **Dependencies**: Domain + Compose
- **Responsibility**: State management, side effects, UI logic

**Key Files:**
- `reducer/` - Pure state reducer (Intent → State)
- `viewmodel/` - ViewModel (orchestrates logic and side effects)

**Why Separate**: Clear MVI pattern ensures one-way data flow.

## 📊 Data Flow (MVI Pattern)

```
User Action (Tap/Drag)
    ↓
Intent (InsertNewPath, InsertNewShape, SetMode, etc.)
    ↓
Reducer (reduce state based on intent)
    ↓
New State (updated elements, mode, colors, etc.)
    ↓
DrawBox Composable (reads state)
    ↓
UI Updated
```

### Example Flow: Switching to Rectangle Mode

```kotlin
// 1. User clicks rectangle button in ShapeSelector
ShapeSelector(selectedShape) { newMode ->
    viewModel.setMode(newMode)  // SetMode intent
}

// 2. Intent → Reducer
fun reduce(state, intent): State {
    return when (intent) {
        is Intent.SetMode -> state.copy(mode = intent.mode)
        ...
    }
}

// 3. DrawBox reads new mode
val isPenMode = state.mode == Mode.PEN
val shapeType = when (state.mode) {
    Mode.RECTANGLE -> ShapeType.RECTANGLE
    // ... other shapes
    Mode.PEN -> null
}

// 4. User drags on canvas - creates shape instead of path
onDragGestures { 
    if (isPenMode) 
        onIntent(Intent.InsertNewPath(offset))
    else 
        onIntent(Intent.InsertNewShape(shapeType, offset))
}
```

## 🎨 Drawing Modes and Shapes

### Drawing Modes (`Mode` sealed class)
Controls what gets drawn when user interacts with the canvas:
- **PEN**: Freehand drawing (creates Path elements)
- **RECTANGLE**: Rectangle shape (creates Shape element)
- **CIRCLE**: Circle shape (creates Shape element)
- **TRIANGLE**: Triangle shape (creates Shape element)
- **ARROW**: Arrow shape with scalable head (creates Shape element)
- **LINE**: Straight line (creates Shape element)

### Shape Types (`ShapeType` sealed class)
Defines how shapes are rendered. Used by `Element.Shape`:
- **RECTANGLE**: Drawn with `drawRect()`
- **CIRCLE**: Drawn with `drawCircle()` using center and radius
- **TRIANGLE**: Drawn as isosceles triangle from start to end points
- **ARROW**: Arrow with head that scales with stroke width, line length reduced by arrow depth
- **LINE**: Straight line between two points

### Elements (`Element` sealed class)
- **Path**: Freehand drawing with points, stroke color, width, opacity
- **Shape**: Geometric shapes with stroke/fill colors and stroke width

## 🧪 Testing Strategy

Each layer is independently testable:

### Domain Layer Tests
```kotlin
// domain/usecase/DrawingUseCaseTest.kt
// No Compose, no Context, pure Kotlin functions
val result = useCase.addElement(element, currentElements)
assertEquals(1, result.size)
```

### Presentation Layer Tests
```kotlin
// presentation/reducer/DrawingReducerTest.kt
// Pure reducer logic testing
val newState = reducer.reduce(state, intent)
assertEquals(1, newState.elements.size)
```

### UI Layer Tests
- Test composables separately (optional - UI logic is in reducer/viewmodel)
- Most logic is testable via domain and presentation layers

## 🎯 Adding New Features

### Adding a New Shape Type

**Step 1: Add to ShapeType**
```kotlin
// domain/model/ShapeType.kt
sealed class ShapeType {
    data object RECTANGLE : ShapeType()
    data object CIRCLE : ShapeType()
    data object TRIANGLE : ShapeType()
    data object ARROW : ShapeType()
    data object LINE : ShapeType()
    data object HEXAGON : ShapeType()  // New shape
}
```

**Step 2: Add to Drawing Mode (if user selectable)**
```kotlin
// domain/model/Mode.kt
sealed class Mode {
    data object PEN : Mode()
    data object HEXAGON : Mode()  // New mode
    // ... other modes
}
```

**Step 3: Update DrawBox rendering**
```kotlin
// DrawBox.kt - drawShape function
ShapeType.HEXAGON -> {
    drawHexagon(topLeft, width, height, shape)
}
```

**Step 4: Add rendering function**
```kotlin
private fun DrawScope.drawHexagon(
    topLeft: Offset,
    width: Float,
    height: Float,
    shape: Element.Shape,
) {
    // Implementation
}
```

**Step 5: Update mode mapping in DrawBox**
```kotlin
val shapeType = when (state.mode) {
    Mode.HEXAGON -> ShapeType.HEXAGON
    // ... other modes
    Mode.PEN -> null
}
```

### Adding a New Intent Type

**Step 1: Add Intent**
```kotlin
// domain/model/Intent.kt
sealed class Intent {
    data class SetStrokeColor(val color: Color) : Intent()
    data class SetCustomProperty(val property: String) : Intent()
    // ... other intents
}
```

**Step 2: Update State**
```kotlin
// domain/model/State.kt
data class State(
    val strokeColor: Color = Color.Red,
    val customProperty: String = "",
    // ... other properties
)
```

**Step 3: Handle in Reducer**
```kotlin
// presentation/reducer/Reducer.kt
is Intent.SetCustomProperty -> state.copy(customProperty = intent.property)
```

**Step 4: Add ViewModel convenience method**
```kotlin
// presentation/viewmodel/DrawBoxController.kt
fun setCustomProperty(property: String) = onIntent(Intent.SetCustomProperty(property))
```

## 🔧 State Management

### State Properties

```kotlin
data class State(
    val elements: List<Element> = emptyList(),        // All drawn elements
    val undoStack: List<Element> = emptyList(),       // For redo functionality
    val strokeColor: Color = Color.Red,               // Current stroke color
    val strokeWidth: Float = 10f,                     // Current stroke width
    val opacity: Float = 1f,                          // Current opacity (0-1)
    val bgColor: Color = Color.Black,                 // Background color
    val mode: Mode = Mode.PEN,                        // Current drawing mode
)
```

### Intent Types

**Element Operations:**
- `AddElement(element)` - Add element to canvas
- `UpdateElement(element)` - Update existing element
- `DeleteElement(elementId)` - Delete element

**Path Operations:**
- `InsertNewPath(offset)` - Start new freehand path
- `UpdateLatestPath(newPoint)` - Add point to current path

**Shape Operations:**
- `InsertNewShape(shapeType, offset)` - Start new shape
- `UpdateLatestShape(newPoint)` - Update shape dimensions

**Style Operations:**
- `SetStrokeColor(color)` - Change stroke color
- `SetStrokeWidth(width)` - Change stroke width
- `SetOpacity(opacity)` - Change opacity
- `SetBgColor(bgColor)` - Change background color
- `SetMode(mode)` - Change drawing mode

**History:**
- `Undo` - Undo last action
- `Redo` - Redo last undone action
- `Reset` - Clear all elements

**Persistence:**
- `SaveBitmap(bitmap, throwable)` - Save drawing as bitmap
- `LoadDrawing` - Load drawing from storage

## 📚 Key Concepts

### Element (Sealed Class)
Base type for all drawable objects. Two subclasses:
- **Path**: Freehand curves with multiple points
- **Shape**: Geometric shapes (rectangle, circle, arrow, etc.)

### Intent (Sealed Class)
Represents user actions and system events. One-way data flow: Intent → Reducer → State.

### State (Data Class)
Immutable snapshot of current drawing state. All state changes create new instances (no mutations).

### Mode (Sealed Class)
Determines what type of drawing is created from user gestures (PEN, RECTANGLE, CIRCLE, TRIANGLE, ARROW, LINE).

### ShapeType (Sealed Class)
Determines how Element.Shape is rendered (RECTANGLE, CIRCLE, TRIANGLE, ARROW, LINE).

### Reducer (Pure Function)
Transforms state based on intent. Pure function: `(State, Intent) → State`

### DrawBoxController (ViewModel)
Manages state and side effects. Orchestrates domain logic, emits events, handles persistence.

## ✅ Architecture Benefits

| Benefit | Why |
|---------|-----|
| **Testability** | 95% of code has zero framework dependencies |
| **Maintainability** | Clear layer boundaries, easy to navigate |
| **Reusability** | Domain logic usable in any UI framework |
| **Extensibility** | Add new elements/features without breaking existing code |
| **Debuggability** | State changes are predictable and trackable |
| **Decoupling** | Layers don't depend on each other's implementations |
| **Hot Mode Switching** | Change drawing mode at any time without state loss |
| **Shape Scaling** | Arrow head scales with stroke width automatically |