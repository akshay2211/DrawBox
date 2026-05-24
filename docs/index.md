# DrawBox - Kotlin Multiplatform Drawing Library

## Overview

**DrawBox** is a powerful, lightweight drawing library built with **Kotlin Multiplatform (KMP)** and **Jetpack Compose**. It enables developers to add intuitive drawing capabilities to applications across **Android**, **iOS**, **Desktop (JVM)**, and **Web (WebAssembly)** from a single, shared codebase.

Featured in **Android Weekly**, **Android Arsenal**, **Kotlin Weekly**, and **Google Dev Library**.

## Key Capabilities

### Drawing Modes
- 🖊️ **Freehand Drawing (Pen)** - Create smooth, continuous paths
- ⬜ **Rectangle** - Draw precise rectangular shapes
- ⭕ **Circle** - Create perfect circles and ellipses
- 🔺 **Triangle** - Draw triangular shapes
- ➜ **Arrow** - Create arrows with intelligent head sizing
- ➖ **Line** - Draw straight lines

### Customization & Control
- 🎨 **Color Control** - Set stroke color and background color
- 📏 **Stroke Width** - Adjustable line thickness from 1px to any size
- 👁️ **Opacity Control** - Alpha transparency from 0.0 to 1.0
- ↩️ **Full Undo/Redo** - Unlimited history tracking
- 🔄 **Reset Canvas** - Clear all drawings instantly

### Export & Integration
- 📄 **SVG Export** - Export drawings as scalable vector graphics
- 🖼️ **PNG Export** - Raster image export with bitmap support
- 📦 **JSON Serialization** - Save/load drawing state for persistence
- 🔌 **Easy Integration** - Simple Compose API with minimal setup

## Why DrawBox?

### Cross-Platform with Shared Code
Build once, deploy everywhere. Write your drawing logic once in Kotlin and run it natively on Android, iOS, Web, and Desktop.

### Modern Architecture
Uses the **MVI (Model-View-Intent)** pattern with immutable state management, making your code predictable, testable, and maintainable.

### Production-Ready
- Published on Maven Central
- Actively maintained and battle-tested
- Comprehensive documentation and examples
- Used in production applications

## Quick Statistics

| Metric | Value |
|--------|-------|
| **Latest Version** | 2.0.0-alpha02 |
| **Platforms** | Android, iOS, Web, Desktop |
| **Architecture** | MVI Pattern |
| **State Management** | Immutable, Functional |
| **Export Formats** | SVG, PNG, JSON |
| **License** | Apache 2.0 |

## Technology Stack

```
Kotlin Multiplatform
├── Android (Native)
├── iOS (Native)
├── JVM (Desktop)
└── WebAssembly (Web)

UI Framework: Jetpack Compose Multiplatform
Build System: Gradle (KTS)
State: MVI Pattern with sealed classes
```

## Getting Started

### Installation (2 minutes)
Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.ak1:drawbox:2.0.0-alpha02")
}
```

### Basic Usage (5 minutes)
```kotlin
@Composable
fun DrawingScreen() {
    val controller = rememberDrawBoxController()
    val state by controller.state.collectAsState()
    
    Column(Modifier.fillMaxSize()) {
        // Drawing canvas
        DrawBox(
            state = state,
            onIntent = controller::onIntent,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        )
        
        // Control panel
        ControlsBar(
            viewModel = controller,
            canUndo = /* ... */,
            canRedo = /* ... */,
            onModeSelected = { mode -> controller.setMode(mode) }
        )
    }
}
```

## Features Comparison

| Feature | DrawBox | Other Libraries |
|---------|---------|-----------------|
| Multiplatform (KMP) | ✅ | ❌ Most are Android-only |
| Kotlin-First | ✅ | ❌ Some use Java |
| MVI Architecture | ✅ | ❌ |
| SVG Export | ✅ | ⚠️ Partial support |
| Compose Native | ✅ | ❌ |
| Undo/Redo | ✅ | ✅ |
| Multiple Shapes | ✅ | ⚠️ Limited |
| Web Support | ✅ | ❌ |

## Documentation Structure

- **[Installation](getting-started/installation.md)** - Setup DrawBox in your project
- **[Quick Start](getting-started/quickstart.md)** - Create your first drawing in 5 minutes
- **[Core Concepts](core-concepts/architecture.md)** - Understand MVI, State, Intent, and Events
- **[API Reference](api-reference/index.md)** - Complete API documentation
- **[Examples](examples/basic-drawing.md)** - Real-world usage examples
- **[Export Guide](guides/export.md)** - SVG and PNG export options
- **[Contributing](contributing.md)** - Help improve DrawBox

## Community & Support

- 🐙 **GitHub Issues** - Report bugs and request features
- 💬 **GitHub Discussions** - Connect with other developers
- 📧 **Email** - Direct support and inquiries
- ☕ **Support Author** - Buy the creator a coffee

## License

DrawBox is licensed under the **Apache License 2.0**. See [LICENSE](license.md) for details.

---

## Next Steps

1. **[Install DrawBox](getting-started/installation.md)** in your project
2. **[Create your first drawing](getting-started/quickstart.md)** with our quick start guide
3. **[Explore the API](api-reference/index.md)** to unlock all capabilities
4. **[Join the community](contributing.md)** and contribute improvements

**Ready to add drawing capabilities to your app?** Let's get started! 🎨