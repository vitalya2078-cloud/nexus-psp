package com.nexus.psp.emulator

import android.view.KeyEvent

/**
 * Раскладки KAPSULA Touch (п. 56–58). Координаты — в точках макета 844×390 (как в веб-прототипе),
 * на экране масштабируются по ширине. x: l/r — от края, c — от центра; y: t/b — от края.
 */
data class Btn(
    val id: String,
    val icon: String?,          // значок: res/drawable/ctl_<имя>.xml; ft/fr/fb/fl — фигуры; null — стик
    val size: Float,
    val pos: String,            // "l16 t14", "r24 b30", "c-30 b14"
    val color: Long? = null,    // цвет кольца, null — слоновая кость
    val act: String? = null,    // pause/save/load/ff — действия приложения
    val dpad: Boolean = false,
    val stick: String? = null,  // "fix" | "float"
    val right: Boolean = false, // правый стик
    val w: Float? = null,       // ширина для вытянутых кнопок (L/R, Select/Start); null — круглая
    val label: String? = null,  // надпись на вытянутой кнопке
    val keys: List<String>? = null, // своя кнопка: какие кнопки консоли нажимает (ft, l1, up…)
    val combo: Boolean = false,     // true — по очереди (комбо), false — все разом
    val key: String? = null,        // какую кнопку консоли нажимает (ft, fb, l1, up…); null — по id
    val to: String? = null,         // переключает на раскладку (id или основа шаблона)
    val hold: Boolean = false,      // true — «пока держишь»: другая раскладка только пока палец на кнопке
)

object ControlLayout {
    const val AMBER = 0xFFD9B36C; const val BLUE = 0xFF7FA7D9; const val GREEN = 0xFF8FC9A3; const val ROSE = 0xFFD98A8A

    /** Экранная кнопка → код Android → LibretroDroid → кнопка консоли (низ = крест, право = круг, верх = треугольник, лево = квадрат). */
    val KEY = mapOf(
        "ft" to KeyEvent.KEYCODE_BUTTON_X, "fr" to KeyEvent.KEYCODE_BUTTON_A, "fb" to KeyEvent.KEYCODE_BUTTON_B, "fl" to KeyEvent.KEYCODE_BUTTON_Y,
        "l1" to KeyEvent.KEYCODE_BUTTON_L1, "r1" to KeyEvent.KEYCODE_BUTTON_R1, "l2" to KeyEvent.KEYCODE_BUTTON_L2, "r2" to KeyEvent.KEYCODE_BUTTON_R2,
        "select" to KeyEvent.KEYCODE_BUTTON_SELECT, "start" to KeyEvent.KEYCODE_BUTTON_START,
    )
    /** Руль кнопками в раскладке «за рулём» — это стрелки влево/вправо. */
    val STEER = mapOf("dl_" to KeyEvent.KEYCODE_DPAD_LEFT, "dr_" to KeyEvent.KEYCODE_DPAD_RIGHT)
    val DIR = mapOf('u' to KeyEvent.KEYCODE_DPAD_UP, 'd' to KeyEvent.KEYCODE_DPAD_DOWN, 'l' to KeyEvent.KEYCODE_DPAD_LEFT, 'r' to KeyEvent.KEYCODE_DPAD_RIGHT)

    /** Код кнопки консоли по имени: ft/fr/fb/fl, l1…r2, select, start, up/down/left/right. */
    fun code(name: String): Int? = KEY[name] ?: when (name) {
        "up" -> KeyEvent.KEYCODE_DPAD_UP; "down" -> KeyEvent.KEYCODE_DPAD_DOWN
        "left" -> KeyEvent.KEYCODE_DPAD_LEFT; "right" -> KeyEvent.KEYCODE_DPAD_RIGHT
        else -> null
    }

    /** Кнопки консоли, из которых собираются свои кнопки (комбо, «две разом»). */
    val PARTS = listOf("ft", "fr", "fb", "fl", "up", "down", "left", "right", "l1", "r1", "l2", "r2", "select", "start")

    /** Своя кнопка из сохранённого описания {id, keys, combo, icon}. Появляется у правого края, дальше — перетаскивается. */
    fun extra(id: String, keys: List<String>, combo: Boolean, icon: String?): Btn =
        Btn(id, icon ?: if (combo) "combo" else "both", 50f, "r230 b150", color = AMBER, keys = keys, combo = combo)

    private val FACE = listOf("ft" to AMBER, "fr" to BLUE, "fb" to GREEN, "fl" to ROSE)

    internal fun faceArc(): List<Btn> {
        val p = listOf(Triple(126, 176, 56), Triple(44, 118, 56), Triple(104, 26, 74), Triple(196, 84, 56))
        return FACE.mapIndexed { i, (id, c) -> Btn(id, id, p[i].third.toFloat(), "r${p[i].first} b${p[i].second}", c) }
    }

    private fun faceDiamond(): List<Btn> {
        val p = listOf(96 to 172, 34 to 112, 96 to 52, 158 to 112)
        return FACE.mapIndexed { i, (id, c) -> Btn(id, id, 56f, "r${p[i].first} b${p[i].second}", c) }
    }


    /** Раскладки «пешком» и «за рулём» (превью в настройках и старые вызовы) — из шаблонов жанров. */
    fun preset(lay: String, swap: Boolean): List<Btn> =
        Templates.build(lay, "psp") + (if (swap) listOf(Templates.swapBtn()) else emptyList())

    /** Служебный ряд сверху: ускорение, сохранить, загрузить, пауза. */
    private fun utilRow() = listOf(
        Btn("ff", "ff", 34f, "c-66 t13", act = "ff"), Btn("save", "save", 34f, "c-22 t13", act = "save"),
        Btn("load", "load", 34f, "c22 t13", act = "load"), Btn("pause", "pause", 34f, "c66 t13", act = "pause"),
    )

    /** Кнопки действий ромбом; координаты — центр ромба (cx от правого края, cy от низа) и шаг. */
    private fun faceAt(r: Int, b: Int, gap: Int, size: Int): List<Btn> {
        val h = size / 2
        // dx > 0 — правее, dy > 0 — выше
        fun at(dx: Int, dy: Int) = "r${r - dx - h} b${b + dy - h}"
        return listOf(
            Btn("ft", "ft", size.toFloat(), at(0, gap), AMBER), Btn("fr", "fr", size.toFloat(), at(gap, 0), BLUE),
            Btn("fb", "fb", size.toFloat(), at(0, -gap), GREEN), Btn("fl", "fl", size.toFloat(), at(-gap, 0), ROSE),
        )
    }

    /** «Классика» — свой джойстик для каждой консоли (выбирается по платформе игры). */
    private fun classic(plat: String): List<Btn> = when (plat) {
        "psp" -> listOf(
            Btn("l1", "l1", 40f, "l16 t14", w = 150f, label = "L"), Btn("r1", "r1", 40f, "r16 t14", w = 150f, label = "R"),
            Btn("dpad", "dpad", 120f, "l58 b162", dpad = true), Btn("ls", null, 92f, "l72 b42", stick = "fix"),
        ) + faceAt(120, 208, 54, 54) + listOf(
            Btn("select", "select", 18f, "r127 b29", w = 46f, label = "SELECT"), Btn("start", "start", 18f, "r67 b29", w = 46f, label = "START"),
        ) + utilRow()
        "ps1" -> listOf(
            Btn("l2", "l2", 34f, "l16 t10", w = 96f, label = "L2"), Btn("l1", "l1", 36f, "l16 t50", w = 128f, label = "L1"),
            Btn("r2", "r2", 34f, "r16 t10", w = 96f, label = "R2"), Btn("r1", "r1", 36f, "r16 t50", w = 128f, label = "R1"),
            Btn("dpad", "dpad", 140f, "l54 b108", dpad = true),
        ) + faceAt(126, 178, 58, 58) + listOf(
            Btn("select", "select", 18f, "c-36 b31", w = 46f, label = "SELECT"), Btn("start", "start", 18f, "c36 b31", w = 46f, label = "START"),
        ) + utilRow()
        else -> listOf(
            Btn("l2", "l2", 34f, "l16 t10", w = 96f, label = "L2"), Btn("l1", "l1", 36f, "l16 t50", w = 128f, label = "L1"),
            Btn("r2", "r2", 34f, "r16 t10", w = 96f, label = "R2"), Btn("r1", "r1", 36f, "r16 t50", w = 128f, label = "R1"),
            Btn("dpad", "dpad", 112f, "l48 b148", dpad = true),
            Btn("ls", null, 116f, "l176 b32", stick = "fix"), Btn("rs", null, 116f, "r176 b32", stick = "fix", right = true),
        ) + faceAt(106, 194, 50, 50) + listOf(
            Btn("select", "select", 18f, "c-34 b29", w = 46f, label = "SELECT"), Btn("start", "start", 18f, "c34 b29", w = 46f, label = "START"),
        ) + utilRow()
    }

    /** mode: touch | classic; plat: psp | ps1 | ps2. */
    fun layout(mode: String, plat: String): List<Btn> {
        if (mode == "classic") return classic(plat)
        val ps = plat != "psp"
        val pause = Btn("pause", "pause", 40f, "c0 b14", act = "pause")
        return (if (ps) listOf(Btn("l1", "l1", 56f, "l16 t12"), Btn("l2", "l2", 46f, "l82 t10"), Btn("r1", "r1", 56f, "r16 t12"), Btn("r2", "r2", 46f, "r82 t10"))
            else listOf(Btn("l1", "l1", 58f, "l16 t12"), Btn("r1", "r1", 58f, "r16 t12"))) +
            listOf(Btn("dpad", "dpad", 80f, "l22 t84", dpad = true), Btn("ls", null, 128f, "l80 b46", stick = "float")) +
            faceArc() +
            listOf(Btn("select", "select", 38f, "c-24 t12"), Btn("start", "start", 38f, "c24 t12"), pause)
    }
}
