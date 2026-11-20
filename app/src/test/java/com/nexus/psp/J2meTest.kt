package com.nexus.psp

import com.nexus.psp.data.AppState
import com.nexus.psp.data.PL
import com.nexus.psp.emulator.Disc
import com.nexus.psp.emulator.GameLibrary
import com.nexus.psp.emulator.J2me
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** Java-игры (J2ME): распознавание .jar и установка в движок J2ME Loader (перевод в dex, файлы игры, настройки). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class J2meTest {
    private val ctx = RuntimeEnvironment.getApplication()

    /** Крошечная «игра»: один класс и MANIFEST.MF с MIDlet-1 (собрана javac + jar). */
    private val helloJar = java.util.Base64.getDecoder().decode("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")

    private fun jar(name: String, bytes: ByteArray = helloJar) = File(ctx.filesDir, name).apply { writeBytes(bytes) }

    @Test fun platformAndRecognition() {
        assertEquals("Java-игры (J2ME)", PL["j2me"]!!.full)
        assertTrue("jar" in GameLibrary.EXT)
        val f = jar("Hello Test.jar")
        assertEquals("j2me", Disc.platformOf(f))
        assertEquals(Disc.Verdict.OK, Disc.verdict("jar", f.length(), Disc.head(f)))
        assertTrue(J2me.isMidlet(f))
        // .jar без MIDlet — не Java-игра для телефона
        val plain = File(ctx.filesDir, "lib.jar").apply {
            java.util.zip.ZipOutputStream(outputStream()).use { z -> z.putNextEntry(java.util.zip.ZipEntry("META-INF/MANIFEST.MF")); z.write("Manifest-Version: 1.0\n".toByteArray()); z.closeEntry() }
        }
        assertFalse(J2me.isMidlet(plain))
        // текст с расширением .jar — не игра
        val txt = File(ctx.filesDir, "readme.jar").apply { writeText("x".repeat(4000)) }
        assertEquals(Disc.Verdict.TEXT, Disc.verdict("jar", txt.length(), Disc.head(txt)))
    }

    @Test fun installConvertsJar() {
        // сама установка (без настроек движка: они в пакете javax.*, который Robolectric не загружает)
        val f = jar("Hello Test.jar")
        val dir = File(ctx.filesDir, "j2me-test/Hello").apply { parentFile!!.deleteRecursively() }
        assertNull(J2me.installTo(f, dir))
        assertTrue(File(dir, "converted.dex").length() > 0)
        assertTrue(File(dir, "res.jar").exists())
        assertTrue(File(dir, "converted.dex.conf").readText().contains("Hello Test"))
        // повторная установка поверх — без ошибок
        assertNull(J2me.installTo(f, dir))
        // ключ установки зависит от пути: одноимённые игры из разных папок не путаются
        AppState.init(ctx)
        val g = AppState.addImported(JSONObject().put("id", "g_j2me").put("name", "Hello Test").put("file", f.name).put("path", f.path).put("size", 0.01).put("plat", "j2me"))
        assertFalse(J2me.key(g) == J2me.key(g.copy(path = "/storage/emulated/0/Other/Hello Test.jar")))
        assertTrue(J2me.key(g).matches(Regex("^[A-Za-z0-9_-]+$")))
    }

    @Test fun brokenJarIsReported() {
        val f = jar("Broken.jar", helloJar.copyOf(200))
        val dir = File(ctx.filesDir, "j2me-test/Broken").apply { parentFile!!.deleteRecursively() }
        assertNotNull(J2me.installTo(f, dir))
        assertFalse(File(dir, "converted.dex").exists())
    }

    @Test fun adpcmDecodes() {
        // WAV IMA ADPCM, моно, 8000 Гц, один блок 36 байт: 4 байта заголовка блока + 32 байта (64 отсчёта)
        val block = ByteArray(36).also { it[0] = 0x10; it[1] = 0; it[2] = 0; for (i in 4 until 36) it[i] = 0x31 }
        fun le(v: Int, n: Int) = ByteArray(n) { (v shr (8 * it)).toByte() }
        val fmt = le(0x11, 2) + le(1, 2) + le(8000, 4) + le(4055, 4) + le(36, 2) + le(4, 2) + le(2, 2) + le(65, 2)
        val body = "WAVE".toByteArray() + "fmt ".toByteArray() + le(fmt.size, 4) + fmt + "data".toByteArray() + le(block.size, 4) + block
        val wav = "RIFF".toByteArray() + le(body.size, 4) + body
        val pcm = ru.kapsula.j2me.Adpcm.decodeWav(wav)!!
        assertEquals("PCM", 1, pcm[20].toInt())
        assertEquals("1 + 64 отсчёта по 2 байта", 44 + 65 * 2, pcm.size)
        assertNull("обычный PCM не трогаем", ru.kapsula.j2me.Adpcm.decodeWav(pcm))
    }
}
