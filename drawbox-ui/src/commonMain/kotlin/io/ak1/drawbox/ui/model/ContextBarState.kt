package io.ak1.drawbox.ui.model

import androidx.compose.ui.graphics.Color
import io.ak1.drawbox.domain.model.StrokeStyle
import io.ak1.drawbox.domain.model.TextAlignment

/**
 * Immutable snapshot of everything the context bar needs to render itself.
 * Hosts derive this from their controller state and feed it to
 * [io.ak1.drawbox.ui.context.ContextBar] (or to individual item builders).
 */
data class ContextBarState(
    val strokeColor: Color = Color.Black,
    val strokeEnabled: Boolean = true,
    val strokeStyle: StrokeStyle = StrokeStyle.SOLID,
    val strokeWidth: Float = 5f,
    val fillColor: Color? = null,
    val cornerRadius: Float = 0f,
    val fontSize: Float = 16f,
    val textAlignment: TextAlignment = TextAlignment.LEFT,
    val fontFamilyKey: String = "sans",
    val fontFamilyKeys: Set<String> = setOf("sans", "serif", "mono"),
    /**
     * "Mixed value" flags for a multi-element text selection whose members
     * disagree on a property. When set, the corresponding chip shows the
     * first element's value dimmed and highlights no preset — picking one
     * applies it to the whole selection. Single selections leave these false.
     */
    val fontSizeMixed: Boolean = false,
    val textAlignmentMixed: Boolean = false,
    val fontFamilyMixed: Boolean = false,
)

/**
 * Which sub-bars to include in the composite [io.ak1.drawbox.ui.context.ContextBar].
 * All default to false so the bar is empty (and invisible) until the host opts in.
 * Consumers that want finer control can bypass the composite and call the
 * per-concern `*ContextItems` builders directly.
 */
data class ContextBarSlots(
    val showStroke: Boolean = false,
    /** When true, the stroke slot becomes a stroke on/off toggle + picker. */
    val strokeToggleable: Boolean = false,
    val showShapeStroke: Boolean = false,
    val showFill: Boolean = false,
    val showCornerRadius: Boolean = false,
    val showText: Boolean = false,
    val showEditText: Boolean = false,
    val showSelectionActions: Boolean = false,
)
