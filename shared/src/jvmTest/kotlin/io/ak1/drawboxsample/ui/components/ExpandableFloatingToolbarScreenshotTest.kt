@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package io.ak1.drawboxsample.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import io.ak1.drawbox.ui.toolbar.ExpandableFloatingToolbar
import io.ak1.drawbox.ui.toolbar.FloatingMenuItem
import io.ak1.drawbox.ui.toolbar.SubmenuPosition
import io.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test

class ExpandableFloatingToolbarScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleItems = listOf(
        FloatingMenuItem(
            id = "edit",
            icon = { Icon(Icons.Filled.Edit, contentDescription = "Edit") },
            children = listOf(
                FloatingMenuItem("edit-add", { Icon(Icons.Filled.Add, "Add") }),
                FloatingMenuItem("edit-person", { Icon(Icons.Filled.Person, "Person") }),
                FloatingMenuItem("edit-fav", { Icon(Icons.Filled.Favorite, "Favorite") }),
            ),
        ),
        FloatingMenuItem(
            id = "more",
            icon = { Icon(Icons.Filled.MoreVert, contentDescription = "More") },
            children = listOf(
                FloatingMenuItem("more-add", { Icon(Icons.Filled.Add, "Add") }),
                FloatingMenuItem("more-fav", { Icon(Icons.Filled.Favorite, "Favorite") }),
            ),
        ),
        FloatingMenuItem(
            id = "fav",
            icon = { Icon(Icons.Filled.Favorite, contentDescription = "Favorite") },
        ),
    )

    @Test
    fun collapsedState_lightTheme() {
        composeRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface(modifier = Modifier.size(320.dp, 80.dp)) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ExpandableFloatingToolbar(
                            items = sampleItems,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(8.dp),
                        )
                    }
                }
            }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "src/jvmTest/snapshots/ExpandableFloatingToolbar_collapsed_light.png",
        )
    }

    @Test
    fun expandedSubmenu_lightTheme() {
        composeRule.setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface(modifier = Modifier.size(320.dp, 160.dp)) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ExpandableFloatingToolbar(
                            items = sampleItems,
                            controlledActiveId = "edit",
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(8.dp),
                        )
                    }
                }
            }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "src/jvmTest/snapshots/ExpandableFloatingToolbar_expanded_edit_light.png",
        )
    }

    @Test
    fun submenuBelow_darkTheme() {
        composeRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(modifier = Modifier.size(320.dp, 160.dp)) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ExpandableFloatingToolbar(
                            items = sampleItems,
                            controlledActiveId = "more",
                            submenuPosition = SubmenuPosition.Below,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(8.dp),
                        )
                    }
                }
            }
        }
        composeRule.onRoot().captureRoboImage(
            filePath = "src/jvmTest/snapshots/ExpandableFloatingToolbar_submenu_below_dark.png",
        )
    }
}
