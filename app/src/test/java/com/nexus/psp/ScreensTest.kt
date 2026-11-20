package com.nexus.psp

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.nexus.psp.data.AppState
import com.nexus.psp.emulator.CoreFiles
import com.nexus.psp.ui.KapsulaApp
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.Ov
import com.nexus.psp.ui.kit.KapsulaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.RandomAccessFile

/** Снимки экранов (телефон в альбомной ориентации 844×390 dp): чистая установка и библиотека со своими файлами. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w844dp-h390dp-land-xhdpi")
class ScreensTest {
    init { System.setProperty("roborazzi.test.record", "true") }

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    /** Пустые файлы нужного размера — как настоящие образы в папке игр. */
    private fun games(vararg names: Pair<String, Long>) {
        val dir = CoreFiles.gamesDir(ApplicationProvider.getApplicationContext())
        names.forEach { (n, mb) -> RandomAccessFile(File(dir, n), "rw").use { it.setLength(mb * 1048576) } }
    }

    private fun shot(name: String, withGames: Boolean, prep: () -> Unit) {
        if (withGames) games("Ridge_Racer_2 (Europe).iso" to 640, "Patapon 3.cso" to 410, "Lumines.iso" to 300, "Daxter (USA).cso" to 520)
        AppState.autoFetch = false
        AppState.init(ApplicationProvider.getApplicationContext())
        AppState.S.onboarded = true; AppState.S.launched = true
        Nav.closeAll(); Nav.tab = "games"
        com.nexus.psp.ui.screens.MyTab.start = 0; com.nexus.psp.data.Cloud.setFriendsForTest(null)
        prep()
        rule.setContent { KapsulaTheme { KapsulaApp() } }
        rule.mainClock.advanceTimeBy(3000)
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/$name.png")
    }

    private fun firstId() = AppState.myList().first().id

    // чистая установка — пустые состояния
    @Test fun e01_games() = shot("e01-games-empty", false) { }
    @Test fun e03_library() = shot("e03-library-empty", false) { Nav.open(Ov.Library) }
    @Test fun e04_search() = shot("e04-search-empty", false) { Nav.open(Ov.Search) }
    @Test fun e05_downloads() = shot("e05-downloads-empty", false) { Nav.open(Ov.Downloads) }
    @Test fun e06_alerts() = shot("e06-alerts-empty", false) { Nav.open(Ov.Alerts) }
    @Test fun e07_profile() = shot("e07-profile-empty", false) { Nav.go("my") }
    @Test fun e08_onboarding() = shot("e08-onboarding", false) { AppState.S.onboarded = false }

    // со своими файлами
    @Test fun g01_games() = shot("g01-games", true) { }
    @Test fun g02_game() = shot("g02-game", true) { Nav.open(Ov.Game(firstId())) }
    @Test fun g03_library() = shot("g03-library", true) { Nav.open(Ov.Library) }
    @Test fun g04_fav_empty() = shot("g04-library-fav-empty", true) { AppState.S.lf = "Избранное"; Nav.open(Ov.Library) }
    @Test fun g05_quick() = shot("g05-quick", true) { Nav.open(Ov.Quick) }
    @Test fun g06_settings_core() = shot("g06-settings-core", true) { Nav.settingsTab = "core"; Nav.open(Ov.Settings) }
    @Test fun g07_pad() = shot("g07-pad", true) { Nav.open(Ov.Pad) }
    @Test fun g08_profile() = shot("g08-profile", true) { Nav.go("my") }
    @Test fun h01_settings_general() = shot("h01-settings-general", true) { Nav.settingsTab = "general"; Nav.open(Ov.Settings) }
    @Test fun h02_settings_ctl() = shot("h02-settings-ctl", true) { Nav.settingsTab = "ctl"; Nav.open(Ov.Settings) }
    @Test fun h03_settings_dl() = shot("h03-settings-dl", true) { Nav.settingsTab = "dl"; Nav.open(Ov.Settings) }
    @Test fun h04_settings_about() = shot("h04-settings-about", true) { Nav.settingsTab = "about"; Nav.open(Ov.Settings) }
    @Test fun h05_game_settings() = shot("h05-game-settings", true) { Nav.open(Ov.GSet(firstId())) }
    @Test fun h06_pad_classic() = shot("h06-pad-classic", true) { AppState.S.ctlMode = "classic"; Nav.open(Ov.Pad) }
    // 0.33: вкладка «Управление» и гайд
    @Test fun n01_controls_tab() = shot("n01-controls-tab", true) { Nav.go("ctl") }
    @Test fun n02_controls_empty() = shot("n02-controls-empty", false) { Nav.go("ctl") }
    @Test fun n03_guide_patapon() = shot("n03-guide-game", true) { Nav.open(Ov.Guide(id = AppState.myList().first { it.t.contains("Patapon") }.id)) }
    @Test fun n04_guide_ac() = shot("n04-guide-ac", false) { Nav.open(Ov.Guide(preset = "Assassin's Creed: Bloodlines")) }
    @Test fun n05_guide_gta() = shot("n05-guide-gta", false) { Nav.open(Ov.Guide(preset = "GTA: Vice City Stories")) }
    @Config(qualifiers = "w844dp-h1500dp-xhdpi")
    @Test fun n07_controls_full() = shot("n07-controls-full", true) { Nav.go("ctl") }
    @Test fun h10_settings_ach() = shot("h10-settings-ach", true) { Nav.settingsTab = "ach"; Nav.open(Ov.Settings) }
    // «Уют»: тема «Вечер», «Ретро-меню», полка консоли, маскот в сообщении
    private fun evening() { com.nexus.psp.ui.kit.Buddy.greeted = false }
    @Test fun v01_games_evening() = shot("v01-games-evening", true) { evening() }
    @Test fun v02_game_evening() = shot("v02-game-evening", true) { evening(); Nav.open(Ov.Game(firstId())) }
    @Test fun v03_settings_evening() = shot("v03-settings-evening", true) { evening(); Nav.settingsTab = "general"; Nav.open(Ov.Settings) }
    @Test fun v04_toast_evening() = shot("v04-toast-evening", true) { evening(); com.nexus.psp.ui.kit.Buddy.greeted = true; AppState.say("Игра добавлена в библиотеку", action = "Открыть") { } }
    @Test fun v05_shelf_empty_ps1() = shot("v05-shelf-ps1", true) { evening(); AppState.S.shelf = "ps1" }
    @Test fun v06_empty_evening() = shot("v06-empty-evening", false) { evening() }
    @Test fun h09_search() = shot("h09-search", true) { Nav.open(Ov.Search) }
    /** Профиль с историей: часы, серия дней, избранное, любимая игра. */
    private fun played() {
        val ids = AppState.myList().map { it.id }
        val now = System.currentTimeMillis()
        ids.forEachIndexed { i, id -> AppState.lib[id] = com.nexus.psp.data.Lib(play = listOf(754, 190, 75, 30)[i % 4], last = listOf("Сегодня", "Вчера", "3 дня назад", "Неделю назад")[i % 4], saves = listOf(9, 4, 2, 1)[i % 4], t = now - i * 86_400_000L) }
        AppState.favs[ids[0]] = true; AppState.favs[ids[2]] = true
        val f = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US); val c = java.util.Calendar.getInstance()
        listOf(45, 80, 0, 120, 35, 60, 20).forEach { m -> AppState.history[f.format(c.time)] = m; c.add(java.util.Calendar.DAY_OF_MONTH, -1) }
        AppState.S.nick = "Виталий"; AppState.S.motto = "Пройду GTA на 100%"; AppState.S.avColor = 0; AppState.S.cardBg = 4
    }
    @Test fun j01_profile_full() = shot("j01-profile", true) { played(); Nav.go("my") }
    @Test fun j02_profile_game_bg() = shot("j02-profile-gamebg", true) { played(); AppState.S.cardBg = -1; AppState.S.avColor = 3; Nav.go("my") }
    @Test fun j03_header_clock() = shot("j03-header", true) { played() }
    @Test fun j04_ach_register() = shot("j04-ach", true) { Nav.settingsTab = "ach"; Nav.open(Ov.Settings) }
    @Test fun j05_ach_logged() = shot("j05-ach-logged", true) { AppState.S.raUser = "KapsulaTest"; AppState.S.raToken = "x"; AppState.S.raScore = "1250"; Nav.settingsTab = "ach"; Nav.open(Ov.Settings) }
    @Test fun j06_bios() = shot("j06-bios", true) { Nav.settingsTab = "bios"; Nav.open(Ov.Settings) }
    @Test fun j07_about() = shot("j07-about", true) { java.io.File(ApplicationProvider.getApplicationContext<android.content.Context>().filesDir, "crash_main.txt").writeText("KAPSULA 0.25.0 · тест\n2026-11-14 · поток main\n\njava.lang.IllegalArgumentException: тест"); Nav.settingsTab = "about"; Nav.open(Ov.Settings) }
    // «Оформление»: тема и цвет, фон, профиль; профиль с аватаром из набора, рамкой, баннером и титулом
    @Test fun j08_looks_theme() = shot("j08-looks-theme", true) { played(); Nav.open(Ov.Look("theme")) }
    @Test fun j09_looks_wall() = shot("j09-looks-wall", true) { played(); Nav.open(Ov.Look("wall")) }
    @Test fun j10_looks_profile() = shot("j10-looks-profile", true) { played(); Nav.open(Ov.Look("profile")) }
    @Test fun j11_profile_dressed() = shot("j11-profile-dressed", true) {
        played(); AppState.S.avMode = "set"; AppState.S.avItem = "av_s_knight"; AppState.S.frame = "fr_bronze"; AppState.S.banner = "bn_city"; AppState.S.titleSel = "tt_night"
        Nav.go("my")
    }
    @Test fun j13_profile_creator() = shot("j13-profile-creator", true) {
        played(); com.nexus.psp.data.Cloud.setForTest("KAPSULA", true, Triple(2.0, "Выходные ×2", System.currentTimeMillis() + 3_600_000),
            org.json.JSONObject().put("status", "Делаю KAPSULA").put("name", "gold"))
        AppState.S.titleSel = "tt_creator"; AppState.S.frame = "fr_creator"; AppState.S.banner = "bn_creator"
        AppState.S.avMode = "set"; AppState.S.avItem = "av_s_king"; Nav.go("my")
    }
    /** Баннер создателя глазами гостя: кнопка «Поприветствовать», Сейвик, «Играет сейчас». */
    @Test fun j15_creator_guest() {
        AppState.autoFetch = false
        AppState.init(ApplicationProvider.getApplicationContext())
        com.nexus.psp.data.Cloud.setForTest("7Q4M2K", false)
        val pl = org.json.JSONObject().put("t", "Patapon 3").put("p", "psp").put("live", true).put("since", java.time.OffsetDateTime.now().minusMinutes(42).toString())
        rule.setContent { KapsulaTheme { androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize().padding(16.dp)) {
            com.nexus.psp.ui.screens.CreatorBanner(com.nexus.psp.ui.screens.CreatorCard("Виталий", "", "Делаю KAPSULA", "gold", "KAPSULA", 12, null, "", "bn_creator",
                { s -> com.nexus.psp.ui.kit.Mascot("head", s) }, pl, cheers = 128, own = false))
        } } }
        rule.mainClock.advanceTimeBy(3000); rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/j15-creator-guest.png")
    }
    /** Подсказка медали выезжает из самой медали (тап по правой). */
    @Test fun j18_medal_tip() {
        AppState.autoFetch = false
        AppState.init(ApplicationProvider.getApplicationContext())
        com.nexus.psp.data.Cloud.setForTest("7Q4M2K", false)
        rule.setContent { KapsulaTheme { androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize().padding(16.dp)) {
            com.nexus.psp.ui.screens.CreatorBanner(com.nexus.psp.ui.screens.CreatorCard("Виталий", "", "Делаю KAPSULA", "gold", "KAPSULA", 12, null, "", "bn_creator",
                { s -> com.nexus.psp.ui.kit.Mascot("head", s) }, null, cheers = 5, own = false))
        } } }
        rule.mainClock.advanceTimeBy(1000); rule.waitForIdle()
        rule.onRoot().performTouchInput { val d = density; click(androidx.compose.ui.geometry.Offset(width - (16 + 14 + 23) * d, (16 + 8 + 23) * d)) }
        rule.mainClock.advanceTimeBy(400); rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/j18-medal-tip.png")
    }
    /** Вкладка «Друзья»: заявка, друзья (один играет сейчас, создатель), исходящая. */
    @Test fun j16_friends() = shot("j16-friends", true) { j16Data(); com.nexus.psp.ui.screens.MyTab.start = 1; Nav.go("my") }
    private fun j16Data() {
        com.nexus.psp.data.Cloud.setForTest("7Q4M2K", false)
        fun card(id: String, nick: String, lvl: Int, admin: Boolean = false, play: org.json.JSONObject? = null) = org.json.JSONObject()
            .put("short_id", id).put("nick", nick).put("level", lvl).put("is_admin", admin).put("avColor", lvl % 8).apply { play?.let { put("playing", it) } }
        val now = java.time.OffsetDateTime.now()
        com.nexus.psp.data.Cloud.setFriendsForTest(org.json.JSONObject()
            .put("incoming", org.json.JSONArray().put(card("H8R2NQ", "Лиса_Pro", 7)))
            .put("friends", org.json.JSONArray()
                .put(card("KAPSULA", "Виталий", 12, true, org.json.JSONObject().put("t", "Patapon 3").put("p", "psp").put("live", true).put("since", now.minusMinutes(42).toString())))
                .put(card("Z3WQ7P", "Gamer.Max", 4, false, org.json.JSONObject().put("t", "Crash Bandicoot").put("p", "ps1").put("live", false).put("at", now.minusHours(3).toString())))
                .put(card("M5T9KD", "Котик 2000", 2)))
            .put("outgoing", org.json.JSONArray().put(card("Q7PL3X", "Neo_X", 9))))
    }
    @Test fun j17_friends_list() = shot("j17-friends-list", true) {
        j16Data(); val f = com.nexus.psp.data.Cloud.friends!!; f.put("incoming", org.json.JSONArray())
        com.nexus.psp.ui.screens.MyTab.start = 1; Nav.go("my")
    }
    @Test fun j14_profile_player_id() = shot("j14-profile-id", true) { played(); com.nexus.psp.data.Cloud.setForTest("7Q4M2K", false); Nav.go("my") }
    @Test fun j12_home_wallpaper() = shot("j12-home-wall", true) { played(); AppState.S.wall = "wp_aurora"; AppState.S.accent = "#E8836B"; AppState.wallChanged() }
    /** Игра PS1: CUE + две дорожки BIN. */
    private fun ps1Game() {
        val dir = CoreFiles.gamesDir(ApplicationProvider.getApplicationContext())
        RandomAccessFile(File(dir, "Spyro the Dragon (USA) (Track 1).bin"), "rw").use { it.setLength(2352L * 300) }
        RandomAccessFile(File(dir, "Spyro the Dragon (USA) (Track 2).bin"), "rw").use { it.setLength(2352L * 100) }
        File(dir, "Spyro the Dragon (USA).cue").writeText("FILE \"Spyro the Dragon (USA) (Track 1).bin\" BINARY\n  TRACK 01 MODE2/2352\nFILE \"Spyro the Dragon (USA) (Track 2).bin\" BINARY\n  TRACK 02 AUDIO\n")
    }
    @Test fun j09_ps1_settings() = shot("j09-ps1-settings", true) { Nav.settingsTab = "ps1"; Nav.open(Ov.Settings) }
    @Test fun j10_ps1_library() { ps1Game(); shot("j10-ps1-library", true) { Nav.open(Ov.Library) } }
    @Test fun j11_ps1_game() { ps1Game(); shot("j11-ps1-game", true) { Nav.open(Ov.Game(AppState.byId.values.first { it.p == "ps1" }.id)) } }
    @Test fun j12_ps1_gset() { ps1Game(); shot("j12-ps1-gset", true) { val id = AppState.byId.values.first { it.p == "ps1" }.id; com.nexus.psp.data.GameConfig(ApplicationProvider.getApplicationContext(), id).edit { o -> o.put("own", true) }; Nav.open(Ov.GSet(id)) } }
    @Test fun m01_consoles() { ps1Game(); shot("m01-consoles", true) { Nav.open(Ov.Consoles) } }
    @Test fun m02_console_ps1() { ps1Game(); shot("m02-console-ps1", true) { Nav.open(Ov.Console("ps1")) } }
    @Test fun m03_console_empty() = shot("m03-console-neogeo", true) { Nav.open(Ov.Console("neogeo")) }
    @Test fun m04_main_others() { ps1Game(); shot("m04-main-others", true) { } }
    @Test fun m05_files_noaccess() = shot("m05-files", true) { Nav.open(Ov.Files("games")) }
    @Test fun m06_scan_noaccess() = shot("m06-scan", true) { Nav.open(Ov.Scan) }
    @Test fun m07_my_full() { shot("m07-my", true) { played(); Nav.go("my") } }
}
