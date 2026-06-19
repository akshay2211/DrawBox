package io.ak1.drawboxsample.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.ak1.rangvikalp.RangVikalp
import io.ak1.rangvikalp.defaultRangVikalpColors
import io.ak1.rangvikalp.rememberRangVikalpState

/**
 * Themed color picker dialog backed by RangVikalp (io.ak1:rang-vikalp).
 * Shows the full tabbed picker (Preset + Custom) inside a Material3 dialog.
 */
@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
) {
    val state = rememberRangVikalpState(initial = initialColor)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Dialog(
        onDismissRequest = onDismiss, content = {
            Column(
                modifier = Modifier.size(220.dp, 390.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RangVikalp(
                    state = state,
                    colors = defaultRangVikalpColors(dark = isDark),
                    onColorChange = { onColorSelected(it) },
                )
            }
        })
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
