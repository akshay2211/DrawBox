# Migration Guide: DrawBox v2.0

This guide helps you migrate from the old callback-based API to the new state-driven MVI architecture, and introduces new v2.0 features like shapes and drawing modes.

---

## PNG Export: `saveBitmap()` → `exportPng()` (breaking in v3.0)

**v3.0 removes the on-screen bitmap-capture route and replaces it with a headless
raster exporter.** If you called `controller.saveBitmap()` or observed
`Event.PngSaved`, you must migrate.

### What was removed

| Removed | Replacement |
| --- | --- |
| `DrawBoxController.saveBitmap()` | `DrawBoxController.exportPng(...)` |
| `Event.PngSaved(bitmap, throwable)` | `Event.PngExported(bytes)` (+ `Event.Error` on failure) |
| `Intent.SaveBitmap` | *(internal — no longer needed)* |

### Why

The old route was a WYSIWYG screenshot bolted onto the render loop: it required
the canvas to be composed on screen, captured only the current viewport, and
handed back an unencoded `ImageBitmap`. `exportPng` is the raster sibling of
`exportSvg` — it renders the **entire scene** headlessly from `state.elements`
(no composition required), honours an explicit `scale`/`background`, clamps to a
pixel cap, and emits **already-encoded PNG bytes**.

### Before (v2.x)

```kotlin
// Trigger
controller.saveBitmap()

// Collect
LaunchedEffect(controller) {
    controller.events.collect { event ->
        if (event is Event.PngSaved) {
            event.bitmap?.let { saveImageBitmap(it) }
                ?: showError(event.throwable?.message)
        }
    }
}
```

### After (v3.0)

```kotlin
// A TextMeasurer is needed for real text glyphs in the headless raster.
// Omit it and text elements render as placeholder boxes.
val textMeasurer = rememberTextMeasurer()

// Trigger
controller.exportPng(
    scale = 2f,               // HiDPI multiplier (auto-clamped to the pixel cap)
    background = null,        // null → transparent backdrop
    textMeasurer = textMeasurer,
)

// Collect — event.bytes is ready to write/share as-is
LaunchedEffect(controller) {
    controller.events.collect { event ->
        when (event) {
            is Event.PngExported -> saveBytes(event.bytes) // e.g. file.writeBytes(...)
            is Event.Error -> showError(event.message)
            else -> {}
        }
    }
}
```

### Behavioural differences to expect

- Output covers the **whole drawing** (union of element bounds + padding), not the
  visible viewport. Pan/zoom no longer affect the result.
- The backdrop is **transparent by default**; pass `background = ...` to fill it.
- You receive **encoded PNG bytes**, so you no longer encode an `ImageBitmap` yourself.
- Export works even when the canvas is not on screen.

---

## What's New in v2.0

### ✨ New Features
- **Shape Drawing**: Draw rectangles, circles, triangles, arrows, and lines
- **Drawing Modes**: Switch between pen mode and shape modes
- **Smart Arrow Heads**: Arrow heads scale with stroke width and don't overlap the line
- **State-Based Architecture**: Fully reactive state management with MVI pattern
- **Serialization**: Export/import drawings as JSON

### 🎨 Drawing Modes
- **PEN**: Freehand drawing (v1.x behavior)
- **RECTANGLE**: Draw filled or stroked rectangles
- **CIRCLE**: Draw filled or stroked circles
- **TRIANGLE**: Draw triangles
- **ARROW**: Draw arrows with intelligent head sizing
- **LINE**: Draw straight lines

## Overview

- **Old API**: Uses `DrawController` with callbacks
- **New API**: Uses `DrawBoxController` with state flows
- **New Features**: Shape drawing and mode switching
- **Compatibility**: Architecture is fully backward compatible

## Migration Path

### Option 1: Gradual Migration (Recommended)
Keep using the old API while you transition. Both work simultaneously.

### Option 2: Full Migration
Switch to the new API immediately for better testability and architecture.

---

## Old API → New API

### Basic Drawing

**Before (Old API)**
```kotlin
@Composable
fun MyDrawingScreen() {
    val controller = rememberDrawController()
    
    DrawBox(
        drawController = controller,
        bitmapCallback = { bitmap, error ->
            bitmap?.let { saveBitmap(it) }
        },
        trackHistory = { undoCount, redoCount ->
            canUndo = undoCount > 0
            canRedo = redoCount > 0
        }
    )
    
    Button(onClick = { controller.unDo() }) {
        Text("Undo")
    }
}
```

**After (New API)**
```kotlin
@Composable
fun MyDrawingScreen() {
    val viewModel = rememberDrawingViewModel()
    val state by viewModel.state.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DrawingEvent.Error -> showError(event.message)
                else -> {}
            }
        }
    }
    
    DrawBox(
        state = state,
        onIntent = viewModel::onIntent,
    )
    
    Button(onClick = viewModel::undo, enabled = canUndo) {
        Text("Undo")
    }
}
```

### Color & Stroke Control

**Before (Old API)**
```kotlin
controller.changeColor(Color.Blue)
controller.changeStrokeWidth(15f)
controller.changeOpacity(0.8f)
```

**After (New API)**
```kotlin
viewModel.setColor(Color.Blue)
viewModel.setStrokeWidth(15f)
viewModel.setOpacity(0.8f)
```

### History Management

**Before (Old API)**
```kotlin
controller.unDo()
controller.reDo()
controller.reset()
```

**After (New API)**
```kotlin
viewModel.undo()
viewModel.redo()
viewModel.reset()
```

### Saving/Loading Drawings

**Before (Old API)**
```kotlin
// Old API doesn't have built-in persistence
val payload = controller.exportPath()
// Manual serialization needed
```

**After (New API)**
```kotlin
// Built-in support via events
viewModel.saveDrawing()

LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
        when (event) {
            is DrawingEvent.DrawingSaved -> showSuccess()
            is DrawingEvent.DrawingLoaded -> showLoaded()
            is DrawingEvent.Error -> showError(event.message)
            else -> {}
        }
    }
}
```

### PNG Export

> As of v3.0 this is the only PNG path. See
> [PNG Export: `saveBitmap()` → `exportPng()`](#png-export-savebitmap--exportpng-breaking-in-v30)
> above for the full rationale and behavioural differences.

**Before (Old API)**
```kotlin
DrawBox(
    drawController = controller,
    bitmapCallback = { bitmap, error ->
        if (bitmap != null) {
            saveBitmap(bitmap)
        } else {
            showError(error?.message ?: "Unknown error")
        }
    }
)
```

**After (New API)**
```kotlin
DrawBox(
    state = state,
    onIntent = viewModel::onIntent,
)

// Export the whole scene headlessly; collect the encoded bytes.
val textMeasurer = rememberTextMeasurer()
LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
        when (event) {
            is Event.PngExported -> saveBytes(event.bytes)
            is Event.Error -> showError(event.message)
            else -> {}
        }
    }
}

// …trigger from a button:
Button(onClick = { viewModel.exportPng(textMeasurer = textMeasurer) }) { Text("PNG") }
```

### Drawing Shapes (NEW in v2.0)

**Enable Shape Drawing**
```kotlin
@Composable
fun DrawingApp() {
    val viewModel = rememberDrawBoxController()
    val state by viewModel.state.collectAsState()
    var drawingMode by remember { mutableStateOf(DrawingMode.PEN) }

    // Shape selector UI
    ShapeSelector(state.mode) { newMode ->
        viewModel.setMode(newMode)
    }

    // Canvas with active mode
    DrawBox(
        state = state,
        onIntent = viewModel::onIntent,
    )
}
```

**Available Shapes**
```kotlin
// User can draw these shapes by selecting the mode
enum class DrawingMode {
    PEN,        // Freehand drawing
    RECTANGLE,  // Rectangle shape
    CIRCLE,     // Circle shape
    TRIANGLE,   // Triangle shape
    ARROW,      // Arrow with smart head sizing
    LINE,       // Straight line
}
```

**Shape Features**
- Shapes automatically use current stroke color and width
- Arrow heads scale proportionally with stroke width
- Line length is automatically reduced by arrow head depth
- All shapes support undo/redo

---

## Key Differences

| Aspect | Old API | New API |
|--------|---------|---------|
| **State** | Callback-based | Flow-based (reactive) |
| **Testing** | Hard to test | Easy to test (pure functions) |
| **Architecture** | Unstructured | Clean Architecture + MVI |
| **Persistence** | Manual | Built-in (repository pattern) |
| **History** | Manual tracking | Automatic (undo stack) |
| **Errors** | Via callback | Via Event flows |
| **Testability** | Limited | 95% testable code |

---

## Example: Complete Migration

### Step 1: Setup ViewModel
```kotlin
@Composable
fun DrawingApp() {
    val viewModel = rememberDrawingViewModel(
        useCase = DrawingUseCase(
            repository = DrawingRepositoryImpl(/* data source */)
        )
    )
    
    DrawingScreen(viewModel)
}
```

### Step 2: Observe State
```kotlin
@Composable
fun DrawingScreen(viewModel: DrawingViewModel) {
    val state by viewModel.state.collectAsState()
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    
    // Use state properties...
}
```

### Step 3: Render Drawing
```kotlin
DrawBox(
    state = state,
    onIntent = viewModel::onIntent,
    modifier = Modifier.fillMaxSize().weight(1f)
)
```

### Step 4: Handle Events
```kotlin
LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
        when (event) {
            is DrawingEvent.DrawingSaved -> showToast("Drawing saved")
            is DrawingEvent.DrawingLoaded -> showToast("Drawing loaded")
            is DrawingEvent.Error -> showError(event.message)
            else -> {}
        }
    }
}
```

### Step 5: Add Controls
```kotlin
Row {
    Button(onClick = viewModel::undo, enabled = canUndo) { Text("Undo") }
    Button(onClick = viewModel::redo, enabled = canRedo) { Text("Redo") }
    Button(onClick = viewModel::reset) { Text("Reset") }
    Button(onClick = { viewModel.setColor(Color.Blue) }) { Text("Blue") }
    Button(onClick = { viewModel.setStrokeWidth(15f) }) { Text("Thick") }
}
```

---

## Testing with New API

### Test Domain Logic
```kotlin
class DrawingUseCaseTest {
    @Test
    fun testAddElement() {
        val element = DrawingElement.Path(...)
        val result = useCase.addElement(element, emptyList())
        assertEquals(1, result.size)
    }
}
```

### Test State Reducer
```kotlin
class DrawingReducerTest {
    @Test
    fun testUndoIntent() {
        val path = createTestPath()
        val state = DrawingState(elements = listOf(path))
        val intent = DrawingIntent.Undo
        
        val newState = reducer.reduce(state, intent)
        
        assertTrue(newState.elements.isEmpty())
    }
}
```

### Test ViewModel (with Coroutines)
```kotlin
class DrawingViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()
    
    @Test
    fun testUndoFlow() = runTest {
        val viewModel = DrawingViewModel(useCase, reducer)
        viewModel.onIntent(DrawingIntent.Undo)
        
        val state = viewModel.state.first()
        assertTrue(state.elements.isEmpty())
    }
}
```

---

## FAQ

### Q: Should I migrate now or wait?
**A**: You can migrate gradually. Both APIs work side-by-side.

### Q: Is the old API deprecated?
**A**: No, it's maintained for backward compatibility. But new code should use the new API.

### Q: How do I inject the repository?
**A**: See ARCHITECTURE.md for DI examples.

### Q: Can I use Redux/MVI middleware?
**A**: Yes! The DrawingReducer is a pure function, so it works with any Redux middleware.

### Q: How do I add custom elements?
**A**: Extend `DrawingElement` sealed class and update the renderer. See ARCHITECTURE.md for details.

---

## Next Steps

1. Read [ARCHITECTURE.md](ARCHITECTURE.md) for detailed architecture documentation
2. Check the sample app for complete examples
3. Run tests to verify everything works: `./gradlew test`
4. Migrate one screen at a time for smooth transition