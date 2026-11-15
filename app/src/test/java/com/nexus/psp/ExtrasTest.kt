package com.nexus.psp

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.nexus.psp.emulator.CheatDb
import com.nexus.psp.emulator.NetPlay
import com.nexus.psp.emulator.Textures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Читы (разбор базы CWCheat), игра по сети (настройки ядра), HD-текстуры (распаковка набора). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExtrasTest {
    private val ctx get() = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test fun parsesCheatsForOneGame() {
        CheatDb.file(ctx).writeText(
            """
            _S ULES-00151
            _G Other Game
            _C0 Nope
            _L 0x20000000 0x00000001
            _S ULUS-10160
            _G Grand Theft Auto: Vice City Stories
            _C0 Infinite Health
            _L 0x20123456 0x000003E7
            _C1 Max Money
            _L 0x20111111 0x05F5E0FF
            _L 0x20111115 0x00000000
            _S ULES-99999
            _C0 After
            _L 0x20000000 0x00000002
            """.trimIndent(),
        )
        val l = CheatDb.forGame(ctx, "ULUS10160")
        assertEquals(listOf("Infinite Health", "Max Money"), l.map { it.name })
        assertEquals("_L 0x20111111 0x05F5E0FF+_L 0x20111115 0x00000000+", l[1].code + "+")
    }

    @Test fun netVarsForJoinAndHost() {
        val j = NetPlay.vars("join", "192.168.1.5", "02aabbccddee")
        assertEquals("enabled", j["ppsspp_enable_wlan"])
        assertEquals("IP address", j["ppsspp_change_pro_ad_hoc_server_address"])
        assertEquals("192168001005", (1..12).joinToString("") { j["ppsspp_pro_ad_hoc_server_address%02d".format(it)]!! })
        assertEquals("02aabbccddee", (1..12).joinToString("") { j["ppsspp_change_mac_address%02d".format(it)]!! })
        val h = NetPlay.vars("host", "", "02aabbccddee")
        assertEquals("localhost", h["ppsspp_change_pro_ad_hoc_server_address"])
        assertEquals("enabled", h["ppsspp_enable_builtin_pro_ad_hoc_server"])
        assertEquals(mapOf("ppsspp_enable_wlan" to "disabled"), NetPlay.vars("off", "", ""))
        assertTrue(NetPlay.validIp("10.0.0.255")); assertTrue(!NetPlay.validIp("10.0.0")); assertEquals(12, NetPlay.mac(ctx).length)
    }

    @Test fun importsTexturePackFromSubfolder() {
        val zip = File(ctx.cacheDir, "pack.zip")
        ZipOutputStream(zip.outputStream()).use { z ->
            z.putNextEntry(ZipEntry("MyPack/ULUS10160/textures.ini")); z.write("[options]\nversion = 1\n".toByteArray()); z.closeEntry()
            z.putNextEntry(ZipEntry("MyPack/ULUS10160/tex/a.png")); z.write(ByteArray(10)); z.closeEntry()
            z.putNextEntry(ZipEntry("readme.txt")); z.write("hi".toByteArray()); z.closeEntry()
        }
        val err = Textures.import(ctx, Uri.fromFile(zip), "ULUS-10160") { }
        assertNull(err)
        assertTrue(Textures.installed(ctx, "ULUS-10160"))
        assertTrue(File(Textures.dir(ctx, "ULUS-10160"), "tex/a.png").exists())
        assertTrue(!File(Textures.dir(ctx, "ULUS-10160"), "readme.txt").exists())
    }
}
