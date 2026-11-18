package com.nexus.psp

import com.nexus.psp.emulator.Disc
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

    @Test fun pspIsoWithUnknownProviderSizeIsAcceptedByHeader() {
        val iso = ByteArray(40 * 1024)
        "PSP GAME".toByteArray().copyInto(iso, 0x8008)
        assertTrue(Disc.plausible("iso", 0L, iso))
        assertTrue(Disc.plausible("iso", -1L, iso))
        assertFalse(Disc.plausible("iso", 0L, ByteArray(0)))
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
}
