package com.nexus.psp.ui.kit

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Акцентный цвет: свой, из фона главного экрана или как в системе (Android 12+).
 * Любой цвет подгоняется по яркости, пока текст и значки этим цветом не станут читаемыми
 * на плашках текущей темы (контраст не меньше 4,5:1 — как для мелкого текста).
 */
object Accent {
    /** Готовые цвета: имя → цвет. */
    val PRESETS = listOf(
        "Янтарь" to Color(0xFFD9B36C), "Небо" to Color(0xFF7FA7D9), "Мята" to Color(0xFF8FC9A3),
        "Роза" to Color(0xFFD98A8A), "Сирень" to Color(0xFFB39DDB), "Бирюза" to Color(0xFF7CC8D9),
    )

    private fun lin(c: Float) = if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    fun luminance(c: Color) = 0.2126f * lin(c.red) + 0.7152f * lin(c.green) + 0.0722f * lin(c.blue)
    fun contrast(a: Color, b: Color): Float { val x = luminance(a); val y = luminance(b); return (max(x, y) + 0.05f) / (min(x, y) + 0.05f) }

    private fun hsl(c: Color): FloatArray { val o = FloatArray(3); androidx.core.graphics.ColorUtils.colorToHSL(c.toArgb(), o); return o }
    private fun fromHsl(h: FloatArray) = Color(androidx.core.graphics.ColorUtils.HSLToColor(h))

    /** Подогнать цвет под фон: светлее на тёмной теме, темнее на светлой, пока контраст не станет ≥ 4,5. */
    fun fit(c: Color, surface: Color, light: Boolean): Color {
        val h = hsl(c)
        // слишком серые цвета чуть насыщаем: акцент должен быть цветом, а не серым
        if (h[1] < 0.25f) h[1] = 0.35f
        var out = fromHsl(h)
        var n = 0
        while (contrast(out, surface) < 4.5f && n++ < 40) {
            h[2] = (h[2] + if (light) -0.02f else 0.02f).coerceIn(0f, 1f)
            out = fromHsl(h)
        }
        return out
    }

    /** Цвет из картинки: самый заметный насыщенный оттенок (по уменьшенной копии 48×48). */
    fun fromImage(b: Bitmap): Color? {
        val s = Bitmap.createScaledBitmap(b, 48, 48, true)
        val buckets = HashMap<Int, FloatArray>() // оттенок по 20° → (вес, сумма r, g, b)
        val hsv = FloatArray(3)
        for (y in 0 until s.height) for (x in 0 until s.width) {
            val p = s.getPixel(x, y)
            android.graphics.Color.colorToHSV(p, hsv)
            if (hsv[1] < 0.3f || hsv[2] < 0.3f) continue
            val w = hsv[1] * hsv[2]
            val k = (hsv[0] / 20f).toInt()
            val a = buckets.getOrPut(k) { FloatArray(4) }
            a[0] += w; a[1] += android.graphics.Color.red(p) * w; a[2] += android.graphics.Color.green(p) * w; a[3] += android.graphics.Color.blue(p) * w
        }
        if (s !== b) s.recycle()
        val best = buckets.values.maxByOrNull { it[0] } ?: return null
        if (best[0] < 3f) return null
        return Color(best[1] / best[0] / 255f, best[2] / best[0] / 255f, best[3] / best[0] / 255f)
    }

    /** Акцент системы (Material You, Android 12+). */
    fun system(c: Context): Color? = if (android.os.Build.VERSION.SDK_INT >= 31) try {
        Color(c.getColor(android.R.color.system_accent1_300))
    } catch (e: Exception) { null } else null
}
