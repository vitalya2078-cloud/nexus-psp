package com.nexus.psp.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.nexus.psp.data.Game
import com.nexus.psp.ui.kit.AssetImage
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Процедурные обложки — перенос генератора art() из прототипа (мотивы sun / planet / city / hills / waves / shards).
 * Рисуются один раз в Bitmap и кэшируются.
 */
object Art {
    private val cache = object : LruCache<String, ImageBitmap>(48 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    private class Rnd(seed: Int) {
        private var s: Long = (seed % 2147483647).toLong().let { if (it <= 0) 1 else it }
        fun next(): Float { s = s * 16807 % 2147483647; return ((s - 1) / 2147483646.0).toFloat() }
    }

    private fun c(v: Long, a: Float = 1f): Int {
        val rgb = (v and 0xFFFFFF).toInt()
        return ((a * 255).toInt().coerceIn(0, 255) shl 24) or rgb
    }

    fun bitmap(g: Game, w: Int, h: Int, k: Int = 0): ImageBitmap {
        val key = "${g.id}:$w:$h:$k"
        cache.get(key)?.let { return it }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        draw(Canvas(bmp), g, w.toFloat(), h.toFloat(), k)
        return bmp.asImageBitmap().also { cache.put(key, it) }
    }

    private fun draw(cv: Canvas, g: Game, w: Float, h: Float, k: Int) {
        val r = Rnd(g.seed + k * 101)
        val a = g.pal[0]; val b = g.pal[1]; val cc = g.pal[2]
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = LinearGradient(0f, 0f, 0f, h, c(a), c(b), Shader.TileMode.CLAMP)
        cv.drawRect(0f, 0f, w, h, p); p.shader = null
        repeat(18) {
            p.color = c(0xFFFFFF, r.next() * .25f + .05f)
            val x = r.next() * w; val y = r.next() * h * .65f; val rad = r.next() * 1.5f + .3f
            cv.drawCircle(x, y, rad, p)
        }
        val m = min(w, h); val cx = w * (.45f + r.next() * .3f); val hz = h * .62f
        fun glow(x: Float, y: Float, rad: Float) {
            p.shader = RadialGradient(x, y, rad, intArrayOf(c(cc, .22f), c(cc, 0f)), null, Shader.TileMode.CLAMP)
            cv.drawCircle(x, y, rad, p); p.shader = null
        }
        val st = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f }
        when (g.m) {
            "sun" -> {
                glow(cx, hz, m * .7f)
                p.color = c(cc); cv.drawCircle(cx, hz, m * .28f, p)
                p.color = c(a)
                for (i in 0 until 6) cv.drawRect(0f, hz - m * .05f + i * m * .05f, w, hz - m * .05f + i * m * .05f + 2 + i * 1.5f, p)
                p.color = c(b); cv.drawRect(0f, hz, w, h, p)
                st.color = c(cc, .5f)
                for (i in -14..14) cv.drawLine(cx, hz, cx + i * w * .1f, h, st)
                st.color = c(cc, .4f)
                for (i in 1 until 9) { val y = hz + (i / 9f).pow(2) * (h - hz); cv.drawLine(0f, y, w, y, st) }
            }
            "planet" -> {
                val pr = m * .26f; val py = h * .42f
                glow(cx, py, pr * 2.4f)
                p.color = c(cc); cv.drawCircle(cx, py, pr, p)
                p.color = c(a, .4f); cv.drawCircle(cx - pr * .35f, py - pr * .3f, pr * .85f, p)
                st.color = c(cc); st.strokeWidth = 3f
                cv.save(); cv.rotate(-15f, cx, py)
                cv.drawOval(RectF(cx - pr * 1.9f, py - pr * .42f, cx + pr * 1.9f, py + pr * .42f), st)
                cv.restore()
            }
            "city" -> {
                glow(cx, h * .3f, m * .55f)
                var x = 0f
                while (x < w) {
                    val bw = w * (.05f + r.next() * .09f); val bh = h * (.2f + r.next() * .45f)
                    p.color = c(b); cv.drawRect(x, h - bh, x + bw, h, p)
                    p.color = c(cc)
                    var wy = h - bh + 8
                    while (wy < h - 6) { var wx = x + 5; while (wx < x + bw - 5) { if (r.next() > .65f) cv.drawRect(wx, wy, wx + 3, wy + 4, p); wx += 8 }; wy += 11 }
                    x += bw + 2
                }
            }
            "hills" -> {
                p.color = c(cc); cv.drawCircle(cx, h * .3f, m * .12f, p)
                glow(cx, h * .3f, m * .45f)
                for (l in 0 until 3) {
                    val path = Path(); path.moveTo(0f, h)
                    val base = h * (.5f + l * .13f)
                    var px = 0f
                    while (px <= w + .1f) { path.lineTo(px, base - r.next() * h * .18f); px += w / 8 }
                    path.lineTo(w, h); path.close()
                    p.color = if (l == 2) c(b) else c(cc, .2f + l * .2f)
                    cv.drawPath(path, p)
                }
            }
            "waves" -> {
                glow(cx, h * .45f, m * .55f)
                for (i in 0 until 10) {
                    val y0 = h * (.3f + i * .07f); val am = h * (.03f + r.next() * .05f)
                    val path = Path(); path.moveTo(0f, y0)
                    var xx = 0f
                    while (xx <= w + .1f) { path.lineTo(xx, y0 + sin(xx / w * PI * (2 + i * .4) + i).toFloat() * am); xx += w / 24 }
                    st.color = c(cc, .2f + i * .07f); st.strokeWidth = 1.5f + i * .3f
                    cv.drawPath(path, st)
                }
            }
            else -> {
                glow(cx, h * .45f, m * .55f)
                repeat(16) {
                    val tx = r.next() * w; val ty = r.next() * h; val z = m * (.08f + r.next() * .25f)
                    val path = Path().apply { moveTo(tx, ty - z); lineTo(tx + z * .9f, ty + z * .6f); lineTo(tx - z * .7f, ty + z * .4f); close() }
                    val col = if (r.next() > .5f) cc else a
                    p.color = c(col, .15f + r.next() * .55f); cv.drawPath(path, p)
                }
            }
        }
    }
}

/** Обложка игры; у своих файлов — нейтральная заглушка «нет обложки». */
@Composable
fun Cover(g: Game, w: Int, h: Int, k: Int = 0, modifier: Modifier = Modifier) {
    if (g.real) { AssetImage("no-cover.jpg", modifier, ContentScale.Crop); return }
    val img = remember(g.id, w, h, k) { Art.bitmap(g, w, h, k) }
    Image(img, null, modifier, contentScale = ContentScale.Crop)
}
