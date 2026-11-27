package com.nexus.psp.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

/**
 * Сейвик-питомец: четыре потребности 0..100 — сытость, бодрость, чистота, настроение. Со временем понемногу падают
 * (по реальным часам, даже когда KAPSULA закрыта), во сне бодрость растёт. Ничего не ломается, если забыть:
 * ниже 10 не опускается, а Сейвик просто грустит и просит внимания. Хранится локально (настройка pet).
 */
object Pet {
    var food = 80f; var energy = 80f; var clean = 80f; var fun_ = 80f
    var asleep = false
    private var at = 0L
    /** Для секретов: сколько раз играли, в какие дни заходили в уголок (последние 7). */
    private var plays = 0
    private var days = ""
    /** Перерисовать экран после изменения. */
    var tick by mutableIntStateOf(0)

    private const val MIN = 10f

    fun load() {
        val o = try { JSONObject(AppState.S.pet) } catch (e: Exception) { JSONObject() }
        food = o.optDouble("food", 80.0).toFloat(); energy = o.optDouble("energy", 80.0).toFloat()
        clean = o.optDouble("clean", 80.0).toFloat(); fun_ = o.optDouble("fun", 80.0).toFloat()
        asleep = o.optBoolean("asleep", false); at = o.optLong("at", System.currentTimeMillis())
        plays = o.optInt("plays", 0); days = o.optString("days", "")
        update()
    }

    private fun save() {
        AppState.S.pet = JSONObject().put("food", food).put("energy", energy).put("clean", clean).put("fun", fun_)
            .put("asleep", asleep).put("at", at).put("plays", plays).put("days", days).toString()
        tick++
    }

    /** Время прошло: потребности падают (за час — сытость −5, чистота −3, настроение −4, бодрость −3 или +20 во сне). */
    fun update(now: Long = System.currentTimeMillis()) {
        val h = ((now - at) / 3600_000f).coerceIn(0f, 24 * 14f)
        at = now
        if (h > 0f) {
            food = (food - 5 * h).coerceAtLeast(MIN); clean = (clean - 3 * h).coerceAtLeast(MIN); fun_ = (fun_ - 4 * h).coerceAtLeast(MIN)
            energy = if (asleep) (energy + 20 * h).coerceAtMost(100f) else (energy - 3 * h).coerceAtLeast(MIN)
            if (asleep && energy >= 100f) asleep = false // выспался — проснулся сам
        }
        save()
    }

    /** Зашли в уголок: день в копилку «Постоянного гостя». */
    fun visit() {
        val c = java.util.Calendar.getInstance()
        val d = "%d%03d".format(c.get(java.util.Calendar.YEAR), c.get(java.util.Calendar.DAY_OF_YEAR))
        val list = days.split(',').filter { it.isNotBlank() }
        if (d !in list) { days = (list + d).takeLast(7).joinToString(","); save() }
        if (days.split(',').size >= 5) Secrets.find("visits")
    }

    fun perfectCheck() { if (food >= 95 && energy >= 95 && clean >= 95 && fun_ >= 95) Secrets.find("perfect") }

    fun feed(): String { update(); if (food >= 100f) Secrets.find("glutton"); asleep = false; food = (food + 35).coerceAtMost(100f); fun_ = (fun_ + 5).coerceAtMost(100f); save(); return if (food >= 100f) "Объелся! Больше не влезет" else "Ням! Спасибо" }
    fun sleep(): String { update(); if (asleep) Secrets.find("alarm"); asleep = !asleep; save(); return if (asleep) "Спокойной ночи…" else "Я проснулся!" }
    fun wash(): String { update(); asleep = false; clean = 100f; fun_ = (fun_ + 5).coerceAtMost(100f); save(); return "Буль-буль! Как новенький" }
    fun play(): String {
        update(); asleep = false
        if (energy < 20f) { save(); return "Сил нет… Уложи меня поспать" }
        fun_ = (fun_ + 30).coerceAtMost(100f); energy = (energy - 8).coerceAtLeast(MIN); food = (food - 4).coerceAtLeast(MIN); plays++; save()
        if (plays >= 30) Secrets.find("bestie")
        return "Ещё! Ещё!"
    }

    /** Общее настроение 0..100 — среднее потребностей. */
    val mood get() = (food + energy + clean + fun_) / 4

    /** Поза по состоянию: спит, голоден, пыльный, скучает, зевает — или радуется. */
    fun pose(): String = when {
        asleep -> "bed"
        food < 30 -> "hungry"
        clean < 30 -> "dusty"
        fun_ < 30 -> "lonely"
        energy < 30 -> "yawn"
        mood > 85 -> "joy"
        else -> "idle"
    }

    /** Что сказать (на главной — когда чего-то не хватает). null — всё хорошо. */
    fun need(): String? = when {
        asleep -> null
        food < 30 -> "Я проголодался…"
        clean < 30 -> "Я весь в пыли, помой меня"
        fun_ < 30 -> "Мне скучно. Поиграем?"
        energy < 30 -> "Глаза слипаются…"
        else -> null
    }
}
