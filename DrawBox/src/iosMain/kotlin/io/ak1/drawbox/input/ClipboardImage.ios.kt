package io.ak1.drawbox.input

import androidx.compose.ui.geometry.Size

/**
 * iOS paste is a no-op in the OSS sample. iOS has a system pasteboard
 * (`UIPasteboard.general`) that could be wired here, but the natural
 * insertion path on iOS is the PHPicker (issue #80) — that covers the
 * screenshot → annotate workflow this issue solves on Desktop / Web.
 */
actual fun pasteImageFromClipboard(
    onLoaded: (bytes: ByteArray, intrinsicSize: Size) -> Unit,
) {
    // intentionally no-op
}
