package io.ak1.drawbox.ui.picker

 import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
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
}

private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue

/**
 * Which color slot the picker is currently editing. Toggled by the Border /
 * Fill tabs at the top of [MultiTargetColorPicker].
 */
enum class ColorTarget { Border, Fill }

/**
 * Color picker for fillable shapes — one dialog that edits both stroke and
 * fill. Tabs at the top switch the RangVikalp grid between the two targets;
 * an off-toggle disables the active target ("no stroke" / "no fill").
 *
 * Mirrors the single-swatch UX used by Figma / Sketch / OPPO Notes — the user
 * sees both current colors at a glance and never has to open two dialogs.
 *
 * @param onStrokeSelected invoked with a non-null color when the user picks a
 *   border color from the grid. Also fires with the previous stroke color when
 *   the user re-enables stroke after having toggled it off.
 * @param onStrokeEnabledChanged invoked with `false` when the user hits the
 *   off-toggle while on the Border tab.
 * @param onFillSelected invoked with a non-null color when the user picks a
 *   fill color, or with `null` when they hit the off-toggle while on the Fill
 *   tab.
 */
@Composable
fun MultiTargetColorPicker(
    strokeColor: Color,
    strokeEnabled: Boolean,
    fillColor: Color?,
    onDismiss: () -> Unit,
    onStrokeSelected: (Color) -> Unit,
    onStrokeEnabledChanged: (Boolean) -> Unit,
    onFillSelected: (Color?) -> Unit,
) {
    var target by remember { mutableStateOf(ColorTarget.Border) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    // Seed RangVikalp with whichever color the current tab is editing so the
    // grid highlights the right swatch on open + tab switch.
    val seed = when (target) {
        ColorTarget.Border -> strokeColor
        ColorTarget.Fill -> fillColor ?: strokeColor
    }
    val state = rememberRangVikalpState(initial = seed)
    LaunchedEffect(target) { state.setFromColor(seed) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TargetTab(
                        label = "Border",
                        color = strokeColor,
                        enabled = strokeEnabled,
                        selected = target == ColorTarget.Border,
                        onClick = { target = ColorTarget.Border },
                    )
                    TargetTab(
                        label = "Fill",
                        color = fillColor ?: Color.Transparent,
                        enabled = fillColor != null,
                        selected = target == ColorTarget.Fill,
                        onClick = { target = ColorTarget.Fill },
                    )
                    Spacer(Modifier.width(4.dp))
                    val offLabel = if (target == ColorTarget.Border) "No stroke" else "No fill"
                    val offEnabled = when (target) {
                        ColorTarget.Border -> strokeEnabled
                        ColorTarget.Fill -> fillColor != null
                    }
                    if (offEnabled) {
                        TextButton(onClick = {
                            when (target) {
                                ColorTarget.Border -> onStrokeEnabledChanged(false)
                                ColorTarget.Fill -> onFillSelected(null)
                            }
                        }) { Text(offLabel) }
                    }
                }
                Column(modifier = Modifier.size(220.dp, 380.dp)) {
                    RangVikalp(
                        state = state,
                        colors = defaultRangVikalpColors(dark = isDark),
                        onColorChange = { color ->
                            when (target) {
                                ColorTarget.Border -> {
                                    if (!strokeEnabled) onStrokeEnabledChanged(true)
                                    onStrokeSelected(color)
                                }
                                ColorTarget.Fill -> onFillSelected(color)
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * Border / Fill target chip inside [MultiTargetColorPicker]. Shows a swatch of
 * the current color; a slash overlay when the target is disabled (no stroke /
 * no fill); a primary-tinted ring when selected.
 */
@Composable
private fun TargetTab(
    label: String,
    color: Color,
    enabled: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (enabled) color else Color.Transparent)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) primary else outline,
                    shape = CircleShape,
                ),
        ) {
            if (!enabled) androidx.compose.foundation.Canvas(Modifier.size(18.dp)) {
                drawLine(
                    color = outline,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.5f,
                    cap = StrokeCap.Round,
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// Suppress unused-import warning for Stroke — reserved for a follow-up that
// gives the split-disc swatch a proper outer stroke ring.
@Suppress("unused")
private val strokeReserved: (Stroke) -> Unit = { _ -> }
