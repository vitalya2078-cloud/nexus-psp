package com.nexus.psp.emulator

import com.nexus.psp.emulator.ControlLayout.AMBER
import com.nexus.psp.emulator.ControlLayout.BLUE
import com.nexus.psp.emulator.ControlLayout.GREEN
import com.nexus.psp.emulator.ControlLayout.ROSE

/** Раскладка игры: id (ключ сохранённых мест и значков), имя для игрока, основа-шаблон. */
data class LaySpec(val id: String, val name: String, val base: String)

/** Жанр: набор раскладок, с которых начинается игра этого жанра. */
data class Genre(val id: String, val name: String, val icon: String, val layouts: List<LaySpec>)

/**
 * Шаблоны раскладок KAPSULA Touch по жанрам. Кнопки — по самой частой схеме PSP:
 * × (fb) — главное действие / газ, □ (fl) — второе / тормоз, ○ (fr) — атака, △ (ft) — взаимодействие.
 * «Пешком / Транспорт / Полёт» сверены с управлением GTA: Vice City Stories (PSP, раскладка 1, WikiGTA).
 */
object Templates {
    val GENRES = listOf(
        Genre("action", "Экшен", "car", listOf(LaySpec("foot", "Пешком", "foot"), LaySpec("drive", "Транспорт", "drive"), LaySpec("heli", "Полёт", "heli"))),
        Genre("race", "Гонки", "wheel", listOf(LaySpec("race", "Гонка", "race"))),
        Genre("shooter", "Шутер", "aim", listOf(LaySpec("shoot", "Обычная", "shoot"), LaySpec("aim", "Прицел", "aim"))),
        Genre("fight", "Файтинг", "jab", listOf(LaySpec("fight", "Бой", "fight"))),
        Genre("slasher", "Слэшер", "sword", listOf(LaySpec("slash", "Бой", "slash"))),
        Genre("platform", "Платформер", "jump", listOf(LaySpec("platform", "Основная", "platform"))),
        Genre("rpg", "RPG", "skill", listOf(LaySpec("rpg", "Основная", "rpg"))),
        Genre("sport", "Спорт", "goal", listOf(LaySpec("sport", "Матч", "sport"))),
        Genre("flight", "Полёты", "jet", listOf(LaySpec("flight", "Полёт", "flight"))),
        Genre("pad", "Джойстик", "fb", listOf(LaySpec("touch", "Основная", "touch"))),
    )
    fun genre(id: String) = GENRES.firstOrNull { it.id == id }

    /** Все основы, из которых можно добавить раскладку в редакторе. */
    val BASES = listOf(
        "touch" to "Джойстик", "foot" to "Пешком", "drive" to "Транспорт", "heli" to "Вертолёт", "race" to "Гонка", "shoot" to "Шутер",
        "aim" to "Прицел", "fight" to "Файтинг", "slash" to "Слэшер", "platform" to "Платформер", "rpg" to "RPG", "sport" to "Спорт", "flight" to "Самолёт",
    )

    fun swapBtn() = Btn("swap", "layout", 40f, "c46 b14", act = "swap")
    private fun stick() = Btn("ls", null, 128f, "l80 b46", stick = "float")
    private fun pause() = Btn("pause", "pause", 40f, "c0 b14", act = "pause")
    private fun small(id: String, icon: String, pos: String, key: String, size: Float = 44f, to: String? = null, hold: Boolean = false, color: Long? = null) =
        Btn(id, icon, size, pos, color, key = key, to = to, hold = hold)

    /** Кнопки действий дугой под большой палец (как в Touch): значки и клавиши по порядку верх/право/низ/лево. */
    private fun arc(top: Pair<String, String>, right: Pair<String, String>, bottom: Pair<String, String>, left: Pair<String, String>,
                    to: Map<String, String> = emptyMap()): List<Btn> {
        val ics = listOf(top, right, bottom, left)
        return ControlLayout.faceArc().mapIndexed { i, b ->
            val (icon, key) = ics[i]
            b.copy(id = "a${b.id}", icon = icon, key = key, to = to[b.id])
        }
    }

    /** Кнопки раскладки по основе. plat — консоль (для «Джойстика» и «Классики»). */
    fun build(base: String, plat: String): List<Btn> = when (base) {
        "classic" -> ControlLayout.layout("classic", plat)
        // GTA VCS: × бег, □ прыжок, ○ атака, △ сесть, L прицел/смотреть, R цель, ←/→ оружие, Select камера, Start меню
        "foot" -> listOf(small("l1", "aim", "l16 t12", "l1", 52f), small("r1", "lock", "r16 t12", "r1", 52f), stick()) +
            arc("car" to "ft", "attack" to "fr", "sprint" to "fb", "jump" to "fl", to = mapOf("ft" to "drive")) +
            listOf(small("wpn", "weapon", "r20 t72", "right", 46f), small("wpl", "swap", "r74 t76", "left", 38f),
                small("menu", "menu", "l20 t72", "start", 40f), small("cam", "camreset", "l20 t120", "select", 40f), pause())
        // в машине: × газ, □ тормоз/назад, △ выйти, ○ атака, L+R ручник, ↓+R сигнал, стик — руль
        "drive" -> listOf(stick(),
            Btn("gas", "gas", 84f, "r22 b24", GREEN, key = "fb"), Btn("brk", "brake", 74f, "r118 b24", ROSE, key = "fl"),
            Btn("hb", "hbrake", 58f, "r136 b110", AMBER, keys = listOf("l1", "r1")), Btn("atk", "attack", 52f, "r40 b122", BLUE, key = "fr"),
            small("exit", "car", "r20 t72", "ft", 46f, to = "foot"), Btn("horn", "horn", 44f, "r20 t124", keys = listOf("down", "r1")),
            small("look", "lookback", "r76 t72", "l1", 44f), small("radio", "right", "r76 t124", "right", 40f), small("cam", "camreset", "l20 t72", "select", 40f), pause())
        // вертолёт/самолёт GTA: × вверх, □ вниз, △ выйти, ○ атака, L/R поворот
        "heli" -> listOf(stick(),
            Btn("up", "thr_up", 74f, "r22 b30", GREEN, key = "fb"), Btn("dn", "thr_down", 66f, "r112 b24", ROSE, key = "fl"),
            Btn("atk", "missile", 56f, "r40 b122", BLUE, key = "fr"), small("exit", "heli", "r20 t72", "ft", 46f, to = "foot"),
            small("yl", "yaw_l", "l16 t12", "l1", 52f), small("yr", "yaw_r", "r16 t12", "r1", 52f), small("cam", "camreset", "l20 t72", "select", 40f), pause())
        "race" -> listOf(stick(),
            Btn("gas", "gas", 84f, "r22 b24", GREEN, key = "fb"), Btn("brk", "brake", 74f, "r118 b24", ROSE, key = "fl"),
            Btn("nos", "nitro", 56f, "r40 b122", BLUE, key = "fr"), Btn("hb", "hbrake", 58f, "r136 b110", AMBER, key = "r1"),
            small("view", "view", "r20 t72", "ft", 44f), small("look", "lookback", "l16 t12", "l1", 48f), small("menu", "menu", "l20 t72", "start", 40f), pause())
        // шутер: L — прицел (пока держишь), R — огонь
        "shoot" -> listOf(stick(), small("aim", "aim", "l16 t12", "l1", 56f, to = "aim", hold = true), small("fire", "shoot", "r16 t12", "r1", 56f)) +
            arc("weapon" to "ft", "crouch" to "fr", "roll" to "fb", "reload" to "fl") +
            listOf(small("grn", "grenade", "r20 t76", "up", 42f), small("menu", "menu", "l20 t72", "start", 40f), pause())
        "aim" -> listOf(stick(), small("aim", "aim", "l16 t12", "l1", 56f, to = "aim", hold = true),
            Btn("fire", "shoot", 92f, "r30 b40", ROSE, key = "r1"), Btn("rld", "reload", 54f, "r150 b30", AMBER, key = "fl"),
            Btn("wpn", "weapon", 50f, "r40 b150", BLUE, key = "ft"), Btn("zoom", "zoom", 46f, "r150 b110", key = "up"), pause())
        // файтинг: крестовина, □ △ × ○ — удары руками и ногами (как в Tekken), L/R — блок и приём
        "fight" -> listOf(Btn("dpad", "dpad", 112f, "l40 b40", dpad = true)) +
            arc("heavy" to "ft", "kick" to "fr", "kick" to "fb", "jab" to "fl") +
            listOf(small("blk", "block", "l16 t12", "l1", 52f), small("spc", "special", "r16 t12", "r1", 52f), small("menu", "menu", "l20 t72", "start", 40f), pause())
        "slash" -> listOf(stick()) + arc("heavy" to "ft", "grab" to "fr", "jump" to "fb", "sword" to "fl") +
            listOf(small("blk", "block", "l16 t12", "l1", 52f), small("mag", "magic", "r16 t12", "r1", 52f), small("menu", "menu", "l20 t72", "start", 40f), pause())
        "platform" -> listOf(stick()) + arc("skill" to "ft", "tap" to "fr", "jump" to "fb", "attack" to "fl") +
            listOf(small("cam", "camreset", "l16 t12", "l1", 48f), small("crh", "crouch", "r16 t12", "r1", 48f), small("menu", "menu", "l20 t72", "start", 40f), pause())
        "rpg" -> listOf(stick()) + arc("menu" to "ft", "back" to "fr", "tap" to "fb", "skill" to "fl") +
            listOf(small("l1", "l1", "l16 t12", "l1", 48f), small("r1", "r1", "r16 t12", "r1", 48f), small("map", "map", "l20 t72", "select", 40f),
                small("bag", "bag", "l20 t120", "start", 40f), pause())
        // спорт (PES): × пас, □ навес, ○ удар, △ пас на ход, R ускорение, L смена игрока
        "sport" -> listOf(stick()) + arc("dash" to "ft", "goal" to "fr", "pass" to "fb", "point" to "fl") +
            listOf(small("spr", "sprint", "r16 t12", "r1", 52f), small("ply", "players", "l16 t12", "l1", 52f), small("menu", "menu", "l20 t72", "start", 40f), pause())
        "flight" -> listOf(stick(),
            Btn("msl", "missile", 80f, "r26 b30", ROSE, key = "fr"), Btn("gun", "mgun", 62f, "r120 b24", AMBER, key = "fb"),
            Btn("tgt", "tgt_switch", 52f, "r40 b128", BLUE, key = "ft"), Btn("spc", "bombs", 46f, "r130 b110", key = "fl"),
            small("up", "thr_up", "r16 t12", "r1", 52f), small("dn", "thr_down", "l16 t12", "l1", 52f), small("cam", "cockpit", "l20 t72", "select", 40f), pause())
        else -> ControlLayout.layout("touch", plat)
    }

    /** Раскладки жанра в виде JSON — для «Применить шаблон» и встроенных пресетов. */
    fun layoutsJson(g: Genre): org.json.JSONArray = org.json.JSONArray().also { a -> g.layouts.forEach { a.put(org.json.JSONObject().put("id", it.id).put("name", it.name).put("base", it.base)) } }
}
