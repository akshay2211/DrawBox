package io.ak1.drawbox.ui.toolbar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the [FloatingMenuItem] value type and its factory helpers. The
 * `icon` slot is a composable lambda and is never invoked here.
 */
class FloatingMenuItemTest {

    @Test
    fun defaultsAreInertLeaf() {
        val item = FloatingMenuItem(id = "x")
        assertTrue(item.children.isEmpty())
        assertNull(item.onClick)
        assertFalse(item.isActive)
        assertFalse(item.isSeparator)
    }

    @Test
    fun separatorFactoryMarksSeparatorAndKeepsId() {
        val sep = separator("sep-1")
        assertEquals("sep-1", sep.id)
        assertTrue(sep.isSeparator)
        assertTrue(sep.children.isEmpty())
        assertNull(sep.onClick)
    }

    @Test
    fun submenuPositionHasBothDirections() {
        assertEquals(setOf("Above", "Below"), SubmenuPosition.entries.map { it.name }.toSet())
    }
}
