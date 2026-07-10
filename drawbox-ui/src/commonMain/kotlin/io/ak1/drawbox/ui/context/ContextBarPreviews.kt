package io.ak1.drawbox.ui.context

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.ak1.drawbox.domain.model.StrokeStyle
import io.ak1.drawbox.domain.model.TextAlignment
import io.ak1.drawbox.ui.model.ContextBarSlots
import io.ak1.drawbox.ui.model.ContextBarState
import androidx.compose.ui.tooling.preview.Preview

private val SampleState = ContextBarState(
    strokeColor = Color(0xFF3D5AFE),
    strokeEnabled = true,
    strokeStyle = StrokeStyle.DASHED,
    strokeWidth = 10f,
    fillColor = Color(0xFFFFC107),
    cornerRadius = 16f,
    fontSize = 20f,
    textAlignment = TextAlignment.CENTER,
    fontFamilyKey = "sans",
    fontFamilyKeys = setOf("sans", "serif", "mono"),
)

/** Drawing a shape (no selection): color + shape stroke + corner + fill. */
@Preview
@Composable
internal fun ContextBarShapeModePreview() {
    ContextBarPreviewSurface {
        ContextBar(
            state = SampleState,
            slots = ContextBarSlots(
                showStroke = true,
                showShapeStroke = true,
                showCornerRadius = true,
                showFill = true,
            ),
            onIntent = {},
        )
    }
}

/** Shape selected: toggleable stroke + shape stroke + corner + fill + selection actions. */
@Preview
@Composable
internal fun ContextBarShapeSelectionPreview() {
    ContextBarPreviewSurface {
        ContextBar(
            state = SampleState,
            slots = ContextBarSlots(
                showStroke = true,
                strokeToggleable = true,
                showShapeStroke = true,
                showCornerRadius = true,
                showFill = true,
                showSelectionActions = true,
            ),
            onIntent = {},
        )
    }
}

/** Text mode with no selection: color + text controls. */
@Preview
@Composable
internal fun ContextBarTextModePreview() {
    ContextBarPreviewSurface {
        ContextBar(
            state = SampleState,
            slots = ContextBarSlots(showStroke = true, showText = true),
            onIntent = {},
        )
    }
}

/** Text selection: color + text controls with edit + selection actions. */
@Preview
@Composable
internal fun ContextBarTextSelectionPreview() {
    ContextBarPreviewSurface {
        ContextBar(
            state = SampleState,
            slots = ContextBarSlots(
                showStroke = true,
                showText = true,
                showEditText = true,
                showSelectionActions = true,
            ),
            onIntent = {},
        )
    }
}

/** Selection with no per-element properties (e.g. mixed selection): just actions. */
@Preview
@Composable
internal fun ContextBarSelectionOnlyPreview() {
    ContextBarPreviewSurface {
        ContextBar(
            state = SampleState,
            slots = ContextBarSlots(showStroke = true, showSelectionActions = true),
            onIntent = {},
        )
    }
}

/** No stroke drawn: swatch renders with slash overlay. */
@Preview
@Composable
internal fun ContextBarStrokeOffPreview() {
    ContextBarPreviewSurface {
        ContextBar(
            state = SampleState.copy(strokeEnabled = false),
            slots = ContextBarSlots(
                showStroke = true,
                strokeToggleable = true,
                showShapeStroke = true,
                showFill = true,
                showSelectionActions = true,
            ),
            onIntent = {},
        )
    }
}

@Composable
private fun ContextBarPreviewSurface(content: @Composable () -> Unit) {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Box(
                modifier = Modifier
                    .size(width = 640.dp, height = 140.dp)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }
}
