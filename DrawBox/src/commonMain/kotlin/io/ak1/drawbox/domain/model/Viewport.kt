package io.ak1.drawbox.domain.model

import androidx.compose.ui.geometry.Offset

/**
 * Camera transform for the infinite canvas. Elements are stored in **world**
 * coordinates; the viewport projects them onto the **screen** using a translate
 * + uniform-scale transform.
 *
 * Screen-pixel point `s` and world point `w` relate as:
 *   `s = w * scale + offset`
 *   `w = (s - offset) / scale`
 *
 * Camera state is session state — not serialized, not in undo history. Mutate
 * by returning a new [Viewport] via [panBy], [zoomBy], or [zoomTo].
 */
data class Viewport(
    val offset: Offset = Offset.Zero,
    val scale: Float = 1f,
) {
    fun screenToWorld(screenPos: Offset): Offset = (screenPos - offset) / scale

    fun worldToScreen(worldPos: Offset): Offset = worldPos * scale + offset

    /** Translate the camera by [delta] screen pixels. */
    fun panBy(delta: Offset): Viewport = copy(offset = offset + delta)

    /**
     * Zoom by [factor] keeping the world point under [focalScreen] anchored to
     * the same screen pixel. Same math as compose-infinite-canvas, just lifted
     * onto an immutable value type.
     */
    fun zoomBy(factor: Float, focalScreen: Offset): Viewport {
        val newScale = (scale * factor).coerceIn(MIN_SCALE, MAX_SCALE)
        if (newScale == scale) return this
        val actual = newScale / scale
        return Viewport(
            offset = focalScreen - (focalScreen - offset) * actual,
            scale = newScale,
        )
    }

    /** Set scale to [targetScale] anchored at [focalScreen]. */
    fun zoomTo(targetScale: Float, focalScreen: Offset): Viewport {
        val clamped = targetScale.coerceIn(MIN_SCALE, MAX_SCALE)
        if (clamped == scale) return this
        val factor = clamped / scale
        return Viewport(
            offset = focalScreen - (focalScreen - offset) * factor,
            scale = clamped,
        )
    }

    /** Integer percent suitable for UI display. */
    val scalePercent: Int get() = (scale * 100).toInt()

    companion object {
        const val MIN_SCALE: Float = 0.1f
        const val MAX_SCALE: Float = 8f
    }
}
