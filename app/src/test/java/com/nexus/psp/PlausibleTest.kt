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
}
