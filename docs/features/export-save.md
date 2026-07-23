# Export & Save

Export your drawings in multiple formats.

## SVG Export

Export as scalable vector graphics:

```kotlin
val svg = controller.exportSvg()
```

## PNG Export

Export the whole drawing as a raster image. `exportPng` renders the scene
headlessly (independent of the on-screen viewport) and emits the encoded PNG
bytes via `Event.PngExported`:

```kotlin
val textMeasurer = rememberTextMeasurer()

// Trigger the export…
controller.exportPng(
    scale = 2f,               // HiDPI multiplier (clamped to the pixel cap)
    background = null,        // null keeps the backdrop transparent
    textMeasurer = textMeasurer, // needed for real text glyphs
)

// …and collect the bytes.
LaunchedEffect(controller) {
    controller.events.collect { event ->
        if (event is Event.PngExported) saveToFile(event.bytes)
    }
}
```

> **Migrating from `saveBitmap()`?** See the
> [PNG export migration notes](../../DrawBox/MIGRATION_GUIDE.md#png-export-savebitmap--exportpng-breaking-in-v30).

## JSON Export

Save drawing state:

```kotlin
val json = controller.exportPath()
```

See [Export Guide](../guides/export.md) for detailed information.
