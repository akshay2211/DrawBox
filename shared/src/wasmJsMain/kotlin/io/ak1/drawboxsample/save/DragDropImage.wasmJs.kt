package io.ak1.drawboxsample.save

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * No-op on WasmJS for now. The Compose Multiplatform drag-drop API
 * doesn't surface a stable browser `DataTransfer` payload yet —
 * tracked as a follow-up to #78.
 */
@Composable
actual fun Modifier.imageDragAndDropTarget(
    onImagesDropped: (drops: List<DroppedImage>) -> Unit,
): Modifier = this
