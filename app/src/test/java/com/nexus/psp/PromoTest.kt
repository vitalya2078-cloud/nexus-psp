package com.nexus.psp

import android.graphics.BitmapFactory
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.nexus.psp.data.AppState
import com.nexus.psp.emulator.ControlLayout
import com.nexus.psp.emulator.ControlsView
import com.nexus.psp.emulator.CoreFiles
import com.nexus.psp.ui.KapsulaApp
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.Ov
import com.nexus.psp.ui.kit.KapsulaTheme
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.RandomAccessFile

/** Промо-кадры: настоящие экраны KAPSULA с вымышленными играми (обложки — plans/promo), высокое разрешение. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w844dp-h390dp-land-xxhdpi")
class PromoTest {
    init { System.setProperty("roborazzi.test.record", "true") }
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private val games = listOf(
        Triple("Last Frontier.iso", "last_frontier", 1580), Triple("Neon Drift.cso", "neon_drift", 940),
        Triple("Shadow Blade.cso", "shadow_blade", 720), Triple("Sky Raiders.iso", "sky_raiders", 610),
    )
    private val promo = File("../plans/promo")

    private fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dir = CoreFiles.gamesDir(ctx)
        games.forEach { (n, _, mb) -> RandomAccessFile(File(dir, n), "rw").use { it.setLength(mb * 1048576L) } }
        AppState.autoFetch = false
        AppState.init(ctx)
        AppState.S.onboarded = true; AppState.S.launched = true
        val played = mapOf("last_frontier" to 754, "neon_drift" to 312, "shadow_blade" to 128, "sky_raiders" to 45)
        // обложки и фон (фон — та же картинка, приложение размывает её само)
        games.reversed().forEach { (n, key, _) ->
            val g = AppState.byId.values.first { it.file == n }
            val d = File(ctx.filesDir, "meta/${g.id}").apply { mkdirs() }
            File(promo, "$key.jpg").copyTo(File(d, "cover.jpg"), true)
            File(promo, "$key.jpg").copyTo(File(d, "snap.jpg"), true)
            val c = BitmapFactory.decodeFile(File(promo, "$key.jpg").path).getPixel(256, 600)
            File(d, "meta.json").writeText(JSONObject().put("tried", 3).put("extra", 1).put("color", c).toString())
            AppState.meta[g.id] = com.nexus.psp.data.Covers.load(ctx, g.id)
            AppState.markPlayed(g.id)
            AppState.lib[g.id] = AppState.lib[g.id]!!.copy(play = played[key] ?: 0, saves = 3)
        }
        Nav.closeAll(); Nav.tab = "games"
    }

    private fun shot(name: String, prep: () -> Unit) {
        setup(); prep()
        rule.setContent { KapsulaTheme { KapsulaApp() } }
        // картинки грузятся в фоновом потоке — даём им время
        repeat(6) { rule.mainClock.advanceTimeBy(1000); Thread.sleep(300); rule.waitForIdle() }
        rule.onRoot().captureRoboImage("build/promo/$name.png")
    }

    @Test fun p1_home() = shot("home") { }
    @Test fun p2_game() = shot("game") { Nav.open(Ov.Game(AppState.myList().first().id)) }
    @Test fun p3_library() = shot("library") { Nav.open(Ov.Library) }

    /** Экранные кнопки KAPSULA Touch поверх кадра игры (сцена — обложка «Last Frontier» без надписи). */
    @Test fun p4_ingame() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val v = ControlsView(ctx, object : ControlsView.Listener {
            override fun key(code: Int, down: Boolean) {}; override fun stick(right: Boolean, x: Float, y: Float) {}; override fun action(act: String) {}
        })
        val w = 2532; val h = 1170
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
        v.layout(0, 0, w, h)
        v.layName = "foot:psp"; v.alpha = .9f
        v.setLayout(ControlLayout.preset("foot", true))
        val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        val scene = BitmapFactory.decodeFile(File(promo, "last_frontier.jpg").path)
        // кадр игры: нижние 2/3 обложки (без названия), растянутые на экран
        val src = android.graphics.Rect(0, scene.height * 30 / 100, scene.width, scene.height * 30 / 100 + scene.width * h / w)
        c.drawBitmap(scene, src, android.graphics.Rect(0, 0, w, h), android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
        c.saveLayerAlpha(null, 235); v.draw(c); c.restore()
        File("build/promo").mkdirs()
        File("build/promo/ingame.png").outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
}
