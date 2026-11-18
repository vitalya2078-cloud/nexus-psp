package com.nexus.psp

import com.nexus.psp.emulator.CoreFiles
import com.nexus.psp.emulator.GameLibrary
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

/** Игры на месте: добавляются без копирования, при пропаже файла остаются в библиотеке, удаление не трогает чужой файл. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LinkedGamesTest {
    private val ctx = RuntimeEnvironment.getApplication()
    private val dir = File(System.getProperty("java.io.tmpdir"), "linkedtest").apply { deleteRecursively(); mkdirs() }

    /** Поддельный образ PSP: метка тома CD001 и «PSP GAME». */
    private fun iso(name: String) = File(dir, name).apply {
        val b = ByteArray(2048 * 20); b[0x8000] = 1; "CD001".toByteArray().copyInto(b, 0x8001)
        "PSP GAME".padEnd(32).toByteArray().copyInto(b, 0x8008)
        writeBytes(b)
    }

    private fun listed(): List<JSONObject> = JSONArray(GameLibrary.list(ctx)).let { a -> (0 until a.length()).map { a.getJSONObject(it) } }

    @Before fun clean() { File(ctx.filesDir, "links.json").delete() }

    @Test fun linkedGamePlaysInPlace() {
        val f = iso("Test Game.iso")
        val info = GameLibrary.link(ctx, f)
        assertEquals(f.absolutePath, info.getString("path"))
        assertTrue(info.getBoolean("linked"))
        val g = listed().first { it.getString("path") == f.absolutePath }
        assertEquals("psp", g.getString("plat"))
        assertFalse(g.optBoolean("missing"))
        // копии в папке приложения нет — играем прямо из места, где лежит файл
        assertFalse(File(CoreFiles.gamesDir(ctx), f.name).exists())
        assertTrue(GameLibrary.isLinked(ctx, f.absolutePath))
    }

    @Test fun missingFileKeepsGame() {
        val f = iso("Lost Game.iso")
        GameLibrary.link(ctx, f)
        f.delete()
        val g = listed().first { it.getString("path") == f.absolutePath }
        assertTrue(g.getBoolean("missing"))
        assertEquals("psp", g.getString("plat"))
        assertEquals(GameLibrary.idOf("Lost Game.iso"), g.getString("id"))
    }

    @Test fun removingKeepsUserFile() {
        val f = iso("Other Folder Game.iso")
        GameLibrary.link(ctx, f)
        assertTrue(GameLibrary.keepsFile(ctx, f.absolutePath))
        assertTrue(GameLibrary.delete(ctx, f.absolutePath))
        assertTrue("файл из чужой папки не удаляется", f.exists())
        assertNull(listed().firstOrNull { it.getString("path") == f.absolutePath })
        assertFalse(GameLibrary.isLinked(ctx, f.absolutePath))
    }

    @Test fun copiedGameWinsOverSameName() {
        val f = iso("Same Name.iso")
        f.copyTo(File(CoreFiles.gamesDir(ctx), f.name), overwrite = true)
        GameLibrary.link(ctx, f)
        val same = listed().filter { it.getString("id") == GameLibrary.idOf(f.name) }
        assertEquals(1, same.size)
        assertFalse(same[0].optBoolean("linked"))
    }
}
