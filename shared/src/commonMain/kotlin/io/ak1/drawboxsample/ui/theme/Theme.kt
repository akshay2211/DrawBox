package io.ak1.drawboxsample.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFF4E33FF)
val WhiteDark = Color(0xFFF9F9F9)
val WhiteLite = Color(0xFFFFFFFF)
val BlackDark = Color(0xFF0E121B)
val BlackLite = Color(0xFF171C26)
val Grey = Color(0xFF586070)

private val DarkColors = darkColorScheme(
    primary = WhiteDark,
    onPrimary = BlackDark,
    secondary = Accent,
    onSecondary = BlackDark,
    background = BlackDark,
    onBackground = WhiteLite,
    surface = BlackLite,
    onSurface = WhiteDark,
    surfaceVariant = Grey,
    onSurfaceVariant = WhiteDark,
)

private val LightColors = lightColorScheme(
    primary = BlackDark,
    onPrimary = WhiteDark,
    secondary = Accent,
    onSecondary = WhiteDark,
    background = WhiteDark,
    onBackground = BlackDark,
    surface = WhiteLite,
    onSurface = BlackDark,
    surfaceVariant = Grey,
    onSurfaceVariant = WhiteDark,
)

@Composable
fun DrawBoxTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}