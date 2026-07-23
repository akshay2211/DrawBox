# Advanced Techniques

Master advanced DrawBox features.

## Exporting Drawings

### SVG Export

Export as vector graphics:

```kotlin
val svg = controller.exportSvg()
```

### PNG Export

Export as image (headless raster; emits `Event.PngExported`):

```kotlin
val textMeasurer = rememberTextMeasurer()
controller.exportPng(textMeasurer = textMeasurer)
```

## Loading Previous Drawings

```kotlin
val json = loadFromFile()
controller.importPath(json)
```

## Event Handling

Listen to drawing events:

```kotlin
controller.events.collect { event ->
    when (event) {
        is Event.ElementAdded -> {}
        is Event.SvgExported -> {}
        is Event.PngExported -> {}
        else -> {}
    }
}
```

---

See [API Reference](../api-reference/controller.md) for complete API.
