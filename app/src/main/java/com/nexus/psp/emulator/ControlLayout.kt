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
    /** Руль кнопками в раскладке «за рулём» — это стрелки влево/вправо. */
    val STEER = mapOf("dl_" to KeyEvent.KEYCODE_DPAD_LEFT, "dr_" to KeyEvent.KEYCODE_DPAD_RIGHT)
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

    /** Пресеты автора каталога: раскладки игры по порядку (первая — стартовая), п. 57. */
    val PRESETS = mapOf("rubezh" to listOf("foot", "drive"), "drift" to listOf("drive"))

    private fun faceArc(ics: List<String>): List<Btn> = faceArc().mapIndexed { i, b -> b.copy(icon = ics[i]) }

    /** Контекстные раскладки «пешком» и «за рулём» (как мобильные порты экшенов). */
    fun preset(lay: String, swap: Boolean): List<Btn> = when (lay) {
        "foot" -> listOf(Btn("l1", "l1", 52f, "l16 t12"), Btn("r1", "r1", 52f, "r16 t12"), Btn("ls", null, 128f, "l80 b46", stick = "float")) +
            faceArc(listOf("grab", "attack", "jump", "sprint")) +
            listOf(Btn("aim", "aim", 46f, "r216 b170"), Btn("car", "car", 46f, "r20 t72", act = "ctx:drive"), Btn("weapon", "weapon", 46f, "r20 t126"),
                Btn("map", "map", 46f, "l20 t72"), Btn("pause", "pause", 40f, "c0 b14", act = "pause")) +
            (if (swap) listOf(Btn("swap", "swap", 40f, "r76 t76", act = "swap")) else emptyList())
        else -> listOf(
            Btn("left", "left", 78f, "l28 b30", act = null).copy(id = "dl_"), Btn("right", "right", 78f, "l122 b30").copy(id = "dr_"), Btn("tilt", "tilt", 44f, "l44 b124", act = "tilt"),
            Btn("fb", "gas", 84f, "r22 b24", GREEN), Btn("fl", "brake", 76f, "r118 b24", ROSE), Btn("r1", "hbrake", 58f, "r136 b112", AMBER), Btn("fr", "nitro", 52f, "r40 b122", BLUE),
            Btn("car", "car", 44f, "r20 t72", act = "ctx:foot"), Btn("ft", "horn", 44f, "r20 t124"), Btn("cam", "cam", 44f, "r72 t72"), Btn("l1", "mirror", 44f, "r72 t124"),
            Btn("pause", "pause", 40f, "c-26 b14", act = "pause"),
        ) + (if (swap) listOf(Btn("swap", "swap", 40f, "c26 b14", act = "swap")) else emptyList())
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
