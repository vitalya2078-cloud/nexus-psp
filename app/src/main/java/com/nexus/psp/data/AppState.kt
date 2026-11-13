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
import kotlinx.coroutines.launch
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

/** Найденный при сканировании папки образ; exists — такая игра уже есть в библиотеке (не копируется). */
data class Found(val name: String, val uri: Uri, val sizeMb: Int, val exists: Boolean = false)

/** Вопрос пользователю с вариантами ответа (диалог в App.kt). */
data class Ask(val title: String, val text: String, val options: List<String>, val answer: kotlinx.coroutines.CompletableDeferred<Int>)

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
    /** Обложки и названия игр из интернета (libretro) и только по Wi-Fi. */
    var covers by b("covers", true)
    var coversWifi by b("coversWifi", true)
    /** Сообщить, когда откроется магазин. */
    var storeNotify by b("storeNotify", false)
    /** Настройки ядра PSP (ключ ppsspp_* → значение), JSON. Пресет: quality / balance / battery / custom. */
    var corePreset by s("corePreset", "")
    var coreVars by s("coreVars", "{}")
    /** Положение кнопок в редакторе: "раскладка:консоль:кнопка" → смещение в точках макета 844×390, JSON. */
    var keyPos by s("keyPos", "{}")
    /** Свои значки и турбо кнопок KAPSULA Touch: "раскладка:кнопка" → значок / true. */
    var keyIcon by s("keyIcon", "{}")
    var keyTurbo by s("keyTurbo", "{}")
    /** Свои кнопки (комбо, «две разом»): "раскладка:платформа" → [{id, keys, combo}]. */
    var extraBtns by s("extraBtns", "{}")
    /** Мгновенное продолжение: «Играть» возвращает туда, где вышел. */
    var autoResume by b("autoResume", true)
    /** Фильтр картинки (default/smooth/pixel/lcd/crt), уровень ускорения (2–4), чувствительность руля наклоном, %. */
    var shader by s("shader", "default")
    var fastLevel by s("fastLevel", "2")
    var tiltSens by s("tiltSens", "100")
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
    /** Сведения об играх: серийный номер, название из базы, обложка, картинки из образа. */
    val meta = mutableStateMapOf<String, Meta>()
    var ask by mutableStateOf<Ask?>(null)
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)
    private val inWork = HashSet<String>()

    /** Спросить пользователя и дождаться ответа (индекс варианта, -1 — закрыл). */
    suspend fun askUser(title: String, text: String, vararg options: String): Int {
        val d = kotlinx.coroutines.CompletableDeferred<Int>()
        ask = Ask(title, text, options.toList(), d)
        return d.await().also { ask = null }
    }

    fun init(c: Context) {
        // повторный вызов с тем же приложением — ничего не делаем; новый контекст (тесты) — начинаем с чистого состояния
        if (::sp.isInitialized && app === c.applicationContext) return
        byId.clear(); lib.clear(); favs.clear(); alerts.clear(); history.clear(); meta.clear(); cols = linkedMapOf(); importing = null; found = null
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
                val g0 = realGame(f.getString("id"), f.getString("name"), f.getString("file"), f.getString("path"), f.getDouble("size"), f.optString("plat", "psp"))
                val m = Covers.load(app, g0.id); meta[g0.id] = m
                val g = if (m.title != null) g0.copy(t = m.title) else g0
                byId[g.id] = g; found.add(g.id)
                if (lib[g.id] == null) lib[g.id] = Lib()
            }
        } catch (e: Exception) { }
        byId.keys.filter { it !in found }.forEach { byId.remove(it); meta.remove(it) }
        lib.keys.filter { it !in found }.forEach { lib.remove(it) }
        favs.keys.filter { it !in found }.forEach { favs.remove(it) }
        if (cols.values.any { l -> l.any { it !in found } }) cols = LinkedHashMap(cols.mapValues { (_, v) -> v.filter { it in found } })
        consumeSession()
        save()
        // обложки: игры, которые ещё не обрабатывались или ждали интернета
        byId.values.forEach { g -> val m = meta[g.id]; if (m == null || m.tried == 0L || (m.cover == null && m.tried == 1L) || (m.extra == 0L && m.tried > 1L)) fetchMeta(g.id) }
    }

    /** Фоновая обработка обложек (в тестах снимков экранов выключается). */
    var autoFetch = true

    /** Картинки из образа, название и обложка — в фоне; force — «Найти обложку заново». */
    fun fetchMeta(id: String, force: Boolean = false) {
        if (!autoFetch && !force) return
        val g = byId[id] ?: return
        if (!inWork.add(id)) return
        scope.launch {
            val net = S.covers && Covers.netAllowed(app, S.coversWifi)
            val m = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { Covers.process(app, g, net, force) }
            inWork.remove(id)
            meta[id] = m
            byId[id]?.let { cur -> if (m.title != null && cur.t != m.title) byId[id] = cur.copy(t = m.title) }
            if (force) say(if (m.cover != null) "Обложка обновлена" else if (!net) "Нет интернета (или не Wi-Fi) — обложка не загружена" else "Обложку для этой игры не нашли", com.nexus.psp.R.drawable.ph_check)
        }
    }

    fun setUserCover(id: String, bytes: ByteArray) {
        scope.launch {
            val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { Covers.setUserCover(app, id, bytes) }
            meta[id] = Covers.load(app, id)
            say(if (ok) "Обложка установлена" else "Не удалось открыть картинку", com.nexus.psp.R.drawable.ph_check)
        }
    }

    fun clearUserCover(id: String) { Covers.clearUserCover(app, id); meta[id] = Covers.load(app, id) }

    /** Игра с тем же названием (без формата и региона), но другим файлом — для вопроса «Заменить / Оставить обе». */
    fun sameTitle(fileName: String): Game? {
        val k = prettyName(fileName.substringBeforeLast('.')).lowercase()
        return byId.values.firstOrNull { prettyName(it.file.substringBeforeLast('.')).lowercase() == k && !it.file.equals(fileName, true) }
    }

    fun hasFile(fileName: String) = byId.values.firstOrNull { it.file.equals(fileName, true) }

    /** «Заменить»: статистика, избранное и коллекции переходят к новому файлу, старый удаляется. */
    fun replaceWith(oldId: String, newId: String) {
        val o = byId[oldId] ?: return
        lib[oldId]?.let { e -> lib[newId] = (lib[newId] ?: Lib()).let { n -> n.copy(play = n.play + e.play, saves = n.saves + e.saves, last = e.last) } }
        if (favs[oldId] == true) favs[newId] = true
        cols = LinkedHashMap(cols.mapValues { (_, v) -> if (oldId in v && newId !in v) v.map { if (it == oldId) newId else it } else v - oldId })
        GameLibrary.delete(app, o.path); Covers.remove(app, oldId)
        lib.remove(oldId); byId.remove(oldId); favs.remove(oldId); meta.remove(oldId)
        save()
    }

    /** Изменения «для всех игр» из меню паузы (процесс игры пишет файл, здесь применяем к общим настройкам). */
    fun consumePendingGlobal() {
        val f = GameConfig.pendingGlobal(app)
        if (!f.exists()) return
        try {
            val o = JSONObject(f.readText())
            o.optJSONObject("vars")?.let { v -> val cur = try { JSONObject(S.coreVars) } catch (e: Exception) { JSONObject() }; v.keys().forEach { k -> cur.put(k, v.get(k)) }; S.coreVars = cur.toString() }
            if (o.has("preset")) S.corePreset = o.getString("preset")
            if (o.has("ctlMode")) S.ctlMode = o.getString("ctlMode")
            if (o.has("op")) S.op = o.getInt("op")
            if (o.has("sc")) S.sc = o.getInt("sc")
            if (o.has("mirror")) S.mirror = o.getBoolean("mirror")
            if (o.has("fps")) S.fps = o.getBoolean("fps")
            o.optJSONObject("keyPos")?.let { v -> val cur = try { JSONObject(S.keyPos) } catch (e: Exception) { JSONObject() }; v.keys().forEach { k -> cur.put(k, v.get(k)) }; S.keyPos = cur.toString() }
            // значки и турбо: удалённые ключи тоже переносятся (сброс к «как было»)
            o.optJSONObject("keyIcon")?.let { v -> S.keyIcon = v.toString() }
            o.optJSONObject("keyTurbo")?.let { v -> S.keyTurbo = v.toString() }
            o.optJSONObject("extraBtns")?.let { v -> S.extraBtns = v.toString() }
            if (o.has("shader")) S.shader = o.optString("shader")
            if (o.has("fastLevel")) S.fastLevel = o.optInt("fastLevel", 2).toString()
            if (o.has("tiltSens")) S.tiltSens = o.optInt("tiltSens", 100).toString()
        } catch (e: Exception) { }
        f.delete()
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
        val isNew = lib[g.id] == null
        byId[g.id] = g; lib[g.id] = lib[g.id] ?: Lib()
        if (isNew) alerts.add(0, Alert("«${g.t}» добавлена в библиотеку", "Только что", true, g.id))
        save()
        fetchMeta(g.id)
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
            lib.remove(id); byId.remove(id); favs.remove(id); meta.remove(id); Covers.remove(app, id)
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
