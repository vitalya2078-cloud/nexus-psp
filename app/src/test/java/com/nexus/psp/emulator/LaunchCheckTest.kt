package com.nexus.psp.emulator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Причина, почему игра не запустилась: каждая проверка на поддельном файле. Камера правым стиком на PS2. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LaunchCheckTest {
    private val tmp = File(System.getProperty("java.io.tmpdir"), "launchtest").apply { deleteRecursively(); mkdirs() }
    private fun le32(b: ByteArray, o: Int, v: Int) { for (i in 0..3) b[o + i] = (v ushr (8 * i)).toByte() }

    /** ISO: метка, размер тома (в секторах), SYSTEM.CNF. */
    private fun iso(name: String, system: String, cnf: String?, volBlocks: Int = 24, size: Int = 24): File {
        val img = ByteArray(2048 * size); val pvd = 16 * 2048
        img[pvd] = 1; "CD001".toByteArray().copyInto(img, pvd + 1); system.padEnd(32).toByteArray().copyInto(img, pvd + 8)
        le32(img, pvd + 80, volBlocks); le32(img, pvd + 156 + 2, 18); le32(img, pvd + 156 + 10, 2048)
        if (cnf != null) {
            val dir = 18 * 2048; val fn = "SYSTEM.CNF;1".toByteArray(); val c = cnf.toByteArray()
            img[dir] = (33 + fn.size).toByte(); le32(img, dir + 2, 19); le32(img, dir + 10, c.size); img[dir + 32] = fn.size.toByte(); fn.copyInto(img, dir + 33); c.copyInto(img, 19 * 2048)
        }
        return File(tmp, name).apply { writeBytes(img) }
    }

    @Test fun ps2GameRunAsPspOffersPs2() {
        val w = LaunchCheck.diagnose(iso("hitman.iso", "PLAYSTATION", "BOOT2 = cdrom0:\\SLES_523.38;1\r\n"), "psp")
        assertEquals("ps2", w.asPlat); assertTrue(w.text, "PS2" in w.text && "PSP" in w.text)
    }

    @Test fun truncatedIso() {
        // том записан на 4000 секторов (~8 МБ), а файл — 24 сектора
        val w = LaunchCheck.diagnose(iso("cut.iso", "PLAYSTATION", "BOOT2 = x\r\n", volBlocks = 4000), "ps2")
        assertTrue(w.text, w.text.startsWith("Файл неполный")); assertNull(w.asPlat)
    }

    @Test fun notAGameInside() {
        val f = File(tmp, "page.iso").apply { writeText("<html>" + "x".repeat(5000) + "</html>") }
        assertTrue(LaunchCheck.diagnose(f, "ps2").text.startsWith("Внутри файла не образ игры"))
    }

    @Test fun missingCueTrack() {
        val cue = File(tmp, "game.cue").apply { writeText("FILE \"game (Track 1).bin\" BINARY\n  TRACK 01 MODE2/2352\n    INDEX 01 00:00:00\n") }
        val w = LaunchCheck.diagnose(cue, "ps1")
        assertTrue(w.text, "game (Track 1).bin" in w.text)
    }

    @Test fun wrongFormatForConsole() {
        val f = File(tmp, "game.pbp").apply { writeBytes(ByteArray(4096).also { it[1] = 'P'.code.toByte(); it[2] = 'B'.code.toByte(); it[3] = 'P'.code.toByte() }) }
        val w = LaunchCheck.diagnose(f, "ps2")
        assertTrue(w.text, w.text.startsWith("PS2 не открывает файлы .pbp")); assertEquals("psp", w.asPlat)
    }

    @Test fun correctGameFallsBackToLogThenGeneric() {
        val f = iso("ok.iso", "PLAYSTATION", "BOOT2 = x\r\n")
        assertTrue(LaunchCheck.diagnose(f, "ps2", listOf("E/Libretro Core( 1): BIOS not found in system/pcsx2/bios")).text.contains("BIOS"))
        assertTrue(LaunchCheck.diagnose(f, "ps2", listOf("E/Libretro Core( 1): failed to allocate 512MB")).text.contains("памяти"))
        val g = LaunchCheck.diagnose(f, "ps2")
        assertTrue(g.text, g.text.startsWith("Эмулятор PS2 не смог открыть")); assertNull(g.asPlat)
    }

    @Test fun ps2CameraBySwipe() {
        for (b in listOf("touch", "foot", "shoot", "slash")) assertTrue(b, Templates.build(b, "ps2").any { it.cam == ControlLayout.CAM_STICK })
        assertTrue(Templates.build("touch", "ps1").any { it.cam == ControlLayout.CAM_STICK })
        assertTrue(Templates.build("touch", "psp").none { it.cam != null })
        assertTrue(Templates.build("classic", "ps2").none { it.cam != null })
        // своя зона камеры у раскладки игры не подменяется
        assertEquals(1, Templates.build("g_ac", "ps2").count { it.cam != null })
    }
}
