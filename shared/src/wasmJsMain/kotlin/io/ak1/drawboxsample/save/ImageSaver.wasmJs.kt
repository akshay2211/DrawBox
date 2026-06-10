package io.ak1.drawboxsample.save

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlinx.browser.document
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.set
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import kotlin.time.Clock
import kotlin.js.ExperimentalWasmJsInterop

@Composable
actual fun rememberImageSaver(): ImageSaver = remember { WasmJsImageSaver() }

@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("UNCHECKED_CAST")
private class WasmJsImageSaver : ImageSaver {
    override fun savePng(bitmap: ImageBitmap) {
        val bytes = Image.makeFromBitmap(bitmap.asSkiaBitmap())
            .encodeToData(EncodedImageFormat.PNG)?.bytes ?: return
        val arr = Uint8Array(bytes.size)
        for (i in bytes.indices) arr[i] = bytes[i]
        val parts = JsArray<JsAny?>()
        parts[0] = arr
        val blob = Blob(parts, BlobPropertyBag(type = "image/png"))
        val url = URL.createObjectURL(blob)
        val anchor = document.createElement("a") as HTMLAnchorElement
        anchor.href = url
        anchor.download = "DrawBox-${Clock.System.now().nanosecondsOfSecond}.png"
        anchor.click()
        URL.revokeObjectURL(url)
    }

    override fun saveSvg(svgContent: String) {
        val parts = JsArray<JsAny?>()
        parts[0] = svgContent.toJsString()
        val blob = Blob(parts, BlobPropertyBag(type = "image/svg+xml"))
        val url = URL.createObjectURL(blob)
        val anchor = document.createElement("a") as HTMLAnchorElement
        anchor.href = url
        anchor.download = "DrawBox-${Clock.System.now().nanosecondsOfSecond}.svg"
        anchor.click()
        URL.revokeObjectURL(url)
    }
}
