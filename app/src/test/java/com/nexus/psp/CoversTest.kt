package com.nexus.psp

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.nexus.psp.data.AppState
import com.nexus.psp.data.Covers
import com.nexus.psp.data.realGame
import com.nexus.psp.emulator.CoreFiles
import com.nexus.psp.emulator.Disc
import com.nexus.psp.ui.KapsulaApp
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.Ov
import com.nexus.psp.ui.kit.KapsulaTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Обложки: чтение образа ISO/CSO и цепочка «серийный номер → база libretro → обложка» (с настоящей сетью). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w844dp-h390dp-land-xhdpi")
class CoversTest {
    init { System.setProperty("roborazzi.test.record", "true") }
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun res(n: String) = File(javaClass.classLoader!!.getResource(n)!!.toURI())

    @Test fun readsIsoAndCso() {
        for (n in listOf("Soulcalibur (Europe).iso", "Soulcalibur (Europe).cso")) {
            val i = Disc.read(res(n))
            assertNotNull(n, i)
            assertEquals(n, "ULES-01298", i!!.serial)
            assertEquals(n, "SOULCALIBUR Broken Destiny", i.title)
            assertTrue(n, (i.icon?.size ?: 0) > 0 && (i.pic?.size ?: 0) > 0)
        }
    }

    @Test fun findsCoverOnline() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val f = File(CoreFiles.gamesDir(ctx), "game1.cso"); res("Soulcalibur (Europe).cso").copyTo(f, true)
        val g = realGame("f_game1_cso", "game1", f.name, f.path, 1.0, "psp")
        val m = Covers.process(ctx, g, online = true)
        println("META ${m.serial} | ${m.title} | cover=${m.cover?.length()} pic=${m.pic?.length()}")
        assertEquals("ULES-01298", m.serial)
        assertEquals("Soulcalibur — Broken Destiny", m.title)
        assertNotNull(m.cover); assertNotNull(m.pic)
        // снимок: библиотека и карточка с найденной обложкой
        AppState.autoFetch = false
        AppState.init(ctx); AppState.S.onboarded = true; AppState.S.launched = true
        Nav.closeAll(); Nav.tab = "games"; Nav.open(Ov.Game(g.id))
        rule.setContent { KapsulaTheme { KapsulaApp() } }
        rule.mainClock.advanceTimeBy(3000); rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/c01-cover-sheet.png")
        Nav.closeAll(); rule.waitForIdle()
        rule.onRoot().captureRoboImage("build/shots/c02-cover-games.png")
    }
}
