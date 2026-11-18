package com.nexus.psp

import com.nexus.psp.emulator.Disc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Сканер не должен принимать текстовые файлы за игры. */
class PlausibleTest {
    @Test fun readmeIsNotMegaDrive() {
        val txt = ("# Проект\n\nОписание игры и инструкция по установке.\n").repeat(200).toByteArray()
        assertFalse(Disc.plausible("md", txt.size.toLong(), txt))
    }

    @Test fun segaHeaderIsMegaDrive() {
        val rom = ByteArray(256 * 1024); "SEGA MEGA DRIVE".toByteArray().copyInto(rom, 0x100)
        assertTrue(Disc.plausible("md", rom.size.toLong(), rom))
        assertTrue(Disc.plausible("bin", rom.size.toLong(), rom))
    }

    @Test fun randomTextBinRejected() {
        val txt = "hello world, this is a log file\n".repeat(500).toByteArray()
        assertFalse(Disc.plausible("bin", txt.size.toLong(), txt))
        assertFalse(Disc.plausible("iso", txt.size.toLong(), txt))
    }

    @Test fun cueNeedsFileAndTrack() {
        val cue = "FILE \"game.bin\" BINARY\n  TRACK 01 MODE2/2352\n    INDEX 01 00:00:00\n".toByteArray()
        assertTrue(Disc.plausible("cue", cue.size.toLong(), cue))
        val notes = "just some notes".toByteArray()
        assertFalse(Disc.plausible("cue", notes.size.toLong(), notes))
    }

    @Test fun dendyCartridge() {
        val rom = ByteArray(40 * 1024); "NES\u001A".toByteArray(Charsets.ISO_8859_1).copyInto(rom, 0)
        assertTrue(Disc.plausible("nes", rom.size.toLong(), rom))
        val txt = "not a game, just notes\n".repeat(2000).toByteArray()
        assertFalse(Disc.plausible("nes", txt.size.toLong(), txt))
        org.junit.Assert.assertEquals("nes", com.nexus.psp.data.PLATFORMS.first { "nes" in it.exts }.id)
        val pad = com.nexus.psp.emulator.ControlLayout.layout("touch", "nes")
        org.junit.Assert.assertEquals("fb", pad.first { it.id == "b" }.key)
        org.junit.Assert.assertEquals("fr", pad.first { it.id == "a" }.key)
        org.junit.Assert.assertTrue(pad.none { it.id in setOf("l1", "r1", "l2", "r2") || it.stick != null })
    }
    /** Начало ISO 9660: метка тома CD001 в 16-м секторе (+ «PSP GAME», если нужно). */
    private fun isoHead(psp: Boolean = true): ByteArray {
        val b = ByteArray(Disc.HEAD); b[0x8000] = 1; "CD001".toByteArray().copyInto(b, 0x8001)
        if (psp) "PSP GAME".padEnd(32).toByteArray().copyInto(b, 0x8008)
        return b
    }

    /** Папки Android иногда не сообщают размер (0 или -1): образ проверяется по заголовку, а не отбрасывается. */
    @Test fun unknownSizeIsoCheckedByHeader() {
        val iso = isoHead()
        assertEquals(Disc.Verdict.OK, Disc.verdict("iso", 0, iso))
        assertEquals(Disc.Verdict.OK, Disc.verdict("iso", -1, iso))
        assertTrue(Disc.plausible("iso", 0, iso))
        assertTrue(Disc.plausible("iso", 1_500_000_000L, iso))
    }

    /** Образ без привычной метки — не «текст» и не «маленький»: его можно добавить с пометкой «не проверен». */
    @Test fun isoWithoutMarkIsNoMark() {
        val zeros = ByteArray(Disc.HEAD)
        assertEquals(Disc.Verdict.NO_MARK, Disc.verdict("iso", 300L * 1024 * 1024, zeros))
        assertEquals(Disc.Verdict.NO_MARK, Disc.verdict("iso", 0, zeros))
        assertTrue("iso" in Disc.DISC_EXT && "cso" in Disc.DISC_EXT)
    }

    @Test fun emptyTinyAndTextFiles() {
        assertEquals(Disc.Verdict.SMALL, Disc.verdict("iso", 0, ByteArray(0)))
        assertEquals(Disc.Verdict.SMALL, Disc.verdict("iso", 500, ByteArray(500)))
        val txt = "hello world, this is a log file\n".repeat(500).toByteArray()
        assertEquals(Disc.Verdict.TEXT, Disc.verdict("iso", txt.size.toLong(), txt))
        assertEquals(Disc.Verdict.TEXT, Disc.verdict("iso", 0, txt))
    }

    /** Картриджи с неизвестным размером: по заголовку — да, без признаков — нет. */
    @Test fun unknownSizeCartridges() {
        val rom = ByteArray(Disc.HEAD); "SEGA MEGA DRIVE".toByteArray().copyInto(rom, 0x100)
        assertEquals(Disc.Verdict.OK, Disc.verdict("md", 0, rom))
        assertEquals(Disc.Verdict.NO_MARK, Disc.verdict("md", 0, ByteArray(Disc.HEAD)))
        assertEquals(Disc.Verdict.SMALL, Disc.verdict("nes", 0, ByteArray(100) { 1 }))
    }
}
