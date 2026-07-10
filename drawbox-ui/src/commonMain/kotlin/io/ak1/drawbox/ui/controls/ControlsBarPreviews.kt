package io.ak1.drawbox.ui.controls

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.ak1.drawbox.domain.model.Mode
import androidx.compose.ui.tooling.preview.Preview

/** Freehand tool active, both undo and redo available. */
@Preview
@Composable
internal fun ControlsBarPenModePreview() {
    ControlsBarPreviewSurface {
        ControlsBar(
            items = defaultControlsBarItems(
                state = ControlsBarState(
                    currentMode = Mode.PEN,
                    canUndo = true,
                    canRedo = true,
                ),
                dispatch = {},
            ),
        )
    }
}

/** Selection tool active. */
@Preview
@Composable
internal fun ControlsBarSelectModePreview() {
    ControlsBarPreviewSurface {
        ControlsBar(
            items = defaultControlsBarItems(
                state = ControlsBarState(
                    currentMode = Mode.SELECT,
                    canUndo = true,
                    canRedo = false,
                ),
                dispatch = {},
            ),
        )
    }
}

/** Shape mode with rectangle selected — the shape slot shows the rectangle icon. */
@Preview
@Composable
internal fun ControlsBarRectangleModePreview() {
    ControlsBarPreviewSurface {
        ControlsBar(
            items = defaultControlsBarItems(
                state = ControlsBarState(
                    currentMode = Mode.RECTANGLE,
                    canUndo = true,
                    canRedo = true,
                ),
                dispatch = {},
            ),
        )
    }
}

/** Eraser tool active. */
@Preview
@Composable
internal fun ControlsBarEraserModePreview() {
    ControlsBarPreviewSurface {
        ControlsBar(
            items = defaultControlsBarItems(
                state = ControlsBarState(
                    currentMode = Mode.ERASER,
                    canUndo = false,
                    canRedo = false,
                ),
                dispatch = {},
            ),
        )
    }
}

/** Text mode active. */
@Preview
@Composable
internal fun ControlsBarTextModePreview() {
    ControlsBarPreviewSurface {
        ControlsBar(
            items = defaultControlsBarItems(
                state = ControlsBarState(
                    currentMode = Mode.TEXT,
                    canUndo = true,
                    canRedo = true,
                ),
                dispatch = {},
            ),
        )
    }
}

/** Fresh canvas — no history yet, disabled undo/redo shown at 0.38 alpha. */
@Preview
@Composable
internal fun ControlsBarEmptyHistoryPreview() {
    ControlsBarPreviewSurface {
        ControlsBar(
            items = defaultControlsBarItems(
                state = ControlsBarState(
                    currentMode = Mode.SELECT,
                    canUndo = false,
                    canRedo = false,
                ),
                dispatch = {},
            ),
        )
    }
}

/** With recent colors — tapping the color slot opens a swatch submenu. */
@Preview
@Composable
internal fun ControlsBarWithRecentColorsPreview() {
    ControlsBarPreviewSurface {
        ControlsBar(
            items = defaultControlsBarItems(
                state = ControlsBarState(
                    currentMode = Mode.PEN,
                    canUndo = true,
                    canRedo = false,
                    strokeColor = Color(0xFF3D5AFE),
                    recentColors = listOf(
                        Color(0xFF3D5AFE),
                        Color(0xFFF44336),
                        Color(0xFFFFC107),
                        Color(0xFF4CAF50),
                        Color.Black,
                    ),
                ),
                dispatch = {},
            ),
        )
    }
}

/** Leading + trailing custom slots demonstrating Pro-drop-in composition. */
@Preview
@Composable
internal fun ControlsBarWithLeadingTrailingPreview() {
    ControlsBarPreviewSurface {
        ControlsBar(
            items = defaultControlsBarItems(
                state = ControlsBarState(
                    currentMode = Mode.PEN,
                    canUndo = true,
                    canRedo = true,
                    strokeColor = Color(0xFF3D5AFE),
                ),
                dispatch = {},
            ),
            leading = emptyList(),
            trailing = emptyList(),
        )
    }
}

@Composable
private fun ControlsBarPreviewSurface(content: @Composable () -> Unit) {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(
                modifier = Modifier
                    .size(width = 480.dp, height = 120.dp)
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }
}
