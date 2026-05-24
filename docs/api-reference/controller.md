# DrawBoxController API Reference

## Overview

The **DrawBoxController** is the main interface for interacting with DrawBox. It manages state, handles intents, and provides methods for drawing operations, customization, and export.

## Creating a Controller

### Using rememberDrawBoxController

The simplest way to create and remember a controller across recompositions:

```kotlin
@Composable
fun MyDrawingScreen() {
    val controller = rememberDrawBoxController()
    // controller is retained across recompositions
}
```

### Manual Creation

```kotlin
val controller = DrawBoxController()
```

## Observable State

### Observing Drawing State

```kotlin
@Composable
fun DrawingScreen() {
    val controller = rememberDrawBoxController()
    val state by controller.state.collectAsState()
    
    // state contains all drawing information
    println("Canvas background: ${state.bgColor}")
    println("Current mode: ${state.mode}")
    println("Elements on canvas: ${state.elements.size}")
}
```

The `State` object contains:

```kotlin
state.elements         // List<Element> - All drawable elements
state.undoStack        // List<Element> - Redo history
state.strokeColor      // Color - Current stroke color
state.strokeWidth      // Float - Current stroke width (pixels)
state.opacity          // Float - Current opacity (0.0-1.0)
state.bgColor          // Color - Canvas background color
state.mode             // Mode - Current drawing mode
```

### Observing Undo/Redo Availability

```kotlin
val canUndo by controller.canUndo.collectAsState(false)
val canRedo by controller.canRedo.collectAsState(false)

Button(onClick = { controller.undo() }, enabled = canUndo) {
    Text("Undo")
}

Button(onClick = { controller.redo() }, enabled = canRedo) {
    Text("Redo")
}
```

## Drawing Mode Control

### Switch Drawing Mode

```kotlin
// Freehand drawing
controller.setMode(Mode.PEN)

// Shape drawing
controller.setMode(Mode.RECTANGLE)
controller.setMode(Mode.CIRCLE)
controller.setMode(Mode.TRIANGLE)
controller.setMode(Mode.ARROW)
controller.setMode(Mode.LINE)
```

Get current mode:

```kotlin
val state by controller.state.collectAsState()
val currentMode = state.mode

when (currentMode) {
    Mode.PEN -> println("Drawing freehand")
    Mode.RECTANGLE -> println("Drawing rectangles")
    // ... etc
}
```

## Style & Appearance

### Stroke Color

```kotlin
// Set stroke color for new drawings
controller.setColor(Color.Blue)
controller.setColor(Color.Red)
controller.setColor(Color(0xFF00FF00))  // Green
controller.setColor(Color(red = 1f, green = 0f, blue = 0f))  // RGB
```

Get current color:

```kotlin
val state by controller.state.collectAsState()
val currentColor = state.strokeColor
```

### Stroke Width

```kotlin
// Set stroke width in pixels (1.0 to infinity)
controller.setStrokeWidth(5f)    // Thin line
controller.setStrokeWidth(10f)   // Medium
controller.setStrokeWidth(20f)   // Thick
controller.setStrokeWidth(50f)   // Very thick

// Get current width
val state by controller.state.collectAsState()
val currentWidth = state.strokeWidth
```

### Opacity/Transparency

```kotlin
// Set opacity from 0.0 (fully transparent) to 1.0 (fully opaque)
controller.setOpacity(1.0f)    // Opaque (default)
controller.setOpacity(0.8f)    // 80% opaque
controller.setOpacity(0.5f)    // 50% transparent
controller.setOpacity(0.2f)    // 20% opaque

// Get current opacity
val state by controller.state.collectAsState()
val currentOpacity = state.opacity
```

### Background Color

```kotlin
// Set canvas background color
controller.setBgColor(Color.White)
controller.setBgColor(Color.Black)
controller.setBgColor(Color.Gray)
controller.setBgColor(Color(0xFFF5F5F5))  // Light gray
```

## History Management

### Undo/Redo

```kotlin
// Undo the last drawing operation
controller.undo()

// Redo the last undone operation
controller.redo()

// Check if undo/redo is available
val canUndo by controller.canUndo.collectAsState(false)
val canRedo by controller.canRedo.collectAsState(false)

if (canUndo) {
    controller.undo()
}
```

### Reset Canvas

```kotlin
// Clear all drawings and reset canvas
controller.reset()

// This clears:
// - All elements
// - Undo/redo history
// - Does NOT reset colors or stroke width
```

## Intent Handling

Send intents directly to the controller:

```kotlin
// For advanced users who want direct control
controller.onIntent(Intent.SetColor(Color.Blue))
controller.onIntent(Intent.SetMode(Mode.RECTANGLE))
```

For most use cases, use the high-level methods above.

## Export & Import

### Export as SVG

Export drawings as scalable vector graphics:

```kotlin
// Get SVG string
val svgString = controller.exportSvg()

// Save to file
File("drawing.svg").writeText(svgString)

// Or listen to export events
controller.events.collect { event ->
    if (event is Event.SvgExported) {
        val svg = event.svg
        saveToFile(svg)
    }
}
```

### Export as PNG

Export drawings as raster image:

```kotlin
// Save as bitmap
controller.saveBitmap()

// Listen for PNG saved event
controller.events.collect { event ->
    if (event is Event.PngSaved) {
        val bitmap = event.bitmap
        if (bitmap != null) {
            saveBitmapToFile(bitmap)
        } else if (event.throwable != null) {
            handleError(event.throwable)
        }
    }
}
```

### Export as JSON

Save/load drawing state:

```kotlin
// Export current state as JSON
val json = controller.exportPath()

// Save to preferences or file
preferences.saveDrawing(json)

// Later, load from JSON
val savedJson = preferences.loadDrawing()
controller.importPath(savedJson)
```

## Event Listening

Listen to important events:

```kotlin
val controller = rememberDrawBoxController()

LaunchedEffect(Unit) {
    controller.events.collect { event ->
        when (event) {
            is Event.ElementAdded -> {
                println("Element added: ${event.element}")
            }
            is Event.ElementUpdated -> {
                println("Element updated: ${event.element}")
            }
            is Event.ElementDeleted -> {
                println("Element deleted: ${event.elementId}")
            }
            is Event.HistoryChanged -> {
                println("Can undo: ${event.canUndo}, Can redo: ${event.canRedo}")
            }
            is Event.SvgExported -> {
                println("SVG exported: ${event.svg.length} bytes")
            }
            is Event.PngSaved -> {
                if (event.bitmap != null) {
                    println("PNG saved successfully")
                } else {
                    println("Error saving PNG: ${event.throwable?.message}")
                }
            }
            is Event.DrawingLoaded -> {
                println("Drawing loaded")
            }
            is Event.Error -> {
                println("Error: ${event.message}")
            }
        }
    }
}
```

## Complete Example

Here's a complete example using all major controller features:

```kotlin
@Composable
fun AdvancedDrawingScreen() {
    val controller = rememberDrawBoxController()
    val state by controller.state.collectAsState()
    val canUndo by controller.canUndo.collectAsState(false)
    val canRedo by controller.canRedo.collectAsState(false)
    
    var showColorPicker by remember { mutableStateOf(false) }
    var selectedColor by remember { mutableStateOf(Color.Red) }
    
    // Listen to events
    LaunchedEffect(Unit) {
        controller.events.collect { event ->
            when (event) {
                is Event.SvgExported -> {
                    saveFile("drawing.svg", event.svg)
                }
                is Event.PngSaved -> {
                    if (event.bitmap != null) {
                        saveBitmap(event.bitmap)
                    }
                }
                is Event.Error -> {
                    showError(event.message)
                }
                else -> {}
            }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Canvas
        DrawBox(
            state = state,
            onIntent = controller::onIntent,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        )
        
        // Control Panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Mode buttons
            IconButton(onClick = { controller.setMode(Mode.PEN) }) {
                Icon(Icons.Default.Edit, "Pen")
            }
            
            IconButton(onClick = { controller.setMode(Mode.RECTANGLE) }) {
                Text("⬜")
            }
            
            // Color picker
            Button(onClick = { showColorPicker = true }) {
                Text("Color")
            }
            
            // Stroke width slider
            Slider(
                value = state.strokeWidth,
                onValueChange = { controller.setStrokeWidth(it) },
                valueRange = 1f..50f,
                modifier = Modifier.weight(1f)
            )
            
            // Undo/Redo
            IconButton(
                onClick = { controller.undo() },
                enabled = canUndo
            ) {
                Text("↶")
            }
            
            IconButton(
                onClick = { controller.redo() },
                enabled = canRedo
            ) {
                Text("↷")
            }
            
            // Export
            IconButton(onClick = { controller.exportSvg() }) {
                Text("SVG")
            }
            
            IconButton(onClick = { controller.saveBitmap() }) {
                Text("PNG")
            }
            
            // Reset
            IconButton(onClick = { controller.reset() }) {
                Text("Reset")
            }
        }
    }
    
    if (showColorPicker) {
        ColorPickerDialog(
            onColorSelected = { color ->
                controller.setColor(color)
                selectedColor = color
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
}
```

## Performance Considerations

- **State collection**: Only use `collectAsState()` for values you need
- **Recomposition**: Each state change triggers recomposition
- **Memory**: Large element lists may impact performance
- **Export**: SVG/PNG export is async and won't block UI

## Thread Safety

DrawBox is safe to use from any thread. The StateFlow and event emissions are thread-safe.

```kotlin
// Safe to call from background threads
viewModelScope.launch(Dispatchers.IO) {
    controller.setColor(Color.Blue)
    controller.exportSvg()
}
```

---

**Next**: Learn about [exporting drawings](../guides/export.md)