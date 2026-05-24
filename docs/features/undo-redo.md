# Undo & Redo

Full history management for your drawings.

## Basic Usage

```kotlin
controller.undo()  // Undo last action
controller.redo()  // Redo last undone action
```

## Checking Availability

```kotlin
val canUndo by controller.canUndo.collectAsState(false)
val canRedo by controller.canRedo.collectAsState(false)
```

## Unlimited History

DrawBox maintains an unlimited undo/redo history.
