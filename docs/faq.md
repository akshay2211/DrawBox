# Frequently Asked Questions

## General

### What is DrawBox?
DrawBox is a powerful, Kotlin Multiplatform drawing library that allows you to add drawing capabilities to applications on Android, iOS, Desktop (JVM), and Web (WebAssembly) platforms. It provides a canvas with multiple drawing modes, customization options, and export capabilities.

### How is DrawBox different from other drawing libraries?
- ✅ **Kotlin Multiplatform** - Single codebase for all platforms
- ✅ **Compose-first** - Native Jetpack Compose integration
- ✅ **MVI Architecture** - Predictable state management
- ✅ **SVG Export** - Vector graphics support
- ✅ **Type-safe** - Sealed classes and type safety throughout
- ✅ **Production-ready** - Published on Maven Central

### Is DrawBox free?
Yes! DrawBox is open-source under the **Apache License 2.0**. You can use it freely in commercial and personal projects.

### Can I use DrawBox in a production app?
Absolutely! DrawBox is actively maintained and used in production applications. It's published on Maven Central and follows semantic versioning.

## Installation & Setup

### Why am I getting "Unresolved reference" errors?
Make sure you've:
1. Added Maven Central to your repositories
2. Added the DrawBox dependency with the correct version
3. Run `./gradlew clean` and synced Gradle
4. Invalidated Android Studio cache (File > Invalidate Caches)

### Which minSdk version do I need?
- **Android**: minSdk 21 or higher
- **iOS**: iOS 14.0 or higher
- **Desktop**: JVM 11 or higher
- **Web**: Modern browsers (Chrome, Firefox, Safari, Edge)

### Can I use DrawBox with Jetpack Compose only?
Yes! DrawBox is built with Compose. However, you need Kotlin Multiplatform set up if you want to use it across multiple platforms. For Android-only projects, you still need the KMP structure.

## Features & Usage

### What drawing modes are available?
DrawBox supports 6 drawing modes:
- **Pen** - Freehand drawing
- **Rectangle** - Rectangular shapes
- **Circle** - Circular shapes
- **Triangle** - Triangular shapes
- **Arrow** - Arrows with intelligent head sizing
- **Line** - Straight lines

### How do I change colors?
```kotlin
// Change stroke color
controller.setColor(Color.Blue)

// Change background
controller.setBgColor(Color.White)
```

Colors apply to new drawings, not existing ones.

### How do I undo/redo?
```kotlin
// Undo last action
controller.undo()

// Redo last undone action
controller.redo()

// Check if available
val canUndo by controller.canUndo.collectAsState(false)
```

### What's the maximum stroke width?
There's no hard limit. You can set stroke width to any positive value:
```kotlin
controller.setStrokeWidth(100f)  // Very thick
```

However, values above 100 pixels may impact performance on older devices.

### Can I draw with transparency?
Yes! Use the opacity setting:
```kotlin
controller.setOpacity(0.5f)  // 50% transparent
```

Values range from 0.0 (fully transparent) to 1.0 (fully opaque).

## Export & Import

### How do I save drawings?
DrawBox supports multiple export formats:

**SVG (Vector):**
```kotlin
val svg = controller.exportSvg()
File("drawing.svg").writeText(svg)
```

**PNG (Bitmap):**
```kotlin
controller.saveBitmap()

// Listen for result
controller.events.collect { event ->
    if (event is Event.PngSaved) {
        saveBitmapToFile(event.bitmap!!)
    }
}
```

**JSON (State):**
```kotlin
val json = controller.exportPath()
File("drawing.json").writeText(json)
```

### Should I use SVG or PNG?
- **SVG** - Use for scalable graphics, smaller file sizes, vector art
- **PNG** - Use for raster images, screenshots, complex drawings with many strokes

### Can I load previously saved drawings?
Yes! Use the JSON export/import:
```kotlin
val json = File("drawing.json").readText()
controller.importPath(json)
```

### Why is my SVG export empty?
- Ensure you've drawn something on the canvas
- Check that elements are being added (listen to events)
- SVG export is async - wait for `Event.SvgExported` event

## Performance & Optimization

### The app is slow when there are many strokes. How do I improve performance?
- **Reduce stroke count**: Clear old drawings with `controller.reset()`
- **Simplify shapes**: Use fewer control points in paths
- **Optimize colors**: Avoid unnecessary color changes
- **Monitor memory**: Use Profiler to check memory usage
- **Consider batching**: Group small drawings together

### Does DrawBox work with Compose Canvas?
Yes, DrawBox uses Compose Canvas internally. You can also integrate DrawBox with existing Canvas-based code.

### Is hardware acceleration enabled?
Yes, DrawBox automatically uses hardware acceleration on all platforms where available.

## Troubleshooting

### The canvas is blank
- **Check state**: Verify `state.elements` is not empty
- **Check background**: Ensure background color contrasts with element color
- **Check bounds**: Ensure drawn elements are within canvas bounds
- **Check events**: Listen to `Event.ElementAdded` to confirm elements are being added

### Touches aren't working
- Ensure `onIntent` parameter is connected: `onIntent = controller::onIntent`
- Check that the DrawBox composable has a size (not 0x0)
- Verify touch events aren't being consumed by parent layouts

### Colors aren't changing
- Remember that `setColor()` only affects new drawings
- Already drawn elements keep their original colors
- If you want to change existing element colors, delete and redraw them

### Undo isn't working
- Check `canUndo` state before calling `undo()`
- Ensure state is being properly observed
- Verify intents are being sent via `onIntent`

### Crash when exporting
- **SVG**: Ensure there are elements to export
- **PNG**: Check that image saving is supported on your platform
- **Memory**: Large images may require more memory
- **Permissions**: Ensure file write permissions are granted (Android)

## Platform-Specific Issues

### Android

**"Missing resource" error:**
- Run `./gradlew clean assembleDebug`
- Invalidate Android Studio cache
- Check that `compileSdk` is 34 or higher

**Canvas shows but doesn't draw:**
- Verify `onIntent` is wired correctly
- Check touch input (logcat for motion events)
- Ensure `fillMaxSize()` or explicit size modifier

### iOS

**Framework not found:**
- Run `./gradlew build` from shared module
- Delete Xcode derived data
- Rebuild Xcode project

**Crashes when drawing:**
- Check iOS minimum version (14.0+)
- Verify Kotlin/Native setup
- Check console logs for specific errors

### Web (WASM)

**WASM compilation error:**
- Update Kotlin to 1.9.0+
- Check browser console for errors
- Enable WASM in browser settings

**Slow performance:**
- WASM has inherent performance overhead
- Consider limiting element count
- Use `pruneElements()` to remove old drawings

### Desktop

**Graphical glitches:**
- Check graphics driver is up to date
- Verify Java version is 11+
- Try with different graphics backend

## Advanced Usage

### Can I customize the DrawBox appearance?
DrawBox provides the canvas only. You can customize controls, colors, and layout in your UI code.

### Can I integrate DrawBox with other Compose libraries?
Yes! DrawBox is fully composable and integrates with any Compose library or code.

### Can I save/restore controller state?
You can save the drawing state via `exportPath()` and restore with `importPath()`. The controller itself isn't saved.

### How do I add custom drawing modes?
Currently, DrawBox has 6 built-in modes. For custom shapes, consider using multiple elements or modifying the open-source code.

## Contributing

### How do I contribute?
1. Fork the repository on GitHub
2. Create a feature branch
3. Make your changes
4. Submit a pull request

### Are there guidelines?
- Follow Kotlin style guide
- Use meaningful commit messages
- Add tests for new features
- Update documentation

### How do I report a bug?
Create an issue on [GitHub](https://github.com/akshay2211/DrawBox/issues) with:
- Clear description of the issue
- Steps to reproduce
- Expected vs. actual behavior
- Your environment (platform, version, etc.)

## Support & Community

### Where can I get help?
- 📖 [Documentation](index.md)
- 🐙 [GitHub Issues](https://github.com/akshay2211/DrawBox/issues)
- 💬 [GitHub Discussions](https://github.com/akshay2211/DrawBox/discussions)

### How can I support the project?
- ⭐ Star the repository
- 🐛 Report bugs
- 💡 Suggest features
- 🤝 Contribute code
- ☕ Buy the author a coffee

### Is there commercial support available?
For enterprise support or custom development, contact the project author directly.

## Licensing

### Can I use DrawBox commercially?
Yes! DrawBox is licensed under Apache 2.0, allowing commercial use.

### What do I need to do?
You must:
- Include the LICENSE file
- Include copyright notice
- Document any modifications

You can:
- Use commercially
- Modify source code
- Distribute modified versions

### What's covered by the license?
See the full [LICENSE](https://github.com/akshay2211/DrawBox/blob/main/LICENSE) file for complete details.

---

**Still have questions?** 
- Open an issue on [GitHub](https://github.com/akshay2211/DrawBox/issues)
- Start a discussion on [GitHub Discussions](https://github.com/akshay2211/DrawBox/discussions)
- Check the [documentation](index.md)