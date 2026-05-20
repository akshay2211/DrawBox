package io.ak1.drawboxsample

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "DrawBox",
        state = rememberWindowState(width = 600.dp, height = 800.dp),
    ) {
        App()
    }
}