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
    val PRESETS: Map<String, List<String>> = emptyMap() // заполняется из каталога, когда появится панель автора

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
