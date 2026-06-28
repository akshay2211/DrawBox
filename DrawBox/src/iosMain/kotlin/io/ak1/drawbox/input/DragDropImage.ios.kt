package io.ak1.drawbox.input

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * No-op on iOS. The natural insertion path is PHPicker (issue #80, wired
 * in `ImageSaver.ios.kt`).
 */
@Composable
actual fun Modifier.imageDragAndDropTarget(
    onImagesDropped: (drops: List<DroppedImage>) -> Unit,
): Modifier = this
