package io.ak1.drawboxsample.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Theme follow-mode. SYSTEM defers to the platform's dark-mode flag; LIGHT and
 * DARK pin the app independent of platform state.
 */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Resolve the follow-mode to a concrete `darkTheme` boolean for [DrawBoxTheme]. */
@Composable
fun ThemeMode.resolveIsDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/** Next mode in the cycle SYSTEM → LIGHT → DARK → SYSTEM, used by the toolbar toggle. */
fun ThemeMode.next(): ThemeMode = when (this) {
    ThemeMode.SYSTEM -> ThemeMode.LIGHT
    ThemeMode.LIGHT -> ThemeMode.DARK
    ThemeMode.DARK -> ThemeMode.SYSTEM
}

// Editorial palette: near-black ink, paper-white background, single restrained
// indigo accent. Primary deliberately stays neutral so the accent reads as
// emphasis (active tool, selection chrome) rather than ambient brand color.
private val Accent = Color(0xFF4E33FF)
private val Ink = Color(0xFF0E121B)
private val InkSoft = Color(0xFF171C26)
private val Paper = Color(0xFFF9F9F9)
private val PaperSoft = Color(0xFFFFFFFF)

// Dark
private val MistDark = Color(0xFF252B36)
private val CloudDark = Color(0xFFB0B6C2)
private val SteelDark = Color(0xFF3A4253)
private val OutlineDark = Color(0xFF2A3040)
private val ErrorDark = Color(0xFFFF6B6B)

// Light
private val MistLight = Color(0xFFE9ECF1)
private val CloudLight = Color(0xFF4A5160)
private val SteelLight = Color(0xFFB7BCC8)
private val OutlineLight = Color(0xFFE0E3EA)
private val ErrorLight = Color(0xFFB22424)

private val DarkColors = darkColorScheme(
    primary = PaperSoft,
    onPrimary = Ink,
    secondary = Accent,
    onSecondary = PaperSoft,
    tertiary = Accent,
    onTertiary = PaperSoft,
    background = Ink,
    onBackground = Paper,
    surface = InkSoft,
    onSurface = Paper,
    surfaceVariant = MistDark,
    onSurfaceVariant = CloudDark,
    outline = SteelDark,
    outlineVariant = OutlineDark,
    error = ErrorDark,
    onError = Ink,
)

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = PaperSoft,
    secondary = Accent,
    onSecondary = PaperSoft,
    tertiary = Accent,
    onTertiary = PaperSoft,
    background = Paper,
    onBackground = Ink,
    surface = PaperSoft,
    onSurface = Ink,
    surfaceVariant = MistLight,
    onSurfaceVariant = CloudLight,
    outline = SteelLight,
    outlineVariant = OutlineLight,
    error = ErrorLight,
    onError = PaperSoft,
)

@Composable
fun DrawBoxTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}