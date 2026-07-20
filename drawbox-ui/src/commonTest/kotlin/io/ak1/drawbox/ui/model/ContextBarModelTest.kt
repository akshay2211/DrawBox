package io.ak1.drawbox.ui.model

import androidx.compose.ui.graphics.Color
import io.ak1.drawbox.domain.model.StrokeStyle
import io.ak1.drawbox.domain.model.TextAlignment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Locks the public defaults and value semantics of the context-bar model types.
 * Hosts derive these from controller state, so the defaults are part of the API
 * contract.
 */
class ContextBarModelTest {

    @Test
    fun contextBarStateDefaultsMatchContract() {
        val s = ContextBarState()
        assertEquals(Color.Black, s.strokeColor)
        assertTrue(s.strokeEnabled)
        assertEquals(StrokeStyle.SOLID, s.strokeStyle)
        assertEquals(5f, s.strokeWidth)
        assertNull(s.fillColor)
        assertEquals(0f, s.cornerRadius)
        assertEquals(16f, s.fontSize)
        assertEquals(TextAlignment.LEFT, s.textAlignment)
        assertEquals("sans", s.fontFamilyKey)
        assertEquals(setOf("sans", "serif", "mono"), s.fontFamilyKeys)
    }

    @Test
    fun contextBarStateMixedFlagsDefaultFalse() {
        val s = ContextBarState()
        assertFalse(s.fontSizeMixed)
        assertFalse(s.textAlignmentMixed)
        assertFalse(s.fontFamilyMixed)
    }

    @Test
    fun contextBarSlotsDefaultAllOff() {
        val slots = ContextBarSlots()
        assertFalse(slots.showStroke)
        assertFalse(slots.strokeToggleable)
        assertFalse(slots.showShapeStroke)
        assertFalse(slots.showFill)
        assertFalse(slots.showCornerRadius)
        assertFalse(slots.showText)
        assertFalse(slots.showEditText)
        assertFalse(slots.showSelectionActions)
    }

    @Test
    fun contextBarIntentDataClassesUseValueEquality() {
        assertEquals(
            ContextBarIntent.SetStrokeColor(Color.Red),
            ContextBarIntent.SetStrokeColor(Color.Red),
        )
        assertNotEquals(
            ContextBarIntent.SetStrokeColor(Color.Red),
            ContextBarIntent.SetStrokeColor(Color.Blue),
        )
        assertEquals(ContextBarIntent.SetFillColor(null), ContextBarIntent.SetFillColor(null))
        assertEquals(ContextBarIntent.SetFontSize(24f), ContextBarIntent.SetFontSize(24f))
    }

    @Test
    fun contextBarIntentObjectsAreSingletons() {
        assertSame(ContextBarIntent.EditText, ContextBarIntent.EditText)
        assertSame(ContextBarIntent.BringToFront, ContextBarIntent.BringToFront)
        assertSame(ContextBarIntent.SendToBack, ContextBarIntent.SendToBack)
        assertSame(ContextBarIntent.Delete, ContextBarIntent.Delete)
        assertSame(ContextBarIntent.ClearSelection, ContextBarIntent.ClearSelection)
    }

    private fun <T> assertSame(a: T, b: T) = assertTrue(a === b, "expected same instance")
}
