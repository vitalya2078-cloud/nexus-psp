package com.nexus.psp

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.nexus.psp.emulator.GameActivity
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import android.os.Looper

/** Меню паузы поверх игры: без ядра (файл игры не задан), состояние задаётся вручную. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w844dp-h390dp-land-xhdpi")
class GameMenuTest {
    init { System.setProperty("roborazzi.test.record", "true"); org.robolectric.shadows.ShadowLog.stream = System.out }
    private fun open(): Pair<GameActivity, org.robolectric.android.controller.ActivityController<GameActivity>> {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val boot = JSONObject().put("id", "t").put("name", "Soulcalibur — Broken Destiny").put("plat", "psp").put("path", "")
            .put("s", JSONObject().put("ctlMode", "touch").put("op", 55).put("sc", 100)).put("vars", JSONObject()).put("own", true).put("preset", "balance")
        val i = Intent(ctx, GameActivity::class.java).putExtra(GameActivity.EXTRA_BOOT, boot.toString())
        val c = Robolectric.buildActivity(GameActivity::class.java, i).setup()
        return c.get() to c
    }

    private fun shot(a: GameActivity, name: String) {
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(700))
        a.window.decorView.captureRoboImage("build/shots/$name.png")
    }

    @Test fun pauseMenuAndSettings() {
        val (a, c) = open()
        val ui = a.ui
        ui.phase = "ready"; ui.paused = true; ui.canUndo = true
        ui.slots = listOf(
            GameActivity.Slot(0, true, System.currentTimeMillis() - 5 * 60000, null),
            GameActivity.Slot(1, true, System.currentTimeMillis() - 26 * 3600000L, null),
            GameActivity.Slot(2, false, 0, null), GameActivity.Slot(3, false, 0, null),
        )
        shot(a, "p01_pause")
        ui.settings = true
        shot(a, "p02_settings_quality")
        ui.restartNeeded = true; ui.scopeGame = false
        shot(a, "p03_settings_restart")
        ui.settings = false; ui.editing = true
        shot(a, "p04_editing")
        ui.picking = "touch:psp:fr"; ui.pickingDefault = "fr"
        shot(a, "p05_icon_picker")
        ui.picking = null; ui.adding = true
        shot(a, "p06_add_button")
        ui.adding = false; ui.presets = true
        shot(a, "p07_presets")
        ui.presets = false; ui.genreAsk = true
        shot(a, "p08_genre_ask")
        ui.genreAsk = false; ui.editing = false; ui.cheatState = "nodb"; ui.cheatsOpen = true
        shot(a, "p09_cheats")
        ui.cheatsOpen = false; ui.netOpen = true
        shot(a, "p10_net")
        ui.netOpen = false; ui.texOpen = true
        shot(a, "p11_textures")
        ui.texOpen = false; ui.settings = false
        shot(a, "p12_pause_links")
        c.pause().stop().destroy()
    }
}
