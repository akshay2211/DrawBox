package io.ak1.drawbox.domain.usecase

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.ak1.drawbox.PathCache
import io.ak1.drawbox.TextLayoutCache
import io.ak1.drawbox.domain.model.Element
import io.ak1.drawbox.domain.model.bounds
import io.ak1.drawbox.encodeToPng
import io.ak1.drawbox.renderElement
import kotlin.math.roundToInt

/**
 * Rasterizes a list of [Element]s to PNG bytes. The raster counterpart to
 * [SvgExporter]: where SVG re-derives each element's geometry as markup, this
 * exporter renders through the SDK's real Compose renderer
 * ([renderElement]) into an offscreen [ImageBitmap], so the output is
 * pixel-identical to what the on-screen canvas draws — no separate drawing
 * path to drift.
 *
 * Fidelity notes:
 * - **Paths, shapes, images** render at full fidelity headlessly. Images are
 *   decoded synchronously (the async on-screen cache is bypassed).
 * - **Text** needs a [TextMeasurer] to lay out — pass one from a composable via
 *   `rememberTextMeasurer()` for real glyphs. When [textMeasurer] is null, text
 *   elements render as their placeholder wrap-box outline (matching the
 *   renderer's own no-measurer fallback). JSON remains the lossless format.
 */
object PngExporter {

    /**
     * @param elements scene to render, drawn in ascending `zIndex` order.
     * @param scale device-pixel multiplier over world units (e.g. `2f` for a
     *   HiDPI-crisp export). Automatically reduced if it would push either side
     *   past [maxDimension].
     * @param padding world-unit margin added on every side, matching
     *   [SvgExporter]'s 20 px default.
     * @param background fill drawn behind the scene; `null` leaves a fully
     *   transparent backdrop.
     * @param maxDimension hard cap (in pixels) on either output side, guarding
     *   against multi-gigabyte bitmaps from a large scene × large [scale].
     * @param textMeasurer optional measurer for real text layout; see class doc.
     * @return PNG bytes, or `null` when [elements] is empty or encoding fails.
     */
    fun exportToPng(
        elements: List<Element>,
        scale: Float = 1f,
        padding: Float = 20f,
        background: Color? = null,
        maxDimension: Int = 4096,
        textMeasurer: TextMeasurer? = null,
    ): ByteArray? {
        if (elements.isEmpty()) return null

        val boxes = elements.map { it.bounds() }
        val left = boxes.minOf { it.left } - padding
        val top = boxes.minOf { it.top } - padding
        val right = boxes.maxOf { it.right } + padding
        val bottom = boxes.maxOf { it.bottom } + padding

        val contentW = (right - left).coerceAtLeast(1f)
        val contentH = (bottom - top).coerceAtLeast(1f)

        // Clamp the requested scale down uniformly so neither axis exceeds the
        // pixel cap; preserves aspect ratio.
        val overflow = maxOf(contentW * scale / maxDimension, contentH * scale / maxDimension, 1f)
        val effScale = (scale / overflow).coerceAtLeast(Float.MIN_VALUE)

        val pxW = (contentW * effScale).roundToInt().coerceIn(1, maxDimension)
        val pxH = (contentH * effScale).roundToInt().coerceIn(1, maxDimension)

        val bitmap = ImageBitmap(pxW, pxH)
        val canvas = Canvas(bitmap)

        CanvasDrawScope().draw(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            canvas = canvas,
            size = Size(pxW.toFloat(), pxH.toFloat()),
        ) {
            background?.let { drawRect(color = it) }
            // World → device: scale to pixels, then shift the padded top-left of
            // the content to the bitmap origin.
            scale(scaleX = effScale, scaleY = effScale, pivot = Offset.Zero) {
                translate(left = -left, top = -top) {
                    val pathCache = PathCache()
                    val textCache = if (textMeasurer != null) TextLayoutCache() else null
                    elements.sortedBy { it.zIndex }.forEach { element ->
                        renderElement(
                            element = element,
                            pathCache = pathCache,
                            imageCache = null,
                            textCache = textCache,
                            textMeasurer = textMeasurer,
                            viewportScale = effScale,
                        )
                    }
                }
            }
        }

        return encodeToPng(bitmap)
    }
}
