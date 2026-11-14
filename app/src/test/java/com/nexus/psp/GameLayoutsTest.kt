package com.nexus.psp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.nexus.psp.emulator.ControlsView
import com.nexus.psp.emulator.GamePresets
import com.nexus.psp.emulator.Templates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Раскладки игр «как в мобильной игре»: подписи, панель «⋯», камера свайпом, автоподбор по названию. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w844dp-h390dp-land-xhdpi")
class GameLayoutsTest {
    private val keys = ArrayList<Pair<Int, Boolean>>()
    private val w = 1688; private val h = 780

    private fun view(): ControlsView {
        val v = ControlsView(ApplicationProvider.getApplicationContext(), object : ControlsView.Listener {
            override fun key(code: Int, down: Boolean) { keys += code to down }
            override fun stick(right: Boolean, x: Float, y: Float) {}; override fun action(act: String) {}
        })
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        v.layout(0, 0, w, h); return v
    }

    private fun shot(name: String, v: ControlsView) {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val c = Canvas(bmp)
        c.drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), intArrayOf(0xFF2A3550.toInt(), 0xFF7A5A6A.toInt(), 0xFFD79A6A.toInt()), null, Shader.TileMode.CLAMP) })
        v.draw(c)
        File("build/shots").mkdirs(); File("build/shots/$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun touch(v: ControlsView, action: Int, x: Float, y: Float, t: Long) =
        v.onTouchEvent(MotionEvent.obtain(0, t, action, x, y, 0))

    @Test fun presetsFoundByTitle() {
        val ac = GamePresets.find("Assassin's Creed - Bloodlines (USA)")
        assertNotNull(ac); assertEquals("g_ac", ac!!.second.getJSONArray("layouts").getJSONObject(0).getString("base"))
        assertEquals("g_mh", GamePresets.find("Monster Hunter Freedom Unite")!!.second.getJSONArray("layouts").getJSONObject(0).getString("base"))
        assertEquals("g_loco", GamePresets.find("LocoRoco 2")!!.second.getJSONArray("layouts").getJSONObject(0).getString("base"))
    }

    @Test fun allGameBasesBuild() {
        for (b in listOf("g_ac", "g_mh", "g_loco", "g_patapon", "g_khbbs", "g_dissidia", "g_wipeout", "g_megaman", "g_tekken", "g_gow", "g_musou")) {
            val l = Templates.build(b, "psp")
            assertTrue(b, l.size >= 5); assertTrue("$b: ⋯", l.any { it.act == "more" })
        }
    }

    @Test fun acShotsAndDrawer() {
        val v = view(); v.layName = "main"; v.setLayout(Templates.build("g_ac", "psp")); shot("g_ac", v)
        // с подписями — для картинок магазина
        val vc = view(); vc.captions = true; vc.layName = "main"; vc.setLayout(Templates.build("g_ac", "psp")); shot("g_ac_caps", vc)
        // открыть «⋯»
        val more = v.current().first { it.act == "more" }
        val cx = w / 2f - 46f * 2; val cy = h - 14f * 2 - 40f
        touch(v, MotionEvent.ACTION_DOWN, cx, cy, 0); touch(v, MotionEvent.ACTION_UP, cx, cy, 50)
        shot("g_ac_more", v)
        for (b in listOf("g_mh", "g_loco", "g_patapon", "g_tekken")) { val x = view(); x.layName = b; x.setLayout(Templates.build(b, "psp")); shot(b, x) }
        assertNotNull(more)
    }

    @Test fun gtaHandbrakeIsROnly() {
        val drive = Templates.build("drive", "psp")
        assertEquals("r1", drive.first { it.id == "hb" }.key)
        assertEquals(listOf("l1", "r1"), drive.first { it.id == "look" }.keys)
        assertEquals("down", drive.first { it.id == "horn" }.key)
    }

    @Test fun acOneLayoutWithRBuiltIn() {
        val l = Templates.build("g_ac", "psp")
        assertTrue(l.none { it.to != null || it.act?.startsWith("latch:") == true })
        assertEquals(listOf("r1", "fb"), l.first { it.id == "run" }.keys)
        assertEquals(listOf("r1", "fl"), l.first { it.id == "kill" }.keys)
        assertEquals(listOf("r1", "fb"), l.first { it.id == "jump" }.keys)
    }

    @Test fun cameraSwipePressesLPlusFace() {
        val v = view(); v.layName = "main"; v.setLayout(Templates.build("g_ac", "psp"))
        keys.clear()
        // пустое место правой половины, свайп вверх
        val x = w * .62f; val y = h * .45f
        touch(v, MotionEvent.ACTION_DOWN, x, y, 0)
        touch(v, MotionEvent.ACTION_MOVE, x, y - 120f, 30)
        // L — сразу, △ — через пару кадров (игра должна увидеть уже зажатую L)
        assertEquals(listOf(KeyEvent.KEYCODE_BUTTON_L1 to true), keys.toList())
        org.robolectric.shadows.ShadowLooper.idleMainLooper(100, java.util.concurrent.TimeUnit.MILLISECONDS)
        assertTrue(keys.contains(KeyEvent.KEYCODE_BUTTON_X to true))
        touch(v, MotionEvent.ACTION_UP, x, y - 120f, 60)
        assertTrue(keys.contains(KeyEvent.KEYCODE_BUTTON_L1 to false)); assertTrue(keys.contains(KeyEvent.KEYCODE_BUTTON_X to false))
    }
}
