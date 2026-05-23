package io.ak1.drawboxsample.save

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Interface for saving drawing exports in various formats.
 */
interface ImageSaver {
    /**
     * Save a bitmap as PNG image
     */
    fun savePng(bitmap: ImageBitmap)

    /**
     * Save SVG content as SVG file
     */
    fun saveSvg(svgContent: String)
}

@Composable
expect fun rememberImageSaver(): ImageSaver