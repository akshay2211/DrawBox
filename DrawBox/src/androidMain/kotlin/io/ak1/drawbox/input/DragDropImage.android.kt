package io.ak1.drawbox.input

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * No-op on Android — touch platforms don't expose OS-level file drag-drop
 * the same way Desktop does. The existing image picker covers this flow.
 */
@Composable
actual fun Modifier.imageDragAndDropTarget(
    onImagesDropped: (drops: List<DroppedImage>) -> Unit,
): Modifier = this
