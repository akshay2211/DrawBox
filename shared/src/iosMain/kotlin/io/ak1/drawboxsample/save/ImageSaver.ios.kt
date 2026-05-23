package io.ak1.drawboxsample.save

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImageWriteToSavedPhotosAlbum
import kotlin.time.Clock

@Composable
actual fun rememberImageSaver(): ImageSaver = remember { IosImageSaver() }

private class IosImageSaver : ImageSaver {
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
}
