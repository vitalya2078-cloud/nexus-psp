package com.nexus.psp.emulator

import androidx.test.core.app.ApplicationProvider
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** PS1: определение консоли по файлу, номер игры из SYSTEM.CNF, CUE с дорожками в библиотеке. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Ps1Test {
    private val tmp = File(System.getProperty("java.io.tmpdir"), "ps1test").apply { deleteRecursively(); mkdirs() }

    private fun le32(b: ByteArray, o: Int, v: Int) { for (i in 0..3) b[o + i] = (v ushr (8 * i)).toByte() }
    private fun be32(b: ByteArray, o: Int, v: Int) { for (i in 0..3) b[o + i] = (v ushr (8 * (3 - i))).toByte() }

    /** Образ ISO 9660: метка системы и SYSTEM.CNF; sector — 2048 (ISO) или 2352 (BIN, режим 2). */
    private fun disc(name: String, system: String, sector: Int): File {
        val off = if (sector == 2352) 24 else 0
        val img = ByteArray(sector * 24)
        fun data(lba: Int) = lba * sector + off
        if (sector == 2352) for (l in 0 until 24) { img[l * sector + 15] = 2 }
        val pvd = data(16)
        img[pvd] = 1; "CD001".toByteArray().copyInto(img, pvd + 1)
        system.padEnd(32).toByteArray().copyInto(img, pvd + 8)
        le32(img, pvd + 156 + 2, 18); le32(img, pvd + 156 + 10, 2048)
        val dir = data(18); val fname = "SYSTEM.CNF;1".toByteArray()
        val cnf = "BOOT = cdrom:\\SLUS_005.94;1\r\nTCB = 4\r\n".toByteArray()
        img[dir] = (33 + fname.size).toByte(); le32(img, dir + 2, 19); le32(img, dir + 10, cnf.size); img[dir + 32] = fname.size.toByte(); fname.copyInto(img, dir + 33)
        cnf.copyInto(img, data(19))
        return File(tmp, name).apply { writeBytes(img) }
    }

    @Test fun rawBinIsPs1WithSerial() {
        val f = disc("Game.bin", "PLAYSTATION", 2352)
        assertEquals("ps1", Disc.platform(f))
        assertEquals("SLUS-00594", Disc.read(f)?.serial)
    }

    @Test fun isoByVolumeLabel() {
        assertEquals("ps1", Disc.platform(disc("ps1.iso", "PLAYSTATION", 2048)))
        assertEquals("psp", Disc.platform(disc("psp.iso", "PSP GAME", 2048)))
    }

    @Test fun chdCdIsPs1DvdIsPsp() {
        fun chd(name: String, tag: String): File {
            val b = ByteArray(160); "MComprHD".toByteArray().copyInto(b); be32(b, 12, 5); be32(b, 52, 124)
            tag.toByteArray().copyInto(b, 124)
            return File(tmp, name).apply { writeBytes(b) }
        }
        assertEquals("ps1", Disc.platform(chd("a.chd", "CHT2")))
        assertEquals("psp", Disc.platform(chd("b.chd", "DVD ")))
    }

    @Test fun pbpPs1ByPsar() {
        val b = ByteArray(256); b[1] = 'P'.code.toByte(); b[2] = 'B'.code.toByte(); b[3] = 'P'.code.toByte()
        for (i in 0 until 8) le32(b, 8 + i * 4, 40)
        le32(b, 8 + 7 * 4, 100); "PSISOIMG0000".toByteArray().copyInto(b, 100)
        assertEquals("ps1", Disc.platform(File(tmp, "eboot.pbp").apply { writeBytes(b) }))
        le32(b, 8 + 7 * 4, 200)
        assertEquals("psp", Disc.platform(File(tmp, "psp.pbp").apply { writeBytes(b) }))
    }

    @Test fun cueHidesTracksInLibrary() {
        val c = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dir = CoreFiles.gamesDir(c); dir.listFiles()?.forEach { it.delete() }
        disc("x.bin", "PLAYSTATION", 2352).copyTo(File(dir, "Crash Bandicoot (USA) (Track 1).bin"))
        File(dir, "Crash Bandicoot (USA) (Track 2).bin").writeBytes(ByteArray(2352 * 10))
        File(dir, "Crash Bandicoot (USA).cue").writeText("FILE \"Crash Bandicoot (USA) (Track 1).bin\" BINARY\n  TRACK 01 MODE2/2352\nFILE \"Crash Bandicoot (USA) (Track 2).bin\" BINARY\n  TRACK 02 AUDIO\n")
        val a = JSONArray(GameLibrary.list(c))
        assertEquals(1, a.length())
        assertEquals("Crash Bandicoot (USA).cue", a.getJSONObject(0).getString("file"))
        assertEquals("ps1", a.getJSONObject(0).getString("plat"))
        assertEquals(emptyList<String>(), GameLibrary.missingParts(File(dir, "Crash Bandicoot (USA).cue")))
        assertEquals("SLUS-00594", Disc.read(File(dir, "Crash Bandicoot (USA).cue"))?.serial)
    }
}
