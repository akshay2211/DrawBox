package io.ak1.drawboxsample

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import io.ak1.drawboxsample.save.rememberImageSaver
import io.ak1.drawboxsample.ui.HomeScreen
import io.ak1.drawboxsample.ui.theme.DrawBoxTheme

@Composable
fun App() {
    val isDark = isSystemInDarkTheme()
    DrawBoxTheme(darkTheme = isDark) {
        Surface(color = MaterialTheme.colorScheme.background) {
            val saver = rememberImageSaver()
            HomeScreen(onSave = { saver.save(it) })
        }
    }
}