package io.ak1.drawbox

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Encode an [ImageBitmap] as PNG bytes, or `null` if encoding fails. The
 * inverse of [decodeImageBitmap]; used by
 * [io.ak1.drawbox.domain.usecase.PngExporter] to turn a rasterized scene into
 * a file-ready payload. Implementations are platform-specific because the host
 * graphics stack drives encoding:
 *
 * - Android: `Bitmap.compress(PNG, ...)`
 * - JVM / iOS / Web: `org.jetbrains.skia.Image.encodeToData(PNG)` via skiko
 */
internal expect fun encodeToPng(bitmap: ImageBitmap): ByteArray?
