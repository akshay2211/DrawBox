package io.ak1.drawboxsample.save

import androidx.compose.ui.geometry.Size

/**
 * Read a single image from the platform clipboard, if one is present.
 *
 * Async because:
 * - Web's `navigator.clipboard.read()` is Promise-based and the browser
 *   may prompt for permission on first call.
 * - Desktop AWT `Toolkit.systemClipboard.getData(DataFlavor.imageFlavor)`
 *   returns a `BufferedImage` we need to re-encode to PNG bytes on a
 *   background thread.
 *
 * Invokes [onLoaded] on the main thread when an image is found and decoded
 * to encoded bytes (PNG / JPEG / WebP — whatever the clipboard contains,
 * re-encoded to PNG on platforms where the clipboard owns a decoded
 * bitmap). Silently no-ops when:
 *  - Clipboard is empty.
 *  - Clipboard holds non-image content (text, files, etc.).
 *  - Permission is denied (Web) or the clipboard is unavailable.
 *
 * Per-platform implementations:
 * - **JVM (Desktop)**: AWT `Toolkit.systemClipboard` + `DataFlavor.imageFlavor`,
 *   re-encoded to PNG via `ImageIO.write`.
 * - **Web (WasmJS / JS)**: `navigator.clipboard.read()` → `ClipboardItem` →
 *   `getType("image/png")` → blob → bytes.
 * - **Android / iOS**: no-op (touch-screen platforms don't have a Cmd/Ctrl+V
 *   flow; covered by drag-drop / native pickers respectively).
 */
expect fun pasteImageFromClipboard(
    onLoaded: (bytes: ByteArray, intrinsicSize: Size) -> Unit,
)
