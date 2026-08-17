package io.ak1.drawbox

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream

actual fun encodeToPng(bitmap: ImageBitmap): ByteArray? = runCatching {
    val stream = ByteArrayOutputStream()
    // Quality is ignored for PNG (lossless); 100 documents intent.
    bitmap.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
    stream.toByteArray()
}.getOrNull()
