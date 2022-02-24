package io.ak1.drawboxsample.ui.theme

import android.content.res.Configuration
import android.view.Window
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import io.ak1.drawboxsample.data.local.dataStore
import io.ak1.drawboxsample.data.local.isDarkThemeOn
import io.ak1.drawboxsample.data.local.themePreferenceKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val DarkColorPalette = darkColors(
    primary = WhiteDark,
    primaryVariant = Grey,
    secondary = Accent,
    secondaryVariant = WhiteDark,
    background = BlackDark,
    surface = BlackLite,
    onPrimary = BlackDark,
    onSecondary = BlackDark,
    onBackground = WhiteLite,
    onSurface = WhiteDark,
)

private val LightColorPalette = lightColors(
    primary = BlackDark,
    primaryVariant = Grey,
    secondary = Accent,
    secondaryVariant = BlackDark,
    background = WhiteDark,
    surface = WhiteLite,
    onPrimary = WhiteDark,
    onSecondary = WhiteDark,
    onBackground = BlackDark,
    onSurface = BlackDark,
)

@Composable
fun DrawBoxTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

@Composable
fun isSystemInDarkThemeCustom(): Boolean {
    val context = LocalContext.current
    val exampleData = runBlocking { context.dataStore.data.first() }
    val theme = context.isDarkThemeOn().collectAsState(initial = exampleData[themePreferenceKey] ?: 0)
    return when (theme.value) {
        2 -> true
        1 -> false
        else -> context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }
}

@Composable
fun Window.StatusBarConfig(darkTheme: Boolean) {
    WindowInsetsControllerCompat(this, this.decorView).isAppearanceLightStatusBars =
        !darkTheme
    this.statusBarColor = MaterialTheme.colors.background.toArgb()
}
