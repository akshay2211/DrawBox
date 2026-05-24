# Export & Save

Export your drawings in multiple formats.

## SVG Export

Export as scalable vector graphics:

```kotlin
val svg = controller.exportSvg()
```

## PNG Export

Export as raster image:

```kotlin
controller.saveBitmap()
```

## JSON Export

Save drawing state:

```kotlin
val json = controller.exportPath()
```

See [Export Guide](../guides/export.md) for detailed information.
