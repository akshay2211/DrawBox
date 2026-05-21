package io.ak1.drawboxsample

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.ak1.drawboxsample.save.rememberImageSaver
import io.ak1.drawboxsample.ui.HomeScreen
import io.ak1.drawboxsample.ui.theme.DrawBoxTheme

@Composable
fun App() {
    Column(Modifier.fillMaxSize().background(Color.Cyan)) {  }
    /*val isDark = isSystemInDarkTheme()
    DrawBoxTheme(darkTheme = isDark) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            val saver = rememberImageSaver()
            HomeScreen(onSave = { saver.save(it) })
        }
    }*/
}