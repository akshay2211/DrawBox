package io.ak1.drawboxsample.save

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.browser.document
import org.jetbrains.skia.EncodedImageFormat
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

@Composable
actual fun rememberImageSaver(): ImageSaver = remember { JsImageSaver() }

/**
 * Legacy Kotlin/JS implementation. Mirrors the WasmJS version but uses
 * dynamic-typed `Array` parts instead of `JsArray<JsAny?>` — the
 * `Blob(parts, options)` constructor on the JS backend accepts a
 * `dynamic`-typed array directly, so the WasmJS interop wrappers
 * (`toJsString`, `JsArray`) aren't needed here.
 */
private class JsImageSaver : ImageSaver {
    override fun savePng(bitmap: ImageBitmap) {
        val bytes = Image.makeFromBitmap(bitmap.asSkiaBitmap())
            .encodeToData(EncodedImageFormat.PNG)?.bytes ?: return
        val arr = Uint8Array(bytes.size)
        for (i in bytes.indices) arr[i] = bytes[i]
        downloadBlob(
            Blob(arrayOf(arr), BlobPropertyBag(type = "image/png")),
            "DrawBox-${Clock.System.now().nanosecondsOfSecond}.png",
        )
    }

    override fun saveSvg(svgContent: String) {
        downloadBlob(
            Blob(arrayOf(svgContent), BlobPropertyBag(type = "image/svg+xml")),
            "DrawBox-${Clock.System.now().nanosecondsOfSecond}.svg",
        )
    }

    override fun saveJson(jsonContent: String) {
        downloadBlob(
            Blob(arrayOf(jsonContent), BlobPropertyBag(type = "application/json")),
            "DrawBox-${Clock.System.now().nanosecondsOfSecond}.json",
        )
    }

    private fun downloadBlob(blob: Blob, name: String) {
        val url = URL.createObjectURL(blob)
        val anchor = document.createElement("a") as HTMLAnchorElement
        anchor.href = url
        anchor.download = name
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
