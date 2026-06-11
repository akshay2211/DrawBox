package io.ak1.drawbox.domain.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter

/**
 * Repeating image painted behind drawing elements, sitting on top of the
 * solid [State.bgColor] but underneath all strokes and shapes.
 *
 * The [painter] is tiled across the canvas at its intrinsic size. Pass an
 * SVG via `painterResource(...)` for vector tiling that stays sharp at any
 * zoom level.
 *
 * When [tint] is provided, the painter is recolored using
 * [androidx.compose.ui.graphics.BlendMode.SrcIn] — works well for
 * monochrome / alpha-driven SVGs. Multi-color SVGs will be collapsed to the
 * tint color; pass `null` to keep the painter's original colors.
 *
 * Not included in JSON/SVG export; treat as a runtime decoration.
 */
data class BackgroundPattern(
    val painter: Painter,
    val tint: Color? = null,
)
