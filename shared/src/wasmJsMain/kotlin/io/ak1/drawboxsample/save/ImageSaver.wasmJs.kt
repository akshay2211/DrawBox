package io.ak1.drawboxsample.save

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlinx.browser.document
import org.jetbrains.skia.Image
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.khronos.webgl.set
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event as DomEvent
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.FileReader
import kotlin.time.Clock
import kotlin.js.ExperimentalWasmJsInterop

@Composable
actual fun rememberImageSaver(): ImageSaver = remember { WasmJsImageSaver() }

@OptIn(ExperimentalWasmJsInterop::class)
@Suppress("UNCHECKED_CAST")
private class WasmJsImageSaver : ImageSaver {
    override fun savePng(bytes: ByteArray) {
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

    override fun saveJson(jsonContent: String) {
        val parts = JsArray<JsAny?>()
        parts[0] = jsonContent.toJsString()
        val blob = Blob(parts, BlobPropertyBag(type = "application/json"))
        val url = URL.createObjectURL(blob)
        val anchor = document.createElement("a") as HTMLAnchorElement
        anchor.href = url
        anchor.download = "DrawBox-${Clock.System.now().nanosecondsOfSecond}.json"
        anchor.click()
        URL.revokeObjectURL(url)
    }

    override fun loadJson(onLoaded: (String) -> Unit) {
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = "application/json,.json"
        input.onchange = { _: DomEvent ->
            val file = input.files?.item(0)
            if (file != null) {
                val reader = FileReader()
                reader.onload = { _: DomEvent ->
                    onLoaded(reader.result.toString())
                }
                reader.readAsText(file)
            }
        }
        input.click()
    }

    override fun loadImage(onLoaded: (ByteArray, Size) -> Unit) {
        val input = document.createElement("input") as HTMLInputElement
        input.type = "file"
        input.accept = "image/*"
        input.onchange = { _: DomEvent ->
            val file = input.files?.item(0)
            if (file != null) {
                val reader = FileReader()
                reader.onload = { _: DomEvent ->
                    val buffer = reader.result as? ArrayBuffer
                    if (buffer != null) {
                        val arr = Uint8Array(buffer)
                        val bytes = ByteArray(arr.length) { i -> arr[i] }
                        // Decode intrinsic size by handing the same bytes to
                        // skia — identical to what the SDK does at render
                        // time, just used here for placement math.
                        val size = runCatching {
                            val img = Image.makeFromEncoded(bytes)
                            Size(img.width.toFloat(), img.height.toFloat())
                        }.getOrElse { Size.Zero }
                        onLoaded(bytes, size)
                    }
                }
                reader.readAsArrayBuffer(file)
            }
        }
        input.click()
    }
}
