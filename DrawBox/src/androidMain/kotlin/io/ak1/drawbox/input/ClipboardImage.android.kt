package io.ak1.drawbox.input

import androidx.compose.ui.geometry.Size

/**
 * Android paste is a no-op in the OSS sample. Touch-screen Android doesn't
 * have a Cmd/Ctrl+V key flow, and the existing file picker covers the
 * "insert image" path on this platform. Could be wired to `ClipboardManager`
 * + `MIMETYPE_TEXT_URILIST` in a follow-up if anyone asks.
 */
actual fun pasteImageFromClipboard(
    onLoaded: (bytes: ByteArray, intrinsicSize: Size) -> Unit,
) {
    // intentionally no-op
}
