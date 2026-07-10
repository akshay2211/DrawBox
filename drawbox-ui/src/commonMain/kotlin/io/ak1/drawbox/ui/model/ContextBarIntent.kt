package io.ak1.drawbox.ui.model

import androidx.compose.ui.graphics.Color
import io.ak1.drawbox.domain.model.StrokeStyle
import io.ak1.drawbox.domain.model.TextAlignment

/**
 * Every user gesture the context bar can emit. Consumers translate these into
 * calls on their controller/view-model. Keeping this a sealed hierarchy lets
 * hosts add layered behavior (undo grouping, analytics, collab broadcast) with
 * a single `when` on the intent.
 */
sealed interface ContextBarIntent {
    data class SetStrokeColor(val color: Color) : ContextBarIntent
    data class SetStrokeEnabled(val enabled: Boolean) : ContextBarIntent
    data class SetStrokeStyle(val style: StrokeStyle) : ContextBarIntent
    data class SetStrokeWidth(val width: Float) : ContextBarIntent

    data class SetFillColor(val color: Color?) : ContextBarIntent

    data class SetCornerRadius(val radius: Float) : ContextBarIntent

    data class SetFontSize(val size: Float) : ContextBarIntent
    data class SetTextAlignment(val alignment: TextAlignment) : ContextBarIntent
    data class SetFontFamily(val key: String) : ContextBarIntent
    data object EditText : ContextBarIntent

    data object BringToFront : ContextBarIntent
    data object SendToBack : ContextBarIntent
    data object Delete : ContextBarIntent
    data object ClearSelection : ContextBarIntent
}

/** Callback consumers supply to receive context-bar intents. */
typealias ContextBarDispatch = (ContextBarIntent) -> Unit
