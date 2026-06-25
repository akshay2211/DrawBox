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
