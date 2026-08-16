package io.ak1.drawboxsample.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.ak1.drawbox.DrawBox
import io.ak1.drawbox.domain.model.State
import io.ak1.drawbox.domain.usecase.UseCase
import io.ak1.drawbox.presentation.reducer.Reducer
import io.ak1.drawbox.presentation.viewmodel.DrawBoxController
import io.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test

/**
 * End-to-end "the user draws" visual regression test.
 *
 * Unlike [CanvasScreenshotTest] (which seeds [io.ak1.drawbox.domain.model.Element]s
 * into the gesture-free [io.ak1.drawbox.DrawingPreview]), this drives the *live*
 * [DrawBox] composable through a real pointer gesture via [performTouchInput]:
 * PEN mode, one continuous stroke that traces the Greek letter **alpha (α)** —
 * from the top-right corner down to slightly mid-left, a U-turn, back across the
 * line it just drew, and out to the bottom-right. Rendered at a *light* alpha
 * (35% opacity).
 *
 * The captured frame is asserted byte-for-byte (changeThreshold = 0f → 100%
 * accuracy) against `src/jvmTest/snapshots/DrawGesture_alpha.png`. The first run
 * records the baseline; subsequent runs fail on any pixel drift.
 *
 * Density is pinned to 1f so the 400×300 dp canvas is exactly 400×300 px and the
 * gesture coordinates below map 1:1 to pixels (and to world space, since the
 * default viewport is identity).
 */
class DrawGestureScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun userDrawsAlphaShape() {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                val controller = remember {
                    DrawBoxController(
                        Reducer(UseCase()),
                        State(
                            // "light alpha": a solid stroke color drawn at low opacity.
                            strokeColor = Color(0xFF3355FF),
                            strokeWidth = 10f,
                            opacity = 0.35f,
                            bgColor = Color.White,
                        ),
                    )
                }
                val state by controller.state.collectAsState()
                Box(Modifier.size(400.dp, 300.dp).background(Color.White)) {
                    DrawBox(
                        state = state,
                        onIntent = controller::onIntent,
                        showGrid = false,
                        modifier = Modifier.fillMaxSize().testTag("canvas"),
                    )
                }
            }
        }

        // One continuous PEN stroke tracing an alpha (α):
        //   A) top-right corner → slightly mid-left (the descending line)
        //   B) a U-turn (upward loop) at the left
        //   C) back across line A, out to the bottom-right corner (the tail)
        composeRule.onNodeWithTag("canvas").performTouchInput {
            down(Offset(360f, 40f))
            // A — descending line to mid-left
            moveTo(Offset(300f, 70f))
            moveTo(Offset(240f, 100f))
            moveTo(Offset(180f, 130f))
            moveTo(Offset(120f, 160f))
            // B — U-turn (loop back up on the left)
            moveTo(Offset(95f, 125f))
            moveTo(Offset(110f, 95f))
            moveTo(Offset(150f, 100f))
            moveTo(Offset(185f, 115f))
            // C — cross line A and tail out to the bottom-right
            moveTo(Offset(240f, 160f))
            moveTo(Offset(300f, 210f))
            moveTo(Offset(360f, 260f))
            up()
        }
        composeRule.waitForIdle()

        // Byte-exact by default (Roborazzi records the baseline on the first run,
        // then fails on any pixel drift on subsequent runs — 100% match).
        composeRule.onRoot().captureRoboImage(
            filePath = "src/jvmTest/snapshots/DrawGesture_alpha.png",
        )
    }
}
