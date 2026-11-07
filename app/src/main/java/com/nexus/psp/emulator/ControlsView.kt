package com.nexus.psp.emulator

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

/**
 * Нативный слой экранных кнопок KAPSULA Touch: рисуется на Canvas, касания — мультитач без задержки.
 * Внешний вид повторяет веб-прототип: тёмное стекло, кольцо цвета кнопки, пиктограмма, подсветка нажатия.
 */
@SuppressLint("ViewConstructor")
class ControlsView(context: Context, private val listener: Listener) : View(context) {

    interface Listener {
        fun key(code: Int, down: Boolean)
        fun stick(right: Boolean, x: Float, y: Float)
        fun action(act: String)
    }

    var mirror = false
    var scaleK = 1f
    var vibrate = true
    private var insetL = 0f
    private var insetR = 0f
    private var buttons: List<Btn> = emptyList()
    private val icons = HashMap<String, Bitmap?>()
    private val toggles = HashSet<String>()

    /** Состояние касания: кнопка (индекс), направление крестовины, смещение стика. */
    private class Touch(var idx: Int, var dir: Char? = null, var sx: Float = 0f, var sy: Float = 0f, var kx: Float = 0f, var ky: Float = 0f)
    private val touches = HashMap<Int, Touch>()

    private val dp = resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val bmp = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val rect = RectF()

    fun setLayout(list: List<Btn>) { releaseAll(); buttons = list; invalidate() }
    fun setInsets(l: Float, r: Float) { insetL = l; insetR = r; invalidate() }
    fun setToggle(act: String, on: Boolean) { if (on) toggles.add(act) else toggles.remove(act); invalidate() }

    private fun icon(name: String): Bitmap? = icons.getOrPut(name) {
        try { context.assets.open("web/img/ctl/$name.png").use { BitmapFactory.decodeStream(it) } } catch (e: Exception) { null }
    }

    // ---------- геометрия ----------
    private val k get() = width / 844f
    private fun base(b: Btn) = b.size * k
    private fun diameter(b: Btn) = b.size * k * scaleK

    private fun center(b: Btn): Pair<Float, Float> {
        val d0 = base(b)
        var cx = 0f; var cy = 0f
        for (t in b.pos.split(' ')) {
            val v = t.substring(1).toFloat() * k
            when (t[0]) {
                'l' -> cx = v + insetL + d0 / 2
                'r' -> cx = width - v - insetR - d0 / 2
                'c' -> cx = width / 2f + v
                't' -> cy = v + d0 / 2
                'b' -> cy = height - v - d0 / 2
            }
        }
        if (mirror) cx = width - cx
        return cx to cy
    }

    // ---------- рисование ----------
    override fun onDraw(c: Canvas) {
        buttons.forEachIndexed { i, b ->
            val t = touches.values.firstOrNull { it.idx == i }
            if (b.stick != null) drawStick(c, b, t) else drawButton(c, b, t)
        }
    }

    private fun mix(a: Int, b: Int, f: Float): Int = Color.rgb(
        (Color.red(a) + (Color.red(b) - Color.red(a)) * f).toInt(),
        (Color.green(a) + (Color.green(b) - Color.green(a)) * f).toInt(),
        (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * f).toInt())

    private fun drawButton(c: Canvas, b: Btn, t: Touch?) {
        val (cx, cy) = center(b)
        val pressed = t != null
        val on = b.act != null && b.act in toggles
        val col = b.color?.toInt() ?: (if (on) ControlLayout.BLUE.toInt() else 0xFFF2F1EC.toInt())
        val dark = 0xFF08090C.toInt()
        val r = diameter(b) / 2 * (if (pressed) .94f else 1f)
        // заливка: тёмное стекло с оттенком кнопки; при нажатии — заметнее
        val f = when { pressed -> .68f; on -> .45f; b.color != null -> .21f; else -> 0f }
        fill.color = mix(dark, col, f); fill.alpha = ((if (pressed) .62f else .56f) * 255).toInt()
        c.drawCircle(cx, cy, r, fill)
        if (pressed) {
            val w = diameter(b) * .08f
            ring.strokeWidth = w; ring.color = col; ring.alpha = 64
            c.drawCircle(cx, cy, r + w / 2, ring)
        }
        val sw = max(2 * dp, diameter(b) * .055f)
        ring.strokeWidth = sw; ring.color = col; ring.alpha = if (b.color == null && !on) 235 else 255
        c.drawCircle(cx, cy, r - sw / 2, ring)
        b.icon?.let { icon(it) }?.let { bm ->
            val s = r * .78f
            rect.set(cx - s, cy - s, cx + s, cy + s)
            c.drawBitmap(bm, null, rect, bmp)
        }
        if (b.dpad && t?.dir != null) {
            val o = r * .62f
            val (dx, dy) = when (t.dir) { 'u' -> 0f to -o; 'd' -> 0f to o; 'l' -> -o to 0f; else -> o to 0f }
            fill.color = 0xFFF2F1EC.toInt(); fill.alpha = 115
            c.drawCircle(cx + dx, cy + dy, r * .14f, fill)
        }
    }

    private fun drawStick(c: Canvas, b: Btn, t: Touch?) {
        var (cx, cy) = center(b)
        val live = t != null
        if (live && b.stick == "float") { cx = t!!.sx; cy = t.sy }
        val r = diameter(b) / 2
        val a = if (b.stick == "float" && !live) .55f else 1f
        fill.color = 0xFF08090C.toInt(); fill.alpha = (.22f * 255 * a).toInt()
        c.drawCircle(cx, cy, r, fill)
        ring.strokeWidth = 2 * dp; ring.color = 0xFFF2F1EC.toInt(); ring.alpha = (.45f * 255 * a).toInt()
        c.drawCircle(cx, cy, r - dp, ring)
        val kr = r * .44f
        val kx = cx + (t?.kx ?: 0f); val ky = cy + (t?.ky ?: 0f)
        fill.color = 0xFFF2F1EC.toInt(); fill.alpha = (.25f * 255 * a).toInt()
        c.drawCircle(kx, ky, kr, fill)
        ring.alpha = (.85f * 255 * a).toInt()
        c.drawCircle(kx, ky, kr - dp, ring)
    }

    // ---------- касания ----------
    private fun hit(x: Float, y: Float): Int {
        var best = -1; var bd = Float.MAX_VALUE
        buttons.forEachIndexed { i, b ->
            if (b.stick == "float") return@forEachIndexed
            val (cx, cy) = center(b)
            val d = hypot(x - cx, y - cy)
            if (d <= diameter(b) / 2 * 1.08f && d < bd) { bd = d; best = i }
        }
        return best
    }

    private fun dpadDir(b: Btn, x: Float, y: Float): Char? {
        val (cx, cy) = center(b); val r = diameter(b) / 2
        val dx = x - cx; val dy = y - cy
        if (abs(dx) < r * .12f && abs(dy) < r * .12f) return null
        return if (abs(dx) > abs(dy)) (if (dx > 0) 'r' else 'l') else (if (dy > 0) 'd' else 'u')
    }

    private fun buzz() { if (vibrate) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }

    private fun press(i: Int, t: Touch, x: Float, y: Float) {
        val b = buttons[i]
        when {
            b.act != null -> listener.action(b.act)
            b.dpad -> { t.dir = dpadDir(b, x, y); t.dir?.let { ControlLayout.DIR[it]?.let { c -> listener.key(c, true) } } }
            else -> ControlLayout.KEY[b.id]?.let { listener.key(it, true) }
        }
        buzz()
    }

    private fun release(t: Touch) {
        val b = buttons.getOrNull(t.idx) ?: return
        when {
            b.stick != null -> listener.stick(b.right, 0f, 0f)
            b.dpad -> t.dir?.let { ControlLayout.DIR[it]?.let { c -> listener.key(c, false) } }
            b.act == null -> ControlLayout.KEY[b.id]?.let { listener.key(it, false) }
        }
    }

    fun releaseAll() { touches.values.forEach { release(it) }; touches.clear(); invalidate() }

    private fun moveStick(b: Btn, t: Touch, x: Float, y: Float) {
        val (hx, hy) = if (b.stick == "float") t.sx to t.sy else center(b)
        val lim = diameter(b) * .3f
        var dx = x - hx; var dy = y - hy
        val d = hypot(dx, dy)
        if (d > lim) { dx = dx / d * lim; dy = dy / d * lim }
        t.kx = dx; t.ky = dy
        listener.stick(b.right, dx / lim, dy / lim)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val p = e.actionIndex; val id = e.getPointerId(p); val x = e.getX(p); val y = e.getY(p)
                var i = hit(x, y)
                if (i < 0) {
                    // плавающий стик: касание свободной части «своей» половины экрана
                    val left = (x < width / 2f) != mirror
                    i = buttons.indexOfFirst { it.stick == "float" }
                    if (i < 0 || !left || touches.values.any { it.idx == i }) return true
                    val t = Touch(i, sx = x, sy = y); touches[id] = t; moveStick(buttons[i], t, x, y)
                } else {
                    val b = buttons[i]
                    val t = Touch(i, sx = x, sy = y); touches[id] = t
                    if (b.stick != null) moveStick(b, t, x, y) else press(i, t, x, y)
                }
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                for (p in 0 until e.pointerCount) {
                    val t = touches[e.getPointerId(p)] ?: continue
                    val b = buttons.getOrNull(t.idx) ?: continue
                    val x = e.getX(p); val y = e.getY(p)
                    when {
                        b.stick != null -> moveStick(b, t, x, y)
                        b.dpad -> {
                            val d = dpadDir(b, x, y)
                            if (d != t.dir) {
                                t.dir?.let { ControlLayout.DIR[it]?.let { c -> listener.key(c, false) } }
                                t.dir = d
                                d?.let { ControlLayout.DIR[it]?.let { c -> listener.key(c, true) }; buzz() }
                            }
                        }
                        b.act == null -> {
                            // палец съехал на соседнюю кнопку действия — переключаемся (удобно для комбинаций)
                            val j = hit(x, y)
                            if (j >= 0 && j != t.idx && buttons[j].act == null && buttons[j].stick == null && !buttons[j].dpad && touches.values.none { it.idx == j }) {
                                release(t); t.idx = j; press(j, t, x, y)
                            }
                        }
                    }
                }
                invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                touches.remove(e.getPointerId(e.actionIndex))?.let { release(it) }
                invalidate()
            }
            MotionEvent.ACTION_CANCEL -> releaseAll()
        }
        return true
    }
}
