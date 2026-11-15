package com.nexus.psp.emulator

import org.json.JSONObject

/** Строка таблицы гайда: значок, действие, какие кнопки консоли нажимает, пояснение. */
data class GuideRow(val icon: String?, val name: String, val keys: List<String>, val how: String? = null, val rare: Boolean = false)

/** Раскладка в гайде: её кнопки (для превью), строки таблицы и 2–3 главные подсказки. */
data class GuideMode(val spec: LaySpec, val btns: List<Btn>, val rows: List<GuideRow>, val tips: List<Pair<String, String>>)

/**
 * Гайд по управлению: что делает каждая экранная кнопка. Собирается из тех же раскладок, что в игре
 * (свои настройки кнопок игры → встроенная раскладка из базы → обычный геймпад), поэтому всегда совпадает с игрой.
 */
object Guide {
    private val SKIP_ACT = setOf("pause", "save", "load", "ff", "swap", "more")

    /** Раскладки из настроек кнопок (ctl). */
    fun modes(ctl: JSONObject?, plat: String): List<GuideMode> {
        val a = ctl?.optJSONArray("layouts")
        val specs = if (a != null && a.length() > 0) (0 until a.length()).map { i -> a.getJSONObject(i).let { LaySpec(it.getString("id"), it.optString("name", "Раскладка"), it.optString("base", "touch")) } }
            else listOf(LaySpec("touch", "Основная", "touch"))
        return specs.map { sp ->
            val btns = Templates.build(sp.base, plat)
            val rows = rows(btns, ctl, "${sp.id}:$plat").let { r -> if (plat == "nes") r.map { it.copy(keys = it.keys.map { k -> NES[k] ?: k }) } else r }
            GuideMode(sp, btns, rows, tips(btns, specs, ctl, "${sp.id}:$plat"))
        }
    }

    private fun str(o: JSONObject?, map: String, key: String): String? = o?.optJSONObject(map)?.optString(key)?.takeIf { it.isNotEmpty() }

    /** Название действия кнопки: свой значок → своя подпись → подпись по значку → кнопка консоли. */
    fun name(b: Btn, ctl: JSONObject? = null, lay: String = ""): String {
        str(ctl, "keyIcon", "$lay:${b.id}")?.let { ic -> CtlIcons.CAPS[ic]?.let { return it } }
        return b.cap ?: b.icon?.let { CtlIcons.CAPS[it] } ?: when {
            b.dpad -> "Крестовина"
            b.stick != null -> if (b.right) "Правый стик" else "Движение"
            else -> KEY_NAMES[b.key ?: b.id] ?: (b.key ?: b.id)
        }
    }

    private fun rows(btns: List<Btn>, ctl: JSONObject?, lay: String): List<GuideRow> = btns.mapNotNull { b ->
        if (ctl?.optJSONObject("keyHide")?.optBoolean("$lay:${b.id}") == true) return@mapNotNull null
        if (b.cam != null || (b.act != null && b.act in SKIP_ACT)) return@mapNotNull null
        val icon = str(ctl, "keyIcon", "$lay:${b.id}") ?: b.icon
        when {
            b.stick != null -> GuideRow(null, if (b.right) "Правый стик" else "Движение", emptyList(), if (b.stick == "float") "коснись левой половины экрана" else null)
            b.dpad -> GuideRow("dpad", "Крестовина", listOf("up", "down", "left", "right"))
            b.act == "tilt" -> GuideRow("tilt", "Руль наклоном", emptyList(), "наклоняй телефон")
            b.act?.startsWith("latch:") == true -> GuideRow(icon, name(b, ctl, lay), listOf(b.act.substringAfter(':')), "касание — вкл., ещё раз — выкл.")
            else -> {
                val keys = str(ctl, "keyMap", "$lay:${b.id}")?.let { listOf(it) } ?: b.keys ?: listOfNotNull(b.key ?: b.id.takeIf { ControlLayout.KEY.containsKey(it) })
                val how = when {
                    b.to != null -> if (b.hold) "пока держишь — другая раскладка" else "переключает раскладку"
                    b.keys != null && b.combo -> "по очереди"
                    b.keys != null && b.keys.size > 1 -> "одним касанием"
                    else -> null
                }
                GuideRow(icon, name(b, ctl, lay), keys, how, b.more)
            }
        }
    }

    private fun tips(btns: List<Btn>, specs: List<LaySpec>, ctl: JSONObject?, lay: String): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        btns.firstOrNull { it.act?.startsWith("latch:") == true }?.let { b -> out += "sprint" to "«${name(b, ctl, lay)}» включается касанием — пока горит, обычные кнопки делают паркур" }
        if (btns.any { it.cam != null }) out += "cam" to "Свайп по правой половине — камера. Два касания — вернуть её за спину"
        btns.firstOrNull { it.keys != null && it.keys.size > 1 && !it.combo && it.to == null }?.let { b ->
            out += "both" to "«${name(b, ctl, lay)}» — одно касание вместо ${b.keys!!.joinToString(" + ") { KEY_NAMES[it] ?: it }}"
        }
        btns.firstOrNull { it.to != null }?.let { b -> specs.firstOrNull { it.id == b.to || it.base == b.to }?.let { t -> out += "layout" to "«${name(b, ctl, lay)}» — переключает на «${t.name}»" } }
        if (btns.any { it.act == "more" }) out += "more" to "⋯ — редкие действия: карта, меню и другое"
        if (out.size < 3 && btns.any { it.stick == "float" }) out += "stick" to "Стик появляется там, где коснулся левой половины экрана"
        return out.take(3)
    }

    /** Кнопки джойстика Денди (в ядре B — низ, A — право, турбо — X/Y). */
    private val NES = mapOf("fb" to "B", "fr" to "A", "fl" to "Турбо B", "ft" to "Турбо A")

    /** Как назвать кнопку консоли словами (в подсказках). */
    val KEY_NAMES = mapOf("ft" to "△", "fr" to "○", "fb" to "×", "fl" to "□", "l1" to "L", "r1" to "R", "l2" to "L2", "r2" to "R2",
        "select" to "Select", "start" to "Start", "up" to "↑", "down" to "↓", "left" to "←", "right" to "→", "steer_l" to "руль ←", "steer_r" to "руль →")
}
