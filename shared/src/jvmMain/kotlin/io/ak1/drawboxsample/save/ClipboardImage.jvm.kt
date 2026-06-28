package io.ak1.drawboxsample.save

import androidx.compose.ui.geometry.Size
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlin.concurrent.thread

actual fun pasteImageFromClipboard(
    onLoaded: (ByteArray, Size) -> Unit,
) {
    // Re-encode runs off the EDT — a large clipboard bitmap can take 100+ ms
    // through ImageIO, and the host fires this from a key handler on the UI
    // thread. Spawn a thread, marshal the result back via SwingUtilities so
    // the SDK's `insertImage` dispatches on the EDT (where Compose Desktop
    // expects state writes).
    thread(name = "drawbox-clipboard-paste", isDaemon = true) {
        val clip = Toolkit.getDefaultToolkit().systemClipboard
        val image: BufferedImage = try {
            if (!clip.isDataFlavorAvailable(DataFlavor.imageFlavor)) return@thread
            clip.getData(DataFlavor.imageFlavor) as? BufferedImage ?: return@thread
        } catch (e: UnsupportedFlavorException) {
            return@thread
        } catch (e: IllegalStateException) {
            // Another process holds the clipboard; the typical AWT response
            // is a transient IllegalStateException. The user can retry; we
            // don't surface this as an error.
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
