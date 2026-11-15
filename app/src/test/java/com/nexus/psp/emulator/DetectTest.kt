package com.nexus.psp.emulator

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Консоль по содержимому файла — для всех консолей и форматов (поддельные образы, только заголовки). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DetectTest {
    private val tmp = File(System.getProperty("java.io.tmpdir"), "detecttest").apply { deleteRecursively(); mkdirs() }

    @After fun reset() { Disc.chosen = emptySet() }

    private fun le32(b: ByteArray, o: Int, v: Int) { for (i in 0..3) b[o + i] = (v ushr (8 * i)).toByte() }
    private fun be32(b: ByteArray, o: Int, v: Int) { for (i in 0..3) b[o + i] = (v ushr (8 * (3 - i))).toByte() }
    private fun be64(b: ByteArray, o: Int, v: Long) { be32(b, o, (v ushr 32).toInt()); be32(b, o + 4, v.toInt()) }

    /** ISO 9660 по 2048 байт: метка системы и SYSTEM.CNF (BOOT2 — PS2, BOOT — PS1). */
    private fun iso(system: String, cnf: String?): ByteArray {
        val img = ByteArray(2048 * 24)
        val pvd = 16 * 2048
        img[pvd] = 1; "CD001".toByteArray().copyInto(img, pvd + 1)
        system.padEnd(32).toByteArray().copyInto(img, pvd + 8)
        le32(img, pvd + 156 + 2, 18); le32(img, pvd + 156 + 10, 2048)
        if (cnf != null) {
            val dir = 18 * 2048; val fname = "SYSTEM.CNF;1".toByteArray(); val c = cnf.toByteArray()
            img[dir] = (33 + fname.size).toByte(); le32(img, dir + 2, 19); le32(img, dir + 10, c.size); img[dir + 32] = fname.size.toByte(); fname.copyInto(img, dir + 33)
            c.copyInto(img, 19 * 2048)
        }
        return img
    }
    private fun file(name: String, b: ByteArray) = File(tmp, name).apply { writeBytes(b) }

    private val PS2 = "BOOT2 = cdrom0:\\SLUS_203.12;1\r\nVER = 1.00\r\n"
    private val PS1 = "BOOT = cdrom:\\SLUS_005.94;1\r\n"

    /** CSO (CISO v1) без сжатия: все блоки помечены «как есть». */
    private fun cso(iso: ByteArray): ByteArray {
        val bs = 2048; val n = iso.size / bs
        val head = 24 + (n + 1) * 4
        val out = ByteArray(head + iso.size)
        "CISO".toByteArray().copyInto(out); le32(out, 4, 24)
        for (i in 0..7) out[8 + i] = (iso.size.toLong() ushr (8 * i)).toByte()
        le32(out, 16, bs); out[20] = 1; out[21] = 0
        for (i in 0..n) le32(out, 24 + i * 4, (head + i * bs) or 0x80000000.toInt())
        iso.copyInto(out, head)
        return out
    }

    /** CHD v5: размер образа и цепочка метаданных (тег + текст). */
    private fun chd(name: String, logical: Long, tag: String, meta: String = ""): File {
        val b = ByteArray(124 + 16 + meta.length)
        "MComprHD".toByteArray().copyInto(b); be32(b, 12, 5); be64(b, 32, logical); be64(b, 48, 124)
        tag.toByteArray().copyInto(b, 124)
        b[129] = (meta.length ushr 16).toByte(); b[130] = (meta.length ushr 8).toByte(); b[131] = meta.length.toByte()
        meta.toByteArray().copyInto(b, 140)
        return file(name, b)
    }

    @Test fun ps2IsoAndPs1IsoAndPsp() {
        assertEquals("ps2", Disc.platform(file("god.iso", iso("PLAYSTATION", PS2))))
        assertEquals("ps1", Disc.platform(file("crash.iso", iso("PLAYSTATION", PS1))))
        assertEquals("psp", Disc.platform(file("gta.iso", iso("PSP GAME", null))))
    }

    @Test fun csoReadsVolumeLabel() {
        assertEquals("ps2", Disc.platform(file("god.cso", cso(iso("PLAYSTATION", PS2)))))
        assertEquals("psp", Disc.platform(file("gta.cso", cso(iso("PSP GAME", null)))))
    }

    @Test fun ps2OnRawCdBin() {
        // PS2 на компакт-диске: сырой образ 2352, режим 2
        val iso = iso("PLAYSTATION", PS2); val raw = ByteArray(2352 * 24)
        for (l in 0 until 24) { raw[l * 2352 + 15] = 2; System.arraycopy(iso, l * 2048, raw, l * 2352 + 24, 2048) }
        assertEquals("ps2", Disc.platform(file("cdgame.bin", raw)))
    }

    @Test fun chdByTracksAndSize() {
        assertEquals("dc", Disc.platform(chd("dc.chd", 1_000_000_000, "CHGD")))
        assertEquals("ps1", Disc.platform(chd("ps1.chd", 700_000_000, "CHT2", "TRACK:1 TYPE:MODE2_RAW SUBTYPE:NONE FRAMES:1000")))
        assertEquals("saturn", Disc.platform(chd("sat.chd", 600_000_000, "CHT2", "TRACK:1 TYPE:MODE1_RAW SUBTYPE:NONE FRAMES:1000")))
        assertEquals("ps2", Disc.platform(chd("big.chd", 4_000_000_000, "DVD ")))
        assertEquals("psp", Disc.platform(chd("small.chd", 900_000_000, "DVD ")))
    }

    @Test fun smallDvdChdFollowsChosenConsoles() {
        Disc.chosen = setOf("ps2", "ps1")
        assertEquals("ps2", Disc.platform(chd("small.chd", 900_000_000, "DVD ")))
        Disc.chosen = setOf("psp", "ps2")
        assertEquals("psp", Disc.platform(chd("small2.chd", 900_000_000, "DVD ")))
    }

    @Test fun segaDiscsAndCarts() {
        val sat = ByteArray(2048 * 20); "SEGA SEGASATURN ".toByteArray().copyInto(sat)
        assertEquals("saturn", Disc.platform(file("nights.iso", sat)))
        val md = ByteArray(256 * 1024); "SEGA MEGA DRIVE ".toByteArray().copyInto(md, 0x100)
        assertEquals("md", Disc.platform(file("sonic.bin", md)))
        val sms = ByteArray(128 * 1024); "TMR SEGA".toByteArray().copyInto(sms, 0x7FF0)
        assertEquals("sms", Disc.platform(file("alex.bin", sms)))
        assertEquals("a26", Disc.platform(file("pitfall.bin", ByteArray(4096))))
    }

    @Test fun cartridgesByExtension() {
        val cases = mapOf("smb.nes" to "nes", "sonic.md" to "md", "alex.sms" to "sms", "shinobi.gg" to "gg", "bonk.pce" to "pce",
            "kof.neo" to "neogeo", "snk.ngp" to "ngp", "klonoa.wsc" to "ws", "pitfall.a26" to "a26", "ninja.a78" to "a78",
            "chip.lnx" to "lynx", "shenmue.gdi" to "dc", "soul.cdi" to "dc", "god.zso" to "ps2", "game.pbp" to "psp")
        for ((name, plat) in cases) assertEquals(name, plat, Disc.platform(file(name, ByteArray(64))))
    }
}
