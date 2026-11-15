package com.nexus.psp

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.nexus.psp.data.AppState
import com.nexus.psp.ui.KapsulaApp
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.kit.KapsulaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Разные телефоны: шапка и знакомство на ширинах от компактного телефона до планшета.
 * Раньше шапку проверяли на одной ширине — на другом телефоне «Управление» наехало на часы.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidthsTest {
    init { System.setProperty("roborazzi.test.record", "true") }
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun shot(name: String, onboarding: Boolean = false, quick: Boolean = false) {
        AppState.autoFetch = false
        AppState.init(ApplicationProvider.getApplicationContext())
        AppState.S.onboarded = !onboarding; AppState.S.launched = true; AppState.S.headClock = true
        Nav.closeAll(); Nav.tab = "ctl"
        if (quick) Nav.open(com.nexus.psp.ui.Ov.Quick)
        rule.setContent { KapsulaTheme { KapsulaApp() } }
        rule.mainClock.advanceTimeBy(3000); rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/w-$name.png")
    }

    @Config(sdk = [34], qualifiers = "w640dp-h320dp-land-xhdpi") @Test fun w640() = shot("640")
    @Config(sdk = [34], qualifiers = "w700dp-h330dp-land-xhdpi") @Test fun w700() = shot("700")
    @Config(sdk = [34], qualifiers = "w780dp-h360dp-land-xhdpi") @Test fun w780() = shot("780")
    @Config(sdk = [34], qualifiers = "w880dp-h400dp-land-xhdpi") @Test fun w880() = shot("880")
    @Config(sdk = [34], qualifiers = "w1100dp-h700dp-land-xhdpi") @Test fun w1100() = shot("1100")
    // быстрая панель целиком на экране (ползунки внизу видны) на низких экранах
    @Config(sdk = [34], qualifiers = "w640dp-h320dp-land-xhdpi") @Test fun quick640() = shot("quick-640", quick = true)
    @Config(sdk = [34], qualifiers = "w780dp-h360dp-land-xhdpi") @Test fun quick780() = shot("quick-780", quick = true)
    @Config(sdk = [34], qualifiers = "w880dp-h390dp-land-xhdpi") @Test fun quick880() = shot("quick-880", quick = true)
    @Config(sdk = [34], qualifiers = "w1100dp-h700dp-land-xhdpi") @Test fun quick1100() = shot("quick-1100", quick = true)
    @Config(sdk = [34], qualifiers = "w700dp-h330dp-land-xhdpi") @Test fun onb700() = shot("onb-700", onboarding = true)
    @Config(sdk = [34], qualifiers = "w880dp-h400dp-land-xhdpi") @Test fun onb880() = shot("onb-880", onboarding = true)
}
