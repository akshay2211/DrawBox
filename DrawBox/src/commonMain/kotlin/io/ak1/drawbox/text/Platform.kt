package io.ak1.drawbox.text

/**
 * True on web targets (WASM + Kotlin/JS browser) where Skia-WASM bundles a
 * single font face and generic Compose FontFamily identifiers all collapse
 * to that face. Used internally by [FontRegistry] to gate a one-shot dev
 * warning that would only be actionable on affected platforms. See #89.
 */
internal expect fun isWebTarget(): Boolean