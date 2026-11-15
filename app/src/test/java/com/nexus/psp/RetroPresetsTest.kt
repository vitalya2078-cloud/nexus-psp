package com.nexus.psp

import com.nexus.psp.emulator.ControlLayout
import com.nexus.psp.emulator.GamePresets
import com.nexus.psp.emulator.Guide
import com.nexus.psp.emulator.Templates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Пресеты игр ретро-консолей: только своя консоль, родной джойстик с подписями действий. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RetroPresetsTest {
    private fun base(plat: String, name: String) = GamePresets.find(plat, name)?.second?.getJSONArray("layouts")?.getJSONObject(0)?.getString("base")

    @Test fun matchesOnlyItsConsole() {
        assertEquals("r_md_sonicthehedgehog", base("md", "Sonic The Hedgehog 2 (World)"))
        assertTrue(base("sms", "Sonic the Hedgehog (Europe)")!!.startsWith("r_sms_"))
        assertEquals("platform", base("psp", "Sonic Rivals"))          // PSP — жанровая раскладка
        assertTrue(base("nes", "Mega Man 2 (USA)")!!.startsWith("r_nes_"))
        assertEquals("g_megaman", base("psp", "Mega Man Powered Up"))
        assertNull(GamePresets.find("md", "Persona 3 Portable"))        // PSP-игра на другой консоли не находится
        assertNotNull(GamePresets.find("ps2", "Grand Theft Auto - San Andreas"))
        assertNull(GamePresets.find("psp", "Grand Theft Auto - San Andreas"))
    }

    @Test fun retroLayoutKeepsPadAndLabelsActions() {
        val b = Templates.build(base("neogeo", "Metal Slug X")!!, "neogeo")
        val pad = ControlLayout.layout("touch", "neogeo")
        assertEquals(pad.map { it.id }, b.map { it.id })                // те же кнопки и места
        val shoot = b.first { it.key == "fb" }
        assertEquals("shoot", shoot.icon); assertEquals("Огонь", shoot.cap)
        assertEquals("grenade", b.first { it.key == "fl" }.icon)
        // гайд называет кнопку консоли так, как на её джойстике
        val ctl = GamePresets.find("neogeo", "Metal Slug X")!!.second
        val row = Guide.modes(ctl, "neogeo").first().rows.first { it.name == "Огонь" }
        assertEquals(listOf("A"), row.keys)
    }

    @Test fun everyRetroPresetBuilds() {
        for (e in GamePresets.entries) {
            val o = GamePresets.byName(e.name)
            assertNotNull(e.name, o)
            val lay = o!!.getJSONArray("layouts").getJSONObject(0).getString("base")
            val btns = Templates.build(lay, e.plat)
            assertTrue(e.name, btns.isNotEmpty())
            if (lay.startsWith("r_")) assertTrue(e.name, btns.any { it.cap != null && it.icon != null && it.key in setOf("fb", "fr", "fl", "ft", "l1", "r1") })
        }
    }

    @Test fun guideNamesForEveryPad() {
        for (p in ControlLayout.NAMES.keys) {
            val rows = Guide.modes(null, p).first().rows
            assertTrue(p, rows.isNotEmpty())
            // ни одной строки с символами PlayStation у ретро-консолей
            assertTrue(p, rows.none { r -> r.keys.any { it in setOf("×", "○", "□", "△", "fb", "fr", "fl", "ft") } })
        }
    }
}
