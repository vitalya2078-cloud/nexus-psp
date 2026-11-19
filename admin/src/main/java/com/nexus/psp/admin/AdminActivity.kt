package com.nexus.psp.admin

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.layout.ContentScale

import androidx.compose.ui.graphics.asImageBitmap

import androidx.compose.foundation.layout.aspectRatio

import androidx.compose.foundation.Image

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Общее состояние: вошёл ли админ, сообщение снизу. */
object St {
    var logged by mutableStateOf(false)
    var toast by mutableStateOf<String?>(null)
    var me by mutableStateOf("")
    /** Стартовая вкладка (для снимков в тестах). */
    var startTab = "sum"
}

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Api.init(applicationContext)
        setContent { AdminApp() }
    }
}

/** Запрос на сервер в фоне; ошибка — сообщением снизу. */
suspend fun req(action: String, args: JSONObject = JSONObject()): Api.Res {
    val r = withContext(Dispatchers.IO) { Api.call(action, args) }
    if (!r.ok) St.toast = r.error
    return r
}

@Composable
fun AdminApp() {
    Box(Modifier.fillMaxSize().background(C.bg).windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars)) {
        if (!St.logged) Login() else Main()
        St.toast?.let { t ->
            LaunchedEffect(t) { kotlinx.coroutines.delay(3500); if (St.toast == t) St.toast = null }
            Text(t, style = F.label.copy(color = C.bg, fontSize = 14.sp), modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 86.dp, start = 16.dp, end = 16.dp)
                .clip(RoundedCornerShape(16.dp)).background(C.ink).clickable { St.toast = null }.padding(horizontal = 16.dp, vertical = 12.dp))
        }
    }
}

// ---------- вход ----------

@Composable
private fun Login() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf(Api.code) }
    var pin by remember { mutableStateOf("") }
    var rebind by remember { mutableStateOf(false) }
    var setup by remember { mutableStateOf(false) }
    var token by remember { mutableStateOf("") }
    var pin2 by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Spacer(Modifier.height(30.dp))
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(C.gold.copy(alpha = .16f)).border(1.5.dp, C.gold, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
            Text("K", style = F.h1.copy(color = C.gold2))
        }
        Text("KAPSULA Админ", style = F.h1)
        Text(if (setup) "Первая настройка: ключ настройки, твой код аккаунта из KAPSULA и новый PIN. Ключ сработает один раз."
             else "Вход по коду аккаунта из KAPSULA (Настройки → Аккаунт) и PIN.", style = F.text)
        Caps("Код аккаунта")
        Field(code, "KPS-XXXX-XXXX-XXXX", Modifier.fillMaxWidth()) { code = it.take(24) }
        if (setup) {
            Caps("Ключ настройки")
            Field(token, "SETUP-XXXX-XXXX-XXXX-XXXX", Modifier.fillMaxWidth()) { token = it.take(30) }
            Caps("Новый PIN (6–12 цифр)")
            Field(pin, "PIN", Modifier.fillMaxWidth(), secret = true, digits = true) { pin = it.filter(Char::isDigit).take(12) }
            Field(pin2, "Повтори PIN", Modifier.fillMaxWidth(), secret = true, digits = true) { pin2 = it.filter(Char::isDigit).take(12) }
        } else {
            Caps("PIN")
            Field(pin, "PIN", Modifier.fillMaxWidth(), secret = true, digits = true) { pin = it.filter(Char::isDigit).take(12) }
            Row(Modifier.clickable { rebind = !rebind }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(if (rebind) C.gold else C.s2), contentAlignment = Alignment.Center) { if (rebind) Text("✓", style = F.label.copy(color = C.bg)) }
                Text("Новый телефон (привязать этот)", style = F.text)
            }
        }
        Spacer(Modifier.height(4.dp))
        Btn(if (busy) "Проверяю…" else if (setup) "Настроить" else "Войти", Modifier.fillMaxWidth(), gold = true, enabled = !busy && code.length >= 12 && pin.length >= 6) {
            if (setup && pin != pin2) { St.toast = "PIN не совпадает"; return@Btn }
            busy = true
            scope.launch {
                Api.remember(ctx, code); Api.pin = pin
                val r = if (setup) req("setup", JSONObject().put("token", token.trim())) else req("ping", JSONObject().put("rebind", rebind))
                busy = false
                if (r.ok) { St.me = r.obj().optString("short_id"); St.logged = true; if (setup) St.toast = "Готово. Твой ID — KAPSULA" }
            }
        }
        Text(if (setup) "У меня уже есть PIN — войти" else "Первая настройка", style = F.label.copy(color = C.gold), modifier = Modifier.clickable { setup = !setup }.padding(vertical = 8.dp))
    }
}

// ---------- основное: вкладки снизу ----------

@Composable
private fun Main() {
    var tab by remember { mutableStateOf(St.startTab) }
    var player by remember { mutableStateOf<String?>(null) }
    BackHandler(player != null) { player = null }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(when (tab) { "sum" -> "Сводка"; "players" -> "Игроки"; "eco" -> "Экономика"; "news" -> "Новости"; else -> "Ещё" }, style = F.h1, modifier = Modifier.weight(1f))
            Tag("Админ · ${St.me.ifEmpty { "KAPSULA" }}", C.gold2)
        }
        Box(Modifier.weight(1f)) {
            when (tab) {
                "sum" -> Summary()
                "players" -> Players { player = it }
                "eco" -> Economy()
                "news" -> News()
                else -> More()
            }
            player?.let { PlayerCard(it) { player = null } }
        }
        Row(Modifier.fillMaxWidth().background(C.s1).padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("sum" to "Сводка", "players" to "Игроки", "eco" to "Экономика", "news" to "Новости", "more" to "Ещё").forEach { (id, t) ->
                val on = tab == id
                Column(Modifier.clip(RoundedCornerShape(14.dp)).clickable { tab = id; player = null }.padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(if (on) C.gold else C.s1))
                    Text(t, style = F.small.copy(color = if (on) C.ink else C.ink3, fontSize = 12.sp))
                }
            }
        }
    }
}

@Composable
private fun Page(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { content() }
}

// ---------- сводка ----------

@Composable
private fun Summary() {
    var d by remember { mutableStateOf<JSONObject?>(null) }
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(tick) { val r = req("summary"); if (r.ok) d = r.obj() }
    Page {
        val s = d
        if (s == null) { Text("Загружаю…", style = F.text); return@Page }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("${s.optInt("players")}", "игроков всего", Modifier.weight(1f), gold = true)
            Stat("${s.optInt("active_day")}", "активны за сутки", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("${s.optInt("active_week")}", "активны за неделю", Modifier.weight(1f))
            Stat("${s.optInt("new_week")}", "новых за неделю", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("${s.optInt("tokens_given_week")}", "жетонов выдано за неделю", Modifier.weight(1f))
            Stat("${s.optInt("tokens_spent_week")}", "жетонов потрачено", Modifier.weight(1f))
        }
        Caps("Заходы по дням (14 дней)")
        Card(Modifier.fillMaxWidth()) {
            val daily = s.optJSONArray("daily") ?: JSONArray()
            val vals = (0 until daily.length()).map { daily.getJSONObject(it) }
            if (vals.isEmpty()) Text("Пока нет данных", style = F.small)
            else {
                val mx = vals.maxOf { it.optInt("players") }.coerceAtLeast(1)
                Canvas(Modifier.fillMaxWidth().height(110.dp)) {
                    val w = size.width / vals.size
                    vals.forEachIndexed { i, o ->
                        val h = size.height * o.optInt("players") / mx
                        drawRoundRect(C.gold, Offset(i * w + w * .18f, size.height - h), Size(w * .64f, h), CornerRadius(6f, 6f))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(vals.first().optString("day").takeLast(5), style = F.small); Text("макс. $mx в день", style = F.small); Text(vals.last().optString("day").takeLast(5), style = F.small)
                }
            }
        }
        Caps("Версии приложения")
        Card(Modifier.fillMaxWidth()) { s.optJSONObject("versions")?.let { v -> v.keys().asSequence().sortedDescending().forEach { KV(it, "${v.optInt(it)}") } } }
        Caps("Консоли (минуты в играх)")
        Card(Modifier.fillMaxWidth()) {
            val c = s.optJSONObject("consoles") ?: JSONObject()
            val keys = c.keys().asSequence().toList().sortedByDescending { c.optInt(it) }
            if (keys.isEmpty()) Text("Пока нет данных", style = F.small)
            keys.forEach { KV(it.uppercase(), "${c.optInt(it) / 60} ч ${c.optInt(it) % 60} мин") }
        }
        Caps("Что покупают")
        Card(Modifier.fillMaxWidth()) {
            val b = s.optJSONArray("top_buys") ?: JSONArray()
            if (b.length() == 0) Text("Покупок пока нет", style = F.small)
            for (i in 0 until b.length()) b.getJSONObject(i).let { KV(it.optString("item"), "${it.optInt("n")}") }
        }
        if (s.optInt("banned") > 0) Text("Заблокировано: ${s.optInt("banned")}", style = F.small)
        AppStats(tick)
        Btn("Обновить", Modifier.fillMaxWidth()) { tick++ }
    }
}

/** Анонимная статистика приложения за 7 дней: суммы по событиям (без игроков). */
@Composable
private fun AppStats(tick: Int) {
    var rows by remember { mutableStateOf<JSONArray?>(null) }
    LaunchedEffect(tick) { val r = req("stats", JSONObject().put("days", 7)); if (r.ok) rows = r.arr() }
    val a = rows ?: return
    val sum = HashMap<String, Long>()
    val players = sortedMapOf<String, Long>()
    for (i in 0 until a.length()) a.getJSONObject(i).let { o ->
        val kind = o.optString("kind"); val n = o.optLong("n")
        if (kind == "players") players[o.optString("day")] = n else sum["$kind|${o.optString("k")}"] = (sum["$kind|${o.optString("k")}"] ?: 0) + n
    }
    Caps("Статистика приложения · 7 дней")
    Card(Modifier.fillMaxWidth()) {
        if (a.length() == 0) { Text("Пока нет данных — придут от версии 0.46 и выше", style = F.small); return@Card }
        if (players.isNotEmpty()) KV("Игроков в день (сред.)", "${players.values.average().toInt()}")
        @Composable fun group(kind: String, title: String, name: (String) -> String = { it }) {
            val g = sum.filterKeys { it.startsWith("$kind|") }.mapKeys { it.key.substringAfter('|') }
            if (g.isEmpty()) return
            Text(title, style = F.label.copy(fontSize = 14.sp), modifier = Modifier.padding(top = 6.dp))
            g.entries.sortedByDescending { it.value }.forEach { KV(name(it.key).ifEmpty { "—" }, "${it.value}") }
        }
        group("app_open", "Запуски приложения") { "всего" }
        group("game_start", "Запуски игр по консолям") { it.uppercase() }
        group("session", "Длина игры, мин")
        group("screen", "Вкладки") { when (it) { "games" -> "Игры"; "my" -> "Моё"; "ctl" -> "Управление"; else -> it } }
        group("crash", "Сбои") { when (it) { "game_native" -> "игра: сбой ядра"; "game_anr" -> "игра: не отвечала"; "game_mem" -> "игра: не хватило памяти"
            "main_native" -> "меню: сбой"; "main_anr" -> "меню: не отвечало"; "main_mem" -> "меню: не хватило памяти"; else -> it } }
        group("add_fail", "Отказы проводника") { when (it) { "format" -> "не тот формат"; "text" -> "текстовый файл"; "small" -> "слишком маленький"
            "nomark" -> "нет метки диска"; "unread" -> "не прочитался"; else -> it } }
    }
}

// ---------- игроки ----------

@Composable
private fun Players(open: (String) -> Unit) {
    var q by remember { mutableStateOf("") }
    var list by remember { mutableStateOf(JSONArray()) }
    LaunchedEffect(q) { kotlinx.coroutines.delay(300); val r = req("players", JSONObject().put("q", q.trim())); if (r.ok) list = r.arr() }
    Page {
        Field(q, "ID или ник", Modifier.fillMaxWidth()) { q = it.take(20) }
        if (list.length() == 0) Text("Никого не нашли", style = F.text)
        for (i in 0 until list.length()) {
            val p = list.getJSONObject(i)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.s1).clickable { open(p.optString("short_id")) }.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(p.optString("short_id"), style = F.label.copy(color = if (p.optBoolean("admin")) C.gold2 else C.ink))
                        if (p.optBoolean("banned")) Tag("Заблокирован", C.bad)
                        if (p.optBoolean("admin")) Tag("Админ", C.gold2)
                    }
                    Text((p.optString("nick").takeIf { it.isNotEmpty() && it != "null" } ?: "без имени") + " · ур. ${p.optInt("level")} · ${p.optInt("play") / 60} ч", style = F.small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${p.optInt("balance")}", style = F.label.copy(color = C.gold))
                    Text(p.optString("ver").takeIf { it.isNotEmpty() && it != "null" } ?: "", style = F.small)
                }
            }
        }
    }
}

@Composable
private fun PlayerCard(id: String, close: () -> Unit) {
    val scope = rememberCoroutineScope()
    var p by remember { mutableStateOf<JSONObject?>(null) }
    var tick by remember { mutableStateOf(0) }
    var amount by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var item by remember { mutableStateOf("") }
    var ask by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    LaunchedEffect(id, tick) { val r = req("player", JSONObject().put("target", id)); if (r.ok) p = r.obj() else close() }
    fun act(a: String, args: JSONObject, done: String) = scope.launch { val r = req(a, args.put("target", id)); if (r.ok) { St.toast = done; tick++ } }
    Box(Modifier.fillMaxSize().background(C.bg)) {
        Page {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("‹ Назад", style = F.label.copy(color = C.gold), modifier = Modifier.clickable { close() }.padding(vertical = 8.dp))
            }
            val o = p
            if (o == null) { Text("Загружаю…", style = F.text); return@Page }
            val pr = o.optJSONObject("profile") ?: JSONObject()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(o.optString("short_id"), style = F.h1.copy(color = if (o.optBoolean("admin")) C.gold2 else C.ink))
                if (o.optBoolean("banned")) Tag("Заблокирован", C.bad)
            }
            Text(pr.optString("nick").ifEmpty { "без имени" } + (pr.optString("motto").takeIf { it.isNotEmpty() }?.let { " · «$it»" } ?: ""), style = F.text)
            Card(Modifier.fillMaxWidth()) {
                KV("Жетоны", "${o.optInt("balance")}"); KV("Уровень", "${o.optInt("level")}"); KV("В играх", "${o.optInt("play") / 60} ч ${o.optInt("play") % 60} мин")
                KV("Сохранения", "${o.optInt("saves")}"); KV("Игр", "${o.optInt("games")}"); KV("Серия дней", "${o.optInt("streak")}"); KV("Заходов подряд", "${o.optInt("visit_streak")}")
                KV("Версия", o.optString("ver").takeIf { it != "null" } ?: "?"); KV("Создан", o.optString("created").take(10)); KV("Был", o.optString("seen").take(16).replace('T', ' '))
            }
            Caps("Жетоны")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(amount, "+100 или −50", Modifier.weight(1f)) { amount = it.filter { c -> c.isDigit() || c == '-' }.take(7) }
                Btn("Начислить", gold = true, enabled = amount.toIntOrNull()?.let { it != 0 } == true) {
                    val n = amount.toInt(); ask = "${if (n > 0) "Начислить" else "Списать"} ${kotlin.math.abs(n)} жетонов игроку $id?" to { act("grant", JSONObject().put("amount", n).put("reason", reason), "Готово"); amount = "" }
                }
            }
            Field(reason, "Причина (видна игроку в истории)", Modifier.fillMaxWidth()) { reason = it.take(80) }
            Caps("Оформление")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(item, "Предмет, например fr_crown", Modifier.weight(1f)) { item = it.trim().take(24) }
                Btn("Выдать", enabled = item.isNotEmpty()) { act("give_item", JSONObject().put("item", item), "Выдано: $item"); item = "" }
            }
            Text("Есть: " + (o.optJSONArray("owned")?.let { a -> (0 until a.length()).joinToString { a.getString(it) } }?.ifEmpty { "ничего" } ?: "ничего"), style = F.small)
            Caps("Модерация")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("Сбросить ник и девиз", Modifier.weight(1f)) { ask = "Сбросить ник и девиз игрока $id?" to { act("reset_nick", JSONObject(), "Ник сброшен") } }
                if (!o.optBoolean("admin")) {
                    if (o.optBoolean("banned")) Btn("Разблокировать", Modifier.weight(1f)) { act("unban", JSONObject(), "Разблокирован") }
                    else Btn("Заблокировать", Modifier.weight(1f), danger = true) { ask = "Заблокировать $id? Жетоны и покупки перестанут работать, игры — нет." to { act("ban", JSONObject().put("reason", reason), "Заблокирован") } }
                }
            }
            Caps("История жетонов")
            val led = o.optJSONArray("ledger") ?: JSONArray()
            Card(Modifier.fillMaxWidth()) {
                if (led.length() == 0) Text("Пусто", style = F.small)
                for (i in 0 until led.length()) led.getJSONObject(i).let { e ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(e.optString("at").take(10), style = F.small, modifier = Modifier.width(78.dp))
                        Text(e.optString("reason"), style = F.text.copy(fontSize = 13.sp), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val a = e.optInt("amount"); Text(if (a > 0) "+$a" else "$a", style = F.label.copy(fontSize = 13.sp, color = if (a > 0) C.ok else C.bad))
                    }
                }
            }
        }
    }
    ask?.let { (t, go) -> Confirm(t, { ask = null }) { ask = null; go() } }
}

/** Подтверждение действия. */
@Composable
fun Confirm(text: String, cancel: () -> Unit, ok: () -> Unit) {
    Dialog(cancel) {
        Column(Modifier.clip(RoundedCornerShape(22.dp)).background(C.s1).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(text, style = F.label)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Btn("Да", Modifier.weight(1f), gold = true, onClick = ok); Btn("Отмена", Modifier.weight(1f), onClick = cancel) }
        }
    }
}

// ---------- экономика ----------

@Composable
private fun Economy() {
    val scope = rememberCoroutineScope()
    var ev by remember { mutableStateOf<JSONObject?>(null) }
    var prices by remember { mutableStateOf(JSONArray()) }
    var tick by remember { mutableStateOf(0) }
    var mult by remember { mutableStateOf(2.0) }
    var hours by remember { mutableStateOf(48) }
    var title by remember { mutableStateOf("") }
    var edit by remember { mutableStateOf<Pair<String, String>?>(null) }
    LaunchedEffect(tick) {
        req("event_get").let { if (it.ok) ev = it.data as? JSONObject }
        req("prices").let { if (it.ok) prices = it.arr() }
    }
    Page {
        Caps("Событие «больше жетонов»")
        Card(Modifier.fillMaxWidth()) {
            val e = ev
            if (e != null) {
                Text("Идёт: ${e.optString("title")} · ×${e.optDouble("mult")}", style = F.label.copy(color = C.gold2))
                Text("До ${e.optString("until").take(16).replace('T', ' ')} (UTC)", style = F.small)
                Btn("Остановить", Modifier.fillMaxWidth(), danger = true) { scope.launch { if (req("event_off").ok) { St.toast = "Событие остановлено"; tick++ } } }
            } else Text("Сейчас событий нет", style = F.text)
        }
        Text("Множитель", style = F.small)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(1.5, 2.0, 3.0).forEach { m -> Chip("×" + (if (m % 1.0 == 0.0) m.toInt() else m), mult == m) { mult = m } } }
        Text("Сколько длится", style = F.small)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(24 to "сутки", 48 to "2 дня", 72 to "3 дня", 168 to "неделя").forEach { (h, t) -> Chip(t, hours == h) { hours = h } } }
        Field(title, "Название, например «Выходные ×2»", Modifier.fillMaxWidth()) { title = it.take(40) }
        Btn("Запустить событие", Modifier.fillMaxWidth(), gold = true) {
            scope.launch { if (req("event_set", JSONObject().put("mult", mult).put("hours", hours).put("title", title)).ok) { St.toast = "Событие запущено"; tick++ } }
        }
        Caps("Цены оформления (жетоны)")
        Card(Modifier.fillMaxWidth()) {
            for (i in 0 until prices.length()) prices.getJSONObject(i).let { p ->
                Row(Modifier.fillMaxWidth().clickable { edit = p.optString("item") to "${p.optInt("price")}" }.padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(p.optString("item"), style = F.text); Text("${p.optInt("price")}", style = F.label.copy(color = C.gold))
                }
            }
        }
    }
    edit?.let { (item, v0) ->
        var v by remember(item) { mutableStateOf(v0) }
        Dialog({ edit = null }) {
            Column(Modifier.clip(RoundedCornerShape(22.dp)).background(C.s1).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Цена: $item", style = F.h2)
                Field(v, "Жетоны", Modifier.fillMaxWidth(), digits = true) { v = it.filter(Char::isDigit).take(6) }
                Btn("Сохранить", Modifier.fillMaxWidth(), gold = true, enabled = (v.toIntOrNull() ?: 0) > 0) {
                    scope.launch { if (req("set_price", JSONObject().put("item", item).put("price", v.toInt())).ok) { St.toast = "Цена изменена"; edit = null; tick++ } }
                }
            }
        }
    }
}

@Composable
fun Chip(t: String, on: Boolean, onClick: () -> Unit) {
    Text(t, style = F.label.copy(fontSize = 13.sp, color = if (on) C.bg else C.ink2), modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(if (on) C.gold else C.s2).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp))
}

/** Баннеры профиля (картинки — те же, что в KAPSULA). Пусто — как выбрано в самом приложении. */
private val BANNERS = listOf("" to "Как в KAPSULA", "bn_creator" to "Зал славы", "bn_city" to "Ночной город", "bn_space" to "Космос",
    "bn_arcade" to "Аркадный зал", "bn_room" to "Уютная комната", "bn_forest" to "Лес на закате", "bn_reef" to "Подводные руины")

@Composable
private fun BannerPick(sel: String, onPick: (String) -> Unit) {
    val ctx = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BANNERS.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (id, t) ->
                    val on = id == sel
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(C.s1)
                        .border(2.dp, if (on) C.gold else Color.Transparent, RoundedCornerShape(14.dp)).clickable { onPick(id) }.padding(6.dp)) {
                        val bmp = remember(id) { if (id.isEmpty()) null else try { ctx.assets.open("look/$id.webp").use { android.graphics.BitmapFactory.decodeStream(it) }?.asImageBitmap() } catch (e: Exception) { null } }
                        Box(Modifier.fillMaxWidth().aspectRatio(3.6f).clip(RoundedCornerShape(10.dp)).background(C.s2), contentAlignment = Alignment.Center) {
                            if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            else Text("выбор из приложения", style = F.small)
                        }
                        Text(t, style = F.small.copy(color = if (on) C.gold else C.ink2), maxLines = 1, modifier = Modifier.padding(top = 4.dp, start = 2.dp))
                    }
                }
                if (row.size == 1) Box(Modifier.weight(1f))
            }
        }
    }
}

// ---------- новости ----------

@Composable
private fun News() {
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var list by remember { mutableStateOf(JSONArray()) }
    var tick by remember { mutableStateOf(0) }
    var ask by remember { mutableStateOf(false) }
    LaunchedEffect(tick) { val r = req("news_list"); if (r.ok) list = r.arr() }
    Page {
        Text("Новость придёт игрокам в уведомления при следующем заходе (30 дней).", style = F.text)
        Field(text, "Текст новости", Modifier.fillMaxWidth()) { text = it.take(300) }
        Text("${text.length} / 300", style = F.small)
        Btn("Отправить всем", Modifier.fillMaxWidth(), gold = true, enabled = text.isNotBlank()) { ask = true }
        Caps("Отправленные")
        for (i in 0 until list.length()) list.getJSONObject(i).let { n ->
            Card(Modifier.fillMaxWidth()) {
                Text(n.optString("text"), style = F.label.copy(fontSize = 14.sp, color = if (n.optBoolean("active")) C.ink else C.ink3))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(n.optString("at").take(16).replace('T', ' ') + if (n.optBoolean("active")) "" else " · скрыта", style = F.small)
                    if (n.optBoolean("active")) Text("Скрыть", style = F.label.copy(color = C.bad, fontSize = 13.sp), modifier = Modifier.clickable {
                        scope.launch { if (req("news_off", JSONObject().put("id", n.optLong("id"))).ok) tick++ }
                    })
                }
            }
        }
    }
    if (ask) Confirm("Отправить новость всем игрокам?", { ask = false }) {
        ask = false
        scope.launch { if (req("news_add", JSONObject().put("text", text.trim())).ok) { St.toast = "Новость отправлена"; text = ""; tick++ } }
    }
}

// ---------- ещё: обновления, журнал, выход ----------

@Composable
private fun More() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var ver by remember { mutableStateOf("") }
    var log by remember { mutableStateOf(JSONArray()) }
    LaunchedEffect(Unit) { val r = req("log"); if (r.ok) log = r.arr() }
    var status by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("gold") }
    var banner by remember { mutableStateOf("") }
    // текущие значения — с сервера (пустой запрос ничего не меняет)
    LaunchedEffect(Unit) {
        val r = req("creator_set")
        if (r.ok) r.obj().let { c -> status = c.optString("status"); name = c.optString("name", "gold"); banner = c.optString("banner") }
    }
    Page {
        Caps("Мой профиль")
        Text("Видно всем, кто откроет твой профиль в KAPSULA: строка под именем, цвет имени (с бликом) и баннер.", style = F.text)
        Field(status, "Статус, например «Делаю KAPSULA»", Modifier.fillMaxWidth()) { status = it.take(60) }
        Text("Цвет имени", style = F.small)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("gold" to "Золото", "rose" to "Роза", "emerald" to "Изумруд").forEach { (id, t) -> Chip(t, name == id) { name = id } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("neon" to "Неон", "ice" to "Лёд").forEach { (id, t) -> Chip(t, name == id) { name = id } }
        }
        Text("Баннер", style = F.small)
        BannerPick(banner) { banner = it }
        Btn("Сохранить профиль", Modifier.fillMaxWidth(), gold = true) {
            scope.launch { if (req("creator_set", JSONObject().put("status", status.trim()).put("name", name).put("banner", banner)).ok) St.toast = "Сохранено. В KAPSULA обновится при следующем заходе" }
        }
        Caps("Обязательное обновление")
        Text("У кого версия ниже — KAPSULA попросит обновиться. Пусто — не просить.", style = F.text)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(ver, "например 0.40.0", Modifier.weight(1f)) { ver = it.filter { c -> c.isDigit() || c == '.' }.take(12) }
            Btn("Сохранить", gold = true) { scope.launch { if (req("min_version", JSONObject().put("version", ver)).ok) St.toast = if (ver.isEmpty()) "Не просить обновляться" else "Минимальная версия: $ver" } }
        }
        Caps("Журнал действий")
        Card(Modifier.fillMaxWidth()) {
            if (log.length() == 0) Text("Пусто", style = F.small)
            for (i in 0 until log.length()) log.getJSONObject(i).let { l ->
                Column(Modifier.padding(vertical = 4.dp)) {
                    Text(word(l.optString("action")) + (l.optString("target").takeIf { it.isNotEmpty() && it != "null" }?.let { " · $it" } ?: ""), style = F.label.copy(fontSize = 14.sp))
                    Text(l.optString("at").take(16).replace('T', ' ') + (l.optJSONObject("details")?.toString()?.take(80)?.let { " · $it" } ?: ""), style = F.small, maxLines = 2)
                }
            }
        }
        Caps("Этот телефон")
        Text("ID телефона: ${Api.device.take(8)}…", style = F.small)
        Btn("Выйти", Modifier.fillMaxWidth(), danger = true) { Api.forget(ctx); St.logged = false }
    }
}

private fun word(a: String) = when (a) {
    "setup" -> "Настройка админа"; "rebind" -> "Новый телефон"; "grant" -> "Жетоны"; "give_item" -> "Выдан предмет"; "reset_nick" -> "Сброс ника"
    "ban" -> "Блокировка"; "unban" -> "Разблокировка"; "set_price" -> "Цена"; "event_set" -> "Событие запущено"; "event_off" -> "Событие остановлено"
    "news_add" -> "Новость"; "news_off" -> "Новость скрыта"; "min_version" -> "Минимальная версия"; "creator_set" -> "Мой профиль"; else -> a
}
