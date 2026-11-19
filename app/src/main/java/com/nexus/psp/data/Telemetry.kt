package com.nexus.psp.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Анонимная статистика: только счётчики «событие · значение», без названий игр и файлов.
 * Копится на телефоне и уходит на сервер пачкой при синхронизации (не чаще раза в 10 минут).
 * На сервере — суммы по дням без привязки к игроку. Выключатель — «Настройки → Общие».
 */
object Telemetry {
    private lateinit var sp: SharedPreferences
    private const val MIN_GAP = 10 * 60_000L

    fun init(c: Context) { if (!::sp.isInitialized) sp = c.getSharedPreferences("kapsula_stats", Context.MODE_PRIVATE) }

    private val on get() = ::sp.isInitialized && (try { AppState.S.stats } catch (e: Exception) { true })

    /** Посчитать событие. Пишется в фоне — дёшево, можно звать часто. */
    @Synchronized fun count(kind: String, k: String = "", n: Int = 1) {
        if (!on || n <= 0) return
        val key = "$kind|$k"
        sp.edit().putInt(key, (sp.getInt(key, 0) + n).coerceAtMost(500)).apply()
    }

    /** Накопленное (для отправки и для тестов). */
    @Synchronized fun pending(): JSONArray = JSONArray().apply {
        if (!::sp.isInitialized) return@apply
        sp.all.forEach { (key, v) ->
            if (key.startsWith("_") || v !is Int || v <= 0) return@forEach
            val (kind, k) = key.split("|", limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            put(JSONObject().put("kind", kind).put("k", k).put("n", v))
        }
    }

    @Synchronized fun clear() {
        if (!::sp.isInitialized) return
        val last = sp.getLong("_sent", 0L)
        sp.edit().clear().putLong("_sent", last).apply()
    }

    /** Пора отправлять: включено, есть что, прошло 10 минут. */
    fun due(now: Long = System.currentTimeMillis()): Boolean = on && now - sp.getLong("_sent", 0L) >= MIN_GAP && pending().length() > 0

    /** Отправлено (или сервер сказал «рано») — отметить время; ok — очистить счётчики. */
    @Synchronized fun sent(ok: Boolean, now: Long = System.currentTimeMillis()) {
        if (!::sp.isInitialized) return
        if (ok) clear()
        sp.edit().putLong("_sent", now).apply()
    }

    /** Длительность сессии → корзина. */
    fun bucket(min: Int): String = when { min < 5 -> "0-5"; min < 30 -> "5-30"; min < 120 -> "30-120"; else -> "120+" }

    /** Причина отказа проводника → короткий код (без текста и путей). */
    fun failCode(why: String?): String = when {
        why == null || why == Storage.NOT_SUPPORTED -> "format"
        why.contains("текстовый") -> "text"
        why.contains("маленький") -> "small"
        why.contains("метка") -> "nomark"
        else -> "unread"
    }
}
