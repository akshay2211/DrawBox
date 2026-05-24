# Basic Drawing

Learn the fundamentals of drawing with DrawBox.

## Starting a Drawing

```kotlin
@Composable
fun DrawingScreen() {
    val controller = rememberDrawBoxController()
    val state by controller.state.collectAsState()
    
    DrawBox(
        state = state,
        onIntent = controller::onIntent
    )
}
```

## Drawing Modes

DrawBox supports 6 different drawing modes:

- **Pen** - Freehand drawing
- **Rectangle** - Draw rectangles
- **Circle** - Draw circles
- **Triangle** - Draw triangles
- **Arrow** - Draw arrows
- **Line** - Draw lines

See [Drawing Modes](../features/drawing-modes.md) for details.

## Customizing Your Drawings

### Color

```kotlin
controller.setColor(Color.Blue)
```

### Stroke Width

```kotlin
controller.setStrokeWidth(10f)
```

### Opacity

```kotlin
controller.setOpacity(0.8f)  // 80% opaque
```

## History Management

```kotlin
// Undo last action
controller.undo()

// Redo
controller.redo()

// Clear canvas
controller.reset()
```

---

See [Advanced Techniques](advanced-techniques.md) for more features.
