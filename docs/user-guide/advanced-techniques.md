# Advanced Techniques

Master advanced DrawBox features.

## Exporting Drawings

### SVG Export

Export as vector graphics:

```kotlin
val svg = controller.exportSvg()
```

### PNG Export

Export as image:

```kotlin
controller.saveBitmap()
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
        is Event.PngSaved -> {}
        else -> {}
    }
}
```

---

See [API Reference](../api-reference/controller.md) for complete API.
