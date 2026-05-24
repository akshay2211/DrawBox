# Project Structure

Overview of DrawBox project layout.

```
DrawBox/
├── src/
│   ├── commonMain/
│   │   ├── kotlin/io/ak1/drawbox/
│   │   │   ├── domain/          # Business logic
│   │   │   ├── presentation/    # Controllers, ViewModels
│   │   │   └── ui/              # Compose components
│   │   └── composeResources/    # Assets, icons
│   ├── androidMain/             # Android specifics
│   ├── iosMain/                 # iOS specifics
│   ├── jvmMain/                 # Desktop specifics
│   └── wasmJsMain/              # Web specifics
├── build.gradle.kts
└── README.md
```

## Module Organization

- **domain/** - Core business logic, models, use cases
- **presentation/** - ViewModels, Controllers, Reducers
- **ui/** - Composable components

## Platform-Specific Code

Each platform has its own implementations for:
- Image saving (bitmap export)
- File I/O
- Platform-specific optimizations

---

See [Contributing](contributing.md) for development guidelines.
