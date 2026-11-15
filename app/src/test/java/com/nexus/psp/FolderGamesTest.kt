package com.nexus.psp

import androidx.test.core.app.ApplicationProvider
import com.nexus.psp.emulator.CoreFiles
import com.nexus.psp.emulator.GameLibrary
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Игры-папки (EBOOT.PBP с данными рядом): видны в библиотеке, сохранения у каждой свои, удаляются целиком. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FolderGamesTest {
    @Test fun folderGameInLibrary() {
        val c = ApplicationProvider.getApplicationContext<android.content.Context>()
        val root = CoreFiles.gamesDir(c); root.listFiles()?.forEach { it.deleteRecursively() }
        for (id in listOf("bg3", "webfest")) {
            val d = File(root, "$id/$id").apply { mkdirs() }
            File(d, "EBOOT.PBP").writeBytes(ByteArray(4096))
            File(root, "$id/${GameLibrary.MARK}").writeText(JSONObject().put("store", id).put("name", id.uppercase()).put("eboot", "$id/EBOOT.PBP").put("plat", "psp").toString())
        }
        val a = JSONArray(GameLibrary.list(c))
        assertEquals(2, a.length())
        val paths = (0 until a.length()).map { a.getJSONObject(it).getString("path") }
        // сохранения у каждой игры свои, хотя файлы называются одинаково
        assertEquals(setOf("dir_bg3", "dir_webfest"), paths.map { GameLibrary.key(it) }.toSet())
        assertTrue(GameLibrary.delete(c, paths[0]))
        assertEquals(1, JSONArray(GameLibrary.list(c)).length())
    }

}
