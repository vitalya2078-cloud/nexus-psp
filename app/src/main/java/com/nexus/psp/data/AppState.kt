package com.nexus.psp.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.StatFs
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nexus.psp.R
import com.nexus.psp.emulator.CoreFiles
import com.nexus.psp.emulator.GameLibrary
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.T
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Запись библиотеки (свой файл на телефоне): время в игре, последний запуск, сохранения. */
data class Lib(val play: Int = 0, val last: String = "Ещё не запускалась", val saves: Int = 0, val t: Long = System.currentTimeMillis())

data class Alert(val t: String, val d: String, val u: Boolean, val g: String? = null)

data class Toast(val text: String, val icon: Int? = null, val action: String? = null, val onAction: (() -> Unit)? = null, val id: Long = System.nanoTime())

/** Найденный при сканировании папки образ. */
data class Found(val name: String, val uri: Uri, val sizeMb: Int)

/** Копирование своих файлов: текущий файл, доля 0..1, номер и всего в очереди. */
data class Importing(val name: String, val p: Float, val n: Int, val total: Int)

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
    var sort by s("sort", "Недавние")
    var lf by s("lf", "Все")
    var lc by s("lc", "")
    var onboarded by b("onboarded", false)
    var launched by b("launched", false)
    var recent by s("recent", "")
    /** Сообщить, когда откроется магазин. */
    var storeNotify by b("storeNotify", false)
    /** Настройки ядра PSP (ключ ppsspp_* → значение), JSON. Пресет: quality / balance / battery / custom. */
    var corePreset by s("corePreset", "")
    var coreVars by s("coreVars", "{}")
    /** Положение кнопок в редакторе: "раскладка:консоль:кнопка" → смещение в точках макета 844×390, JSON. */
    var keyPos by s("keyPos", "{}")
}

object AppState {
    private lateinit var sp: SharedPreferences
    private lateinit var app: Context
    lateinit var S: Settings

    /** Версия формата данных; 3 — без демо-каталога (0.13). */
    private const val DATA_V = 3

    val byId = mutableStateMapOf<String, Game>()
    val lib = mutableStateMapOf<String, Lib>()
    val favs = mutableStateMapOf<String, Boolean>()
    var cols by mutableStateOf(linkedMapOf<String, List<String>>())
    val alerts = mutableStateListOf<Alert>()
    /** Минуты в игре по дням: "yyyy-MM-dd" → минуты. */
    val history = mutableStateMapOf<String, Int>()
    var toast by mutableStateOf<Toast?>(null)
    var online by mutableStateOf(true)
    var importing by mutableStateOf<Importing?>(null)
    /** Результат «Найти игры на телефоне» — ждёт подтверждения. */
    var found by mutableStateOf<List<Found>?>(null)
    var scanning by mutableStateOf(false)

    fun init(c: Context) {
        // повторный вызов с тем же приложением — ничего не делаем; новый контекст (тесты) — начинаем с чистого состояния
        if (::sp.isInitialized && app === c.applicationContext) return
        byId.clear(); lib.clear(); favs.clear(); alerts.clear(); history.clear(); cols = linkedMapOf(); importing = null; found = null
        app = c.applicationContext
        sp = app.getSharedPreferences("kapsula", Context.MODE_PRIVATE)
        S = Settings(sp)
        load()
        loadReal()
        applyLook()
    }

    fun applyLook() { K.theme = S.theme; K.contrast = S.contrast; T.big = S.big }

    // ---------- сохранение ----------
    private fun load() {
        val raw = sp.getString("state", null) ?: return
        val old = sp.getInt("dataV", 0) < DATA_V
        try {
            val o = JSONObject(raw)
            o.optJSONObject("lib")?.let { l -> l.keys().forEach { id -> val e = l.getJSONObject(id); lib[id] = Lib(e.optInt("play"), e.optString("last", "Ещё не запускалась"), e.optInt("saves"), e.optLong("t")) } }
            o.optJSONArray("favs")?.let { a -> for (i in 0 until a.length()) favs[a.getString(i)] = true }
            val cc = linkedMapOf<String, List<String>>()
            o.optJSONObject("cols")?.let { cs -> cs.keys().forEach { n -> val a = cs.getJSONArray(n); cc[n] = (0 until a.length()).map { a.getString(it) } } }
            cols = cc
            o.optJSONObject("history")?.let { h -> h.keys().forEach { d -> history[d] = h.optInt(d) } }
            if (!old) o.optJSONArray("alerts")?.let { a -> for (i in 0 until a.length()) { val e = a.getJSONObject(i); alerts.add(Alert(e.optString("t"), e.optString("d"), e.optBoolean("u"), e.optString("g").ifEmpty { null })) } }
        } catch (e: Exception) { }
        if (old) {
            // 0.12 и раньше: стартовое демо — убираем коллекции-примеры (свои файлы останутся после loadReal)
            val demoCols = setOf("Пройти", "Любимые", "С другом")
            cols = LinkedHashMap(cols.filterKeys { it !in demoCols })
        }
    }

    fun save() {
        val l = JSONObject()
        lib.forEach { (id, e) -> l.put(id, JSONObject().put("play", e.play).put("last", e.last).put("saves", e.saves).put("t", e.t)) }
        val cs = JSONObject(); cols.forEach { (n, ids) -> cs.put(n, JSONArray(ids)) }
        val al = JSONArray(); alerts.forEach { al.put(JSONObject().put("t", it.t).put("d", it.d).put("u", it.u).put("g", it.g ?: "")) }
        val hs = JSONObject(); history.forEach { (d, m) -> hs.put(d, m) }
        sp.edit().putString("state", JSONObject().put("lib", l).put("favs", JSONArray(favs.keys.toList())).put("cols", cs).put("alerts", al).put("history", hs).toString())
            .putInt("dataV", DATA_V).apply()
    }

    // ---------- свои файлы ----------
    /** Библиотека = файлы в папке игр; записи без файла (удалили вручную, демо прошлых версий) убираются. */
    fun loadReal() {
        val found = HashSet<String>()
        try {
            val a = JSONArray(GameLibrary.list(app))
            for (i in 0 until a.length()) {
                val f = a.getJSONObject(i)
                val g = realGame(f.getString("id"), f.getString("name"), f.getString("file"), f.getString("path"), f.getDouble("size"), f.optString("plat", "psp"))
                byId[g.id] = g; found.add(g.id)
                if (lib[g.id] == null) lib[g.id] = Lib()
            }
        } catch (e: Exception) { }
        byId.keys.filter { it !in found }.forEach { byId.remove(it) }
        lib.keys.filter { it !in found }.forEach { lib.remove(it) }
        favs.keys.filter { it !in found }.forEach { favs.remove(it) }
        if (cols.values.any { l -> l.any { it !in found } }) cols = LinkedHashMap(cols.mapValues { (_, v) -> v.filter { it in found } })
        consumeSession()
        save()
    }

    /** Итог последней игровой сессии из процесса игры: время, сохранения, история по дням. */
    private fun consumeSession() {
        val f = File(app.filesDir, "last_session.json")
        if (!f.exists()) return
        try {
            val o = JSONObject(f.readText()); val id = o.getString("id"); val min = o.optInt("minutes")
            lib[id]?.let { e -> lib[id] = e.copy(play = e.play + min, saves = e.saves + o.optInt("saves")) }
            if (min > 0) { val d = day(Date()); history[d] = (history[d] ?: 0) + min }
        } catch (e: Exception) { }
        f.delete()
    }

    fun addImported(info: JSONObject): Game {
        val g = realGame(info.getString("id"), info.getString("name"), info.getString("file"), info.getString("path"), info.getDouble("size"), info.optString("plat", "psp"))
        byId[g.id] = g; lib[g.id] = lib[g.id] ?: Lib()
        alerts.add(0, Alert("«${g.t}» добавлена в библиотеку", "Только что", true, g.id))
        save()
        return g
    }

    // ---------- память ----------
    /** Свободно на телефоне, МБ (там, где лежат игры). */
    fun freeMb(): Int = try { (StatFs(CoreFiles.gamesDir(app).absolutePath).availableBytes / 1048576).toInt() } catch (e: Exception) { 0 }
    fun totalMb(): Int = try { (StatFs(CoreFiles.gamesDir(app).absolutePath).totalBytes / 1048576).toInt() } catch (e: Exception) { 0 }
    fun usedMb(): Int = byId.values.sumOf { it.size }

    // ---------- сохранения в игре ----------
    fun stateFile(g: Game, slot: Int = 0) = File(CoreFiles.statesDir(app), File(g.path).nameWithoutExtension + ".state" + slot)

    // ---------- списки ----------
    fun inCon(g: Game) = S.con == "all" || g.p == S.con
    fun myList(): List<Game> = lib.keys.mapNotNull { byId[it] }.filter { inCon(it) }.sortedByDescending { lib[it.id]?.t ?: 0 }

    // ---------- действия ----------
    fun say(text: String, icon: Int? = null, action: String? = null, onAction: (() -> Unit)? = null) { toast = Toast(text, icon, action, onAction) }

    fun uninstall(id: String) {
        val g = byId[id] ?: return
        if (GameLibrary.delete(app, g.path)) {
            lib.remove(id); byId.remove(id); favs.remove(id)
            cols = LinkedHashMap(cols.mapValues { (_, v) -> v - id })
            say("«${g.t}» удалена с телефона", R.drawable.sl_trash); save()
        } else say("Не удалось удалить файл")
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
        lib[id]?.let { lib[id] = it.copy(last = "Сегодня, " + SimpleDateFormat("HH:mm", Locale("ru")).format(Date()), t = System.currentTimeMillis()) }
        save()
    }

    fun addRecent(q: String) {
        val v = q.trim(); if (v.isEmpty()) return
        S.recent = (listOf(v) + recentList().filter { it != v }).take(5).joinToString("\n")
    }
    fun recentList() = S.recent.split("\n").filter { it.isNotBlank() }

    // ---------- история ----------
    private fun day(d: Date) = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d)

    /** Последние 7 дней: подпись и минуты (сегодня первым). */
    fun week(): List<Pair<String, Int>> {
        val c = Calendar.getInstance(); val names = listOf("Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб")
        return (0 until 7).map { i ->
            val label = when (i) { 0 -> "Сегодня"; 1 -> "Вчера"; else -> names[c.get(Calendar.DAY_OF_WEEK) - 1] }
            val v = label to (history[day(c.time)] ?: 0)
            c.add(Calendar.DAY_OF_MONTH, -1); v
        }
    }
}
