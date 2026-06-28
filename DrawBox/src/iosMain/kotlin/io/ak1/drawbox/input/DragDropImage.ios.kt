package io.ak1.drawbox.input

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget

/**
 * iOS drag-and-drop probe. Compose Multiplatform 1.11 exposes
 * `Modifier.dragAndDropTarget` on iOS targets but doesn't (yet) surface
 * a payload accessor on the `DragAndDropEvent` analogous to JVM's
 * `awtTransferable`. Without `DragData` or `nativeEvent`, the contents
 * of the drop session (an array of `NSItemProvider`s in UIKit terms)
 * aren't reachable through the Compose API.
 *
 * Reaching the underlying `UIDropInteraction` requires going outside
 * Compose's modifier — adding `UIDropInteraction` to the host view's
 * `UIView` and implementing `UIDropInteractionDelegate`. That's a real
 * implementation but warrants a separate design pass (locating the host
 * view, lifecycle ownership of the interaction, threading the host
 * callback back through the SDK boundary).
 *
 * Tracked as part of the residual on issue #78.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
actual fun Modifier.imageDragAndDropTarget(
    onImagesDropped: (drops: List<DroppedImage>) -> Unit,
): Modifier {
    // Keep the modifier present so the host's call site stays
    // platform-independent and we can fill in this body later without
    // touching HomeScreen. shouldStartDragAndDrop = false makes Compose
    // never accept a drop — equivalent to a no-op modifier, but the
    // chain is in place for when payload extraction lands.
    val target = remember {
        object : DragAndDropTarget {
            override fun onDrop(event: DragAndDropEvent): Boolean = false
        }
    }
    return this.then(
        Modifier.dragAndDropTarget(
            shouldStartDragAndDrop = { false },
            target = target,
        ),
    )
}
