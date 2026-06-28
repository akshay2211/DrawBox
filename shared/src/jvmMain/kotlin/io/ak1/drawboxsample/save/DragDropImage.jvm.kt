package io.ak1.drawboxsample.save

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DropTargetDropEvent
import java.io.File
import javax.imageio.ImageIO

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
actual fun Modifier.imageDragAndDropTarget(
    onImagesDropped: (drops: List<DroppedImage>) -> Unit,
): Modifier {
    // Cache the target across recompositions so AWT doesn't re-register a
    // new DropTarget for every recomp (each registration walks the parent
    // window's hierarchy on Linux + Windows — non-trivial cost).
    val target = remember(onImagesDropped) {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean {
                val transferable = event.awtTransferable
                if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                    return false
                }
                @Suppress("UNCHECKED_CAST")
                val files = (transferable.getTransferData(DataFlavor.javaFileListFlavor)
                    as? List<File>) ?: return false

                // Drop position from the underlying AWT
                // DropTargetDropEvent. Coordinates are window-local px,
                // which the host's viewport math (`screenToWorld`)
                // expects directly. Fall back to viewport center if the
                // platform somehow surfaces an unexpected event type.
                val dropPos = (event.nativeEvent as? DropTargetDropEvent)?.let {
                    Offset(it.location.x.toFloat(), it.location.y.toFloat())
                } ?: Offset.Zero

                val drops = files.mapNotNull { file -> file.toDroppedImage(dropPos) }
                if (drops.isEmpty()) return false
                onImagesDropped(drops)
                return true
            }
        }
    }
    return this.then(
        Modifier.dragAndDropTarget(
            shouldStartDragAndDrop = { event ->
                event.awtTransferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
            },
            target = target,
        ),
    )
}

/**
 * Read the file's bytes and decode its intrinsic dimensions. Returns null
 * when the file isn't an image ImageIO can read — the user dragging a PDF
 * or a folder onto the canvas shouldn't crash, just skip.
 */
private fun File.toDroppedImage(dropPos: Offset): DroppedImage? {
    val bytes = try {
        readBytes()
    } catch (e: Throwable) {
        return null
    }
    val size: Size = try {
        val decoded = ImageIO.read(this)
        if (decoded != null) Size(decoded.width.toFloat(), decoded.height.toFloat())
        else return null  // Not an image format ImageIO knows about.
    } catch (e: Throwable) {
        return null
    }
    return DroppedImage(bytes = bytes, intrinsicSize = size, dropPositionScreen = dropPos)
}
