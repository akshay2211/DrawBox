package io.ak1.drawbox.input

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/**
 * Attach an OS-level drag-and-drop target to a composable so files dropped
 * onto it from Finder / Explorer / a browser tab get decoded and surfaced
 * through [onImagesDropped].
 *
 * One callback fire per drop event — if the user drops three files at
 * once, [onImagesDropped] receives a list of three triples in drop order:
 *
 * ```
 * (bytes, intrinsicSize, dropPositionScreen) for each accepted file
 * ```
 *
 * The host is expected to translate `dropPositionScreen` to world space
 * via the viewport and dispatch `Intent.InsertImage` per entry, offsetting
 * subsequent placements so they don't perfectly overlap.
 *
 * Per-platform behavior:
 * - **JVM (Desktop)**: Hooks `Modifier.dragAndDropTarget` and reads
 *   `event.awtTransferable.getTransferData(DataFlavor.javaFileListFlavor)`.
 *   Image MIME sniffing via [io.ak1.drawbox.decodeImageBitmap] isn't run —
 *   we just attempt `ImageIO.read` and skip files that can't be decoded.
 * - **Web (WasmJS / JS)**: Out of scope for this PR. The Compose
 *   Multiplatform drag-drop API doesn't surface a stable web payload
 *   yet; tracked separately.
 * - **Android / iOS**: No-op. Touch platforms don't expose OS-level
 *   drag-drop in the same shape.
 */
@Composable
expect fun Modifier.imageDragAndDropTarget(
    onImagesDropped: (drops: List<DroppedImage>) -> Unit,
): Modifier

/**
 * A single image extracted from a drag-and-drop event.
 *
 * @property bytes raw encoded payload (PNG / JPEG / WebP / GIF / BMP),
 *   whichever the OS handed us. The SDK's `decodeImageBitmap` accepts
 *   the same formats.
 * @property intrinsicSize source pixel dimensions, used by the host to
 *   preserve aspect ratio when sizing the placed element.
 * @property dropPositionScreen the drop point in *screen* pixels — the
 *   host translates to world coords via `viewport.screenToWorld(...)`
 *   before dispatching the insert intent.
 */
data class DroppedImage(
    val bytes: ByteArray,
    val intrinsicSize: Size,
    val dropPositionScreen: Offset,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DroppedImage) return false
        return intrinsicSize == other.intrinsicSize &&
            dropPositionScreen == other.dropPositionScreen &&
            bytes.contentEquals(other.bytes)
    }
    override fun hashCode(): Int {
        var r = intrinsicSize.hashCode()
        r = 31 * r + dropPositionScreen.hashCode()
        r = 31 * r + bytes.size
        return r
    }
}
