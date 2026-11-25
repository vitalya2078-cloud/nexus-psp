package com.nexus.psp

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
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

/**
 * Снимки для сайта и 4PDA (build/site-shots): выдуманная библиотека (без названий и обложек настоящих игр),
 * обжитой профиль, без всплывающих сообщений. Телефон альбомно 844×390 dp.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w844dp-h390dp-land-xhdpi")
class SiteShotsTest {
    init { System.setProperty("roborazzi.test.record", "true") }

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun file(name: String, mb: Long, head: ByteArray = ByteArray(0)) {
        val dir = CoreFiles.gamesDir(ApplicationProvider.getApplicationContext())
        RandomAccessFile(File(dir, name), "rw").use { it.setLength(mb * 1048576); it.seek(0); it.write(head) }
    }

    private fun shot(name: String, prep: () -> Unit) {
        file("Neon Drift.iso", 720); file("Ember Knights.cso", 410); file("Star Forge.iso", 530); file("Tide Runners.cso", 300)
        file("Pixel Hearts.nes", 1, "NES\u001A".toByteArray()); file("Moon Courier.md", 2)
        AppState.autoFetch = false
        AppState.init(ApplicationProvider.getApplicationContext())
        AppState.S.onboarded = true; AppState.S.launched = true
        val ids = AppState.myList().map { it.id }
        val now = System.currentTimeMillis()
        ids.forEachIndexed { i, id ->
            AppState.lib[id] = com.nexus.psp.data.Lib(play = listOf(754, 190, 75, 30, 12, 0)[i % 6], last = listOf("Сегодня", "Вчера", "3 дня назад", "Неделю назад", "2 недели назад", "Ещё не запускалась")[i % 6],
                saves = listOf(9, 4, 2, 1, 1, 0)[i % 6], t = now - i * 86_400_000L)
        }
        AppState.favs[ids[0]] = true
        val f = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US); val c = java.util.Calendar.getInstance()
        listOf(45, 80, 0, 120, 35, 60, 20).forEach { m -> AppState.history[f.format(c.time)] = m; c.add(java.util.Calendar.DAY_OF_MONTH, -1) }
        AppState.S.nick = "Сейвик"; AppState.S.motto = "Каждый вечер — одна игра"; AppState.S.avColor = 3; AppState.S.cardBg = 4
        AppState.toast = null
        Nav.closeAll(); Nav.tab = "games"
        com.nexus.psp.ui.screens.MyTab.start = 0; com.nexus.psp.data.Cloud.setFriendsForTest(null)
        com.nexus.psp.ui.kit.Buddy.greeted = true
        prep()
        rule.setContent { KapsulaTheme { KapsulaApp() } }
        rule.mainClock.advanceTimeBy(3000)
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/site-shots/$name.png")
    }

    private fun first() = AppState.myList().first().id

    @Test fun s1_home() = shot("1-home") { com.nexus.psp.ui.kit.Buddy.greeted = false }
    @Test fun s2_game() = shot("2-game") { Nav.open(Ov.Game(first())) }
    @Test fun s3_controls() = shot("3-controls") { Nav.go("ctl") }
    @Test fun s4_profile() = shot("4-profile") { Nav.go("my") }
    @Test fun s5_looks() = shot("5-looks") { Nav.open(Ov.Look("profile")) }
    @Test fun s6_consoles() = shot("6-consoles") { Nav.open(Ov.Consoles) }
}
