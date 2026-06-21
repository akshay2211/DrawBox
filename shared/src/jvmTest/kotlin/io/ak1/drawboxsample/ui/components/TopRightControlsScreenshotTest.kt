@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.ak1.drawboxsample.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import io.ak1.drawboxsample.ui.theme.ThemeMode
import io.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test

class TopRightControlsScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun wideLayout_lightTheme_systemMode() {
        composeRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface {
                    TopRightControls(
                        isNarrow = false,
                        scalePercent = 100,
                        themeMode = ThemeMode.SYSTEM,
                        onThemeModeChange = {},
                        onZoomIn = {},
                        onZoomOut = {},
                        onZoomReset = {},
                        onSettingsClick = {},
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "src/jvmTest/snapshots/TopRightControls_wide_light_system.png",
        )
    }

    @Test
    fun narrowLayout_lightTheme_zoom75() {
        composeRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface {
                    TopRightControls(
                        isNarrow = true,
                        scalePercent = 75,
                        themeMode = ThemeMode.LIGHT,
                        onThemeModeChange = {},
                        onZoomIn = {},
                        onZoomOut = {},
                        onZoomReset = {},
                        onSettingsClick = {},
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "src/jvmTest/snapshots/TopRightControls_narrow_light_zoom75.png",
        )
    }

    @Test
    fun narrowLayout_darkTheme_zoom150_darkMode() {
        composeRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface {
                    TopRightControls(
                        isNarrow = true,
                        scalePercent = 150,
                        themeMode = ThemeMode.DARK,
                        onThemeModeChange = {},
                        onZoomIn = {},
                        onZoomOut = {},
                        onZoomReset = {},
                        onSettingsClick = {},
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "src/jvmTest/snapshots/TopRightControls_narrow_dark_zoom150.png",
        )
    }

    @Test
    fun zoomToolbar_lightTheme_zoom100() {
        composeRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface(modifier = Modifier.size(200.dp, 64.dp)) {
                    ZoomToolbar(
                        scalePercent = 100,
                        onZoomIn = {},
                        onZoomOut = {},
                        onZoomReset = {},
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "src/jvmTest/snapshots/ZoomToolbar_light_100.png",
        )
    }
}