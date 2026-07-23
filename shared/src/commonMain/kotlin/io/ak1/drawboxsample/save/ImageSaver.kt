package io.ak1.drawboxsample.save

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size

/**
 * Interface for saving drawing exports in various formats.
 */
interface ImageSaver {
    /**
     * Save already-encoded PNG bytes to a file the user can keep. The bytes
     * come straight from [io.ak1.drawbox.presentation.viewmodel.DrawBoxController.exportPng]
     * (via [io.ak1.drawbox.domain.model.Event.PngExported]), so no re-encoding
     * is needed here.
     */
    fun savePng(bytes: ByteArray)

    /**
     * Save SVG content as SVG file
     */
    fun saveSvg(svgContent: String)

    /**
     * Save drawing JSON to a file the user can keep.
     */
    fun saveJson(jsonContent: String)

    /**
     * Prompt the user to pick a JSON file and deliver its contents to [onLoaded].
     */
    fun loadJson(onLoaded: (String) -> Unit)

    /**
     * Prompt the user to pick a bitmap image and deliver the raw encoded
     * bytes plus its intrinsic pixel size to [onLoaded]. Implementations
     * should silently no-op when cancelled. Errors (unreadable file, decode
     * failure) should be logged but not propagated — the sample app treats
     * this as a best-effort affordance.
     */
    fun loadImage(onLoaded: (bytes: ByteArray, intrinsicSize: Size) -> Unit)
}

@Composable
expect fun rememberImageSaver(): ImageSaver
