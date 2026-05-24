# Quick Start Guide

Get a fully functional drawing app running in **5 minutes**!

## Prerequisites
- ✅ [DrawBox installed](installation.md)
- ✅ Jetpack Compose set up in your project
- ✅ Android minSdk 21+ or equivalents for other platforms

## Step 1: Create a Drawing Screen (1 minute)

Create a new Composable function in your `MainActivity.kt`:

```kotlin
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.weight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import io.ak1.drawbox.DrawBox
import io.ak1.drawbox.presentation.viewmodel.rememberDrawBoxController

@Composable
fun DrawingScreen() {
    // Create and remember the DrawBox controller
    val controller = rememberDrawBoxController()
    
    // Observe the current drawing state
    val state by controller.state.collectAsState()
    
    // Render the canvas
    Column(modifier = Modifier.fillMaxSize()) {
        DrawBox(
            state = state,
            onIntent = controller::onIntent,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        )
    }
}
```

## Step 2: Display the Screen (1 minute)

In your `MainActivity`:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            DrawingScreen()
        }
    }
}
```

That's it! 🎉 You now have a fully functional drawing canvas!

## Step 3: Add Controls (2 minutes)

To change drawing modes, colors, and stroke width, add this to your `DrawingScreen`:

```kotlin
@Composable
fun DrawingScreen() {
    val controller = rememberDrawBoxController()
    val state by controller.state.collectAsState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Canvas
        DrawBox(
            state = state,
            onIntent = controller::onIntent,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        )
        
        // Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Change mode
            Button(onClick = { controller.setMode(Mode.PEN) }) {
                Text("Pen")
            }
            
            Button(onClick = { controller.setMode(Mode.RECTANGLE) }) {
                Text("Rectangle")
            }
            
            // Undo button
            Button(
                onClick = { controller.undo() },
                enabled = controller.canUndo.collectAsState(false).value
            ) {
                Text("Undo")
            }
            
            // Redo button
            Button(
                onClick = { controller.redo() },
                enabled = controller.canRedo.collectAsState(false).value
            ) {
                Text("Redo")
            }
        }
    }
}
```

## Step 4: Add Color Picker (1 minute)

```kotlin
Button(onClick = { 
    // Show color picker dialog
    showColorPicker = true
}) {
    Text("Color")
}

if (showColorPicker) {
    // Your color picker dialog
    ColorPickerDialog(
        onColorSelected = { color ->
            controller.setColor(color)
            showColorPicker = false
        }
    )
}
```

## Complete Example

Here's a minimal but complete example:

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.ak1.drawbox.DrawBox
import io.ak1.drawbox.domain.model.Mode
import io.ak1.drawbox.presentation.viewmodel.rememberDrawBoxController

@Composable
fun SimpleDrawingApp() {
    val controller = rememberDrawBoxController()
    val state by controller.state.collectAsState()
    val canUndo by controller.canUndo.collectAsState(false)
    val canRedo by controller.canRedo.collectAsState(false)
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Drawing Canvas
        DrawBox(
            state = state,
            onIntent = controller::onIntent,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        )
        
        // Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = { controller.setMode(Mode.PEN) }) {
                Text("✏️ Pen")
            }
            
            Button(onClick = { controller.setMode(Mode.RECTANGLE) }) {
                Text("⬜ Rect")
            }
            
            Button(onClick = { controller.setMode(Mode.CIRCLE) }) {
                Text("⭕ Circle")
            }
            
            Button(
                onClick = { controller.undo() },
                enabled = canUndo
            ) {
                Text("↶ Undo")
            }
            
            Button(
                onClick = { controller.redo() },
                enabled = canRedo
            ) {
                Text("↷ Redo")
            }
            
            Button(onClick = { controller.reset() }) {
                Text("🔄 Reset")
            }
        }
    }
}
```

## What You Can Do Now

✅ Draw freehand paths
✅ Create shapes (rectangle, circle, triangle, arrow, line)
✅ Change colors and stroke width
✅ Undo/Redo unlimited times
✅ Clear canvas

## Next Steps

- 📚 [Core Concepts](../core-concepts/architecture.md) - Understand MVI architecture
- 🎨 [API Reference](../api-reference/controller.md) - All available methods
- 💾 [Export Guide](../guides/export.md) - Save drawings as SVG/PNG
- 🔌 [Advanced Examples](../examples/advanced-drawing.md) - Complex use cases

## Troubleshooting

### "Cannot resolve symbol DrawBox"
- Ensure DrawBox is installed: `implementation("io.ak1:drawbox:2.0.0-alpha01")`
- Run `./gradlew clean` and sync Gradle

### Canvas appears but doesn't respond to touches
- Ensure `onIntent` is connected to controller: `onIntent = controller::onIntent`
- Check that `state` is being collected properly with `collectAsState()`

### Colors not changing
- Use `controller.setColor(Color.Red)` before drawing
- Changes apply to new strokes, not existing ones

**Stuck?** Check the [FAQ](../faq.md) or open an [GitHub issue](https://github.com/akshay2211/DrawBox/issues)!