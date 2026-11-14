package com.nexus.psp.emulator

import com.nexus.psp.emulator.ControlLayout.AMBER
import com.nexus.psp.emulator.ControlLayout.BLUE
import com.nexus.psp.emulator.ControlLayout.GREEN
import com.nexus.psp.emulator.ControlLayout.ROSE
import com.nexus.psp.emulator.Templates.pause
import com.nexus.psp.emulator.Templates.stick

/**
 * Раскладки конкретных игр «как в мобильной игре»: кнопка = действие с подписью, сочетания кнопок консоли
 * собраны в одну экранную кнопку, камера — свайпом по правой половине, редкое — в «⋯».
 * Основа раскладки называется "g_<игра>_<режим>"; раскладки переключаются кнопками с to = id раскладки.
 *
 * Откуда схемы (кнопки консоли под каждым действием):
 *  - Assassin's Creed: Bloodlines — схема серии: △ голова (в Bloodlines — захват цели), ○ свободная рука, □ оружие,
 *    × ноги; R — «открытый» режим (бег, паркур, убийство) — в KAPSULA это переключатель «Паркур»;
 *    L + кнопки действий — камера (L нажимается первой), L — камеру назад.
 *  - Monster Hunter Freedom / Unite — △ и ○ атаки, × перекат, □ предмет, R блок/рывок, крестовина — камера, L — камера назад.
 *  - LocoRoco — L/R наклон мира, L+R прыжок, ○ разделить/собрать.
 *  - Patapon — □ Пата, ○ Пон, △ Чака, × Дон.
 *  - Kingdom Hearts: Birth by Sleep — × команда, ○ прыжок, □ уклон, △ действие, R цель, L+R прицельная атака, ↑/↓ выбор команды.
 *  - Dissidia — ○ удар храбрости, □ удар по HP, × прыжок, △ рывок, R блок, L цель.
 *  - WipEout Pure / Pulse — × газ, □ оружие, ○ поглотить оружие, L/R воздушные тормоза.
 *  - Mega Man (Maverick Hunter X / Powered Up) — □ выстрел, × прыжок, ○ рывок, L/R смена оружия.
 *  - Tekken — □ левая рука, △ правая рука, × левая нога, ○ правая нога.
 *  - God of War (Chains of Olympus, Ghost of Sparta) — □ лёгкий, △ сильный, ○ захват, × прыжок, R блок, L+R + стик — уклон.
 *  - Dynasty Warriors / Warriors Orochi — □ обычная атака, △ заряженная, ○ мусо, × прыжок, L защита.
 */
object GameLayouts {
    private fun b(id: String, icon: String, size: Float, pos: String, key: String? = null, keys: List<String>? = null, color: Long? = null,
                  cap: String? = null, to: String? = null, hold: Boolean = false, more: Boolean = false) =
        Btn(id, icon, size, pos, color, key = key, keys = keys, cap = cap, to = to, hold = hold, more = more)

    private fun more() = ControlLayout.moreBtn()
    private fun menu() = b("menu", "menu", 44f, "c0 t0", key = "start", more = true)
    private fun mapBtn() = b("map", "map", 44f, "c0 t0", key = "select", more = true)

    // главные места под большой палец (как дуга Touch): большая снизу, вокруг поменьше
    private const val P_MAIN = "r104 b26"   // большая, 74
    private const val P_RIGHT = "r44 b118"
    private const val P_TOP = "r126 b176"
    private const val P_LEFT = "r196 b84"

    fun build(base: String): List<Btn>? = when (base) {
        // ---------- Assassin's Creed: Bloodlines ----------
        // «Паркур» — переключатель открытого режима (R держится сам): пока включён, × — бег, прыжки и лазание,
        // □ — убийство, ○ — схватить. Ничего не нужно держать вторым пальцем.
        "g_ac_city" -> listOf(
            stick(),
            b("legs", "jump", 74f, P_MAIN, key = "fb", color = GREEN, cap = "Бег / прыжок"),
            b("hit", "knife", 56f, P_RIGHT, key = "fl", color = ROSE, cap = "Удар / убить"),
            Btn("hp", "sprint", 56f, P_LEFT, AMBER, act = "latch:r1", cap = "Паркур"),
            b("hand", "grab", 56f, P_TOP, key = "fr", color = BLUE, cap = "Рука"),
            b("lock", "lock", 46f, "r20 t72", key = "ft", cap = "Цель"),
            b("fight", "block", 46f, "r76 t72", to = "fight", cap = "Бой"),
            mapBtn(), menu(), more(), pause(),
            ControlLayout.camZone("u=l1+ft,d=l1+fb,l=l1+fl,r=l1+fr;tap=l1"),
        )
        "g_ac_fight" -> listOf(
            stick(),
            b("hit", "sword", 74f, P_MAIN, key = "fl", color = ROSE, cap = "Удар"),
            b("block", "block", 56f, P_RIGHT, key = "r1", color = BLUE, cap = "Блок"),
            b("counter", "special", 56f, P_TOP, keys = listOf("r1", "fl"), color = AMBER, cap = "Контратака"),
            b("dodge", "roll", 56f, P_LEFT, keys = listOf("r1", "fb"), color = GREEN, cap = "Уклон"),
            b("lock", "lock", 46f, "r20 t72", key = "ft", cap = "Цель"),
            b("city", "sprint", 46f, "r76 t72", to = "city", cap = "Город"),
            b("throw", "throw", 44f, "c0 t0", keys = listOf("r1", "fr"), cap = "Бросок", more = true),
            menu(), more(), pause(),
            ControlLayout.camZone("u=l1+ft,d=l1+fb,l=l1+fl,r=l1+fr;tap=l1"),
        )
        // ---------- Monster Hunter ----------
        "g_mh" -> listOf(
            stick(),
            b("atk", "sword", 74f, P_MAIN, key = "ft", color = ROSE, cap = "Атака"),
            b("atk2", "heavy", 56f, P_RIGHT, key = "fr", color = AMBER, cap = "Атака 2"),
            b("roll", "roll", 56f, P_LEFT, key = "fb", color = GREEN, cap = "Перекат"),
            b("item", "potion", 56f, P_TOP, key = "fl", color = BLUE, cap = "Предмет"),
            b("guard", "block", 50f, "r16 t12", key = "r1", cap = "Блок / рывок"),
            b("itl", "left", 40f, "r70 t84", keys = listOf("l1", "fl"), cap = "Пред. ◀"),
            b("itr", "right", 40f, "r20 t84", keys = listOf("l1", "fr"), cap = "Пред. ▶"),
            b("kick", "kick", 44f, "c0 t0", key = "select", cap = "Пинок", more = true),
            menu(), more(), pause(),
            ControlLayout.camZone("u=up,d=down,l=left,r=right;tap=l1"),
        )
        // ---------- LocoRoco ----------
        "g_loco" -> listOf(
            b("tl", "roll_l", 96f, "l30 b30", key = "l1", color = BLUE, cap = "Наклон"),
            b("tr", "roll_r", 96f, "r30 b30", key = "r1", color = BLUE, cap = "Наклон"),
            b("jump", "jump", 74f, "r150 b40", keys = listOf("l1", "r1"), color = GREEN, cap = "Прыжок"),
            b("split", "both", 56f, "r40 b170", key = "fr", color = AMBER, cap = "Разделить"),
            menu(), more(), pause(),
        )
        // ---------- Patapon ----------
        "g_patapon" -> listOf(
            b("pata", "fl", 74f, "l40 b40", key = "fl", color = ROSE, cap = "Пата"),
            b("pon", "fr", 74f, "r40 b40", key = "fr", color = BLUE, cap = "Пон"),
            b("chaka", "ft", 64f, "r150 b130", key = "ft", color = AMBER, cap = "Чака"),
            b("don", "fb", 64f, "l150 b130", key = "fb", color = GREEN, cap = "Дон"),
            b("l", "l1", 46f, "l16 t12", key = "l1"), b("r", "r1", 46f, "r16 t12", key = "r1"),
            menu(), more(), pause(),
        )
        // ---------- Kingdom Hearts: Birth by Sleep ----------
        "g_khbbs" -> listOf(
            stick(),
            b("cmd", "attack", 74f, P_MAIN, key = "fb", color = ROSE, cap = "Команда"),
            b("jump", "jump", 56f, P_RIGHT, key = "fr", color = GREEN, cap = "Прыжок"),
            b("dodge", "roll", 56f, P_LEFT, key = "fl", color = BLUE, cap = "Уклон"),
            b("act", "tap", 56f, P_TOP, key = "ft", color = AMBER, cap = "Действие"),
            b("lock", "lock", 48f, "r16 t12", key = "r1", cap = "Цель"),
            b("shot", "aim", 48f, "l16 t12", keys = listOf("l1", "r1"), cap = "Прицел"),
            b("cup", "up", 40f, "r20 t84", key = "up", cap = "Команда ▲"),
            b("cdn", "down", 40f, "r70 t84", key = "down", cap = "Команда ▼"),
            menu(), more(), pause(),
        )
        // ---------- Dissidia ----------
        "g_dissidia" -> listOf(
            stick(),
            b("brave", "attack", 74f, P_MAIN, key = "fr", color = ROSE, cap = "Храбрость"),
            b("hp", "heavy", 56f, P_RIGHT, key = "fl", color = AMBER, cap = "Удар HP"),
            b("jump", "jump", 56f, P_LEFT, key = "fb", color = GREEN, cap = "Прыжок"),
            b("dash", "dash", 56f, P_TOP, key = "ft", color = BLUE, cap = "Рывок"),
            b("block", "block", 50f, "r16 t12", key = "r1", cap = "Блок"),
            b("lock", "lock", 50f, "l16 t12", key = "l1", cap = "Цель"),
            menu(), more(), pause(),
        )
        // ---------- WipEout ----------
        "g_wipeout" -> listOf(
            stick(),
            b("gas", "gas", 84f, "r22 b24", key = "fb", color = GREEN, cap = "Газ"),
            b("fire", "missile", 62f, "r128 b30", key = "fl", color = ROSE, cap = "Оружие"),
            b("abs", "both", 52f, "r40 b130", key = "fr", color = AMBER, cap = "Поглотить"),
            b("abl", "airbrake", 56f, "l16 t12", key = "l1", color = BLUE, cap = "Тормоз ◀"),
            b("abr", "airbrake", 56f, "r16 t12", key = "r1", color = BLUE, cap = "Тормоз ▶"),
            b("view", "view", 44f, "c0 t0", key = "ft", cap = "Вид", more = true),
            menu(), more(), pause(),
        )
        // ---------- Mega Man ----------
        "g_megaman" -> listOf(
            Btn("dpad", "dpad", 112f, "l40 b40", dpad = true),
            b("shoot", "shoot", 74f, P_MAIN, key = "fl", color = ROSE, cap = "Выстрел"),
            b("jump", "jump", 64f, P_RIGHT, key = "fb", color = GREEN, cap = "Прыжок"),
            b("dash", "dash", 56f, P_LEFT, key = "fr", color = BLUE, cap = "Рывок"),
            b("wl", "swap", 46f, "l16 t12", key = "l1", cap = "Оружие ◀"),
            b("wr", "weapon", 46f, "r16 t12", key = "r1", cap = "Оружие ▶"),
            menu(), more(), pause(),
        )
        // ---------- Tekken ----------
        "g_tekken" -> listOf(
            Btn("dpad", "dpad", 112f, "l40 b40", dpad = true),
            b("lp", "jab", 60f, P_LEFT, key = "fl", color = ROSE, cap = "Л. рука"),
            b("rp", "heavy", 60f, P_TOP, key = "ft", color = AMBER, cap = "П. рука"),
            b("lk", "kick", 66f, P_MAIN, key = "fb", color = GREEN, cap = "Л. нога"),
            b("rk", "kick", 60f, P_RIGHT, key = "fr", color = BLUE, cap = "П. нога"),
            b("throw", "throw", 46f, "r16 t12", keys = listOf("fl", "fb"), cap = "Бросок"),
            b("rage", "special", 46f, "l16 t12", keys = listOf("ft", "fr"), cap = "Обе руки"),
            menu(), more(), pause(),
        )
        // ---------- God of War ----------
        "g_gow" -> listOf(
            stick(),
            b("light", "sword", 74f, P_MAIN, key = "fl", color = ROSE, cap = "Удар"),
            b("heavy", "heavy", 56f, P_TOP, key = "ft", color = AMBER, cap = "Сильный"),
            b("grab", "grab", 56f, P_RIGHT, key = "fr", color = BLUE, cap = "Схватить"),
            b("jump", "jump", 56f, P_LEFT, key = "fb", color = GREEN, cap = "Прыжок"),
            b("block", "block", 50f, "r16 t12", key = "r1", cap = "Блок"),
            b("evade", "roll", 50f, "r20 t84", keys = listOf("l1", "r1"), cap = "Уклон"),
            b("magic", "magic", 50f, "l16 t12", key = "l1", cap = "Магия"),
            menu(), more(), pause(),
        )
        // ---------- Dynasty Warriors / Warriors Orochi ----------
        "g_musou" -> listOf(
            stick(),
            b("atk", "sword", 74f, P_MAIN, key = "fl", color = ROSE, cap = "Удар"),
            b("chg", "heavy", 56f, P_TOP, key = "ft", color = AMBER, cap = "Заряд"),
            b("musou", "special", 56f, P_RIGHT, key = "fr", color = BLUE, cap = "Мусо"),
            b("jump", "jump", 56f, P_LEFT, key = "fb", color = GREEN, cap = "Прыжок"),
            b("guard", "block", 50f, "l16 t12", key = "l1", cap = "Защита"),
            b("r", "r1", 46f, "r16 t12", key = "r1"),
            menu(), more(), pause(),
        )
        else -> null
    }
}
