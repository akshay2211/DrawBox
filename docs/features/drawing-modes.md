# Drawing Modes

DrawBox supports 6 distinct drawing modes, each creating different types of elements on the canvas.

## Pen Mode (Freehand Drawing)

Create smooth, continuous freehand paths.

```kotlin
controller.setMode(Mode.PEN)
```

**How it works:**
- Click and drag to draw
- Creates a `Path` element with multiple points
- Points are captured as you drag
- Lines connect the points smoothly

**Use cases:**
- Sketching
- Drawing signatures
- Creating curves and organic shapes

## Rectangle Mode

Draw precise rectangular shapes.

```kotlin
controller.setMode(Mode.RECTANGLE)
```

**How it works:**
- Click and drag from one corner
- Rectangle dimensions calculated from start and end points
- Creates a `Shape` element with `ShapeType.RECTANGLE`

## Circle Mode

Create perfect circular shapes.

```kotlin
controller.setMode(Mode.CIRCLE)
```

**How it works:**
- Click and drag to define diameter
- Circle is calculated from center and radius
- Creates a `Shape` element with `ShapeType.CIRCLE`

## Triangle Mode

Draw triangular shapes.

```kotlin
controller.setMode(Mode.TRIANGLE)
```

**How it works:**
- Click and drag to define triangle orientation
- Creates a `Shape` element with `ShapeType.TRIANGLE`

## Arrow Mode

Create arrows with intelligent head sizing.

```kotlin
controller.setMode(Mode.ARROW)
```

**Special features:**
- Arrow head automatically sizes based on arrow length
- Maintains proportions for short and long arrows
- Creates a `Shape` element with `ShapeType.ARROW`

## Line Mode

Draw straight lines between two points.

```kotlin
controller.setMode(Mode.LINE)
```

**How it works:**
- Click start point, drag to end point
- Creates a straight `Shape` element with `ShapeType.LINE`

## Switching Modes

```kotlin
// Change mode at any time
controller.setMode(Mode.PEN)
controller.setMode(Mode.RECTANGLE)

// Get current mode
val state by controller.state.collectAsState()
val currentMode = state.mode

// Use mode in UI
when (currentMode) {
    Mode.PEN -> showPenUI()
    Mode.RECTANGLE -> showRectangleUI()
    // ... etc
}
```

---

See [Customization](../features/colors-styles.md) for stroke and color control.