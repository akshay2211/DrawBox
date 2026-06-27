package io.ak1.drawbox.text

import androidx.compose.ui.text.font.FontFamily
import io.ak1.drawbox.domain.model.BuiltinFontFamilyKeys
import io.ak1.drawbox.domain.model.DEFAULT_FONT_FAMILY_KEY

/**
 * In-memory registry mapping a [String] key (as stored on
 * [io.ak1.drawbox.domain.model.Element.Text.fontFamilyKey]) to a
 * Compose [FontFamily]. The registry is process-wide and intentionally
 * non-persistent — hosts re-register their custom families on app launch,
 * which keeps the SDK free of font-asset bundling and version skew.
 *
 * Three built-in keys (`sans`, `serif`, `mono`) are pre-registered to
 * Compose's platform defaults so an embedder that does nothing special
 * still gets readable text.
 *
 * Lookups for unknown keys fall back to `sans` and log a hint (the OSS
 * SDK doesn't bundle a logger so the warning is best-effort via `println`).
 */
object FontRegistry {
    private val families: MutableMap<String, FontFamily> = mutableMapOf(
        BuiltinFontFamilyKeys.SANS to FontFamily.SansSerif,
        BuiltinFontFamilyKeys.SERIF to FontFamily.Serif,
        BuiltinFontFamilyKeys.MONO to FontFamily.Monospace,
    )

    fun register(key: String, family: FontFamily) {
        families[key] = family
    }

    /**
     * Resolve [key] to a [FontFamily]. Falls back to the registered default
     * ([DEFAULT_FONT_FAMILY_KEY]) when [key] is unknown. Returns
     * [FontFamily.SansSerif] when even the default is missing — that's only
     * possible if a host explicitly removed it (e.g. via [clear]).
     */
    fun resolve(key: String): FontFamily {
        families[key]?.let { return it }
        println("[DrawBox] Font family key '$key' not registered; falling back to '$DEFAULT_FONT_FAMILY_KEY'.")
        return families[DEFAULT_FONT_FAMILY_KEY] ?: FontFamily.SansSerif
    }

    /** Returns every currently registered key. Useful for picker UIs. */
    fun keys(): Set<String> = families.keys.toSet()

    /** Test-only: drop every registration including the built-ins. */
    internal fun clear() {
        families.clear()
    }
}
