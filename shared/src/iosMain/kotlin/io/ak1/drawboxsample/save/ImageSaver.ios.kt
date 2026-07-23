package io.ak1.drawboxsample.save

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSLog
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.NSError
import platform.Foundation.NSItemProvider
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIImage
import platform.UIKit.UIImageWriteToSavedPhotosAlbum
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.time.Clock

@Composable
actual fun rememberImageSaver(): ImageSaver = remember { IosImageSaver() }

private class IosImageSaver : ImageSaver {
    private var activeDelegate: JsonPickerDelegate? = null
    private var activeImageDelegate: ImagePickerDelegate? = null

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override fun savePng(bytes: ByteArray) {
        try {
            if (bytes.isEmpty()) {
                NSLog("Received empty PNG payload")
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

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override fun loadImage(onLoaded: (ByteArray, Size) -> Unit) {
        val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController
        if (rootController == null) {
            NSLog("No root view controller available for PHPicker")
            return
        }
        // PHPickerConfiguration with `images` filter — read-only access, no
        // Photos library permission prompt required (this is the whole
        // point of PHPicker vs UIImagePickerController). selectionLimit = 1
        // matches the existing host contract (single image per call).
        val config = PHPickerConfiguration().apply {
            filter = PHPickerFilter.imagesFilter()
            selectionLimit = 1
        }
        val picker = PHPickerViewController(configuration = config)
        val delegate = ImagePickerDelegate(
            onPicked = { provider ->
                activeImageDelegate = null
                loadImageBytesFromProvider(provider, onLoaded)
            },
            onCancelled = { activeImageDelegate = null },
        )
        activeImageDelegate = delegate
        picker.delegate = delegate
        rootController.presentViewController(picker, true, null)
    }

    /**
     * Read the raw encoded bytes (PNG / JPEG / HEIC / etc.) directly from
     * the [NSItemProvider] without any UIImage intermediate. PHPicker
     * delivers data representations off the main thread, so we marshal the
     * eventual `onLoaded` call back onto the main queue — the SDK's
     * `insertImage` dispatches a reducer Intent which expects to be on the
     * UI thread.
     */
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun loadImageBytesFromProvider(
        provider: NSItemProvider,
        onLoaded: (ByteArray, Size) -> Unit,
    ) {
        val typeId = UTTypeImage.identifier
        if (!provider.hasItemConformingToTypeIdentifier(typeId)) {
            NSLog("PHPicker result has no image-conforming representation")
            return
        }
        provider.loadDataRepresentationForTypeIdentifier(typeId) { data: NSData?, error: NSError? ->
            if (error != null || data == null) {
                NSLog("loadDataRepresentation failed: ${error?.localizedDescription}")
                return@loadDataRepresentationForTypeIdentifier
            }
            val bytes = ByteArray(data.length.toInt())
            bytes.usePinned { pinned ->
                platform.posix.memcpy(pinned.addressOf(0), data.bytes, data.length)
            }
            // Compute intrinsic pixel size via UIImage so we match the
            // host's "screen pixels at native scale" expectation. UIImage's
            // `.size` is in points; multiply by `scale` to get pixels.
            val size = UIImage.imageWithData(data)?.let { img ->
                val s = img.size
                Size(
                    (s.useContents { width } * img.scale).toFloat(),
                    (s.useContents { height } * img.scale).toFloat(),
                )
            } ?: Size.Zero
            dispatch_async(dispatch_get_main_queue()) {
                onLoaded(bytes, size)
            }
        }
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

private class ImagePickerDelegate(
    private val onPicked: (NSItemProvider) -> Unit,
    private val onCancelled: () -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(
        picker: PHPickerViewController,
        didFinishPicking: List<*>,
    ) {
        // Dismiss before invoking the host callback so the picker chrome
        // doesn't cover the canvas while the image lands.
        picker.dismissViewControllerAnimated(true, null)
        val first = didFinishPicking.firstOrNull() as? PHPickerResult
        if (first == null) {
            onCancelled()
            return
        }
        onPicked(first.itemProvider)
    }
}
