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

/** Адаптивность: те же экраны на w740dp-h360dp. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w740dp-h360dp-land-xhdpi")
class NarrowScreensTest {
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

    @Test fun a1() = shot("n1-games", true) { }
    @Test fun a2() = shot("n2-game", true) { Nav.open(Ov.Game(firstId())) }
    @Test fun a3() = shot("n3-pad", true) { Nav.open(Ov.Pad) }
    @Test fun a4() = shot("n4-core", true) { Nav.settingsTab = "core"; Nav.open(Ov.Settings) }
    @Test fun a5() = shot("n5-quick", true) { Nav.open(Ov.Quick) }
    @Test fun a6() = shot("n6-onboarding", false) { AppState.S.onboarded = false }
    @Test fun a7() = shot("n7-library", true) { Nav.open(Ov.Library) }
}
