package io.ak1.drawbox.ui.context

import androidx.compose.ui.graphics.Color
import io.ak1.drawbox.ui.model.ContextBarIntent
import io.ak1.drawbox.ui.model.ContextBarState
import io.ak1.drawbox.ui.toolbar.FloatingMenuItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for the non-composable context-bar item builders. These exercise
 * the tri-state colors invariants and intent dispatch without a Compose runtime
 * — the `icon` lambdas are never invoked, only the returned [FloatingMenuItem]
 * tree structure and `onClick` behaviour are asserted.
 */
class ContextBarItemsTest {

    /** Collects intents emitted through a [io.ak1.drawbox.ui.model.ContextBarDispatch]. */
    private class Recorder {
        val intents = mutableListOf<ContextBarIntent>()
        val dispatch: (ContextBarIntent) -> Unit = { intents.add(it) }
    }

    private fun List<FloatingMenuItem>.ids() = map { it.id }

    private fun FloatingMenuItem.child(id: String): FloatingMenuItem =
        children.first { it.id == id }

    // region strokeColorContextItem

    @Test
    fun strokeColorNonToggleableIsSingleLeafThatOpensPicker() {
        var picked = false
        val rec = Recorder()
        val items = strokeColorContextItem(
            state = ContextBarState(),
            dispatch = rec.dispatch,
            onPickColor = { picked = true },
            toggleable = false,
        )

        assertEquals(listOf("stroke-color"), items.ids())
        val root = items.single()
        assertTrue(root.children.isEmpty())

        root.onClick?.invoke()
        assertTrue(picked)
        assertTrue(rec.intents.isEmpty())
    }

    @Test
    fun strokeColorToggleableExposesNoneAndPickChildren() {
        val rec = Recorder()
        val root = strokeColorContextItem(
            state = ContextBarState(strokeEnabled = true),
            dispatch = rec.dispatch,
            onPickColor = {},
            toggleable = true,
        ).single()

        assertEquals(listOf("stroke-none", "stroke-pick"), root.children.ids())
        // Enabled → "pick" is the active child, "none" is inactive.
        assertTrue(root.child("stroke-pick").isActive)
        assertFalse(root.child("stroke-none").isActive)
    }

    @Test
    fun strokeNoneChildDisablesStroke() {
        val rec = Recorder()
        val root = strokeColorContextItem(
            state = ContextBarState(strokeEnabled = true),
            dispatch = rec.dispatch,
            onPickColor = {},
            toggleable = true,
        ).single()

        root.child("stroke-none").onClick?.invoke()
        assertEquals(listOf<ContextBarIntent>(ContextBarIntent.SetStrokeEnabled(false)), rec.intents)
    }

    @Test
    fun strokePickFromDisabledReenablesStrokeThenOpensPicker() {
        val order = mutableListOf<String>()
        val rec = Recorder()
        val root = strokeColorContextItem(
            state = ContextBarState(strokeEnabled = false),
            dispatch = { rec.dispatch(it); order.add("dispatch") },
            onPickColor = { order.add("pick") },
            toggleable = true,
        ).single()

        root.child("stroke-pick").onClick?.invoke()
        // Picking implies stroke-on so the user doesn't round-trip through "none".
        assertEquals(listOf<ContextBarIntent>(ContextBarIntent.SetStrokeEnabled(true)), rec.intents)
        assertEquals(listOf("dispatch", "pick"), order)
    }

    @Test
    fun strokePickWhenAlreadyEnabledOnlyOpensPicker() {
        val rec = Recorder()
        var picked = false
        val root = strokeColorContextItem(
            state = ContextBarState(strokeEnabled = true),
            dispatch = rec.dispatch,
            onPickColor = { picked = true },
            toggleable = true,
        ).single()

        root.child("stroke-pick").onClick?.invoke()
        assertTrue(picked)
        assertTrue(rec.intents.isEmpty(), "no SetStrokeEnabled when already enabled")
    }

    // endregion

    // region colorsContextItem

    @Test
    fun colorsDegenerateCaseFallsBackToPlainStrokePicker() {
        var picked = false
        val items = colorsContextItem(
            state = ContextBarState(),
            dispatch = {},
            onPickStrokeColor = { picked = true },
            onPickFillColor = {},
            strokeToggleable = false,
            showFill = false,
        )

        // Same shape as the plain stroke-only chip.
        assertEquals(listOf("stroke-color"), items.ids())
        assertTrue(items.single().children.isEmpty())
        items.single().onClick?.invoke()
        assertTrue(picked)
    }

    @Test
    fun colorsStrokeNoneAppearsOnlyWhenFillIsOn() {
        // Fill off → no "no stroke" toggle (turning stroke off would hide the shape).
        val noFill = colorsContextItem(
            state = ContextBarState(strokeEnabled = true, fillColor = null),
            dispatch = {},
            onPickStrokeColor = {},
            onPickFillColor = {},
            strokeToggleable = true,
            showFill = true,
        ).single()
        assertFalse("colors-stroke-none" in noFill.children.ids())

        // Fill on → toggle is offered.
        val withFill = colorsContextItem(
            state = ContextBarState(strokeEnabled = true, fillColor = Color.Blue),
            dispatch = {},
            onPickStrokeColor = {},
            onPickFillColor = {},
            strokeToggleable = true,
            showFill = true,
        ).single()
        assertTrue("colors-stroke-none" in withFill.children.ids())
    }

    @Test
    fun colorsFillNoneAppearsOnlyWhenStrokeIsOn() {
        // Stroke off → no "no fill" toggle.
        val strokeOff = colorsContextItem(
            state = ContextBarState(strokeEnabled = false, fillColor = Color.Blue),
            dispatch = {},
            onPickStrokeColor = {},
            onPickFillColor = {},
            strokeToggleable = false,
            showFill = true,
        ).single()
        assertEquals(listOf("colors-stroke", "colors-sep", "colors-fill"), strokeOff.children.ids())

        // Stroke on → "no fill" toggle offered.
        val strokeOn = colorsContextItem(
            state = ContextBarState(strokeEnabled = true, fillColor = Color.Blue),
            dispatch = {},
            onPickStrokeColor = {},
            onPickFillColor = {},
            strokeToggleable = true,
            showFill = true,
        ).single()
        assertEquals(
            listOf("colors-stroke", "colors-stroke-none", "colors-sep", "colors-fill", "colors-fill-none"),
            strokeOn.children.ids(),
        )
    }

    @Test
    fun colorsSeparatorChildIsMarkedSeparator() {
        val root = colorsContextItem(
            state = ContextBarState(strokeEnabled = true, fillColor = Color.Blue),
            dispatch = {},
            onPickStrokeColor = {},
            onPickFillColor = {},
            strokeToggleable = true,
            showFill = true,
        ).single()
        assertTrue(root.child("colors-sep").isSeparator)
    }

    @Test
    fun colorsStrokeChildFromDisabledReenablesStrokeThenPicks() {
        var picked = false
        val rec = Recorder()
        val root = colorsContextItem(
            state = ContextBarState(strokeEnabled = false, fillColor = Color.Blue),
            dispatch = rec.dispatch,
            onPickStrokeColor = { picked = true },
            onPickFillColor = {},
            strokeToggleable = true,
            showFill = true,
        ).single()

        root.child("colors-stroke").onClick?.invoke()
        assertEquals(listOf<ContextBarIntent>(ContextBarIntent.SetStrokeEnabled(true)), rec.intents)
        assertTrue(picked)
    }

    @Test
    fun colorsFillNoneChildClearsFill() {
        val rec = Recorder()
        val root = colorsContextItem(
            state = ContextBarState(strokeEnabled = true, fillColor = Color.Blue),
            dispatch = rec.dispatch,
            onPickStrokeColor = {},
            onPickFillColor = {},
            strokeToggleable = true,
            showFill = true,
        ).single()

        root.child("colors-fill-none").onClick?.invoke()
        assertEquals(listOf<ContextBarIntent>(ContextBarIntent.SetFillColor(null)), rec.intents)
    }

    // endregion

    // region fillContextItems

    @Test
    fun fillItemsExposeNoneAndPickChildrenWithActiveReflectingFill() {
        val rec = Recorder()
        val filled = fillContextItems(
            state = ContextBarState(fillColor = Color.Red),
            dispatch = rec.dispatch,
            onPickFillColor = {},
        ).single()

        assertEquals(listOf("fill-none", "fill-pick"), filled.children.ids())
        assertTrue(filled.child("fill-pick").isActive)
        assertFalse(filled.child("fill-none").isActive)

        // With no fill, "none" is the active child.
        val empty = fillContextItems(
            state = ContextBarState(fillColor = null),
            dispatch = rec.dispatch,
            onPickFillColor = {},
        ).single()
        assertTrue(empty.child("fill-none").isActive)
        assertFalse(empty.child("fill-pick").isActive)
    }

    @Test
    fun fillNoneChildClearsFillAndPickInvokesCallback() {
        var picked = false
        val rec = Recorder()
        val root = fillContextItems(
            state = ContextBarState(fillColor = Color.Red),
            dispatch = rec.dispatch,
            onPickFillColor = { picked = true },
        ).single()

        root.child("fill-none").onClick?.invoke()
        assertEquals(listOf<ContextBarIntent>(ContextBarIntent.SetFillColor(null)), rec.intents)

        root.child("fill-pick").onClick?.invoke()
        assertTrue(picked)
    }

    // endregion

    // region presets

    @Test
    fun presetListsHaveExpectedValues() {
        assertEquals(listOf(5f, 10f, 15f, 20f), DefaultStrokeWidths)
        assertEquals(listOf(12f, 16f, 20f, 24f, 32f, 48f), DefaultTextSizePresets)
        assertEquals(listOf(0f, 16f, 40f), DefaultCornerRadii)
    }

    @Test
    fun rootColorItemsCarryNoClickHandlerWhenTheyHostChildren() {
        val root = colorsContextItem(
            state = ContextBarState(strokeEnabled = true, fillColor = Color.Blue),
            dispatch = {},
            onPickStrokeColor = {},
            onPickFillColor = {},
            strokeToggleable = true,
            showFill = true,
        ).single()
        // Parent chip only hosts the submenu; tapping it must not fire an action.
        assertNull(root.onClick)
    }

    // endregion
}
