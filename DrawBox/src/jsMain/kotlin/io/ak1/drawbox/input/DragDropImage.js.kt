package io.ak1.drawbox.input

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * No-op on legacy Kotlin/JS — same status as the WasmJS actual.
 */
@Composable
actual fun Modifier.imageDragAndDropTarget(
    onImagesDropped: (drops: List<DroppedImage>) -> Unit,
): Modifier = this
