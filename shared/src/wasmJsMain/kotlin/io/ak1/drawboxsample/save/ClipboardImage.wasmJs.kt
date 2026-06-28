package io.ak1.drawboxsample.save

import androidx.compose.ui.geometry.Size
import org.jetbrains.skia.Image
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get

actual fun pasteImageFromClipboard(
    onLoaded: (ByteArray, Size) -> Unit,
) {
    // navigator.clipboard.read() returns Promise<ClipboardItem[]>. On
    // browsers without the Async Clipboard API (older Safari, some
    // mobile Chromium variants), this silently no-ops. The whole chain
    // is kept in `js("...")` because WasmJS's typed bindings for the
    // Async Clipboard API are incomplete — going through dynamic JS
    // avoids fighting JsAny / Promise<JsAny> ceremony for what's
    // ultimately a four-line callback chain.
    readClipboardImageAsArrayBuffer { buffer ->
        val bytes = arrayBufferToBytes(buffer)
        val size = runCatching {
            val img = Image.makeFromEncoded(bytes)
            Size(img.width.toFloat(), img.height.toFloat())
        }.getOrElse { Size.Zero }
        onLoaded(bytes, size)
    }
}

@JsFun(
    """(callback) => {
        if (typeof navigator === 'undefined' || !navigator.clipboard || !navigator.clipboard.read) return;
        navigator.clipboard.read().then(function(items) {
            for (var i = 0; i < items.length; i++) {
                var t = items[i].types;
                for (var j = 0; j < t.length; j++) {
                    if (t[j].indexOf('image/') === 0) {
                        items[i].getType(t[j]).then(function(blob) {
                            blob.arrayBuffer().then(function(buffer) {
                                callback(buffer);
                            });
                        });
                        return;
                    }
                }
            }
        }).catch(function() { });
    }""",
)
private external fun readClipboardImageAsArrayBuffer(callback: (ArrayBuffer) -> Unit)

private fun arrayBufferToBytes(buffer: ArrayBuffer): ByteArray {
    val arr = Uint8Array(buffer)
    return ByteArray(arr.length) { i -> arr[i] }
}
