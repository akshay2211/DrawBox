package io.ak1.drawbox.domain.model

import androidx.compose.ui.graphics.ImageBitmap

sealed class Event {
    data class ElementAdded(val element: Element) : Event()
    data class ElementUpdated(val element: Element) : Event()
    data class ElementDeleted(val elementId: String) : Event()
    data class HistoryChanged(val canUndo: Boolean, val canRedo: Boolean) : Event()
    data class PngSaved(val bitmap: ImageBitmap?, val throwable: Throwable?) : Event()
    data class DrawingLoaded(val state: State) : Event()
    data class Error(val message: String, val throwable: Throwable? = null) : Event()
}
