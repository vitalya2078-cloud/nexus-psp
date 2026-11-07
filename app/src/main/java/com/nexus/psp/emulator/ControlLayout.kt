package com.nexus.psp.emulator

import android.view.KeyEvent

/**
 * Раскладки KAPSULA Touch (п. 56–58). Координаты — в точках макета 844×390 (как в веб-прототипе),
 * на экране масштабируются по ширине. x: l/r — от края, c — от центра; y: t/b — от края.
 */
data class Btn(
    val id: String,
    val icon: String?,          // имя PNG в assets/web/img/ctl, null — стик
    val size: Float,
    val pos: String,            // "l16 t14", "r24 b30", "c-30 b14"
    val color: Long? = null,    // цвет кольца, null — слоновая кость
    val act: String? = null,    // pause/save/load/ff — действия приложения
    val dpad: Boolean = false,
    val stick: String? = null,  // "fix" | "float"
    val right: Boolean = false, // правый стик
)

object ControlLayout {
    const val AMBER = 0xFFD9B36C; const val BLUE = 0xFF7FA7D9; const val GREEN = 0xFF8FC9A3; const val ROSE = 0xFFD98A8A

    /** Экранная кнопка → код Android → LibretroDroid → кнопка консоли (низ = крест, право = круг, верх = треугольник, лево = квадрат). */
    val KEY = mapOf(
        "ft" to KeyEvent.KEYCODE_BUTTON_X, "fr" to KeyEvent.KEYCODE_BUTTON_A, "fb" to KeyEvent.KEYCODE_BUTTON_B, "fl" to KeyEvent.KEYCODE_BUTTON_Y,
        "l1" to KeyEvent.KEYCODE_BUTTON_L1, "r1" to KeyEvent.KEYCODE_BUTTON_R1, "l2" to KeyEvent.KEYCODE_BUTTON_L2, "r2" to KeyEvent.KEYCODE_BUTTON_R2,
        "select" to KeyEvent.KEYCODE_BUTTON_SELECT, "start" to KeyEvent.KEYCODE_BUTTON_START,
    )
    val DIR = mapOf('u' to KeyEvent.KEYCODE_DPAD_UP, 'd' to KeyEvent.KEYCODE_DPAD_DOWN, 'l' to KeyEvent.KEYCODE_DPAD_LEFT, 'r' to KeyEvent.KEYCODE_DPAD_RIGHT)

    private val FACE = listOf("ft" to AMBER, "fr" to BLUE, "fb" to GREEN, "fl" to ROSE)

    private fun faceArc(): List<Btn> {
        val p = listOf(Triple(126, 176, 56), Triple(44, 118, 56), Triple(104, 26, 74), Triple(196, 84, 56))
        return FACE.mapIndexed { i, (id, c) -> Btn(id, id, p[i].third.toFloat(), "r${p[i].first} b${p[i].second}", c) }
    }

    private fun faceDiamond(): List<Btn> {
        val p = listOf(96 to 172, 34 to 112, 96 to 52, 158 to 112)
        return FACE.mapIndexed { i, (id, c) -> Btn(id, id, 56f, "r${p[i].first} b${p[i].second}", c) }
    }

    /** mode: touch | classic; plat: psp | ps1 | ps2. */
    fun layout(mode: String, plat: String): List<Btn> {
        val ps = plat != "psp"
        val pause = Btn("pause", "pause", 40f, "c0 b14", act = "pause")
        return if (mode == "classic") {
            (if (ps) listOf(Btn("l2", "l2", 52f, "l16 t14"), Btn("l1", "l1", 60f, "l80 t24"), Btn("r2", "r2", 52f, "r16 t14"), Btn("r1", "r1", 60f, "r80 t24"))
            else listOf(Btn("l1", "l1", 62f, "l20 t16"), Btn("r1", "r1", 62f, "r20 t16"))) +
                listOf(Btn("dpad", "dpad", 104f, "l40 t104", dpad = true), Btn("ls", null, 110f, "l170 b30", stick = "fix")) +
                (if (ps) listOf(Btn("rs", null, 96f, "r250 b30", stick = "fix", right = true)) else emptyList()) +
                faceDiamond() +
                listOf(
                    Btn("ff", "ff", 36f, "c-66 t12", act = "ff"), Btn("save", "save", 36f, "c-22 t12", act = "save"),
                    Btn("load", "load", 36f, "c22 t12", act = "load"), Btn("pause", "pause", 36f, "c66 t12", act = "pause"),
                    Btn("select", "select", 44f, "c-30 b14"), Btn("start", "start", 44f, "c30 b14"),
                )
        } else {
            (if (ps) listOf(Btn("l1", "l1", 56f, "l16 t12"), Btn("l2", "l2", 46f, "l82 t10"), Btn("r1", "r1", 56f, "r16 t12"), Btn("r2", "r2", 46f, "r82 t10"))
            else listOf(Btn("l1", "l1", 58f, "l16 t12"), Btn("r1", "r1", 58f, "r16 t12"))) +
                listOf(Btn("dpad", "dpad", 80f, "l22 t84", dpad = true), Btn("ls", null, 128f, "l80 b46", stick = "float")) +
                faceArc() +
                listOf(Btn("select", "select", 38f, "c-24 t12"), Btn("start", "start", 38f, "c24 t12"), pause)
        }
    }
}
