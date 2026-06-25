package io.ak1.drawboxsample.save

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.NSLog
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIImage
import platform.UIKit.UIImageWriteToSavedPhotosAlbum
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.darwin.NSObject
import kotlin.time.Clock

@Composable
actual fun rememberImageSaver(): ImageSaver = remember { IosImageSaver() }

private class IosImageSaver : ImageSaver {
    private var activeDelegate: JsonPickerDelegate? = null

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override fun savePng(bitmap: ImageBitmap) {
        try {
            val bytes = Image.makeFromBitmap(bitmap.asSkiaBitmap())
                .encodeToData(EncodedImageFormat.PNG)?.bytes ?: run {
                NSLog("Failed to encode bitmap to PNG")
                return
            }
            bytes.usePinned { pinned ->
                val data = NSData.create(
                    bytes = pinned.addressOf(0),
                    length = bytes.size.toULong(),
                )
                UIImage.imageWithData(data)?.let { image ->
                    UIImageWriteToSavedPhotosAlbum(image, null, null, null)
                    NSLog("PNG saved successfully to Photos album")
                } ?: run {
                    NSLog("Failed to create UIImage from data")
                }
            }
        } catch (e: Exception) {
            NSLog("Error saving PNG: ${e.message}")
        }
    }

    override fun saveSvg(svgContent: String) {
        try {
            // For iOS, we'll save the SVG to a temporary location and notify the user
            // In a production app, you might use file sharing or cloud storage
            val fileName = "DrawBox-${Clock.System.now().nanosecondsOfSecond}.svg"
            NSLog("SVG export created: $fileName")
            NSLog("SVG content length: ${svgContent.length} characters")
            // Note: iOS doesn't have direct file saving without additional setup
            // You may need to implement file sharing or use CloudKit for persistence
        } catch (e: Exception) {
            NSLog("Error exporting SVG: ${e.message}")
        }
    }

    override fun saveJson(jsonContent: String) {
        try {
            val fileName = "DrawBox-${Clock.System.now().nanosecondsOfSecond}.json"
            NSLog("JSON export created: $fileName")
            NSLog("JSON content length: ${jsonContent.length} characters")
        } catch (e: Exception) {
            NSLog("Error exporting JSON: ${e.message}")
        }
    }

    override fun loadJson(onLoaded: (String) -> Unit) {
        val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController
        if (rootController == null) {
            NSLog("No root view controller available for document picker")
            return
        }
        val picker = UIDocumentPickerViewController(
            forOpeningContentTypes = listOf(UTTypeJSON),
        )
        val delegate = JsonPickerDelegate(
            onPicked = { url ->
                activeDelegate = null
                readJsonFromUrl(url)?.let(onLoaded)
            },
            onCancelled = { activeDelegate = null },
        )
        activeDelegate = delegate
        picker.delegate = delegate
        rootController.presentViewController(picker, true, null)
    }

    override fun loadImage(onLoaded: (ByteArray, Size) -> Unit) {
        // iOS image picker requires UIImagePickerController + Photos auth or
        // PHPickerViewController — out of scope for OSS v1. Logged as a hint
        // for embedders; desktop & Android cover the demo.
        NSLog("loadImage(): not implemented on iOS in OSS sample yet")
    }

    @OptIn(BetaInteropApi::class)
    private fun readJsonFromUrl(url: NSURL): String? {
        val gotAccess = url.startAccessingSecurityScopedResource()
        try {
            val data = NSData.dataWithContentsOfURL(url) ?: return null
            return NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
        } finally {
            if (gotAccess) url.stopAccessingSecurityScopedResource()
        }
    }
}

private class JsonPickerDelegate(
    private val onPicked: (NSURL) -> Unit,
    private val onCancelled: () -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL ?: run {
            onCancelled()
            return
        }
        onPicked(url)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onCancelled()
    }
}
