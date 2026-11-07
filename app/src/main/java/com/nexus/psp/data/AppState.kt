package com.nexus.psp.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nexus.psp.emulator.GameLibrary
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.T
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.properties.ReadWriteProperty
import kotlin.random.Random
import kotlin.reflect.KProperty

/** Запись библиотеки: s — ok (установлена) / own (в библиотеке) / q (очередь) / dl (качается) / pause. */
data class Lib(val s: String, val pr: Float = 0f, val play: Int = 0, val last: String = "Ещё не запускалась", val saves: Int = 0, val t: Long = System.currentTimeMillis())

data class Alert(val t: String, val d: String, val u: Boolean, val g: String? = null)

data class Toast(val text: String, val icon: Int? = null, val action: String? = null, val onAction: (() -> Unit)? = null, val id: Long = System.nanoTime())

/** Единый статус игры (п. 13). */
data class Stat(val k: String, val lbl: String)

/** Настройки — каждое поле наблюдаемо Compose и сразу пишется в SharedPreferences. */
class Settings(private val sp: SharedPreferences) {
    private fun <T> pref(key: String, def: T, read: (String, T) -> T, write: SharedPreferences.Editor.(String, T) -> Unit) =
        object : ReadWriteProperty<Any?, T> {
            var st = mutableStateOf(read(key, def))
            override fun getValue(thisRef: Any?, property: KProperty<*>): T = st.value
            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) { st.value = value; sp.edit().apply { write(key, value) }.apply() }
        }
    private fun b(k: String, d: Boolean) = pref(k, d, { kk, dd -> sp.getBoolean(kk, dd) }, { kk, v -> putBoolean(kk, v) })
    private fun s(k: String, d: String) = pref(k, d, { kk, dd -> sp.getString(kk, dd) ?: dd }, { kk, v -> putString(kk, v) })
    private fun i(k: String, d: Int) = pref(k, d, { kk, dd -> sp.getInt(kk, dd) }, { kk, v -> putInt(kk, v) })

    var con by s("con", "all")
    var theme by s("theme", "dark")
    var big by b("big", false)
    var contrast by b("contrast", false)
    var calm by b("calm", false)
    var mirror by b("mirror", false)
    var saver by b("saver", false)
    var wifi by b("wifi", true)
    var auto by b("auto", true)
    var fps by b("fps", false)
    var vol by i("vol", 80)
    var bright by i("bright", 100)
    var vib by b("vib", true)
    var sound by b("sound", false)
    var op by i("op", 55)
    var sc by i("sc", 100)
    var hide by b("hide", true)
    var cloud by b("cloud", true)
    var ctlMode by s("ctlMode", "touch")
    var lang by s("lang", "RU")
    var fit by b("fit", false)
    var sort by s("sort", "Недавние")
    var lf by s("lf", "Все")
    var lc by s("lc", "")
    var onboarded by b("onboarded", false)
    var launched by b("launched", false)
    var recent by s("recent", "")
    /** Настройки ядра PSP (ключ ppsspp_* → значение), JSON. Пресет: quality / balance / battery / custom. */
    var corePreset by s("corePreset", "")
    var coreVars by s("coreVars", "{}")
    /** Положение кнопок в редакторе: "mode:plat:id" → смещение в точках макета 844×390, JSON. */
    var keyPos by s("keyPos", "{}")
}

object AppState {
    private lateinit var sp: SharedPreferences
    private lateinit var app: Context
    lateinit var S: Settings
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ticker: Job? = null

    val byId = mutableStateMapOf<String, Game>()
    val lib = mutableStateMapOf<String, Lib>()
    val favs = mutableStateMapOf<String, Boolean>()
    var cols by mutableStateOf(linkedMapOf<String, List<String>>())
    val alerts = mutableStateListOf<Alert>()
    val readyIds = mutableStateListOf<String>()
    var toast by mutableStateOf<Toast?>(null)
    var online by mutableStateOf(true)
    /** Импорт своего файла: имя и доля 0..1, null — нет импорта. */
    var importing by mutableStateOf<Pair<String, Float>?>(null)

    const val TOTAL = 32768; const val OTHER = 14000

    fun init(c: Context) {
        if (::sp.isInitialized) return
        app = c.applicationContext
        sp = app.getSharedPreferences("kapsula", Context.MODE_PRIVATE)
        S = Settings(sp)
        GAMES.forEach { byId[it.id] = it }
        load()
        loadReal()
        applyLook()
        startTicker()
    }

    fun applyLook() { K.theme = S.theme; K.contrast = S.contrast; T.big = S.big }

    // ---------- сохранение ----------
    private fun load() {
        val raw = sp.getString("state", null)
        if (raw == null) { demo(); save(); return }
        try {
            val o = JSONObject(raw)
            o.optJSONObject("lib")?.let { l -> l.keys().forEach { id -> val e = l.getJSONObject(id); lib[id] = Lib(e.optString("s"), e.optDouble("pr", 0.0).toFloat(), e.optInt("play"), e.optString("last", "Ещё не запускалась"), e.optInt("saves"), e.optLong("t")) } }
            o.optJSONArray("favs")?.let { a -> for (i in 0 until a.length()) favs[a.getString(i)] = true }
            val cc = linkedMapOf<String, List<String>>()
            o.optJSONObject("cols")?.let { cs -> cs.keys().forEach { n -> val a = cs.getJSONArray(n); cc[n] = (0 until a.length()).map { a.getString(it) } } }
            cols = cc
            o.optJSONArray("alerts")?.let { a -> for (i in 0 until a.length()) { val e = a.getJSONObject(i); alerts.add(Alert(e.optString("t"), e.optString("d"), e.optBoolean("u"), e.optString("g").ifEmpty { null })) } }
        } catch (e: Exception) { demo() }
    }

    /** Стартовое демо-состояние, как в прототипе. */
    private fun demo() {
        lib.clear()
        lib["drift"] = Lib("ok", play = 762, last = "Сегодня, 21:40", saves = 3, t = 9)
        lib["ash"] = Lib("ok", play = 1345, last = "Вчера", saves = 5, t = 8)
        lib["shards"] = Lib("ok", play = 96, last = "3 дня назад", saves = 1, t = 7)
        lib["rubezh"] = Lib("ok", play = 210, last = "На прошлой неделе", saves = 2, t = 6)
        lib["orbit"] = Lib("dl", pr = 46f, t = 5)
        lib["fist"] = Lib("pause", pr = 12f, t = 4)
        lib["deep"] = Lib("own", t = 3)
        lib["courier"] = Lib("own", t = 2)
        cols = linkedMapOf("Пройти" to listOf("deep", "courier"), "Любимые" to listOf("drift", "ash"), "С другом" to listOf("rubezh"))
        favs.clear(); favs["drift"] = true; favs["deep"] = true
        alerts.clear()
        alerts.add(Alert("«Хроники Пепла» готова к игре", "Вчера", true, "ash"))
        alerts.add(Alert("Новое в магазине: «Звёздный Курьер»", "2 дня назад", true, "courier"))
        alerts.add(Alert("KAPSULA 0.12 — нативный интерфейс", "Сегодня", false))
    }

    fun save() {
        val l = JSONObject()
        lib.forEach { (id, e) -> l.put(id, JSONObject().put("s", e.s).put("pr", e.pr.toDouble()).put("play", e.play).put("last", e.last).put("saves", e.saves).put("t", e.t)) }
        val cs = JSONObject(); cols.forEach { (n, ids) -> cs.put(n, JSONArray(ids)) }
        val al = JSONArray(); alerts.forEach { al.put(JSONObject().put("t", it.t).put("d", it.d).put("u", it.u).put("g", it.g ?: "")) }
        sp.edit().putString("state", JSONObject().put("lib", l).put("favs", JSONArray(favs.keys.toList())).put("cols", cs).put("alerts", al).toString()).apply()
    }

    // ---------- свои файлы ----------
    fun loadReal() {
        val found = HashSet<String>()
        try {
            val a = JSONArray(GameLibrary.list(app))
            for (i in 0 until a.length()) {
                val f = a.getJSONObject(i)
                val g = realGame(f.getString("id"), f.getString("name"), f.getString("file"), f.getString("path"), f.getDouble("size"), f.optString("plat", "psp"))
                byId[g.id] = g; found.add(g.id)
                if (lib[g.id]?.s != "ok") lib[g.id] = Lib("ok", t = System.currentTimeMillis())
            }
        } catch (e: Exception) { }
        // файл удалили вручную — убираем из библиотеки
        byId.values.filter { it.real && it.id !in found }.map { it.id }.forEach { byId.remove(it); lib.remove(it) }
        consumeSession()
        save()
    }

    /** Итог последней игровой сессии из процесса игры: время и сохранения. */
    private fun consumeSession() {
        val f = File(app.filesDir, "last_session.json")
        if (!f.exists()) return
        try {
            val o = JSONObject(f.readText()); val id = o.getString("id")
            lib[id]?.let { e -> lib[id] = e.copy(play = e.play + o.optInt("minutes"), saves = e.saves + o.optInt("saves"), last = o.optString("last", e.last)) }
        } catch (e: Exception) { }
        f.delete()
    }

    fun addImported(info: JSONObject): Game {
        val g = realGame(info.getString("id"), info.getString("name"), info.getString("file"), info.getString("path"), info.getDouble("size"), info.optString("plat", "psp"))
        byId[g.id] = g; lib[g.id] = Lib("ok", t = System.currentTimeMillis()); save()
        return g
    }

    // ---------- статусы ----------
    fun stat(id: String): Stat {
        val e = lib[id] ?: return Stat("none", "")
        return when (e.s) {
            "ok" -> Stat("ok", if (byId[id]?.real == true) "Из файла" else "Установлена")
            "own" -> Stat("own", "В библиотеке")
            "q" -> Stat("q", "В очереди")
            "dl" -> Stat("dl", "${e.pr.toInt()}%")
            else -> Stat("pause", "Пауза · ${e.pr.toInt()}%")
        }
    }

    fun inCon(g: Game) = S.con == "all" || g.p == S.con
    fun used() = lib.filter { it.value.s in setOf("ok", "dl", "pause", "q") }.keys.sumOf { byId[it]?.size ?: 0 }
    fun free() = TOTAL - OTHER - used()
    fun active() = lib.filter { it.value.s in setOf("dl", "pause", "q") }.keys.toList()

    /** Моя библиотека: установленные → качаются → не установленные, затем по недавним. */
    fun myList(): List<Game> {
        val ord = mapOf("ok" to 0, "dl" to 1, "pause" to 1, "q" to 1, "own" to 2)
        return lib.keys.mapNotNull { byId[it] }.filter { inCon(it) }
            .sortedWith(compareBy<Game> { ord[lib[it.id]?.s] ?: 3 }.thenByDescending { lib[it.id]?.t ?: 0 })
    }

    // ---------- действия ----------
    fun say(text: String, icon: Int? = null, action: String? = null, onAction: (() -> Unit)? = null) { toast = Toast(text, icon, action, onAction) }

    fun add(id: String) {
        val g = byId[id] ?: return
        lib[id] = Lib("own", t = System.currentTimeMillis())
        say("«${g.t}» в библиотеке", com.nexus.psp.R.drawable.ph_check)
        if (S.auto) install(id)
        save()
    }

    fun install(id: String) {
        val g = byId[id] ?: return
        if (!online) say("Нет интернета — установка начнётся, когда он появится", com.nexus.psp.R.drawable.sl_globe)
        if (g.size > free()) { say("Мало памяти: нужно ${size(g.size)}, свободно ${size(free())}"); return }
        lib[id] = (lib[id] ?: Lib("own")).copy(s = "q", pr = 0f)
        pump()
        say("«${g.t}» — установка", com.nexus.psp.R.drawable.sl_dl)
        save()
    }

    fun pause(id: String) { lib[id]?.let { lib[id] = it.copy(s = "pause") }; pump(); save() }

    fun resume(id: String) {
        lib.filter { it.value.s == "dl" }.keys.forEach { k -> lib[k] = lib[k]!!.copy(s = "q") }
        lib[id]?.let { lib[id] = it.copy(s = "dl") }; save()
    }

    fun cancel(id: String) {
        val g = byId[id] ?: return
        lib[id]?.let { lib[id] = it.copy(s = "own", pr = 0f) }; pump()
        say("Установка «${g.t}» отменена — игра осталась в библиотеке"); save()
    }

    fun unlib(id: String) {
        val g = byId[id] ?: return
        if (g.real) { say("Свой файл убирается кнопкой «Удалить с телефона»"); return }
        lib.remove(id); favs.remove(id)
        cols = LinkedHashMap(cols.mapValues { (_, v) -> v - id })
        say("«${g.t}» убрана из библиотеки"); save()
    }

    fun uninstall(id: String) {
        val g = byId[id] ?: return
        if (g.real) {
            if (GameLibrary.delete(app, g.path!!)) { lib.remove(id); byId.remove(id); favs.remove(id); say("«${g.t}» удалена с телефона", com.nexus.psp.R.drawable.sl_trash) }
            else say("Не удалось удалить файл")
        } else {
            lib[id]?.let { lib[id] = Lib("own", t = it.t, saves = it.saves) }
            say("«${g.t}» удалена с телефона, осталась в библиотеке", com.nexus.psp.R.drawable.sl_trash)
        }
        save()
    }

    fun toggleFav(id: String) { if (favs[id] == true) favs.remove(id) else favs[id] = true; save() }

    fun toggleCol(name: String, id: String) {
        val m = LinkedHashMap(cols); val l = m[name] ?: return
        m[name] = if (id in l) l - id else l + id
        cols = m; save()
    }

    fun newCol(name: String): Boolean {
        val n = name.trim(); if (n.isEmpty()) return false
        if (n !in cols) { val m = LinkedHashMap(cols); m[n] = emptyList(); cols = m; save() }
        return true
    }

    fun readAll() { for (i in alerts.indices) alerts[i] = alerts[i].copy(u = false); save() }

    fun markPlayed(id: String) {
        readyIds.remove(id)
        lib[id]?.let { lib[id] = it.copy(last = "Сегодня, " + SimpleDateFormat("HH:mm", Locale("ru")).format(Date()), t = System.currentTimeMillis()) }
        save()
    }

    fun addRecent(q: String) {
        val v = q.trim(); if (v.isEmpty()) return
        S.recent = (listOf(v) + recentList().filter { it != v }).take(5).joinToString("\n")
    }
    fun recentList() = S.recent.split("\n").filter { it.isNotBlank() }

    // ---------- движок загрузок (демо-каталог) ----------
    private fun pump() {
        if (lib.values.any { it.s == "dl" }) return
        lib.entries.firstOrNull { it.value.s == "q" }?.let { lib[it.key] = it.value.copy(s = "dl") }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                delay(600)
                var changed = false
                lib.entries.filter { it.value.s == "dl" }.forEach { (id, e) ->
                    val g = byId[id] ?: return@forEach
                    val pr = minOf(100f, e.pr + ((9 + Random.nextDouble() * 5) / g.size * 100 * 1.5).toFloat())
                    if (pr >= 100f) {
                        lib[id] = Lib("ok", t = System.currentTimeMillis())
                        readyIds.add(id)
                        alerts.add(0, Alert("«${g.t}» установлена", "Только что", true, id))
                        onReady?.invoke(g)
                    } else lib[id] = e.copy(pr = pr)
                    changed = true
                }
                if (changed) { pump(); save() }
            }
        }
    }

    /** Вызывается, когда игра докачалась (подсказка «Играть» и вибрация — в интерфейсе). */
    var onReady: ((Game) -> Unit)? = null
}
