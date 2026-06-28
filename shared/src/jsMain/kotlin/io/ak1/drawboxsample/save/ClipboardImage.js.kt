package io.ak1.drawboxsample.save

import androidx.compose.ui.geometry.Size
import kotlinx.browser.window
import org.jetbrains.skia.Image
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get

/**
 * Legacy Kotlin/JS implementation. Stays in fully-dynamic territory because
 * Kotlin/JS's standard `Promise<T>` doesn't surface chained dynamic types
 * cleanly through the `.then()` overloads — using `js("...")` keeps the
 * Async Clipboard call chain in JS land where the API was designed.
 */
actual fun pasteImageFromClipboard(
    onLoaded: (ByteArray, Size) -> Unit,
) {
    val handler: (dynamic) -> Unit = { bytes ->
        val byteArray = arrayBufferToBytes(bytes as ArrayBuffer)
        val size = runCatching {
            val img = Image.makeFromEncoded(byteArray)
            Size(img.width.toFloat(), img.height.toFloat())
        }.getOrElse { Size.Zero }
        onLoaded(byteArray, size)
    }
    readClipboardImageAsArrayBuffer(handler)
}

private fun readClipboardImageAsArrayBuffer(onArrayBuffer: (dynamic) -> Unit) {
    js("""
        if (typeof navigator === 'undefined' || !navigator.clipboard || !navigator.clipboard.read) return;
        navigator.clipboard.read().then(function(items) {
            for (var i = 0; i < items.length; i++) {
                var t = items[i].types;
                for (var j = 0; j < t.length; j++) {
                    if (t[j].indexOf('image/') === 0) {
                        items[i].getType(t[j]).then(function(blob) {
                            blob.arrayBuffer().then(function(buffer) {
                                onArrayBuffer(buffer);
                            });
                        });
                        return;
                    }
                }
            }
        }).catch(function() { });
    """)
}

private fun arrayBufferToBytes(buffer: ArrayBuffer): ByteArray {
    val arr = Uint8Array(buffer)
    return ByteArray(arr.length) { i -> arr[i] }
}
