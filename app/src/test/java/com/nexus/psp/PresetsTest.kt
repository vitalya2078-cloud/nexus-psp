package com.nexus.psp

import com.nexus.psp.emulator.GamePresets
import com.nexus.psp.emulator.Templates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** База игр: узнаёт игры по именам файлов и названиям из базы дисков; у шаблонов нет повторяющихся кнопок. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PresetsTest {
    private fun genreOf(vararg n: String) = GamePresets.find(*n)?.second?.getJSONArray("layouts")?.getJSONObject(0)?.getString("base")

    @Test fun findsByFileAndDiscTitle() {
        assertEquals("foot", genreOf("Grand Theft Auto - Vice City Stories (Europe) (En,Fr,De,Es,It)"))
        assertEquals("foot", genreOf("GTA_LCS", "Grand Theft Auto - Liberty City Stories (USA)"))
        assertEquals("fight", genreOf("Tekken 6 (USA)"))
        assertEquals("fight", genreOf("Soulcalibur (Europe)", "Soulcalibur - Broken Destiny (Europe)"))
        assertEquals("race", genreOf("Need for Speed - Most Wanted 5-1-0 (USA)"))
        assertEquals("shoot", genreOf("Metal Gear Solid - Peace Walker"))
        assertEquals("flight", genreOf("Ace Combat X - Skies of Deception (USA)"))
        assertEquals("sport", genreOf("World Soccer Winning Eleven 2014 (Japan)"))
        assertEquals("slash", genreOf("God of War - Ghost of Sparta (USA)"))
        assertEquals("rpg", genreOf("Persona 3 Portable"))
        assertNull(GamePresets.find("Last Frontier"))
        assertTrue(GamePresets.size >= 100)
    }

    @Test fun vcsHasThreeLayoutsWithSwitches() {
        val o = GamePresets.find("Grand Theft Auto - Vice City Stories")!!.second
        val ids = (0 until o.getJSONArray("layouts").length()).map { o.getJSONArray("layouts").getJSONObject(it).getString("id") }
        assertEquals(listOf("foot", "drive", "heli"), ids)
        // «Сесть» (△) переключает на транспорт, «Выйти» — обратно
        assertEquals("drive", Templates.build("foot", "psp").first { it.key == "ft" }.to)
        assertEquals("foot", Templates.build("drive", "psp").first { it.key == "ft" }.to)
    }

    @Test fun templatesHaveUniqueIds() {
        Templates.BASES.forEach { (b, _) ->
            val ids = Templates.build(b, "psp").map { it.id }
            assertEquals("повтор id в шаблоне $b", ids.size, ids.toSet().size)
            assertNotNull(b)
        }
    }
}
