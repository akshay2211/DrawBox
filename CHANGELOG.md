# Changelog

All notable changes to DrawBox are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The `2.0.x` line is the Kotlin Multiplatform rewrite. The `1.x` line was an Android-only library and is now in maintenance mode. See [Migration: 1.x → 2.x](#migration-1x--2x) at the bottom of this file.

## [Unreleased]

### Added
- Roborazzi-based snapshot testing harness for canvas rendering.

### Changed
- UI/UX restructure of the sample application and control bar.

> Items above are merged on `main` and will land in the next published alpha.

## [2.0.0-alpha02]

### Added
- Connectors between elements.
- Infinite canvas with zoom and pan.
- Shape selection with drag and scale.
- Stroke style and corner-radius options.
- Tileable SVG background pattern with optional tint.
- JSON import/export in the sample application.

### Fixed
- SVG export crash on `wasmJs` target.
- Image storing on Android and iOS.
- iOS sample UI rendering issues.
- Release script issues for Maven publishing.

### Changed
- Dokka v2 HTML documentation generation.
- Live WASM sample published under `/sample/` and linked from the README.
- Contributor Covenant text updated from 2.0 to 3.0.

## [2.0.0-alpha01] — 2026-05-22

### Added
- **Kotlin Multiplatform rewrite.** Targets Android, iOS, Desktop/JVM, and Web (WASM) from one shared Kotlin codebase.
- **MVI architecture.** Reducer-based state management with sealed `Intent`, `State`, and `Event` types.
- Multiple drawing modes: pen, rectangle, circle, triangle, arrow, line.
- SVG export of vector drawings.
- JSON export/import of drawing state.
- Bitmap export for raster output.
- Dokka and Spotless integration.
- mkdocs documentation site with auto-deploy.
- Maven publishing via `vanniktech-maven-publish`.
- Release script for tagged Maven Central publishing.

### Changed
- Package layout reorganised under `io.ak1.drawbox` with `domain`, `presentation`, and `data` separation.
- Public API surfaces a `DrawBoxController` and a `DrawBox` composable; the old `DrawController` from `1.x` is replaced — see migration notes.

### Removed
- Android-only `View`/`Canvas` internals from `1.x`. The new core uses Compose drawing primitives shared across platforms.

## [1.0.2] — 2022-01-24

### Fixed
- Removed stray log statements.
- Reverted `refreshState` behaviour to match documented contract.

### Added
- Launcher icons in the sample app.

## [1.0.1] — 2022-01-24

### Added
- Initial public release on Maven Central as `io.ak1:drawbox` (Android-only).
- `DrawController` for programmatic control of undo/redo, color, stroke, and bitmap capture.
- Sample application demonstrating the controller API.

---

## Migration: 1.x → 2.x

DrawBox `2.0.x` is a Kotlin Multiplatform rewrite. The public API has changed; the artifact coordinates (`io.ak1:drawbox`) are unchanged.

### Coordinates

```kotlin
// 1.x (Android only)
implementation("io.ak1:drawbox:1.0.2")

// 2.x (Android + iOS + JVM + WASM)
implementation("io.ak1:drawbox:2.0.0-alpha02")
```

### Composable entry point

`1.x`:

```kotlin
val controller = rememberDrawController()
DrawBox(
    drawController = controller,
    backgroundColor = Color.White,
    bitmapCallback = { bitmap, error -> /* ... */ }
)
```

`2.x`:

```kotlin
val controller = rememberDrawBoxController()
val state by controller.state.collectAsState()

DrawBox(
    state = state,
    onIntent = controller::onIntent,
    modifier = Modifier.fillMaxSize()
)
```

### State and events

`1.x` returned bitmaps via callback. `2.x` exposes a typed `Flow<Event>`:

```kotlin
controller.events.collect { event ->
    when (event) {
        is Event.SvgExported -> saveFile(event.svg)
        is Event.PngSaved   -> processImage(event.bitmap)
        is Event.Error      -> showError(event.message)
        else -> Unit
    }
}
```

### Controller API renames

| `1.x`                              | `2.x`                                |
| ---------------------------------- | ------------------------------------ |
| `rememberDrawController()`         | `rememberDrawBoxController()`        |
| `controller.changeColor(c)`        | `controller.setColor(c)`             |
| `controller.changeStrokeWidth(w)`  | `controller.setStrokeWidth(w)`       |
| `controller.changeOpacity(a)`      | `controller.setOpacity(a)`           |
| `controller.changeBgColor(c)`      | `controller.setBgColor(c)`           |
| `controller.unDo()` / `reDo()`     | `controller.undo()` / `redo()`       |
| `controller.reset()`               | `controller.reset()`                 |
| `controller.saveBitmap()`          | `controller.saveBitmap()` (now emits `Event.PngSaved`) |
| _not available_                    | `controller.setMode(Mode.RECTANGLE)` etc. |
| _not available_                    | `controller.exportSvg()`             |
| _not available_                    | `controller.exportPath()` / `importPath(json)` |

### Platform support

| Platform     | `1.x` | `2.x` |
| ------------ | :---: | :---: |
| Android      | ✅    | ✅    |
| iOS          | ❌    | ✅    |
| Desktop/JVM  | ❌    | ✅    |
| Web (WASM)   | ❌    | ✅    |

### JDK and Kotlin

- `2.x` requires JDK 21 to build, Kotlin `2.4.x`, and Compose Multiplatform `1.11.x`.
- Consuming apps on Android need `compileSdk 36+` and `minSdk 24+`.

### Stability

`2.0.0-alpha02` is an alpha — the public API may still change before `2.0.0`. Pin to an exact version while integrating. Stable `2.0.0` will freeze the public API and add an experimental opt-in annotation for incubating surfaces.

[Unreleased]: https://github.com/akshay2211/DrawBox/compare/2.0.0-alpha02...HEAD
[2.0.0-alpha02]: https://github.com/akshay2211/DrawBox/compare/2.0.0-alpha01...2.0.0-alpha02
[2.0.0-alpha01]: https://github.com/akshay2211/DrawBox/releases/tag/2.0.0-alpha01
[1.0.2]: https://github.com/akshay2211/DrawBox/releases/tag/1.0.2
[1.0.1]: https://github.com/akshay2211/DrawBox/releases/tag/1.0.1