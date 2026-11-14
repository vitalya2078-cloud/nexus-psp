package com.nexus.psp

import androidx.test.core.app.ApplicationProvider
import com.nexus.psp.data.StoreCatalog
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

/** Магазин: каталог читается, игра-папка видна в библиотеке, сохранения разных игр не смешиваются. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StoreTest {
    @Test fun parsesCatalog() {
        StoreCatalog.parse(SAMPLE)
        assertEquals(2, StoreCatalog.games.size)
        assertEquals("Экшен", StoreCatalog.cats["action"])
        assertEquals("bg3/EBOOT.PBP", StoreCatalog.games[0].eboot)
    }

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

    companion object {
        val SAMPLE = """{"version":1,"categories":{"action":"Экшен","demo":"Демо-сцена"},"games":[
            {"id":"bg3","name":"Battlegrounds 3","author":"wickles, xfacter","category":"action","plat":"psp","license":"GPL-3.0","source":"https://github.com/xfacter/battlegrounds3","desc":"Сражения на ховер-танках.","url":"https://example/bg3.zip","size":2789524,"unpacked":3797924,"sha256":"x","eboot":"bg3/EBOOT.PBP"},
            {"id":"Yellow_Rose","name":"Yellow Rose of Texas","author":"Fit & Bandwagon","category":"demo","plat":"psp","license":"Демо-сцена","desc":"Демо 4 КБ.","url":"https://example/yr.zip","size":199642,"unpacked":300000,"sha256":"y","eboot":"Yellow_Rose/EBOOT.PBP"}]}"""
    }
}
