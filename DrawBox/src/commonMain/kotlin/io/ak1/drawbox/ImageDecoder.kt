package io.ak1.drawbox

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decode an encoded image payload (PNG / JPEG / WebP) into an [ImageBitmap]
 * usable by the renderer. Returns `null` if the bytes can't be decoded; the
 * caller renders a placeholder instead of crashing. Implementations are
 * platform-specific because the host graphics stack drives decoding:
 *
 * - Android: `BitmapFactory.decodeByteArray(...)`
 * - JVM / iOS / WASM: `org.jetbrains.skia.Image.makeFromEncoded(...)` via skiko
 */
expect fun decodeImageBitmap(bytes: ByteArray): ImageBitmap?

/**
 * Decode and downsample [bytes] to fit within [targetWidth] × [targetHeight]
 * while preserving aspect ratio. The returned bitmap's longer side may be
 * smaller than `targetWidth` / `targetHeight` if the shorter axis hits the
 * cap first, but never larger.
 *
 * Used by the renderer to avoid storing source-resolution bitmaps for
 * thumbnail-sized placements — a 4096×3000 photo placed at 600×450 stays at
 * roughly that resolution in memory instead of the full 12 MP.
 *
 * Per-platform strategy:
 * - Android: `BitmapFactory.Options.inSampleSize` — decodes at sub-resolution
 *   directly, avoiding the full-size intermediate bitmap.
 * - Skia (JVM / iOS / Web): decode at source, draw scaled into a smaller
 *   `Surface`, snapshot. The full-size intermediate is short-lived.
 *
 * Returns `null` when bytes can't be decoded (matches [decodeImageBitmap]).
 */
expect fun decodeImageBitmapDownsampled(

    bytes: ByteArray,
    targetWidth: Int,
    targetHeight: Int,
): ImageBitmap?
