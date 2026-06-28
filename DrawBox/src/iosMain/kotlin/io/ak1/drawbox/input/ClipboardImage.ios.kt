package io.ak1.drawbox.input

import androidx.compose.ui.geometry.Size
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.UIKit.UIImage
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIPasteboard

/**
 * iOS clipboard paste — reads `UIPasteboard.general` for an image and
 * delivers the encoded bytes plus intrinsic pixel size. Wired primarily
 * for **iPad with a hardware keyboard** where Cmd+V is the natural
 * paste shortcut; iPhone users without keyboards trigger this through
 * a host-supplied toolbar button instead.
 *
 * Re-encoded to PNG via `UIImagePNGRepresentation` so the SDK's decoder
 * sees a known container regardless of what the source app put on the
 * pasteboard (could be PNG, JPEG, TIFF, or raw pixel data).
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun pasteImageFromClipboard(
    onLoaded: (bytes: ByteArray, intrinsicSize: Size) -> Unit,
) {
    val pasteboard = UIPasteboard.generalPasteboard
    if (!pasteboard.hasImages) return
    val image: UIImage = pasteboard.image ?: return
    val data: NSData = UIImagePNGRepresentation(image) ?: return
    val bytes = ByteArray(data.length.toInt())
    bytes.usePinned { pinned ->
        platform.posix.memcpy(pinned.addressOf(0), data.bytes, data.length)
    }
    // UIImage.size is in points; multiply by `scale` so we report pixel
    // dimensions (matches Desktop / Android / Web reporting).
    val size = image.size.useContents {
        Size(
            (width * image.scale).toFloat(),
            (height * image.scale).toFloat(),
        )
    }
    onLoaded(bytes, size)
}
