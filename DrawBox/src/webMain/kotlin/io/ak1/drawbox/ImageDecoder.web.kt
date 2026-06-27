package io.ak1.drawbox

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

/**
 * Shared between the `js` and `wasmJs` targets. Both run on top of Skiko
 * for the canvas backend, so the encoded-bytes → ImageBitmap path is
 * identical — `org.jetbrains.skia.Image.makeFromEncoded(...).toComposeImageBitmap()`
 * works on both. Keeping a single actual here avoids the maintenance cost
 * of two identical files and the risk of one drifting from the other.
 */
actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? = runCatching {
    Image.makeFromEncoded(bytes).toComposeImageBitmap()
}.getOrNull()
