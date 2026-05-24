# Compose Architecture

DrawBox uses Jetpack Compose Multiplatform for the UI layer.

## Compose Multiplatform

A modern UI toolkit that works across all platforms:
- Declarative UI
- Reactive state management
- Hot reload support
- Type-safe DSL

## DrawBox Composition

The main composable is `DrawBox`:

```kotlin
@Composable
fun DrawBox(
    state: State,
    onIntent: (Intent) -> Unit,
    modifier: Modifier = Modifier
)
```

## State-Driven UI

UI always reflects current state:

```kotlin
val state by controller.state.collectAsState()

DrawBox(
    state = state,  // Reactive state
    onIntent = controller::onIntent
)
```

---

Learn more: [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
