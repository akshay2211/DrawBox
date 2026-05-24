# Keyboard Shortcuts

DrawBox doesn't include built-in keyboard shortcuts, but you can easily add them in your UI implementation.

## Implementing Keyboard Shortcuts

### Desktop (JVM)

Use Compose's `onKeyEvent` modifier:

```kotlin
@Composable
fun DrawingScreen() {
    val controller = rememberDrawBoxController()
    val state by controller.state.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent { keyEvent ->
                when {
                    keyEvent.isCtrlPressed && keyEvent.key == Key.Z -> {
                        controller.undo()
                        true
                    }
                    keyEvent.isCtrlPressed && keyEvent.key == Key.Y -> {
                        controller.redo()
                        true
                    }
                    keyEvent.key == Key.Delete -> {
                        controller.reset()
                        true
                    }
                    else -> false
                }
            }
    ) {
        DrawBox(state = state, onIntent = controller::onIntent)
    }
}
```

### Web (WASM)

Use JavaScript interop or Compose event handlers.

### Android

Implement `onBackPressed()` or use `KeyEvent` handling in your Activity.

### iOS

Handle keyboard events through Swift interop.

## Suggested Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+Z` / `Cmd+Z` | `controller.undo()` |
| `Ctrl+Y` / `Cmd+Y` | `controller.redo()` |
| `Ctrl+S` / `Cmd+S` | `controller.exportSvg()` |
| `Delete` | `controller.reset()` |

---

See [API Reference](../api-reference/controller.md) for all available methods.
