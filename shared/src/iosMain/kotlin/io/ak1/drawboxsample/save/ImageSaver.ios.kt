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
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImageWriteToSavedPhotosAlbum

@Composable
actual fun rememberImageSaver(): ImageSaver = remember { IosImageSaver() }

private class IosImageSaver : ImageSaver {
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override fun save(bitmap: ImageBitmap) {
        val bytes = Image.makeFromBitmap(bitmap.asSkiaBitmap())
            .encodeToData(EncodedImageFormat.PNG)?.bytes ?: return
        bytes.usePinned { pinned ->
            val data = NSData.create(
                bytes = pinned.addressOf(0),
                length = bytes.size.toULong(),
            )
            UIImage.imageWithData(data)?.let { image ->
                UIImageWriteToSavedPhotosAlbum(image, null, null, null)
            }
        }
    }
}