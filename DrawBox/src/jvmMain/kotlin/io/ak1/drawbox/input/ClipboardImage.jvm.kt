package io.ak1.drawbox.input

import androidx.compose.ui.geometry.Size
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

actual fun pasteImageFromClipboard(
    onLoaded: (bytes: ByteArray, intrinsicSize: Size) -> Unit,
) {
    // Re-encode runs off the EDT — a large clipboard bitmap can take 100+ ms
    // through ImageIO, and the host fires this from a key handler on the UI
    // thread. Spawn a thread, marshal the result back via SwingUtilities so
    // the SDK's `insertImage` dispatches on the EDT (where Compose Desktop
    // expects state writes).
    thread(name = "drawbox-clipboard-paste", isDaemon = true) {
        val clip = Toolkit.getDefaultToolkit().systemClipboard

        // Tried-in-order list of flavors that can yield a usable image:
        //
        // 1. DataFlavor.imageFlavor — the canonical Java path. Most
        //    screenshot apps and copy-image-from-browser flows expose
        //    this. AWT decodes to a BufferedImage for us.
        //
        // 2. DataFlavor.javaFileListFlavor — Finder / Explorer
        //    "Copy" on an image file lands on the clipboard as a list of
        //    File references, not the bitmap. macOS Cmd+Shift+4 → Cmd+C
        //    also takes this path on some JVMs.
        //
        // 3. Custom "image/png" MIME flavor — some browser image-context
        //    Copy commands expose only the encoded bytes under this MIME.
        //
        // If none of the three pan out, log what WAS on the clipboard so
        // the user can see why a paste was a no-op (instead of silently
        // doing nothing, which was the v1 behavior and led to a bug
        // report).
        val image: BufferedImage = try {
            tryImageFlavor(clip)
                ?: tryFileListFlavor(clip)
                ?: tryPngMimeFlavor(clip)
                ?: run {
                    val available = clip.availableDataFlavors
                        .joinToString(", ") { it.humanPresentableName }
                    println("[DrawBox] Paste: clipboard had no usable image flavor. Available flavors: [$available]")
                    return@thread
                }
        } catch (e: UnsupportedFlavorException) {
            return@thread
        } catch (e: IllegalStateException) {
            // Another process holds the clipboard; transient AWT failure.
            return@thread
        }

        // BufferedImage.TYPE_CUSTOM (which is what some screenshot tools
        // produce) doesn't survive ImageIO PNG writers cleanly — repaint
        // into a TYPE_INT_ARGB intermediate so the encoder always sees a
        // known format.
        val normalized: BufferedImage =
            if (image.type == BufferedImage.TYPE_INT_ARGB ||
                image.type == BufferedImage.TYPE_INT_RGB
            ) {
                image
            } else {
                BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_ARGB).apply {
                    val g = createGraphics()
                    try {
                        g.drawImage(image, 0, 0, null)
                    } finally {
                        g.dispose()
                    }
                }
            }

        val bytes = ByteArrayOutputStream(64 * 1024).use { out ->
            if (!ImageIO.write(normalized, "png", out)) return@thread
            out.toByteArray()
        }
        val size = Size(normalized.width.toFloat(), normalized.height.toFloat())
        SwingUtilities.invokeLater { onLoaded(bytes, size) }
    }
}

private fun tryImageFlavor(clip: java.awt.datatransfer.Clipboard): BufferedImage? {
    if (!clip.isDataFlavorAvailable(DataFlavor.imageFlavor)) return null
    return clip.getData(DataFlavor.imageFlavor) as? BufferedImage
}

private fun tryFileListFlavor(clip: java.awt.datatransfer.Clipboard): BufferedImage? {
    if (!clip.isDataFlavorAvailable(DataFlavor.javaFileListFlavor)) return null
    @Suppress("UNCHECKED_CAST")
    val files = clip.getData(DataFlavor.javaFileListFlavor) as? List<File> ?: return null
    // Pick the first file that decodes as an image. Skip everything else
    // — pasting a folder or a .txt shouldn't insert a placeholder.
    return files.firstNotNullOfOrNull { f ->
        try {
            ImageIO.read(f)
        } catch (e: Throwable) {
            null
        }
    }
}

private fun tryPngMimeFlavor(clip: java.awt.datatransfer.Clipboard): BufferedImage? {
    // Match any "image/png" MIME flavor regardless of representation
    // class. AWT exposes browsers' clipboard-image MIME via this path.
    val pngFlavor = clip.availableDataFlavors.firstOrNull { it.mimeType.startsWith("image/png") }
        ?: return null
    val data = clip.getData(pngFlavor) ?: return null
    return when (data) {
        is BufferedImage -> data
        is java.io.InputStream -> data.use { ImageIO.read(it) }
        is ByteArray -> ImageIO.read(java.io.ByteArrayInputStream(data))
        else -> null
    }
}
