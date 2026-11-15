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
    val cap: String? = null,        // подпись под кнопкой (по умолчанию — название значка)
    val more: Boolean = false,      // редкая кнопка: спрятана в панель «⋯»
    val cam: String? = null,        // зона камеры свайпом: "u=l1+ft,d=l1+fb,l=l1+fl,r=l1+fr;tap=l1"
)

object ControlLayout {
    /** Кнопка «⋯»: открывает панель редких действий. */
    fun moreBtn(pos: String = "c-46 b14") = Btn("more", "more", 40f, pos, act = "more")
    /** Невидимая зона камеры на правой половине экрана (свайп → кнопки камеры). */
    fun camZone(spec: String) = Btn("camz", null, 0f, "r0 b0", cam = spec)

    /** Зона камеры: направление (u/d/l/r) → кнопки консоли; tap — двойное касание. */
    fun camSpec(spec: String): Map<String, List<String>> = spec.split(',', ';').mapNotNull { p ->
        val i = p.indexOf('='); if (i <= 0) null else p.substring(0, i).trim() to p.substring(i + 1).split('+').map { it.trim() }.filter { it.isNotEmpty() }
    }.toMap()

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
    /** Руль кнопками: держишь — левый стик до упора влево/вправо (машина поворачивает как от стика). */
    const val STEER_L = -201; const val STEER_R = -202

    fun code(name: String): Int? = KEY[name] ?: when (name) {
        "steer_l" -> STEER_L; "steer_r" -> STEER_R
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

    /**
     * Денди: крупная крестовина, B и A — красные, как на джойстике, над ними турбо (ядро: Y — турбо B, X — турбо A).
     * Кнопки подписаны буквами — это кнопки самого джойстика Денди.
     */
    private fun nes(): List<Btn> = listOf(
        Btn("dpad", "dpad", 132f, "l44 b40", dpad = true),
        Btn("b", null, 74f, "r128 b40", ROSE, key = "fb", label = "B"),
        Btn("a", null, 74f, "r30 b72", ROSE, key = "fr", label = "A"),
        Btn("tb", "turbo", 50f, "r150 b136", key = "fl", cap = "Турбо B"),
        Btn("ta", "turbo", 50f, "r52 b168", key = "ft", cap = "Турбо A"),
        Btn("select", "select", 38f, "c-24 t12", key = "select", cap = "Select"),
        Btn("start", "start", 38f, "c24 t12", key = "start", cap = "Start"),
        Btn("pause", "pause", 40f, "c0 b14", act = "pause"),
    )

    // ---------- свои джойстики ретро-консолей ----------
    // Какая кнопка экрана какую кнопку ядра нажимает — сверено с исходниками ядер (описания кнопок libretro):
    // clownmdemu, Gearsystem, Yabasanshiro, Flycast, Geargrafx, Geolith, Beetle NGP/WonderSwan, Stella, ProSystem, Gearlynx.

    /** Кнопка RetroPad → как она подписана на джойстике самой консоли (для гайда и подсказок). */
    val NAMES: Map<String, Map<String, String>> = mapOf(
        "nes" to mapOf("fb" to "B", "fr" to "A", "fl" to "Турбо B", "ft" to "Турбо A"),
        "md" to mapOf("fl" to "A", "fb" to "B", "fr" to "C", "l1" to "X", "ft" to "Y", "r1" to "Z", "select" to "Mode"),
        "sms" to mapOf("fb" to "1", "fr" to "2", "start" to "Pause"),
        "gg" to mapOf("fb" to "1", "fr" to "2"),
        "saturn" to mapOf("fb" to "A", "fr" to "B", "l1" to "C", "fl" to "X", "ft" to "Y", "r1" to "Z", "l2" to "L", "r2" to "R"),
        "dc" to mapOf("fb" to "A", "fr" to "B", "fl" to "X", "ft" to "Y", "l2" to "L", "r2" to "R"),
        "pce" to mapOf("fr" to "I", "fb" to "II", "start" to "Run", "l2" to "Турбо II", "r2" to "Турбо I"),
        "neogeo" to mapOf("fb" to "A", "fr" to "B", "fl" to "C", "ft" to "D", "select" to "Монетка"),
        "ngp" to mapOf("fb" to "A", "fr" to "B", "start" to "Option"),
        "ws" to mapOf("fr" to "A", "fb" to "B", "r2" to "Y1", "r1" to "Y2", "l2" to "Y3", "l1" to "Y4", "select" to "Поворот"),
        "a26" to mapOf("fb" to "Огонь", "start" to "Reset", "l1" to "Сложность A", "l2" to "Сложность B"),
        "a78" to mapOf("fb" to "1", "fr" to "2", "ft" to "Reset", "start" to "Pause"),
        "lynx" to mapOf("fr" to "A", "fb" to "B", "l1" to "Option 1", "r1" to "Option 2", "start" to "Pause"),
    )

    /** Подпись кнопки RetroPad, если у консоли нет своей буквы. */
    val KEY_TEXT = mapOf("l1" to "L1", "r1" to "R1", "l2" to "L2", "r2" to "R2", "select" to "SEL", "start" to "START",
        "ft" to "△", "fr" to "○", "fb" to "×", "fl" to "□", "up" to "↑", "down" to "↓", "left" to "←", "right" to "→")

    /** Значок «родная кнопка консоли»: k:<кнопка RetroPad> — рисуется буквой/цифрой консоли её цветом. */
    const val CLASSIC = "k:"

    /** Родные кнопки консоли для редактора: значок → подпись (группа «Классика»). */
    fun classicIcons(plat: String): List<Pair<String, String>> {
        val p = pad(plat) ?: return when (plat) {
            "psp" -> listOf("ft" to "Треугольник", "fr" to "Круг", "fb" to "Крест", "fl" to "Квадрат", "l1" to "L", "r1" to "R", "select" to "Select", "start" to "Start")
            else -> listOf("ft" to "Треугольник", "fr" to "Круг", "fb" to "Крест", "fl" to "Квадрат", "l1" to "L1", "r1" to "R1", "l2" to "L2", "r2" to "R2", "select" to "Select", "start" to "Start")
        }
        return p.filter { !it.dpad && it.stick == null && it.act == null && it.cam == null }.mapNotNull { b ->
            val k = b.key ?: b.id
            when {
                b.label != null -> CLASSIC + k to (NAMES[plat]?.get(k) ?: b.label)
                b.icon != null -> b.icon to (b.cap ?: NAMES[plat]?.get(k) ?: k)
                else -> CLASSIC + k to (NAMES[plat]?.get(k) ?: b.cap ?: k)
            }
        }.distinctBy { it.first }
    }

    /** Кнопки консоли, из которых собираются свои кнопки: только те, что есть у консоли, и стрелки. */
    fun parts(plat: String): List<String> {
        val p = pad(plat) ?: return if (plat == "psp") PARTS.filter { it != "l2" && it != "r2" } else PARTS
        val ks = p.filter { !it.dpad && it.stick == null && it.act == null && it.cam == null }.map { it.key ?: it.id }.filter { code(it) != null }
        return (ks + listOf("up", "down", "left", "right")).distinct()
    }

    /** Буква/цифра кнопки на джойстике консоли (null — у консоли своих букв нет). */
    fun classicLabel(plat: String, key: String): String? = pad(plat)?.firstOrNull { (it.key ?: it.id) == key }?.let { it.label ?: NAMES[plat]?.get(key) } ?: NAMES[plat]?.get(key)

    /** Цвет родной кнопки консоли (как на её джойстике). */
    fun classicColor(plat: String, key: String): Long? = pad(plat)?.firstOrNull { (it.key ?: it.id) == key }?.color

    /** Как выглядит новая своя кнопка с одной кнопкой консоли: родная кнопка (буква или значок) или палец. */
    fun classicIcon(plat: String, key: String): String {
        if (key in setOf("up", "down", "left", "right")) return "arr_$key"
        val b = pad(plat)?.firstOrNull { (it.key ?: it.id) == key } ?: return if (key in KEY) key else "tap"
        return b.icon?.takeIf { b.label == null } ?: (CLASSIC + key)
    }

    /** У консоли свой джойстик (один и в Touch, и в «Классике»; жанровые шаблоны PSP к ней не подходят). */
    fun hasPad(plat: String) = plat in NAMES

    private const val GREY = 0xFFB9BCC6
    private fun dpad() = Btn("dpad", "dpad", 132f, "l44 b40", dpad = true)
    private fun pauseB() = Btn("pause", "pause", 40f, "c0 b14", act = "pause")
    /** Круглая кнопка с буквой или цифрой, как на джойстике консоли. */
    private fun rb(id: String, key: String, label: String, size: Float, pos: String, color: Long? = null, more: Boolean = false, cap: String? = null) =
        Btn(id, null, size, pos, color, key = key, label = label, more = more, cap = cap)
    /** Маленькая кнопка-значок (Select, Start, Монетка…). */
    private fun ib(id: String, icon: String, key: String, cap: String, pos: String) = Btn(id, icon, 38f, pos, key = key, cap = cap)
    private fun shoulder(id: String, key: String, label: String, pos: String) = Btn(id, key, 40f, pos, w = 150f, label = label, key = key)
    /** Две большие кнопки справа (как у Денди): нижняя левее, верхняя правее. */
    private fun two(lo: Triple<String, String, String>, hi: Triple<String, String, String>, color: Long? = null) = listOf(
        rb(lo.first, lo.second, lo.third, 74f, "r128 b40", color), rb(hi.first, hi.second, hi.third, 74f, "r30 b72", color),
    )

    private fun pad(plat: String): List<Btn>? = when (plat) {
        "nes" -> nes()
        // Mega Drive, 6 кнопок: снизу A B C, над ними X Y Z (поменьше — нужны файтингам)
        "md" -> listOf(dpad(),
            rb("a", "fl", "A", 66f, "r214 b40"), rb("b", "fb", "B", 66f, "r122 b58"), rb("c", "fr", "C", 66f, "r30 b76"),
            rb("x", "l1", "X", 46f, "r224 b126"), rb("y", "ft", "Y", 46f, "r132 b144"), rb("z", "r1", "Z", 46f, "r40 b162"),
            ib("select", "select", "select", "Mode", "c-24 t12"), ib("start", "start", "start", "Start", "c24 t12"), pauseB())
        "sms" -> listOf(dpad()) + two(Triple("b1", "fb", "1"), Triple("b2", "fr", "2")) +
            listOf(ib("start", "start", "start", "Pause", "c24 t12"), pauseB())
        "gg" -> listOf(dpad()) + two(Triple("b1", "fb", "1"), Triple("b2", "fr", "2")) +
            listOf(ib("start", "start", "start", "Start", "c24 t12"), pauseB())
        // Saturn: A B C снизу, X Y Z сверху, L и R — плечи
        "saturn" -> listOf(dpad(),
            rb("a", "fb", "A", 62f, "r214 b40", ROSE), rb("b", "fr", "B", 62f, "r128 b56", AMBER), rb("c", "l1", "C", 62f, "r42 b72", GREEN),
            rb("x", "fl", "X", 46f, "r222 b122", BLUE), rb("y", "ft", "Y", 46f, "r136 b138", GREY), rb("z", "r1", "Z", 46f, "r50 b154", GREY),
            shoulder("l2", "l2", "L", "l16 t14"), shoulder("r2", "r2", "R", "r16 t14"),
            ib("start", "start", "start", "Start", "c0 t12"), pauseB())
        // Dreamcast: стик, крестовина, A B X Y ромбом, курки L и R
        "dc" -> listOf(shoulder("l2", "l2", "L", "l16 t14"), shoulder("r2", "r2", "R", "r16 t14"),
            Btn("dpad", "dpad", 80f, "l22 t84", dpad = true), Btn("ls", null, 128f, "l80 b46", stick = "float"),
            rb("y", "ft", "Y", 58f, "r96 b172", GREEN), rb("b", "fr", "B", 58f, "r34 b112", BLUE),
            rb("a", "fb", "A", 58f, "r96 b52", ROSE), rb("x", "fl", "X", 58f, "r158 b112", AMBER),
            ib("start", "start", "start", "Start", "c0 t12"), pauseB())
        // PC Engine: II слева, I справа (как на джойстике), переключатели турбо над ними
        "pce" -> listOf(dpad()) + two(Triple("b2", "fb", "II"), Triple("b1", "fr", "I")) + listOf(
            Btn("t2", "turbo", 46f, "r150 b136", key = "l2", cap = "Турбо II"), Btn("t1", "turbo", 46f, "r52 b168", key = "r2", cap = "Турбо I"),
            ib("select", "select", "select", "Select", "c-24 t12"), ib("start", "start", "start", "Run", "c24 t12"), pauseB())
        // Neo Geo: четыре кнопки дугой, цвета как на джойстике
        "neogeo" -> listOf(dpad(),
            rb("a", "fb", "A", 64f, "r250 b34", ROSE), rb("b", "fr", "B", 64f, "r170 b52", AMBER),
            rb("c", "fl", "C", 64f, "r96 b84", GREEN), rb("d", "ft", "D", 64f, "r28 b124", BLUE),
            ib("select", "coin", "select", "Монетка", "c-24 t12"), ib("start", "start", "start", "Start", "c24 t12"), pauseB())
        "ngp" -> listOf(dpad()) + two(Triple("a", "fb", "A"), Triple("b", "fr", "B")) +
            listOf(ib("start", "start", "start", "Option", "c24 t12"), pauseB())
        // WonderSwan: крестовина — X1–X4, Y1–Y4 нужны вертикальным играм — в панели «⋯»
        "ws" -> listOf(dpad()) + two(Triple("b", "fb", "B"), Triple("a", "fr", "A")) + listOf(
            ib("select", "rotate", "select", "Поворот", "c-24 t12"), ib("start", "start", "start", "Start", "c24 t12"),
            rb("y1", "r2", "Y1", 40f, "c0 t0", more = true), rb("y2", "r1", "Y2", 40f, "c0 t0", more = true),
            rb("y3", "l2", "Y3", 40f, "c0 t0", more = true), rb("y4", "l1", "Y4", 40f, "c0 t0", more = true),
            moreBtn(), pauseB())
        // Atari 2600: джойстик и одна кнопка огня; переключатели сложности — в «⋯»
        "a26" -> listOf(dpad(), Btn("fire", null, 92f, "r56 b48", ROSE, key = "fb", cap = "Огонь"),
            ib("select", "select", "select", "Select", "c-24 t12"), ib("start", "reset", "start", "Reset", "c24 t12"),
            rb("da", "l1", "A", 40f, "c0 t0", more = true, cap = "Сложность A"), rb("db", "l2", "B", 40f, "c0 t0", more = true, cap = "Сложность B"),
            moreBtn(), pauseB())
        "a78" -> listOf(dpad()) + two(Triple("b1", "fb", "1"), Triple("b2", "fr", "2"), ROSE) + listOf(
            ib("select", "select", "select", "Select", "c-48 t12"), ib("start", "start", "start", "Pause", "c0 t12"),
            ib("reset", "reset", "ft", "Reset", "c48 t12"), pauseB())
        "lynx" -> listOf(dpad()) + two(Triple("b", "fb", "B"), Triple("a", "fr", "A")) + listOf(
            rb("o1", "l1", "1", 44f, "r150 b136", cap = "Option 1"), rb("o2", "r1", "2", 44f, "r52 b168", cap = "Option 2"),
            ib("start", "start", "start", "Pause", "c24 t12"), pauseB())
        else -> null
    }

    /** mode: touch | classic; plat: psp | ps1 | ps2 | ретро-консоли. */
    fun layout(mode: String, plat: String): List<Btn> {
        // у ретро-консолей один свой джойстик — и в «Классике», и в Touch
        pad(plat)?.let { return it }
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
