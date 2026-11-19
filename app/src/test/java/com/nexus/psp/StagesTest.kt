package com.nexus.psp

import com.nexus.psp.data.AppState
import com.nexus.psp.data.CloudSaves
import com.nexus.psp.data.CloudSaves.Plan
import com.nexus.psp.data.GameConfig
import com.nexus.psp.data.Invite
import com.nexus.psp.data.Telemetry
import com.nexus.psp.emulator.CoreFiles
import com.nexus.psp.ui.screens.challengeProgress
import com.nexus.psp.ui.screens.weekLeft
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
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

/** 0.46: статистика, облачные сохранения, испытания недели, «Позвать в игру». */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StagesTest {
    private val ctx = RuntimeEnvironment.getApplication()

    // ---------- облачные сохранения: что делать с сейвом ----------
    @Test fun cloudPlan() {
        assertEquals(Plan.NONE, CloudSaves.plan(null, null, null))
        assertEquals(Plan.NONE, CloudSaves.plan("a", "a", null))
        assertEquals(Plan.UPLOAD, CloudSaves.plan("a", null, null))       // в облаке нет
        assertEquals(Plan.DOWNLOAD, CloudSaves.plan(null, "a", null))     // новый телефон
        assertEquals(Plan.UPLOAD, CloudSaves.plan("b", "a", "a"))         // играл на этом телефоне
        assertEquals(Plan.DOWNLOAD, CloudSaves.plan("a", "b", "a"))       // играл на другом
        assertEquals(Plan.ASK, CloudSaves.plan("b", "c", "a"))            // и там и там — спросить
        assertEquals(Plan.ASK, CloudSaves.plan("b", "c", null))           // первая сверка, разные — спросить
    }

    @Test fun srmRoundTripAndStableKey() {
        AppState.init(ctx)
        val f = File(ctx.filesDir, "Some Game.nes").apply { writeBytes(ByteArray(64)) }
        val g = AppState.addImported(JSONObject().put("id", "g_nes").put("name", "Some Game").put("file", f.name).put("path", f.path).put("size", 0.1).put("plat", "nes"))
        val key = CloudSaves.keyOf(g)!!
        assertTrue(key.matches(Regex("^[a-z0-9_]{3,64}$")))
        // ключ зависит от имени файла, а не от папки — на другом телефоне тот же
        val other = g.copy(path = "/storage/emulated/0/Other/Some Game.nes")
        assertEquals(key, CloudSaves.keyOf(other))
        assertNull(CloudSaves.local(ctx, g))
        val data = ByteArray(8192) { (it % 251).toByte() }
        assertTrue(CloudSaves.write(ctx, g, data))
        assertArrayEquals(data, CloudSaves.local(ctx, g))
    }

    @Test fun pspZipIsStableAndSafe() {
        AppState.init(ctx)
        val f = File(ctx.filesDir, "Tekken.iso").apply { writeBytes(ByteArray(64)) }
        val g = AppState.addImported(JSONObject().put("id", "g_psp").put("name", "Tekken").put("file", f.name).put("path", f.path).put("size", 1.0).put("plat", "psp"))
        AppState.meta["g_psp"] = com.nexus.psp.data.Meta(serial = "ULUS-10041")
        assertEquals("psp_ulus10041", CloudSaves.keyOf(g))
        val sd = File(CoreFiles.savesDir(ctx), "PSP/SAVEDATA").apply { deleteRecursively() }
        File(sd, "ULUS10041DATA00").mkdirs(); File(sd, "ULUS10041DATA00/DATA.BIN").writeText("save-1")
        File(sd, "ULUS99999OTHER").mkdirs(); File(sd, "ULUS99999OTHER/DATA.BIN").writeText("other game")
        val a = CloudSaves.local(ctx, g)!!
        Thread.sleep(1100); File(sd, "ULUS10041DATA00/DATA.BIN").setLastModified(System.currentTimeMillis())
        assertEquals("одинаковые файлы — одинаковый архив", CloudSaves.sha(a), CloudSaves.sha(CloudSaves.local(ctx, g)!!))
        // восстановление: только папки этой игры, чужая не тронута
        File(sd, "ULUS10041DATA00").deleteRecursively()
        assertTrue(CloudSaves.write(ctx, g, a))
        assertEquals("save-1", File(sd, "ULUS10041DATA00/DATA.BIN").readText())
        assertEquals("other game", File(sd, "ULUS99999OTHER/DATA.BIN").readText())
        // архив с выходом за папку не пишет ничего наружу
        val evil = java.io.ByteArrayOutputStream().also { bo ->
            java.util.zip.ZipOutputStream(bo).use { z -> z.putNextEntry(java.util.zip.ZipEntry("ULUS10041/../../../evil.txt")); z.write(1); z.closeEntry() }
        }.toByteArray()
        CloudSaves.write(ctx, g, evil)
        assertFalse(File(CoreFiles.savesDir(ctx), "evil.txt").exists())
        assertFalse(File(ctx.filesDir, "evil.txt").exists())
    }

    // ---------- статистика ----------
    @Test fun telemetryCountsAndRespectsSwitch() {
        AppState.init(ctx); Telemetry.init(ctx); Telemetry.clear()
        AppState.S.stats = true
        Telemetry.count("game_start", "psp"); Telemetry.count("game_start", "psp"); Telemetry.count("app_open")
        val p = Telemetry.pending()
        val m = (0 until p.length()).associate { p.getJSONObject(it).let { o -> o.getString("kind") + "|" + o.getString("k") to o.getInt("n") } }
        assertEquals(2, m["game_start|psp"]); assertEquals(1, m["app_open|"])
        AppState.S.stats = false
        Telemetry.count("app_open")
        assertEquals(1, (0 until Telemetry.pending().length()).map { Telemetry.pending().getJSONObject(it) }.first { it.getString("kind") == "app_open" }.getInt("n"))
        assertFalse(Telemetry.due())
        AppState.S.stats = true
        assertTrue(Telemetry.due())
        Telemetry.sent(ok = true)
        assertEquals(0, Telemetry.pending().length())
        assertFalse("10 минут не прошло", Telemetry.due())
    }

    @Test fun telemetryCodes() {
        assertEquals("0-5", Telemetry.bucket(0)); assertEquals("5-30", Telemetry.bucket(5)); assertEquals("30-120", Telemetry.bucket(119)); assertEquals("120+", Telemetry.bucket(500))
        assertEquals("format", Telemetry.failCode(null))
        assertEquals("text", Telemetry.failCode("Это текстовый файл, а не образ игры"))
        assertEquals("small", Telemetry.failCode("Файл слишком маленький для игры"))
        assertEquals("unread", Telemetry.failCode("Нет доступа к файлу — проверим при запуске"))
    }

    // ---------- испытания ----------
    @Test fun challengeTexts() {
        assertEquals("3 ч 20 мин из 5 ч", challengeProgress(JSONObject().put("kind", "play_min").put("progress", 200).put("target", 300)))
        assertEquals("2 из 4 дн.", challengeProgress(JSONObject().put("kind", "days").put("progress", 2).put("target", 4)))
        assertEquals("1 из 2", challengeProgress(JSONObject().put("kind", "consoles").put("progress", 1).put("target", 2)))
        val now = java.time.OffsetDateTime.parse("2026-11-19T12:00:00+03:00").toInstant().toEpochMilli()
        assertEquals("ещё 4 дн.", weekLeft("2026-11-23T21:00:00+00:00", now))
        assertEquals("ещё 5 ч", weekLeft("2026-11-19T14:00:00+00:00", now))
        assertNull(weekLeft("мусор", now))
    }

    // ---------- «Позвать в игру» ----------
    @Test fun inviteFindsGameAndSetsNet() {
        AppState.init(ctx)
        val f = File(ctx.filesDir, "Monster Hunter.iso").apply { writeBytes(ByteArray(64)) }
        val g = AppState.addImported(JSONObject().put("id", "g_mh").put("name", "Monster Hunter").put("file", f.name).put("path", f.path).put("size", 1.0).put("plat", "psp"))
        assertTrue(Invite.supported(g))
        assertNotNull(Invite.find("MONSTER HUNTER", "psp"))
        assertNull("другая консоль", Invite.find("Monster Hunter", "ps1"))
        assertNull(Invite.find("Tekken", "psp"))
        Invite.setNet(ctx, g.id, "join", "192.168.1.5")
        val net = GameConfig(ctx, g.id).read().getJSONObject("gx").getJSONObject("net")
        assertEquals("join", net.getString("mode")); assertEquals("192.168.1.5", net.getString("ip"))
        // после игры по приглашению сеть выключается
        Invite.reset(ctx, g.id)
        assertEquals("off", GameConfig(ctx, g.id).read().getJSONObject("gx").getJSONObject("net").getString("mode"))
        val tag = Invite.tag(JSONObject().put("t", "Monster Hunter").put("p", "psp").put("ip", "10.0.0.2").put("from", "ABC123"))
        assertTrue(tag.startsWith(Invite.PREFIX)); assertFalse("ID отправителя в метке не нужен", tag.contains("ABC123"))
    }
}
