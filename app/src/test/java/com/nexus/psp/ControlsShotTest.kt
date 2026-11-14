package com.nexus.psp

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.nexus.psp.emulator.ControlLayout
import com.nexus.psp.emulator.ControlsView
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Снимки экранных кнопок: «Классика» для PSP / PS1 / PS2 и KAPSULA Touch со своими значками и турбо. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w844dp-h390dp-land-xhdpi")
class ControlsShotTest {
    private fun shot(name: String, setup: (ControlsView) -> Unit) {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val v = ControlsView(ctx, object : ControlsView.Listener {
            override fun key(code: Int, down: Boolean) {}; override fun stick(right: Boolean, x: Float, y: Float) {}; override fun action(act: String) {}
        })
        val w = 1688; val h = 780
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        v.layout(0, 0, w, h)
        setup(v)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        // условная сцена игры, чтобы проверить читаемость
        c.drawPaint(Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), intArrayOf(0xFF2A3550.toInt(), 0xFF7A5A6A.toInt(), 0xFFD79A6A.toInt()), null, Shader.TileMode.CLAMP) })
        c.saveLayerAlpha(null, 230)
        v.draw(c)
        c.restore()
        File("build/shots").mkdirs()
        File("build/shots/$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun classicPerConsole() {
        for (p in listOf("psp", "ps1", "ps2")) shot("c_classic_$p") { v ->
            v.style = "classic"; v.platform = p; v.layName = "classic:$p"; v.setLayout(ControlLayout.layout("classic", p))
        }
    }

    @Test fun touchWithIcons() {
        shot("c_touch_default") { v -> v.layName = "touch:psp"; v.setLayout(ControlLayout.layout("touch", "psp")) }
        shot("c_touch_icons") { v ->
            v.layName = "touch:psp"
            v.customIcons["touch:psp:ft"] = "grab"; v.customIcons["touch:psp:fr"] = "attack"; v.customIcons["touch:psp:fb"] = "jump"; v.customIcons["touch:psp:fl"] = "sprint"
            v.customIcons["touch:psp:l1"] = "block"; v.customIcons["touch:psp:r1"] = "lock"
            v.turbo.add("touch:psp:fr")
            v.setLayout(ControlLayout.layout("touch", "psp") + ControlLayout.extra("x1", listOf("l1", "r1"), false, null) +
                ControlLayout.extra("x2", listOf("fb", "fb", "fr"), true, null).copy(pos = "r330 b60"))
        }
        shot("c_touch_drive") { v -> v.layName = "drive:psp"; v.setLayout(ControlLayout.preset("drive", true)) }
        // все шаблоны жанров
        com.nexus.psp.emulator.Templates.BASES.forEach { (b, _) -> shot("t_$b") { v -> v.layName = "$b:psp"; v.setLayout(com.nexus.psp.emulator.Templates.build(b, "psp")) } }
    }
}
