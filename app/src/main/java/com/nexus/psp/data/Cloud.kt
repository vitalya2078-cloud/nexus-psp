package com.nexus.psp.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Аккаунт без регистрации на нашем сервере (Supabase, функция kapsula).
 * Вход — код восстановления KPS-XXXX-XXXX-XXXX, на сервере хранится только его хэш. Личных данных нет.
 * На сервере: жетоны и их история, купленное оформление, профиль (имя, девиз, аватар, рамка, баннер, титул),
 * статистика (минуты всего и по консолям, сохранения, игры, серия дней, уровень).
 * Без сети всё работает как раньше: изменения копятся и уходят при следующей синхронизации;
 * жетоны считает сервер по приросту — с теми же правилами и дневными лимитами.
 */
object Cloud {
    private const val TAG = "KapsulaCloud"
    const val URL_FN = "https://borubfgvjhntepnijxxo.supabase.co/functions/v1/kapsula"
    private lateinit var sp: SharedPreferences

    /** Код восстановления (пусто — аккаунта ещё нет). */
    var code by mutableStateOf("")
        private set
    /** Статистика с сервера (на новом телефоне — больше, чем на нём наиграно). */
    var server: JSONObject? = null
        private set
    var lastOk by mutableStateOf(0L)
        private set

    fun init(c: Context) {
        sp = c.getSharedPreferences("kapsula_cloud", Context.MODE_PRIVATE)
        code = sp.getString("code", "") ?: ""
        server = sp.getString("server", null)?.let { try { JSONObject(it) } catch (e: Exception) { null } }
        lastOk = sp.getLong("lastOk", 0L)
    }
    private val ready get() = ::sp.isInitialized

    // ---------- что считается на телефоне ----------
    private fun localPlay() = AppState.lib.values.sumOf { it.play }
    private fun localSaves() = AppState.lib.values.sumOf { it.saves }
    private fun localCons(): Map<String, Int> = AppState.lib.entries.mapNotNull { (id, l) -> AppState.byId[id]?.p?.let { it to l.play } }
        .groupBy({ it.first }, { it.second }).mapValues { it.value.sum() }.filterValues { it > 0 }

    /** Запомнить «уже отправлено» — дальше на сервер уходит только прирост. */
    private fun snapshot() {
        sp.edit().putInt("sPlay", localPlay()).putInt("sSaves", localSaves()).putString("sCons", JSONObject(localCons()).toString()).apply()
    }

    private fun profile() = JSONObject().apply {
        val S = AppState.S
        put("nick", S.nick); put("motto", S.motto); put("avMode", S.avMode); put("avItem", S.avItem); put("avColor", S.avColor)
        put("frame", S.frame); put("banner", S.banner); put("titleSel", S.titleSel)
    }

    // ---------- покупки без сети ----------
    fun pendingBuy(id: String) { if (ready) sp.edit().putStringSet("buys", (sp.getStringSet("buys", emptySet())!! + id)).apply() }

    // ---------- запросы ----------
    private fun call(body: JSONObject): Pair<Int, JSONObject?> = try {
        val c = URL(URL_FN).openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.doOutput = true; c.connectTimeout = 10_000; c.readTimeout = 15_000
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        c.outputStream.use { it.write(body.toString().toByteArray()) }
        val st = c.responseCode
        val txt = (if (st in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.readText() ?: ""
        st to (try { JSONObject(txt) } catch (e: Exception) { null })
    } catch (e: Exception) { Log.w(TAG, "сеть: $e"); 0 to null }

    /** Применить состояние с сервера: жетоны, купленное, статистика. profile=true — ещё и оформление профиля (при восстановлении). */
    private fun apply(state: JSONObject, profile: Boolean) {
        val owned = state.optJSONArray("owned")?.let { a -> (0 until a.length()).map { a.getString(it) }.toSet() } ?: emptySet()
        Tokens.fromServer(state.optInt("balance"), owned, state.optInt("visit_streak"))
        state.optJSONObject("stats")?.let { server = it; sp.edit().putString("server", it.toString()).apply() }
        if (profile) state.optJSONObject("profile")?.let { p ->
            val S = AppState.S
            p.optString("nick").takeIf { it.isNotEmpty() }?.let { S.nick = it }
            p.optString("motto").takeIf { it.isNotEmpty() }?.let { S.motto = it }
            p.optString("avMode").takeIf { it == "set" || it == "letter" }?.let { S.avMode = it }
            p.optString("avItem").takeIf { it.isNotEmpty() }?.let { S.avItem = it }
            if (p.has("avColor")) S.avColor = p.optInt("avColor")
            S.frame = p.optString("frame"); S.banner = p.optString("banner"); S.titleSel = p.optString("titleSel")
        }
        lastOk = System.currentTimeMillis(); sp.edit().putLong("lastOk", lastOk).apply()
    }

    private fun got(state: JSONObject): List<Pair<String, Int>> = state.optJSONArray("got")?.let { a ->
        (0 until a.length()).map { a.getJSONObject(it).let { e -> e.optString("reason") to e.optInt("amount") } }
    } ?: emptyList()

    /**
     * Синхронизация (в фоне, при каждом возвращении в приложение). Нет аккаунта — создаём (с исходной статистикой телефона).
     * Ответ: начисленное сервером (для уведомления); null — сервер недоступен (тогда жетоны считаются на телефоне).
     */
    @Synchronized fun sync(): List<Pair<String, Int>>? {
        if (!ready) return null
        if (code.isEmpty()) {
            val base = JSONObject().put("play", localPlay()).put("saves", localSaves()).put("games", AppState.byId.size)
                .put("streak", AppState.streak()).put("cons", JSONObject(localCons()))
                // жетоны и купленное до появления аккаунта (0.38) — переносятся один раз
                .put("balance", Tokens.balance).put("owned", JSONArray(Tokens.owned.toList()))
            val (st, r) = call(JSONObject().put("action", "register").put("base", base))
            val c = r?.optString("code").orEmpty(); val state = r?.optJSONObject("state")
            if (st != 200 || c.isEmpty() || state == null) return null
            code = c; sp.edit().putString("code", c).apply()
            snapshot()
            apply(state, profile = false)
            AppState.alerts.add(0, Alert("Твой код восстановления: $c", "Сохрани его — по нему вернутся жетоны и оформление на новом телефоне. Он есть в «Настройки → Общие»", true))
            AppState.save()
            // сразу отправить оформление профиля и покупки, сделанные до регистрации
            return got(state) + (push() ?: emptyList())
        }
        return push()
    }

    private fun push(): List<Pair<String, Int>>? {
        val sPlay = sp.getInt("sPlay", 0); val sSaves = sp.getInt("sSaves", 0)
        val sCons = try { JSONObject(sp.getString("sCons", "{}")!!) } catch (e: Exception) { JSONObject() }
        val cons = JSONObject(); localCons().forEach { (k, v) -> val d = v - sCons.optInt(k, 0); if (d > 0) cons.put(k, d) }
        val buys = sp.getStringSet("buys", emptySet())!!
        val p = JSONObject().put("d_play", (localPlay() - sPlay).coerceAtLeast(0)).put("d_saves", (localSaves() - sSaves).coerceAtLeast(0))
            .put("games", AppState.byId.size).put("streak", AppState.streak()).put("cons", cons).put("profile", profile()).put("buys", JSONArray(buys.toList()))
        val (st, r) = call(JSONObject().put("action", "sync").put("code", code).put("p", p))
        val state = r?.optJSONObject("state")
        if (st != 200 || state == null) { if (st == 404) Log.w(TAG, "код не найден на сервере"); return null }
        snapshot()
        sp.edit().remove("buys").apply()
        apply(state, profile = false)
        return got(state)
    }

    /** Восстановить по коду (на новом телефоне). Ответ: null — получилось, иначе понятная причина. */
    @Synchronized fun restore(input: String): String? {
        if (!ready) return "Подожди секунду и попробуй ещё раз"
        val (st, r) = call(JSONObject().put("action", "restore").put("code", input.trim()))
        val state = r?.optJSONObject("state")
        return when {
            st == 200 && state != null -> {
                val c = input.trim().uppercase().replace(Regex("[^A-Z0-9]"), "").removePrefix("KPS").let { "KPS-${it.take(4)}-${it.drop(4).take(4)}-${it.drop(8).take(4)}" }
                code = c; sp.edit().putString("code", c).remove("buys").apply()
                snapshot() // наигранное на этом телефоне до восстановления не переносится в чужой аккаунт
                apply(state, profile = true)
                null
            }
            st == 400 -> "Код не похож на код KAPSULA. Он выглядит так: KPS-XXXX-XXXX-XXXX"
            st == 404 -> "Такого кода нет. Проверь буквы и цифры"
            st == 0 -> "Нет связи с сервером. Проверь интернет"
            else -> "Сервер не ответил. Попробуй позже"
        }
    }

    /** Число из статистики сервера (0 — нет). */
    fun stat(key: String): Int = server?.optInt(key) ?: 0
    fun consoleMinutes(p: String): Int = server?.optJSONObject("consoles")?.optInt(p) ?: 0
    fun consoles(): Set<String> = server?.optJSONObject("consoles")?.let { o -> o.keys().asSequence().filter { o.optInt(it) > 0 }.toSet() } ?: emptySet()
}
