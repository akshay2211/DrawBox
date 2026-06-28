package io.ak1.drawbox.input

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * No-op on WasmJS. Investigated against Compose Multiplatform 1.11 — the
 * `androidx.compose.ui.draganddrop` package doesn't expose either:
 *
 * - `DragData` (the cross-platform abstraction over `DataTransfer`), nor
 * - `DragAndDropEvent.nativeEvent` (the browser `DragEvent` escape hatch
 *   we use on JVM via `awtTransferable`).
 *
 * Both would land on web targets in a future Compose release; until they
 * do, the only path is raw DOM `dragover` / `drop` listeners attached
 * outside Compose's modifier. That's a real implementation but warrants
 * a separate design pass (positioning the listener relative to the
 * canvas bounds, filtering drops, cleaning up on disposal).
 *
 * Tracked as the residual on issue #78.
 */
@Composable
actual fun Modifier.imageDragAndDropTarget(
    onImagesDropped: (drops: List<DroppedImage>) -> Unit,
): Modifier = this
