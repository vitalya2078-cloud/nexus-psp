package com.nexus.psp

import com.nexus.psp.emulator.ControlLayout
import com.nexus.psp.emulator.CtlIcons
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Редактор: стрелки и родные кнопки консоли для своих кнопок. */
class EditorButtonsTest {
    @Test fun arrowsInButtonsGroup() {
        val g = CtlIcons.groups("psp").first { it.first == "Кнопки" }.second.map { it.first }
        assertTrue(g.containsAll(listOf("arr_up", "arr_down", "arr_left", "arr_right")))
    }

    @Test fun classicGroupPerConsole() {
        val md = CtlIcons.groups("md").last().also { assertEquals("Классика", it.first) }.second.toMap()
        assertEquals("A", md["k:fl"]); assertEquals("C", md["k:fr"]); assertEquals("Z", md["k:r1"]); assertEquals("Mode", md["select"])
        val neo = ControlLayout.classicIcons("neogeo").toMap()
        assertEquals("D", neo["k:ft"]); assertEquals("Монетка", neo["coin"])
        assertEquals(ControlLayout.ROSE, ControlLayout.classicColor("neogeo", "fb"))
        val psp = ControlLayout.classicIcons("psp").map { it.first }
        assertTrue("fb" in psp && "l1" in psp && "l2" !in psp)
    }

    @Test fun partsOnlyWhatConsoleHas() {
        val md = ControlLayout.parts("md")
        assertTrue(md.containsAll(listOf("fl", "fb", "fr", "l1", "ft", "r1", "select", "start", "up", "down", "left", "right")))
        assertFalse("l2" in md || "r2" in md)
        assertEquals(listOf("fb", "start", "select", "l1", "l2", "up", "down", "left", "right").toSet(), ControlLayout.parts("a26").toSet())
        assertFalse("l2" in ControlLayout.parts("psp"))
        assertTrue("r2" in ControlLayout.parts("ps2"))
    }

    @Test fun newOwnButtonLooksNative() {
        assertEquals("k:fr", ControlLayout.classicIcon("md", "fr"))
        assertEquals("coin", ControlLayout.classicIcon("neogeo", "select"))
        assertEquals("arr_up", ControlLayout.classicIcon("md", "up"))
        assertEquals("fb", ControlLayout.classicIcon("psp", "fb"))
    }
}
