# Colors & Stroke Styles

Customize the appearance of your drawings.

## Stroke Color

```kotlin
controller.setColor(Color.Red)
controller.setColor(Color.Blue)
controller.setColor(Color(0xFF00FF00))
```

Changes apply to new drawings only.

## Stroke Width

```kotlin
controller.setStrokeWidth(5f)
controller.setStrokeWidth(10f)
controller.setStrokeWidth(20f)
```

## Opacity

```kotlin
controller.setOpacity(1.0f)   // Fully opaque
controller.setOpacity(0.5f)   // 50% transparent
```

## Background Color

```kotlin
controller.setBgColor(Color.White)
controller.setBgColor(Color.Black)
```

See [API Reference](../api-reference/controller.md) for more details.
