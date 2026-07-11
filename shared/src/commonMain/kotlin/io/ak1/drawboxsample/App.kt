package io.ak1.drawboxsample

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.ak1.drawboxsample.ui.HomeScreen
import io.ak1.drawboxsample.ui.theme.DrawBoxTheme
import io.ak1.drawboxsample.ui.theme.ThemeMode
import io.ak1.drawboxsample.ui.theme.resolveIsDark

@Composable
fun App() {
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