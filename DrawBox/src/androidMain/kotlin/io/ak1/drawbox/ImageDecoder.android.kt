package io.ak1.drawbox

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? {
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    return bitmap.asImageBitmap()
}

actual fun decodeImageBitmapDownsampled(
    bytes: ByteArray,
    targetWidth: Int,
    targetHeight: Int,
): ImageBitmap? {
    // Two-pass decode via BitmapFactory.Options:
    // pass 1 — `inJustDecodeBounds = true` reads only the header so we know
    // the source dimensions without allocating the full bitmap;
    // pass 2 — `inSampleSize = N` decodes at 1/N resolution. The result is
    // never larger than `target × N`, so we pick the smallest power of two
    // that still fits the target. This keeps memory low and is a single
    // allocation rather than full-size-then-shrink.
    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
    val srcW = boundsOpts.outWidth
    val srcH = boundsOpts.outHeight
    if (srcW <= 0 || srcH <= 0) return null

    val widthCap = targetWidth.coerceAtLeast(1)
    val heightCap = targetHeight.coerceAtLeast(1)
    var sample = 1
    while ((srcW / sample) > widthCap || (srcH / sample) > heightCap) {
        sample *= 2
    }

    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
        ?: return null
    return bitmap.asImageBitmap()
}
