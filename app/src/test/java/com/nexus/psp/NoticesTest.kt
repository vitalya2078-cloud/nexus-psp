package com.nexus.psp

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureScreenRoboImage
import com.nexus.psp.data.AppState
import com.nexus.psp.ui.KapsulaApp
import com.nexus.psp.ui.kit.KapsulaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * 0.50.1: окна после обновления не должны запирать игрока на низком альбомном экране —
 * кнопки видны и нажимаются, «Назад» закрывает. (В 0.50.0 «Отлично!» уезжала за край экрана.)
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w780dp-h340dp-land-xhdpi")
class NoticesTest {
    init { System.setProperty("roborazzi.test.record", "true") }
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun whatsNewThenStatsCanBeClosed() {
        AppState.autoFetch = false
        AppState.init(ApplicationProvider.getApplicationContext())
        AppState.S.onboarded = true; AppState.S.launched = true
        AppState.S.seenVer = "0.49.0"; AppState.S.statsAsked = false
        rule.setContent { KapsulaTheme { KapsulaApp() } }
        rule.mainClock.advanceTimeBy(3000); rule.waitForIdle()
        captureScreenRoboImage("build/shots/x01-whatsnew.png")
        rule.onNodeWithText("Отлично!").assertIsDisplayed().performClick()
        rule.mainClock.advanceTimeBy(500); rule.waitForIdle()
        assertEquals(BuildConfig.VERSION_NAME, AppState.S.seenVer)
        captureScreenRoboImage("build/shots/x02-stats.png")
        rule.onNodeWithText("Не сейчас").assertIsDisplayed().performClick()
        rule.mainClock.advanceTimeBy(500); rule.waitForIdle()
        assertTrue(AppState.S.statsAsked); assertFalse(AppState.S.stats)
    }
}
