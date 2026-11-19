package com.nexus.psp.data

import android.content.Context
import com.nexus.psp.emulator.NetPlay
import org.json.JSONObject

/**
 * «Позвать в игру»: игра по сети PSP (ad-hoc) в одной Wi-Fi-сети.
 * Хост: отправляет другу приглашение со своим адресом, у себя включает «комнату» и запускает игру.
 * Друг: получает приглашение в колокольчике, «Присоединиться» — та же игра в режиме подключения к адресу хоста.
 * Игра через интернет сюда не входит.
 */
object Invite {
    const val PREFIX = "@invite:"

    /** Игра поддерживает «Позвать в игру» (сейчас — только PSP ad-hoc). */
    fun supported(g: Game) = g.p == "psp"

    /** Режим сети для игры — применится при запуске (GameActivity читает gx.net). */
    fun setNet(c: Context, id: String, mode: String, ip: String = "") {
        GameConfig(c, id).edit { o ->
            val gx = o.optJSONObject("gx") ?: JSONObject()
            gx.put("net", JSONObject().put("mode", mode).put("ip", ip).put("inv", mode != "off")); o.put("gx", gx)
        }
    }

    /** После игры по приглашению — сеть снова выключена: следующий обычный запуск без подключения к старому адресу. */
    fun reset(c: Context, id: String) {
        val net = GameConfig(c, id).read().optJSONObject("gx")?.optJSONObject("net") ?: return
        if (net.optBoolean("inv")) setNet(c, id, "off")
    }

    /** Позвать друга: приглашение на сервер, у себя — комната, запуск игры. Ответ — текст ошибки или null. */
    fun send(c: Context, g: Game, to: String): String? {
        val ip = NetPlay.myIp(c) ?: return "Подключись к Wi-Fi — играть вместе можно только в одной сети"
        if (!NetPlay.validIp(ip)) return "Не вышло узнать адрес телефона в Wi-Fi"
        val (st, r) = Cloud.call(JSONObject().put("action", "invite").put("code", Cloud.code).put("to", to)
            .put("p", JSONObject().put("t", g.t.take(60)).put("p", g.p).put("ip", ip)))
        if (st == 200 && r?.optBoolean("ok") == true) { setNet(c, g.id, "host"); return null }
        return when (r?.optString("error")) {
            "not_friends" -> "Позвать можно только друга"
            "daily" -> "На сегодня приглашений хватит"
            "no_player" -> "Игрока с таким ID нет"
            "banned" -> "Аккаунт заблокирован"
            else -> if (st == 0) "Нет связи с сервером. Проверь интернет" else "Сервер не ответил. Попробуй позже"
        }
    }

    /** Метка уведомления в колокольчике: по ней «Присоединиться». */
    fun tag(data: JSONObject) = PREFIX + JSONObject().put("t", data.optString("t")).put("p", data.optString("p")).put("ip", data.optString("ip"))

    /** Своя игра для приглашения: та же консоль и то же название (без учёта регистра и знаков). */
    fun find(t: String, p: String): Game? {
        fun n(s: String) = s.lowercase().filter { it.isLetterOrDigit() }
        val want = n(t)
        return AppState.byId.values.firstOrNull { g -> g.p == p && !g.missing && (n(g.t) == want || AppState.meta[g.id]?.title?.let { n(it) } == want) }
    }

    /** Присоединиться по метке: найти игру, включить подключение к адресу друга и запустить. */
    fun join(c: Context, tag: String) {
        val o = try { JSONObject(tag.removePrefix(PREFIX)) } catch (e: Exception) { return }
        val ip = o.optString("ip"); val t = o.optString("t")
        if (!NetPlay.validIp(ip)) { AppState.say("Приглашение повреждено"); return }
        val g = find(t, o.optString("p")) ?: run { AppState.say("У тебя нет «$t» — добавь игру в библиотеку, чтобы играть вместе"); return }
        setNet(c, g.id, "join", ip)
        AppState.say("Подключаюсь к другу · одна Wi-Fi сеть")
        com.nexus.psp.ui.Act.play(c, g.id)
    }
}
