package io.ak1.drawbox.domain.model

sealed class Event {
    data class ElementAdded(val element: Element) : Event()
    data class ElementUpdated(val element: Element) : Event()
    data class ElementDeleted(val elementId: String) : Event()
    data class HistoryChanged(val canUndo: Boolean, val canRedo: Boolean) : Event()
    data class SvgExported(val svg: String) : Event()
    data class PngExported(val bytes: ByteArray) : Event() {
        // ByteArray uses identity equality by default; compare by content so
        // two exports of the same payload are treated as equal.
        override fun equals(other: Any?): Boolean =
            this === other || (other is PngExported && bytes.contentEquals(other.bytes))

        override fun hashCode(): Int = bytes.contentHashCode()
    }
    data class JsonExported(val json: String) : Event()
    data class DrawingLoaded(val state: State) : Event()
    data class Error(val message: String, val throwable: Throwable? = null) : Event()

    /**
     * The user requested in-place editing of a text element — a double-tap on
     * an [Element.Text] in [Mode.SELECT], or a second tap on an
     * already-selected one. The element is selected as a side effect; the host
     * opens its inline editor for [id]. See docs/rfcs/0001-text-elements.md.
     */
    data class TextEditRequested(val id: String) : Event()
}
