# Architecture

DrawBox uses the **MVI (Model-View-Intent)** pattern for state management.

See [Core Concepts](../core-concepts/architecture.md) for detailed explanation of the MVI pattern.

## Key Components

- **Model** - Immutable State representing the canvas
- **View** - Compose UI components (DrawBox)
- **Intent** - User actions and system events
- **Reducer** - Business logic processing intents to produce new state

## MVI Flow

```
User Interaction
    ↓
Intent
    ↓
Reducer
    ↓
New State
    ↓
UI Recomposes
```

---

Learn more: [MVI Pattern Explanation](../core-concepts/architecture.md)
