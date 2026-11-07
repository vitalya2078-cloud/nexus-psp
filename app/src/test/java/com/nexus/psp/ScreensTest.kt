package com.nexus.psp

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.nexus.psp.data.AppState
import com.nexus.psp.ui.KapsulaApp
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.Ov
import com.nexus.psp.ui.kit.KapsulaTheme
import org.junit.Rule
import androidx.compose.foundation.layout.fillMaxSize
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Снимки экранов нативного интерфейса (телефон в альбомной ориентации 844×390 dp). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w844dp-h390dp-land-xhdpi")
class ScreensTest {
    init { System.setProperty("roborazzi.test.record", "true") }

    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun shot(name: String, prep: () -> Unit) {
        AppState.init(ApplicationProvider.getApplicationContext())
        AppState.S.onboarded = true; AppState.S.launched = true
        Nav.closeAll(); Nav.tab = "games"
        prep()
        rule.setContent { KapsulaTheme { KapsulaApp() } }
        rule.mainClock.advanceTimeBy(3000)
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/$name.png")
    }

    @Test fun s01_games() = shot("01-games") { }
    @Test fun s02_store() = shot("02-store") { Nav.tab = "store" }
    @Test fun s03_game() = shot("03-game") { Nav.open(Ov.Game("drift")) }
    @Test fun s04_library() = shot("04-library") { Nav.open(Ov.Library) }
    @Test fun s05_search() = shot("05-search") { Nav.open(Ov.Search) }
    @Test fun s06_downloads() = shot("06-downloads") { Nav.open(Ov.Downloads) }
    @Test fun s07_quick() = shot("07-quick") { Nav.open(Ov.Quick) }
    @Test fun s08_profile() = shot("08-profile") { Nav.open(Ov.Profile) }
    @Test fun s09_settings_core() = shot("09-settings-core") { Nav.settingsTab = "core"; Nav.open(Ov.Settings) }
    @Test fun s10_pad() = shot("10-pad") { Nav.open(Ov.Pad) }
    @Test fun s11_alerts() = shot("11-alerts") { Nav.open(Ov.Alerts) }
    @Test fun s12_onboarding() = shot("12-onboarding") { AppState.S.onboarded = false }
    @Test fun s13_light() = shot("13-light-store") { AppState.S.theme = "light"; AppState.applyLook(); Nav.tab = "store" }
    @Test fun s14_touch() = controls("14-controls-touch", "touch", "ps2")
    @Test fun s15_classic() = controls("15-controls-classic", "classic", "psp")
    @Test fun s16_drive() = controls("16-controls-drive", "drive", "ps2")

    /** Нативный слой кнопок поверх обложки — как в игре (без редактора). */
    private fun controls(name: String, lay: String, plat: String) {
        AppState.init(ApplicationProvider.getApplicationContext())
        AppState.S.theme = "dark"; AppState.applyLook()
        val g = AppState.byId["rubezh"]!!
        rule.setContent {
            androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize()) {
                com.nexus.psp.ui.Cover(g, 1600, 740, 11, androidx.compose.ui.Modifier.fillMaxSize())
                androidx.compose.ui.viewinterop.AndroidView({ c ->
                    com.nexus.psp.emulator.ControlsView(c, object : com.nexus.psp.emulator.ControlsView.Listener {
                        override fun key(code: Int, down: Boolean) {}; override fun stick(right: Boolean, x: Float, y: Float) {}; override fun action(act: String) {}
                    }).apply {
                        alpha = .8f
                        setLayout(if (lay == "drive") com.nexus.psp.emulator.ControlLayout.preset("drive", true) else com.nexus.psp.emulator.ControlLayout.layout(lay, plat))
                    }
                }, androidx.compose.ui.Modifier.fillMaxSize())
            }
        }
        rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/$name.png")
    }
}
