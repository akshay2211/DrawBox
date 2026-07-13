package io.ak1.drawboxsample

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import drawboxsample.shared.generated.resources.Res
import drawboxsample.shared.generated.resources.crimsonText_Regular
import drawboxsample.shared.generated.resources.inter_Regular
import drawboxsample.shared.generated.resources.jetBrainsMono_Regular
import io.ak1.drawbox.domain.model.BuiltinFontFamilyKeys
import io.ak1.drawbox.text.FontRegistry
import io.ak1.drawboxsample.ui.HomeScreen
import io.ak1.drawboxsample.ui.theme.DrawBoxTheme
import io.ak1.drawboxsample.ui.theme.ThemeMode
import io.ak1.drawboxsample.ui.theme.resolveIsDark
import org.jetbrains.compose.resources.Font

@Composable
fun App() {
    RegisterBuiltinFonts()
    var themeMode by rememberSaveable { mutableStateOf(ThemeMode.SYSTEM) }
    DrawBoxTheme(darkTheme = themeMode.resolveIsDark()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxSize(),
        ) {
            HomeScreen(
                themeMode = themeMode,
                onThemeModeChange = { themeMode = it },
            )
        }
    }
}

/**
 * Maps the built-in `sans` / `serif` / `mono` keys to real bundled OFL
 * fonts (Inter, Crimson Text, JetBrains Mono).
 *
 * Registration happens here, before [HomeScreen] composes and reads the
 * registry, so the picker resolves distinct faces on the first frame. This
 * is required on web: Skia-WASM bundles a single face, so the generic
 * `FontFamily.SansSerif/Serif/Monospace` defaults would otherwise render
 * identically. See [FontRegistry] KDoc and issue #89.
 */
@Composable
private fun RegisterBuiltinFonts() {
    val sans = FontFamily(Font(Res.font.inter_Regular))
    val serif = FontFamily(Font(Res.font.crimsonText_Regular))
    val mono = FontFamily(Font(Res.font.jetBrainsMono_Regular))
    remember(sans, serif, mono) {
        FontRegistry.register(BuiltinFontFamilyKeys.SANS, sans)
        FontRegistry.register(BuiltinFontFamilyKeys.SERIF, serif)
        FontRegistry.register(BuiltinFontFamilyKeys.MONO, mono)
    }
}