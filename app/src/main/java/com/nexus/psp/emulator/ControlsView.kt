package com.nexus.psp.emulator

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.PointF
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
 * Touch — тёмное стекло, кольцо цвета кнопки, векторная пиктограмма; «Классика» — физический джойстик консоли (колпачки с фигурами, крестовина, стики).
 */
@SuppressLint("ViewConstructor")
class ControlsView(context: Context, private val listener: Listener) : View(context) {

    companion object {
        /** Значки-фигуры кнопок действий (рисуются кодом, цветом места). */
        val GLYPHS = setOf("ft", "fr", "fb", "fl")
    }

    interface Listener {
        fun key(code: Int, down: Boolean)
        fun stick(right: Boolean, x: Float, y: Float)
        fun action(act: String)
    }

    /** Имя раскладки (ключ для сохранённых положений кнопок) и режим редактора. */
    var layName = "touch"
    var editMode = false
    /** Смещения кнопок в точках макета 844×390: "раскладка:кнопка" → (dx, dy). */
    val offsets = HashMap<String, PointF>()
    var onMoved: ((String, Float, Float) -> Unit)? = null
    private var downT = 0L; private var moved = false
    private var dragIdx = -1; private var dragX = 0f; private var dragY = 0f; private var dragOx = 0f; private var dragOy = 0f
    private val dash = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; pathEffect = DashPathEffect(floatArrayOf(10f, 8f), 0f); color = 0xFF7FA7D9.toInt() }

    var mirror = false
    var scaleK = 1f
    var vibrate = true
    /** touch — KAPSULA Touch (стекло и пиктограммы); classic — «физический» джойстик консоли. */
    var style = "touch"
    var platform = "psp"
    /** Свои значки кнопок Touch: "раскладка:кнопка" → имя значка (ctl_<имя>). */
    val customIcons = HashMap<String, String>()
    /** Кнопки с турбо: пока палец на кнопке, она быстро нажимается сама. */
    val turbo = HashSet<String>()
    /** Короткое касание кнопки в редакторе — выбрать значок / турбо. */
    var onTap: ((String, Btn) -> Unit)? = null
    private var insetL = 0f
    private var insetR = 0f
    private var buttons: List<Btn> = emptyList()
    private val icons = HashMap<String, Drawable?>()
    private val toggles = HashSet<String>()

    /** Состояние касания: кнопка (индекс), направление крестовины, смещение стика, повтор турбо. */
    private class Touch(var idx: Int, var dir: Char? = null, var sx: Float = 0f, var sy: Float = 0f, var kx: Float = 0f, var ky: Float = 0f, var job: Runnable? = null)
    private val touches = HashMap<Int, Touch>()

    private val dp = resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val shade = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val glyph = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    private val rect = RectF()
    private val path = Path()
    private val ivory = 0xFFF2F1EC.toInt()

    fun setLayout(list: List<Btn>) { releaseAll(); buttons = list; invalidate() }
    fun setInsets(l: Float, r: Float) { insetL = l; insetR = r; invalidate() }
    fun setToggle(act: String, on: Boolean) { if (on) toggles.add(act) else toggles.remove(act); invalidate() }

    fun keyOf(b: Btn) = "$layName:${b.id}"
    fun iconOf(b: Btn): String? = customIcons[keyOf(b)] ?: b.icon

    /** Векторный значок res/drawable/ctl_<имя>.xml. */
    private fun icon(name: String): Drawable? = icons.getOrPut(name) {
        val id = resources.getIdentifier("ctl_$name", "drawable", context.packageName)
        if (id == 0) null else try { ContextCompat.getDrawable(context, id)?.mutate() } catch (e: Exception) { null }
    }

    private fun drawIcon(c: Canvas, name: String, cx: Float, cy: Float, half: Float, color: Int, alpha: Int = 255) {
        if (name in GLYPHS) { drawGlyph(c, name, cx, cy, half * 1.1f, color); return }
        val d = icon(name) ?: return
        d.setBounds((cx - half).toInt(), (cy - half).toInt(), (cx + half).toInt(), (cy + half).toInt())
        d.setTint(color); d.alpha = alpha
        d.draw(c)
    }

    // ---------- геометрия ----------
    private val k get() = width / 844f
    private fun diameter(b: Btn) = b.size * k * scaleK
    private fun halfW(b: Btn) = (b.w ?: b.size) * k * scaleK / 2
    private fun halfH(b: Btn) = b.size * k * scaleK / 2

    private fun center(b: Btn): Pair<Float, Float> {
        val dw = (b.w ?: b.size) * k; val dh = b.size * k
        var cx = 0f; var cy = 0f
        for (t in b.pos.split(' ')) {
            val v = t.substring(1).toFloat() * k
            when (t[0]) {
                'l' -> cx = v + insetL + dw / 2
                'r' -> cx = width - v - insetR - dw / 2
                'c' -> cx = width / 2f + v
                't' -> cy = v + dh / 2
                'b' -> cy = height - v - dh / 2
            }
        }
        if (mirror) cx = width - cx
        offsets[keyOf(b)]?.let { cx += it.x * k; cy += it.y * k }
        return cx to cy
    }

    // ---------- рисование ----------
    override fun onDraw(c: Canvas) {
        val classic = style == "classic"
        buttons.forEachIndexed { i, b ->
            val t = touches.values.firstOrNull { it.idx == i }
            when {
                b.stick != null -> if (classic) drawStickClassic(c, b, t) else drawStick(c, b, t)
                b.w != null -> drawWide(c, b, t)
                classic && b.dpad -> drawDpad(c, b, t)
                classic && b.act != null -> drawUtil(c, b, t)
                classic -> drawCap(c, b, t)
                else -> drawButton(c, b, t)
            }
            if (keyOf(b) in turbo) drawTurboBadge(c, b)
            if (editMode) {
                val (x, y) = center(b); dash.strokeWidth = 2 * dp
                if (b.w != null) { rect.set(x - halfW(b) - 4 * dp, y - halfH(b) - 4 * dp, x + halfW(b) + 4 * dp, y + halfH(b) + 4 * dp); c.drawRoundRect(rect, halfH(b), halfH(b), dash) }
                else c.drawCircle(x, y, diameter(b) / 2 + 4 * dp, dash)
            }
        }
    }

    private fun mix(a: Int, b: Int, f: Float): Int = Color.rgb(
        (Color.red(a) + (Color.red(b) - Color.red(a)) * f).toInt(),
        (Color.green(a) + (Color.green(b) - Color.green(a)) * f).toInt(),
        (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * f).toInt())

    /** Фигуры кнопок действий: верх — треугольник, право — круг, низ — крест, лево — квадрат. */
    private fun drawGlyph(c: Canvas, g: String, x: Float, y: Float, half: Float, col: Int) {
        glyph.color = col; glyph.strokeWidth = max(2.4f * dp, half * .2f)
        val a = half * .72f
        when (g) {
            "ft" -> { path.reset(); path.moveTo(x, y - a); path.lineTo(x + a * 1.05f, y + a * .72f); path.lineTo(x - a * 1.05f, y + a * .72f); path.close(); c.drawPath(path, glyph) }
            "fr" -> c.drawCircle(x, y, a * .82f, glyph)
            "fb" -> { val q = a * .74f; c.drawLine(x - q, y - q, x + q, y + q, glyph); c.drawLine(x + q, y - q, x - q, y + q, glyph) }
            else -> { val q = a * .7f; rect.set(x - q, y - q, x + q, y + q); c.drawRoundRect(rect, q * .14f, q * .14f, glyph) }
        }
    }

    /** KAPSULA Touch: тёмное стекло, кольцо цвета кнопки, пиктограмма или фигура. */
    private fun drawButton(c: Canvas, b: Btn, t: Touch?) {
        val (cx, cy) = center(b)
        val pressed = t != null
        val on = b.act != null && b.act in toggles
        val col = b.color?.toInt() ?: (if (on) ControlLayout.BLUE.toInt() else ivory)
        val dark = 0xFF08090C.toInt()
        val r = diameter(b) / 2 * (if (pressed) .94f else 1f)
        val f = when { pressed -> .68f; on -> .45f; b.color != null -> .21f; else -> 0f }
        fill.shader = null; fill.color = mix(dark, col, f); fill.alpha = ((if (pressed) .62f else .56f) * 255).toInt()
        c.drawCircle(cx, cy, r, fill)
        if (pressed) {
            val w = diameter(b) * .08f
            ring.strokeWidth = w; ring.color = col; ring.alpha = 64
            c.drawCircle(cx, cy, r + w / 2, ring)
        }
        val sw = max(2 * dp, diameter(b) * .055f)
        ring.strokeWidth = sw; ring.color = col; ring.alpha = if (b.color == null && !on) 235 else 255
        c.drawCircle(cx, cy, r - sw / 2, ring)
        iconOf(b)?.let { name ->
            // фигуры — цветом места; пиктограммы — белым
            drawIcon(c, name, cx, cy, r * (if (name in GLYPHS) .5f else .62f), if (name in GLYPHS) col else ivory)
        }
        if (b.dpad && t?.dir != null) {
            val o = r * .62f
            val (dx, dy) = when (t.dir) { 'u' -> 0f to -o; 'd' -> 0f to o; 'l' -> -o to 0f; else -> o to 0f }
            fill.color = ivory; fill.alpha = 115
            c.drawCircle(cx + dx, cy + dy, r * .14f, fill)
        }
    }

    private fun drawStick(c: Canvas, b: Btn, t: Touch?) {
        var (cx, cy) = center(b)
        val live = t != null
        if (live && b.stick == "float") { cx = t!!.sx; cy = t.sy }
        val r = diameter(b) / 2
        val a = if (b.stick == "float" && !live) .55f else 1f
        fill.shader = null; fill.color = 0xFF08090C.toInt(); fill.alpha = (.22f * 255 * a).toInt()
        c.drawCircle(cx, cy, r, fill)
        ring.strokeWidth = 2 * dp; ring.color = ivory; ring.alpha = (.45f * 255 * a).toInt()
        c.drawCircle(cx, cy, r - dp, ring)
        val kr = r * .44f
        val kx = cx + (t?.kx ?: 0f); val ky = cy + (t?.ky ?: 0f)
        fill.color = ivory; fill.alpha = (.25f * 255 * a).toInt()
        c.drawCircle(kx, ky, kr, fill)
        ring.alpha = (.85f * 255 * a).toInt()
        c.drawCircle(kx, ky, kr - dp, ring)
    }

    // ---------- «Классика»: физический джойстик ----------
    private fun shadow(c: Canvas, x: Float, y: Float, r: Float) {
        shade.shader = RadialGradient(x, y + r * .12f, r * 1.18f, intArrayOf(0x8C000000.toInt(), 0x00000000), floatArrayOf(.72f, 1f), Shader.TileMode.CLAMP)
        c.drawCircle(x, y + r * .12f, r * 1.18f, shade)
    }

    /** Кнопка действия: выпуклый колпачок с ободком, бликом и фигурой. */
    private fun drawCap(c: Canvas, b: Btn, t: Touch?) {
        val (x, y) = center(b)
        val pressed = t != null
        val col = b.color?.toInt() ?: ivory
        val r = diameter(b) / 2
        shadow(c, x, y, r)
        val rr = r * (if (pressed) .9f else .94f)
        fill.shader = RadialGradient(x - rr * .24f, y - rr * .42f, rr * 1.6f, intArrayOf(0xFF4A4D57.toInt(), 0xFF23252C.toInt(), 0xFF141519.toInt()), floatArrayOf(0f, .55f, 1f), Shader.TileMode.CLAMP)
        fill.alpha = 255
        c.drawCircle(x, y, rr, fill); fill.shader = null
        if (pressed) { fill.color = col; fill.alpha = 60; c.drawCircle(x, y, rr, fill) }
        ring.strokeWidth = 1.6f * dp; ring.color = col; ring.alpha = if (pressed) 230 else 140
        c.drawCircle(x, y, rr, ring)
        if (pressed) { ring.strokeWidth = 3 * dp; ring.alpha = 200; c.drawCircle(x, y, r + 3 * dp, ring) }
        fill.color = Color.WHITE; fill.alpha = 23
        rect.set(x - rr * .64f, y - rr * .62f, x + rr * .28f, y - rr * .22f); c.drawOval(rect, fill)
        iconOf(b)?.let { drawIcon(c, it, x, y, r * .5f, col) }
    }

    /** Крестовина: крест со скруглениями, лунка в центре, стрелки; нажатое направление подсвечено. */
    private fun drawDpad(c: Canvas, b: Btn, t: Touch?) {
        val (x, y) = center(b)
        val s = diameter(b); val a = s * .17f; val h = s * .5f; val rr = s * .06f
        fill.shader = null; fill.color = Color.BLACK; fill.alpha = 56
        c.drawCircle(x, y, h + 6 * dp, fill)
        val v = Path().apply { addRoundRect(RectF(x - a, y - h, x + a, y + h), rr, rr, Path.Direction.CW) }
        val hz = Path().apply { addRoundRect(RectF(x - h, y - a, x + h, y + a), rr, rr, Path.Direction.CW) }
        v.op(hz, Path.Op.UNION)
        fill.shader = LinearGradient(x, y - h, x, y + h, 0xFF3A3D46.toInt(), 0xFF1A1B20.toInt(), Shader.TileMode.CLAMP); fill.alpha = 255
        c.drawPath(v, fill); fill.shader = null
        ring.strokeWidth = 1.2f * dp; ring.color = Color.WHITE; ring.alpha = 36; c.drawPath(v, ring)
        t?.dir?.let { d ->
            fill.color = ivory; fill.alpha = 46
            when (d) { 'u' -> rect.set(x - a, y - h, x + a, y - a); 'd' -> rect.set(x - a, y + a, x + a, y + h); 'l' -> rect.set(x - h, y - a, x - a, y + a); else -> rect.set(x + a, y - a, x + h, y + a) }
            c.drawRoundRect(rect, rr, rr, fill)
        }
        fill.color = Color.BLACK; fill.alpha = 90; c.drawCircle(x, y, a * .62f, fill)
        fill.color = ivory
        val q = s * .055f; val o = h * .72f
        for ((dx, dy, dir) in listOf(Triple(0f, -1f, 'u'), Triple(0f, 1f, 'd'), Triple(-1f, 0f, 'l'), Triple(1f, 0f, 'r'))) {
            fill.alpha = if (t?.dir == dir) 230 else 140
            val px = x + dx * o; val py = y + dy * o
            path.reset()
            path.moveTo(px + dx * q, py + dy * q)
            path.lineTo(px - dy * q - dx * q * .2f, py + dx * q - dy * q * .2f)
            path.lineTo(px + dy * q - dx * q * .2f, py - dx * q - dy * q * .2f)
            path.close(); c.drawPath(path, fill)
        }
    }

    /** Вытянутые кнопки: плечевые L/R (с надписью) и Select/Start (подпись снизу). */
    private fun drawWide(c: Canvas, b: Btn, t: Touch?) {
        val (x, y) = center(b)
        val hw = halfW(b); val hh = halfH(b)
        val pill = b.id == "select" || b.id == "start"
        val rad = if (pill) hh else hh * .62f
        rect.set(x - hw, y - hh + 3 * dp, x + hw, y + hh + 3 * dp)
        fill.shader = null; fill.color = Color.BLACK; fill.alpha = 80; c.drawRoundRect(rect, rad, rad, fill)
        rect.set(x - hw, y - hh, x + hw, y + hh)
        fill.shader = LinearGradient(x, y - hh, x, y + hh, 0xFF43464F.toInt(), 0xFF1B1C21.toInt(), Shader.TileMode.CLAMP); fill.alpha = if (b.id == "l2" || b.id == "r2") 235 else 255
        c.drawRoundRect(rect, rad, rad, fill); fill.shader = null
        if (t != null) { fill.color = ivory; fill.alpha = 46; c.drawRoundRect(rect, rad, rad, fill) }
        ring.strokeWidth = 1.2f * dp; ring.color = Color.WHITE; ring.alpha = if (t != null) 110 else 41
        c.drawRoundRect(rect, rad, rad, ring)
        val label = b.label ?: return
        text.color = ivory
        if (pill) {
            text.textSize = 9.5f * k * scaleK; text.letterSpacing = .12f; text.alpha = 150
            c.drawText(label, x, y + hh + text.textSize * 1.45f, text)
        } else {
            text.textSize = hh * .8f; text.letterSpacing = 0f; text.alpha = 215
            c.drawText(label, x, y + text.textSize * .36f, text)
        }
    }

    /** Служебные кнопки в «Классике»: маленькое тёмное стекло со значком. */
    private fun drawUtil(c: Canvas, b: Btn, t: Touch?) {
        val (x, y) = center(b)
        val r = diameter(b) / 2
        val on = b.act in toggles
        fill.shader = null; fill.color = if (on) ControlLayout.BLUE.toInt() else Color.BLACK; fill.alpha = if (t != null) 170 else if (on) 110 else 97
        c.drawCircle(x, y, r, fill)
        ring.strokeWidth = 1 * dp; ring.color = if (on) ControlLayout.BLUE.toInt() else Color.WHITE; ring.alpha = if (on) 230 else 46
        c.drawCircle(x, y, r, ring)
        iconOf(b)?.let { drawIcon(c, it, x, y, r * .5f, ivory) }
    }

    /** Стик: лунка и шляпка с рифлением; у PSP — плоская «таблетка». */
    private fun drawStickClassic(c: Canvas, b: Btn, t: Touch?) {
        val (cx, cy) = center(b)
        val r = diameter(b) / 2
        val nub = platform == "psp"
        fill.shader = null; fill.color = 0xFF0C0D10.toInt(); fill.alpha = 128
        if (nub) { rect.set(cx - r, cy - r * .87f, cx + r, cy + r * .87f); c.drawOval(rect, fill) } else c.drawCircle(cx, cy, r, fill)
        ring.strokeWidth = 1 * dp; ring.color = Color.WHITE; ring.alpha = 31
        if (nub) c.drawOval(rect, ring) else c.drawCircle(cx, cy, r, ring)
        val kx = cx + (t?.kx ?: 0f); val ky = cy + (t?.ky ?: 0f)
        val kr = r * (if (nub) .56f else .56f)
        shadow(c, kx, ky, kr)
        fill.shader = RadialGradient(kx - kr * .2f, ky - kr * .36f, kr * 1.7f, intArrayOf(0xFF55585F.toInt(), 0xFF2A2C33.toInt(), 0xFF16171B.toInt()), floatArrayOf(0f, .6f, 1f), Shader.TileMode.CLAMP); fill.alpha = 255
        if (nub) { rect.set(kx - kr, ky - kr * .88f, kx + kr, ky + kr * .88f); c.drawOval(rect, fill) } else c.drawCircle(kx, ky, kr, fill)
        fill.shader = null
        ring.alpha = if (t != null) 110 else 46
        if (nub) c.drawOval(rect, ring) else c.drawCircle(kx, ky, kr, ring)
        ring.alpha = 18
        if (nub) { rect.set(kx - kr * .62f, ky - kr * .55f, kx + kr * .62f, ky + kr * .55f); c.drawOval(rect, ring) }
        else for (i in 1..3) c.drawCircle(kx, ky, kr * (.28f + i * .18f), ring)
        b.label?.let { text.textSize = 11f * k; text.color = ivory; text.alpha = 128; text.letterSpacing = 0f; c.drawText(it, kx, ky + text.textSize * .36f, text) }
    }

    private fun drawTurboBadge(c: Canvas, b: Btn) {
        val (x, y) = center(b)
        val r = if (b.w != null) halfH(b) else diameter(b) / 2
        val bx = x + (if (b.w != null) halfW(b) else r * .72f); val by = y - r * .72f
        val br = max(8 * dp, r * .28f)
        fill.shader = null; fill.color = ControlLayout.AMBER.toInt(); fill.alpha = 255
        c.drawCircle(bx, by, br, fill)
        drawIcon(c, "turbo", bx, by, br * .72f, 0xFF0B0C0F.toInt())
    }

    // ---------- касания ----------
    private fun hit(x: Float, y: Float): Int {
        var best = -1; var bd = Float.MAX_VALUE
        buttons.forEachIndexed { i, b ->
            if (b.stick == "float") return@forEachIndexed
            val (cx, cy) = center(b)
            val d = hypot(x - cx, y - cy)
            val inside = if (b.w != null) abs(x - cx) <= halfW(b) * 1.08f + 4 * dp && abs(y - cy) <= halfH(b) * 1.4f + 4 * dp
                else d <= diameter(b) / 2 * 1.08f
            if (inside && d < bd) { bd = d; best = i }
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
            else -> (ControlLayout.KEY[b.id] ?: ControlLayout.STEER[b.id])?.let { code ->
                listener.key(code, true)
                if (keyOf(b) in turbo) {
                    // турбо: ~11 нажатий в секунду, пока палец на кнопке
                    val job = object : Runnable { var down = true; override fun run() { down = !down; listener.key(code, down); postDelayed(this, 45) } }
                    t.job = job; postDelayed(job, 45)
                }
            }
        }
        buzz()
    }

    private fun release(t: Touch) {
        t.job?.let { removeCallbacks(it) }; t.job = null
        val b = buttons.getOrNull(t.idx) ?: return
        when {
            b.stick != null -> listener.stick(b.right, 0f, 0f)
            b.dpad -> t.dir?.let { ControlLayout.DIR[it]?.let { c -> listener.key(c, false) } }
            b.act == null -> (ControlLayout.KEY[b.id] ?: ControlLayout.STEER[b.id])?.let { listener.key(it, false) }
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

    /** Редактор: кнопку можно перетащить пальцем; положение сохраняется в точках макета. */
    private fun editTouch(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragIdx = hit(e.x, e.y).takeIf { it >= 0 } ?: buttons.indices.firstOrNull { i -> buttons[i].stick == "float" && run { val (cx, cy) = center(buttons[i]); hypot(e.x - cx, e.y - cy) <= diameter(buttons[i]) / 2 } } ?: -1
                downT = e.eventTime; moved = false
                // мимо кнопок — касание уходит в интерфейс под слоем (плашка «Готово»)
                if (dragIdx < 0) return false
                val key = keyOf(buttons[dragIdx]); val o = offsets[key] ?: PointF()
                dragX = e.x; dragY = e.y; dragOx = o.x; dragOy = o.y; buzz()
            }
            MotionEvent.ACTION_MOVE -> if (dragIdx >= 0) {
                if (hypot(e.x - dragX, e.y - dragY) > 8 * dp) moved = true
                if (!moved) return true
                val key = "$layName:${buttons[dragIdx].id}"
                offsets[key] = PointF(dragOx + (e.x - dragX) / k, dragOy + (e.y - dragY) / k); invalidate()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (dragIdx >= 0) {
                val b = buttons[dragIdx]; val key = keyOf(b)
                if (moved) offsets[key]?.let { onMoved?.invoke(key, it.x, it.y) }
                else if (e.actionMasked == MotionEvent.ACTION_UP && e.eventTime - downT < 500 && b.stick == null && !b.dpad && b.act == null) onTap?.invoke(key, b)
                dragIdx = -1
            }
        }
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (editMode) return editTouch(e)
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
