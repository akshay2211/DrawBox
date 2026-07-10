package io.ak1.drawbox.ui.picker

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
 * Slot type for a color-picker dialog. Callers of the context bar pass any
 * composable that matches this signature — the RangVikalp default below, a
 * Material `AlertDialog` wrapper, or a fully custom picker.
 *
 * `initial` is the color to seed the picker with. Invoke `onDismiss` when the
 * user cancels (tap-outside, back gesture, close button) and `onSelected` when
 * the user commits a color.
 */
typealias ColorPickerSlot = @Composable (
    initial: Color,
    onDismiss: () -> Unit,
    onSelected: (Color) -> Unit,
) -> Unit

/**
 * RangVikalp-backed color picker dialog. This is the default slot used by
 * [io.ak1.drawbox.ui.context.ContextBar]; consumers who want a different
 * picker just pass their own [ColorPickerSlot].
 */
@Composable
fun RangVikalpColorPicker(
    initial: Color,
    onDismiss: () -> Unit,
    onSelected: (Color) -> Unit,
) {
    val state = rememberRangVikalpState(initial = initial)
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.size(220.dp, 390.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RangVikalp(
                state = state,
                colors = defaultRangVikalpColors(dark = isDark),
                onColorChange = onSelected,
            )
        }
    }
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
