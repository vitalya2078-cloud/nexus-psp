package com.nexus.psp

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
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
        prep()
        rule.setContent { KapsulaTheme { KapsulaApp() } }
        rule.mainClock.advanceTimeBy(3000)
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/$name.png")
    }

    private fun firstId() = AppState.myList().first().id

    // чистая установка — пустые состояния
    @Test fun e01_games() = shot("e01-games-empty", false) { }
    @Test fun e02_store() = shot("e02-store", false) { Nav.tab = "store" }
    @Test fun e03_library() = shot("e03-library-empty", false) { Nav.open(Ov.Library) }
    @Test fun e04_search() = shot("e04-search-empty", false) { Nav.open(Ov.Search) }
    @Test fun e05_downloads() = shot("e05-downloads-empty", false) { Nav.open(Ov.Downloads) }
    @Test fun e06_alerts() = shot("e06-alerts-empty", false) { Nav.open(Ov.Alerts) }
    @Test fun e07_profile() = shot("e07-profile-empty", false) { Nav.open(Ov.Profile) }
    @Test fun e08_onboarding() = shot("e08-onboarding", false) { AppState.S.onboarded = false }

    // со своими файлами
    @Test fun g01_games() = shot("g01-games", true) { }
    @Test fun g02_game() = shot("g02-game", true) { Nav.open(Ov.Game(firstId())) }
    @Test fun g03_library() = shot("g03-library", true) { Nav.open(Ov.Library) }
    @Test fun g04_fav_empty() = shot("g04-library-fav-empty", true) { AppState.S.lf = "Избранное"; Nav.open(Ov.Library) }
    @Test fun g05_quick() = shot("g05-quick", true) { Nav.open(Ov.Quick) }
    @Test fun g06_settings_core() = shot("g06-settings-core", true) { Nav.settingsTab = "core"; Nav.open(Ov.Settings) }
    @Test fun g07_pad() = shot("g07-pad", true) { Nav.open(Ov.Pad) }
    @Test fun g08_profile() = shot("g08-profile", true) { Nav.open(Ov.Profile) }
    @Test fun h01_settings_general() = shot("h01-settings-general", true) { Nav.settingsTab = "general"; Nav.open(Ov.Settings) }
    @Test fun h02_settings_ctl() = shot("h02-settings-ctl", true) { Nav.settingsTab = "ctl"; Nav.open(Ov.Settings) }
    @Test fun h03_settings_dl() = shot("h03-settings-dl", true) { Nav.settingsTab = "dl"; Nav.open(Ov.Settings) }
    @Test fun h04_settings_about() = shot("h04-settings-about", true) { Nav.settingsTab = "about"; Nav.open(Ov.Settings) }
    @Test fun h05_game_settings() = shot("h05-game-settings", true) { Nav.open(Ov.GSet(firstId())) }
    @Test fun h06_pad_classic() = shot("h06-pad-classic", true) { AppState.S.ctlMode = "classic"; Nav.open(Ov.Pad) }
    @Test fun h07_games_light() = shot("h07-games-light", true) { AppState.S.theme = "light"; AppState.applyLook() }
    @Test fun h08_game_light() = shot("h08-game-light", true) { AppState.S.theme = "light"; AppState.applyLook(); Nav.open(Ov.Game(firstId())) }
    @Test fun h10_settings_ach() = shot("h10-settings-ach", true) { Nav.settingsTab = "ach"; Nav.open(Ov.Settings) }
    @Test fun h09_search() = shot("h09-search", true) { Nav.open(Ov.Search) }
    @Test fun g09_quick_light() = shot("g09-quick-light", true) { AppState.S.theme = "light"; AppState.applyLook(); Nav.open(Ov.Quick) }
}
