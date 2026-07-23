package io.ak1.drawbox

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

/**
 * Shared between the `js` and `wasmJs` targets — both run on Skiko, so the
 * ImageBitmap → PNG-bytes path is identical. Mirrors [decodeImageBitmap]'s
 * single shared `web` actual to avoid two files drifting apart.
 */
actual fun encodeToPng(bitmap: ImageBitmap): ByteArray? = runCatching {
    Image.makeFromBitmap(bitmap.asSkiaBitmap())
        .encodeToData(EncodedImageFormat.PNG)
        ?.bytes
}.getOrNull()
