package io.ak1.drawboxsample.ui.components

import android.view.Window
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import io.ak1.drawboxsample.ui.theme.DrawBoxTheme
import io.ak1.drawboxsample.ui.theme.isSystemInDarkThemeCustom
import io.ak1.drawboxsample.ui.theme.StatusBarConfig

/**
 * Created by akshay on 29/12/21
 * https://ak1.io
 */
@Composable
fun Root(window: Window, content: @Composable () -> Unit) {
    val isDark = isSystemInDarkThemeCustom()
    DrawBoxTheme(isDark) {
        window.StatusBarConfig(isDark)
        Surface(color = MaterialTheme.colors.background) {
            content.invoke()
        }
    }
}