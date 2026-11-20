package com.nexus.psp.ui.kit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Акцентный цвет: янтарь KAPSULA или один из тёплых цветов набора (уютная атмосфера — без холодных).
 * Любой цвет подгоняется по яркости, пока текст и значки этим цветом не станут читаемыми
 * на плашках текущей темы (контраст не меньше 4,5:1 — как для мелкого текста).
 */
object Accent {
    /** Готовые цвета: имя → цвет. */
    val PRESETS = listOf(
        "Медь" to Color(0xFFD9824A), "Коралл" to Color(0xFFE8836B), "Мёд" to Color(0xFFE6C25A),
        "Роза" to Color(0xFFD98A8A), "Мята" to Color(0xFF8FC9A3),
    )

    fun hex(c: Color) = "#%06X".format(c.toArgb() and 0xFFFFFF)

    /** Цвет из набора по сохранённому "#RRGGBB"; всё прочее (пусто, старые цвета, «из фона», системный) — null, то есть янтарь. */
    fun preset(saved: String): Color? = PRESETS.firstOrNull { hex(it.second).equals(saved, true) }?.second

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
}
