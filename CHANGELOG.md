# Changelog

All notable changes to DrawBox are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

The `2.0.x` line is the Kotlin Multiplatform rewrite. The `1.x` line was an Android-only library and is now in maintenance mode. See [Migration: 1.x → 2.x](#migration-1x--2x) at the bottom of this file.

## [Unreleased]

### Added
- **In-place text editing gestures (#83).** Double-tapping an `Element.Text`
  in `SELECT` mode — or tapping an already-selected one again — now opens the
  host editor via the new `Event.TextEditRequested(id)` (backed by the new
  `Intent.RequestTextEditAt`). See [RFC 0001](docs/rfcs/0001-text-elements.md).
- **Multi-text style editing (#83.3).** The context bar's text chips now apply
  to every selected text element and show a dimmed "mixed" state
  (`ContextBarState.fontSizeMixed` / `textAlignmentMixed` / `fontFamilyMixed`)
  when the selection disagrees on a property.
- **Configurable selection chrome (`SelectionChromeStyle`).** `DrawBox` takes a
  new `selectionStyle` parameter for tuning the accent-colored selection box:
  `padding` grows/shrinks the gap between an element and its box, plus
  `handleSize`, `cornerRadius`, `strokeWidth`, and `accent`. The box now draws
  with rounded corners and hollow rounded-square handles, and the box outline is
  clipped so it never shows through a handle; resize hit-testing tracks the
  padded box so grab targets stay aligned.

### Changed
- **Sample: mid-edit styling and editor commit semantics (#83.1, #83.5).**
  Tapping a style chip while the inline editor is open now restyles the element
  instead of committing; `Esc` cancels without committing and losing focus
  commits.
- **`FontRegistry` now warns once per key on web targets** when a built-in
  key (`sans` / `serif` / `mono`) resolves to a generic `FontFamily` —
  the case Skia-WASM cannot differentiate. Warning is a one-shot `println`
  guarded by `expect fun isWebTarget()`, so non-web builds pay nothing.
  `FontRegistry.register(...)` resets the guard for the affected key so a
  re-registration will re-warn if the host swaps back to a generic family.
  KDoc on `FontRegistry` and `BuiltinFontFamilyKeys` now documents the
  Skia-WASM bundled-font behavior. Addresses #89 SDK-side.

## [2.1.0-alpha02] — 2026-07-07

Second `2.1` pre-release. Two additive seams for hosts that want to layer on
top of `DrawBoxController` (an intents `SharedFlow` and an `overlay` slot on
`DrawBox`), plus a fix for finger-drawn strokes on WASM/browser and for
translucent pen colours rendering as beaded dots.

No breaking changes on top of `2.1.0-alpha01`. New surfaces stay under the
alpha stability bucket described in that release.

### Added
- **`DrawBoxController.intents: SharedFlow<Intent>`** — observable stream of
  processed intents. Emission happens after the reducer runs and `state` has
  been updated, so subscribers can read `state.value` at emission time and
  see the resulting state. Unblocks layered controllers (brush), collaboration
  transports, and analytics recorders without subclassing or state-diffing.
  Buffered so slow subscribers don't back-pressure the drawing loop. (#99)
- **`overlay` slot on `DrawBox`** — new `overlay: @Composable BoxScope.() -> Unit`
  parameter, composed inside the canvas `Box` above strokes and selection
  chrome. Renders in screen space; hosts anchor to world coordinates via
  `state.viewport`. Default is a no-op, so existing call sites are
  unchanged. (#99)

### Fixed
- **Finger strokes halved on WASM/browser (#98).** Pointer pressure is now
  trusted only from `Stylus` and `Eraser` input types. WASM/browser touch
  reports the W3C default `0.5` when there is no pressure sensor, which
  was silently halving finger strokes. Mouse and capacitive touch now fall
  back to unit pressure.
- **Translucent pen strokes beading into dots (#97).** The same touch
  pressure jitter was pushing uniform strokes into the variable-width
  branch, where per-segment round caps stacked alpha and beaded
  translucent colours. Also composites the variable-width path through
  `canvas.saveLayer` when `alpha < 1`, so a legitimate pen stroke with a
  translucent colour renders as a single translucent line instead of a
  chain of overlapping dots. (#101)

## [2.1.0-alpha01] — 2026-06-28

First pre-release of the `2.1` line. New element types (image, text), a
fifth published target (Kotlin/JS browser), cross-platform image input
plumbing (OS drag-drop on Desktop; clipboard paste on Desktop, Web,
Android, iOS; iOS PHPicker), pen-pressure sampling on supported
hardware, and independent fill / stroke for shapes.

Alpha because the new surfaces (image / text elements, `io.ak1.drawbox.input`,
pen-pressure sample fields) may iterate based on integration feedback. The
frozen 2.0 surfaces (`Mode`, `Intent`, `Event`, `State`, `DrawBoxController`
core) are extended only — see _Changed_ for the one structural rename
inside `Element.Path` and the [migration note](#migration-20x--21x) at the
bottom.

### Added
- **Image element** (`Element.Image`, `Mode.IMAGE`). Place, transform, and
  export bitmaps as first-class canvas elements; raw bytes are the source
  of truth and decoding is cached per element id. (#76)
- **Cross-platform image inputs** under `io.ak1.drawbox.input`:
  - `pasteImageFromClipboard(...)` — JVM (AWT), Web (Async Clipboard API),
    Android (`ClipboardManager` + `ContentResolver`), iOS
    (`UIPasteboard.general.image` → PNG). (#91, #93)
  - `Modifier.imageDragAndDropTarget(...)` — full implementation on JVM
    via `awtTransferable` + `DataFlavor.javaFileListFlavor`; iOS / Web
    targets are intentional no-ops blocked on Compose Multiplatform 1.11
    not exposing `DragData` / `nativeEvent` on those targets (tracked as
    residual on #78). (#92)
  - `iOS PHPicker` wired into `ImageSaver.ios.kt` for the native
    insertion flow on iOS / iPadOS. (#91)
- **Text element** (`Element.Text`, `Mode.TEXT`) with inline editor,
  `TextAlignment` enum, font-family keys via `BuiltinFontFamilyKeys`,
  per-element wrap width / measured height, and SVG export. (#84)
- **Pen pressure** sampling on PEN and ERASER modes. New
  `Element.PathSample` carries `position`, `width`, and optional `tilt` /
  `azimuth` (degrees) reported by Apple Pencil / S Pen. Mouse / capacitive
  touch defaults to unit pressure with zero overhead. `Intent.UpdateLatestPath`
  now takes an optional `pressure: Float = 1f`. (#75)
- **Kotlin/JS browser target.** DrawBox now publishes a `js(browser)`
  variant alongside Android / iOS / JVM / WASM. Sample app's `webApp` wires
  the JS distribution end-to-end. (#86)
- **Independent fill + stroke** on `Element.Shape`. New `strokeEnabled`
  field (defaults `true`); `Intent.SetSelectedFillColor` and
  `Intent.SetSelectedStrokeEnabled`; `DrawBoxController.setSelectionFillColor`
  / `setSelectionStrokeEnabled`. ContextBar gains stroke + fill swatch
  dropdowns with explicit "none" states. SVG export emits `stroke="none"`
  when disabled. LINE / ARROW remain stroke-only and ignore both flags. (#82)
- **Connector endpoint side-anchoring.** Connector endpoints snap to the
  midpoint of the facing side of the target element instead of the nearest
  corner, and respect rotation. (#71)
- Roborazzi snapshot harness covering a complex multi-element scene; SVG
  export correctness asserted against the rendered baseline. (#88)

### Changed
- **`Element.Path.points: List<Offset>` → `Element.Path.samples: List<PathSample>`.**
  Pen-pressure work moved per-sample width / tilt / azimuth onto the path
  itself instead of carrying a parallel widths list. A compatibility
  extension `Element.Path.positions: List<Offset>` is provided (note: new
  name, not a rename of `points`). **This breaks direct construction and
  field access on `Element.Path`** — see [Migration: 2.0.x → 2.1.x](#migration-20x--21x).
- **Async + downsampled image decode** with a mip-level cache. Image
  rendering no longer blocks the UI thread; the decoded bitmap is sized
  to viewport resolution and cached per `(id, zoom level)` so panning a
  large image stays smooth. Internal; no API change. (#87)
- iOS image saving routed through PHPicker / Files for a native feel. (#91)
- SVG export correctness fixes surfaced by the new snapshot harness
  (stroke caps, fill defaults, rotation transforms). (#88)

### Fixed
- Misc. SVG export edge cases caught by the new complex-scene baseline
  (see commit `431d5d2`). (#88)

### Stability
- New surfaces in this release that may iterate before `2.1.0` stable:
  `Element.Image`, `Element.Text`, the `io.ak1.drawbox.input` package, the
  pen-pressure sample fields, and Kotlin/JS publishing coordinates.
  Feedback on these surfaces is the point of the alpha — file issues
  against [`akshay2211/DrawBox`](https://github.com/akshay2211/DrawBox/issues).
- The 2.0 frozen surfaces are not loosened by the alpha label.

## [2.0.0] — 2026-06-24

First stable release of the Kotlin Multiplatform line. Public API across
`DrawBoxController`, `Mode`, `Intent`, `Event`, and `State` is now frozen
under semver; incubating surfaces opt in via explicit annotations.

### Added
- **Object eraser tool** (`Mode.ERASER`). Tap to remove the element under
  the pointer; drag to sweep across multiple elements. Hit detection reuses
  `Element.hitTest`, so stroke width is automatically respected.
- **Lazy history snapshot for the eraser.** Taps and drags through empty
  space consume no undo slot; the snapshot is taken once on the first
  actual removal of a gesture, so a single undo reverts the whole sweep.
- **World-space eraser cursor overlay** that scales with zoom, with
  auto-contrast against light/dark backgrounds.
- `DrawBoxController.eraseAt(point, radius)` and `setEraserSize(size)` for
  programmatic erasing.
- `BeginErase`, `EraseAt`, `EndErase`, `SetEraserSize` intents and the
  `eraser` drawable resource.
- **LWW timestamps on `Element`.** Every element now carries `createdAt`
  and `modifiedAt` epoch-ms fields, bumped on every mutation via
  `Element.touched()`. Foundational plumbing for the collab work on the
  roadmap; consumers can ignore it.
- Roborazzi-based snapshot testing harness for canvas rendering.

### Changed
- **`Mode.PAN` removed from the bottom controls bar** and replaced with the
  eraser slot. Pan navigation itself stays — Space-bar (hold for temp-pan),
  middle-mouse drag, two-finger touch, and the scroll wheel all still pan.
- UI/UX restructure of the sample application and control bar.
- Toolbar visual polish and small interaction fixes.
- Internal path-cache / static-layer optimizations to keep the render
  cache fresh across erase ticks without thrash.

### Fixed
- Misc. bug fixes around element serialization and Android image saving
  rolled in from the pre-stable cleanup.

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

## Migration: 2.0.x → 2.1.x

`2.1.0-alpha01` is additive across the frozen `2.0` surfaces with one
exception inside `Element.Path` to make room for pen-pressure data.

### `Element.Path`: `points` → `samples`

Pen-pressure work moved per-sample width / tilt / azimuth onto the path
itself. The `points: List<Offset>` field is gone; `samples: List<PathSample>`
takes its place. Each sample carries its own width, so uniform strokes
simply set every sample's width to the active `strokeWidth`.

**Before (`2.0.x`):**

```kotlin
val path = Element.Path(
    points = listOf(Offset(10f, 10f), Offset(20f, 20f), Offset(30f, 25f)),
    strokeColor = Color.Black,
    strokeWidth = 4f,
    alpha = 1f,
)

path.points.forEach { /* ... */ }
```

**After (`2.1.x`):**

```kotlin
val path = Element.Path(
    samples = listOf(
        Element.PathSample(position = Offset(10f, 10f), width = 4f),
        Element.PathSample(position = Offset(20f, 20f), width = 4f),
        Element.PathSample(position = Offset(30f, 25f), width = 4f),
    ),
    strokeColor = Color.Black,
    strokeWidth = 4f,
    alpha = 1f,
)

// Read-only positions accessor for code that just needs the geometry:
path.positions.forEach { /* ... */ }   // List<Offset>

// Full sample stream for pressure-aware consumers:
path.samples.forEach { /* sample.position, sample.width, sample.tilt, sample.azimuth */ }
```

Notes:
- `positions` is an extension, not a constructor field — it's read-only.
  Mutating the path's geometry means producing a new `samples` list.
- `tilt` / `azimuth` are nullable; `null` means "no signal" (mouse /
  capacitive touch). Renderers that don't care can ignore them.
- The active stroke's `strokeWidth` on `Element.Path` is still meaningful:
  it's the default applied to newly-appended samples while drawing.
  Per-sample widths override it during the stroke; setters that resize the
  whole stroke (`SetSelectedStrokeWidth`) rewrite every sample's width.

### `Intent.UpdateLatestPath`: optional `pressure`

`UpdateLatestPath` now takes an optional `pressure: Float = 1f`.
Existing dispatch sites that pass only `newPoint` continue to compile
and behave identically (unit pressure = uniform stroke). Hosts that
want to wire pressure through pass the per-sample value.

### Additive: new modes / elements / inputs

These are pure additions; ignore them unless you want the feature:

- `Mode.IMAGE` / `Element.Image` — image insertion + transform.
- `Mode.TEXT` / `Element.Text` — text element with inline editor.
- `Element.Shape.strokeEnabled: Boolean = true` — independent fill /
  stroke. Existing serialized shapes default to `true` (matches the
  pre-2.1 implicit "always stroked" behaviour).
- `io.ak1.drawbox.input.pasteImageFromClipboard(...)` — opt-in clipboard
  paste entry point. Wire it from your host's key handler / toolbar.
- `io.ak1.drawbox.input.Modifier.imageDragAndDropTarget(...)` — opt-in
  drag-drop modifier. Attach to the canvas composable.

### Kotlin/JS coordinates

If you weren't already targeting Kotlin/JS browser, nothing changes.
If you want to consume the new JS variant, add the standard
`js(browser)` target in your `kotlin {}` block — DrawBox publishes a
matching klib alongside the existing Android / iOS / JVM / WASM ones.

### JDK and Kotlin

Unchanged from `2.0.x`: JDK 21 to build, Kotlin `2.4.x`, Compose
Multiplatform `1.11.x`. Consuming Android apps still need
`compileSdk 36+` and `minSdk 24+`.

---

## Migration: 1.x → 2.x

DrawBox `2.0.x` is a Kotlin Multiplatform rewrite. The public API has changed; the artifact coordinates (`io.ak1:drawbox`) are unchanged.

### Coordinates

```kotlin
// 1.x (Android only)
implementation("io.ak1:drawbox:1.0.2")

// 2.x (Android + iOS + JVM + WASM)
implementation("io.ak1:drawbox:2.0.0")
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

`2.0.0` is the stable Kotlin Multiplatform release. The public surfaces
`DrawBoxController`, `Mode`, `Intent`, `Event`, and `State` are frozen
under semver. Incubating surfaces (collab plumbing, advanced eraser
modes, etc.) are gated behind opt-in annotations and may evolve in
minor versions.

[Unreleased]: https://github.com/akshay2211/DrawBox/compare/v2.1.0-alpha02...HEAD
[2.1.0-alpha02]: https://github.com/akshay2211/DrawBox/compare/v2.1.0-alpha01...v2.1.0-alpha02
[2.1.0-alpha01]: https://github.com/akshay2211/DrawBox/compare/v2.0.0...v2.1.0-alpha01
[2.0.0]: https://github.com/akshay2211/DrawBox/compare/2.0.0-alpha02...v2.0.0
[2.0.0-alpha02]: https://github.com/akshay2211/DrawBox/compare/2.0.0-alpha01...2.0.0-alpha02
[2.0.0-alpha01]: https://github.com/akshay2211/DrawBox/releases/tag/2.0.0-alpha01
[1.0.2]: https://github.com/akshay2211/DrawBox/releases/tag/1.0.2
[1.0.1]: https://github.com/akshay2211/DrawBox/releases/tag/1.0.1