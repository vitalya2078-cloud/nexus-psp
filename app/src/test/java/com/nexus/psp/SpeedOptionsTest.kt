package com.nexus.psp

import com.nexus.psp.data.CoreOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 0.50.3: игры PSP на 30 к/с не должны ускоряться — пропуск кадров только переключателем, по умолчанию выкл. */
class SpeedOptionsTest {
    @Test fun frameskipSwitchIsOffByDefault() {
        val o = CoreOptions.SMOOTH.first { it.key == "kapsula_frameskip" }
        assertEquals("disabled", o.def)
        assertEquals(listOf("disabled", "enabled"), o.values)
    }

    @Test fun oldFrameskipOptionsAreGone() {
        val keys = CoreOptions.ALL.map { it.key }
        listOf("ppsspp_frameskip", "ppsspp_auto_frameskip", "ppsspp_frame_duplication").forEach { assertFalse(it, it in keys) }
    }

    @Test fun presetsDoNotTouchFrameskip() {
        CoreOptions.PRESETS.values.forEach { p -> assertFalse(p.keys.any { "frameskip" in it }) }
    }

    @Test fun keysAreUnique() {
        val keys = CoreOptions.ALL.map { it.key } + CoreOptions.PS1.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        assertTrue(CoreOptions.byKey("ppsspp_cache_iso")?.restart == true)
    }
}
