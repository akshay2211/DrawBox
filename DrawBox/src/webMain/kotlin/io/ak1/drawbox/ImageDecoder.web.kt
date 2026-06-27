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

actual fun decodeImageBitmapDownsampled(
    bytes: ByteArray,
    targetWidth: Int,
    targetHeight: Int,
): ImageBitmap? = decodeAndDownsampleSkia(bytes, targetWidth, targetHeight)

private fun decodeAndDownsampleSkia(
    bytes: ByteArray,
    targetWidth: Int,
    targetHeight: Int,
): ImageBitmap? = runCatching {
    val src = org.jetbrains.skia.Image.makeFromEncoded(bytes)
    val srcW = src.width.coerceAtLeast(1)
    val srcH = src.height.coerceAtLeast(1)
    val capW = targetWidth.coerceAtLeast(1)
    val capH = targetHeight.coerceAtLeast(1)
    val scale = minOf(capW.toFloat() / srcW, capH.toFloat() / srcH)
    if (scale >= 1f) {
        return@runCatching src.toComposeImageBitmap()
    }
    val outW = (srcW * scale).toInt().coerceAtLeast(1)
    val outH = (srcH * scale).toInt().coerceAtLeast(1)
    val surface = org.jetbrains.skia.Surface.makeRasterN32Premul(outW, outH)
    surface.canvas.drawImageRect(
        src,
        org.jetbrains.skia.Rect(0f, 0f, srcW.toFloat(), srcH.toFloat()),
        org.jetbrains.skia.Rect(0f, 0f, outW.toFloat(), outH.toFloat()),
    )
    surface.makeImageSnapshot().toComposeImageBitmap()
}.getOrNull()
