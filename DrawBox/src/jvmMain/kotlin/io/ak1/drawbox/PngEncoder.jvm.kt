package io.ak1.drawbox

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

actual fun encodeToPng(bitmap: ImageBitmap): ByteArray? = runCatching {
    Image.makeFromBitmap(bitmap.asSkiaBitmap())
        .encodeToData(EncodedImageFormat.PNG)
        ?.bytes
}.getOrNull()
