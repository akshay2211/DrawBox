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
 *
 * ### Web targets (WASM + Kotlin/JS browser) — bundled-font caveat
 *
 * On web targets, Compose renders through Skia-WASM, which bundles a
 * single default font face. The generic [FontFamily.SansSerif],
 * [FontFamily.Serif], and [FontFamily.Monospace] instances all resolve to
 * that one bundled face at glyph time, so `sans` / `serif` / `mono`
 * blocks render identically. This is a Skia-WASM limitation downstream
 * of the SDK, not a bug in the registry.
 *
 * To differentiate the built-in keys on web, register real fonts via
 * Compose Multiplatform Resources at app launch:
 *
 * ```kotlin
 * controller.registerFont(BuiltinFontFamilyKeys.SANS,  FontFamily(Font(Res.font.inter_Regular)))
 * controller.registerFont(BuiltinFontFamilyKeys.SERIF, FontFamily(Font(Res.font.crimsonText_Regular)))
 * controller.registerFont(BuiltinFontFamilyKeys.MONO,  FontFamily(Font(Res.font.jetBrainsMono_Regular)))
 * ```
 *
 * The registry emits a one-shot dev-console warning per unresolved key
 * on web to make the issue visible during development. See #89.
 *
 * SVG export is unaffected — the exporter emits `font-family="sans"`
 * etc. as attributes, and the browser picks a system font when the SVG
 * is opened.
 */
object FontRegistry {
    private val families: MutableMap<String, FontFamily> = mutableMapOf(
        BuiltinFontFamilyKeys.SANS to FontFamily.SansSerif,
        BuiltinFontFamilyKeys.SERIF to FontFamily.Serif,
        BuiltinFontFamilyKeys.MONO to FontFamily.Monospace,
    )

    // Web-only guard for the one-shot generic-family warning. Non-web
    // targets never write to this set.
    private val webWarnedKeys: MutableSet<String> = mutableSetOf()

    fun register(key: String, family: FontFamily) {
        families[key] = family
        // Host replaced a generic mapping — future resolves for this key
        // should re-warn if they ever get downgraded again.
        webWarnedKeys.remove(key)
    }

    /**
     * Resolve [key] to a [FontFamily]. Falls back to the registered default
     * ([DEFAULT_FONT_FAMILY_KEY]) when [key] is unknown. Returns
     * [FontFamily.SansSerif] when even the default is missing — that's only
     * possible if a host explicitly removed it (e.g. via [clear]).
     *
     * On web targets, emits a one-shot `println` warning per key when the
     * resolved family is one of the generic Compose defaults — that's the
     * case Skia-WASM cannot differentiate. See the class KDoc for context.
     */
    fun resolve(key: String): FontFamily {
        val direct = families[key]
        if (direct != null) {
            maybeWarnGenericOnWeb(key, direct)
            return direct
        }
        println("[DrawBox] Font family key '$key' not registered; falling back to '$DEFAULT_FONT_FAMILY_KEY'.")
        val fallback = families[DEFAULT_FONT_FAMILY_KEY] ?: FontFamily.SansSerif
        maybeWarnGenericOnWeb(DEFAULT_FONT_FAMILY_KEY, fallback)
        return fallback
    }

    /** Returns every currently registered key. Useful for picker UIs. */
    fun keys(): Set<String> = families.keys.toSet()

    /** Test-only: drop every registration including the built-ins. */
    internal fun clear() {
        families.clear()
        webWarnedKeys.clear()
    }

    private fun maybeWarnGenericOnWeb(key: String, family: FontFamily) {
        if (!isWebTarget()) return
        if (key in webWarnedKeys) return
        val isGeneric = family === FontFamily.SansSerif ||
            family === FontFamily.Serif ||
            family === FontFamily.Monospace
        if (!isGeneric) return
        webWarnedKeys += key
        println(
            "[DrawBox] Font family key '$key' resolves to a generic FontFamily " +
                "on a web target. Skia-WASM bundles a single font face, so " +
                "'sans', 'serif', and 'mono' all render identically. Register " +
                "a real font via FontRegistry.register(\"$key\", ...) or " +
                "DrawBoxController.registerFont(...) to differentiate. " +
                "See https://github.com/akshay2211/DrawBox/issues/89",
        )
    }
}