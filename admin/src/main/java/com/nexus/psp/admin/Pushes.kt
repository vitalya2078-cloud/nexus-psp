package com.nexus.psp.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Картинка по https-ссылке (превью пуша), до 2 МБ. null — нет или не загрузилась. */
@Composable
fun NetImage(url: String, modifier: Modifier) {
    val img by produceState<ImageBitmap?>(null, url) {
        value = if (!url.startsWith("https://")) null else withContext(Dispatchers.IO) {
            try {
                val c = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                c.connectTimeout = 8000; c.readTimeout = 10000
                val b = c.inputStream.use { it.readBytes() }
                if (b.size > 2 * 1024 * 1024) null else android.graphics.BitmapFactory.decodeByteArray(b, 0, b.size)?.asImageBitmap()
            } catch (e: Exception) { null }
        }
    }
    img?.let { Image(it, null, modifier, contentScale = ContentScale.Crop) }
}

/** Как пуш будет выглядеть на телефоне игрока. */
@Composable
fun PushPreview(title: String, body: String, image: String) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(C.s2).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(18.dp).clip(RoundedCornerShape(5.dp)).background(C.gold), contentAlignment = Alignment.Center) { Text("K", style = F.small.copy(color = C.bg, fontSize = 11.sp)) }
            Text("KAPSULA · сейчас", style = F.small)
        }
        Text(title.ifBlank { "Заголовок" }, style = F.label.copy(fontSize = 15.sp, color = if (title.isBlank()) C.ink3 else C.ink))
        if (body.isNotBlank()) Text(body, style = F.text.copy(fontSize = 14.sp))
        if (image.isNotBlank()) {
            if (!image.startsWith("https://")) Text("Картинка — только ссылка https://", style = F.small.copy(color = C.bad))
            else NetImage(image, Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(12.dp)))
        }
    }
}

/** Поиск игрока по ID или нику — тот же, что во вкладке «Игроки». */
@Composable
fun PlayerPick(onPick: (String, String) -> Unit) {
    var q by remember { mutableStateOf("") }
    var list by remember { mutableStateOf(JSONArray()) }
    LaunchedEffect(q) { if (q.isBlank()) { list = JSONArray(); return@LaunchedEffect }; kotlinx.coroutines.delay(300); val r = req("players", JSONObject().put("q", q.trim())); if (r.ok) list = r.arr() }
    Field(q, "ID или ник игрока", Modifier.fillMaxWidth()) { q = it.take(20) }
    if (q.isNotBlank() && list.length() == 0) Text("Никого не нашли", style = F.small)
    for (i in 0 until minOf(list.length(), 6)) list.getJSONObject(i).let { p ->
        val nick = p.optString("nick").takeIf { it.isNotEmpty() && it != "null" } ?: "без имени"
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.s1).clickable { onPick(p.optString("short_id"), nick) }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(p.optString("short_id"), style = F.label)
            Text(nick, style = F.small, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("ур. ${p.optInt("level")}", style = F.small)
        }
    }
}

@Composable
fun Pushes() {
    val scope = rememberCoroutineScope()
    var tick by remember { mutableStateOf(0) }
    var hist by remember { mutableStateOf(JSONObject()) }
    var tips by remember { mutableStateOf(JSONObject()) }
    LaunchedEffect(tick) {
        req("push_list").let { if (it.ok) hist = it.obj() }
        req("tips_list").let { if (it.ok) tips = it.obj() }
    }
    // новый пуш
    var all by remember { mutableStateOf(true) }
    var target by remember { mutableStateOf<Pair<String, String>?>(null) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var image by remember { mutableStateOf("") }
    var ask by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    var edit by remember { mutableStateOf<JSONObject?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat("${hist.optInt("phones")}", "телефонов с push (вошли в аккаунт)", Modifier.weight(1f), gold = true)
            Stat("${hist.optInt("players_with_push")}", "игроков с push", Modifier.weight(1f))
        }
        Text("Пуш «всем» приходит всем установившим KAPSULA 0.50.4+ (и без аккаунта). Одному игроку — на его телефоны и в колокольчик.", style = F.small)

        Caps("Новый пуш")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip("Всем", all) { all = true }
            Chip("Одному игроку", !all) { all = false }
        }
        if (!all) {
            val t = target
            if (t != null) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(C.gold.copy(alpha = .14f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Кому: ${t.first} · ${t.second}", style = F.label, modifier = Modifier.weight(1f))
                Text("Сменить", style = F.label.copy(color = C.gold, fontSize = 13.sp), modifier = Modifier.clickable { target = null })
            } else PlayerPick { id, nick -> target = id to nick }
        }
        Field(title, "Заголовок", Modifier.fillMaxWidth()) { title = it.take(80) }
        Field(body, "Текст", Modifier.fillMaxWidth()) { body = it.take(300) }
        Field(image, "Картинка: ссылка https://… (необязательно)", Modifier.fillMaxWidth()) { image = it.trim().take(500) }
        Text("Предпросмотр", style = F.small)
        PushPreview(title, body, image)
        val ready = title.isNotBlank() && (all || target != null) && (image.isBlank() || image.startsWith("https://"))
        Btn(if (all) "Отправить всем" else "Отправить игроку", Modifier.fillMaxWidth(), gold = true, enabled = ready) {
            ask = (if (all) "Отправить пуш всем игрокам?" else "Отправить пуш игроку ${target?.first}?") to {
                scope.launch {
                    val a = JSONObject().put("title", title.trim()).put("body", body.trim()).put("image", image)
                    if (!all) a.put("target", target!!.first)
                    val r = req("push_send", a)
                    if (r.ok) {
                        St.toast = if (all) "Отправлено всем" else r.obj().optInt("phones").let { n -> if (n > 0) "Отправлено: телефонов $n" else "Отправлено в колокольчик. Push у игрока не включён — увидит при заходе" }
                        title = ""; body = ""; image = ""; tick++
                    }
                }
            }
        }

        // советы по расписанию
        Caps("Советы от Сейвика (по расписанию)")
        val cfg = tips.optJSONObject("cfg") ?: JSONObject()
        var on by remember(cfg.toString()) { mutableStateOf(cfg.optBoolean("enabled", true)) }
        var hours by remember(cfg.toString()) { mutableStateOf(cfg.optJSONArray("hours")?.let { a -> (0 until a.length()).map { a.getInt(it) }.toSet() } ?: setOf(10, 16, 21)) }
        Card(Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Chip("Включены", on) { on = true }; Chip("Выключены", !on) { on = false }
            }
            Text("Часы отправки (Москва). Раз в 6 часов — 0, 6, 12, 18. Игрок может выключить советы у себя в настройках.", style = F.small, modifier = Modifier.padding(top = 8.dp))
            (0..23).chunked(6).forEach { row ->
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { h -> HourChip(h, h in hours) { hours = if (h in hours) hours - h else hours + h } }
                }
            }
            Btn("Сохранить расписание", Modifier.fillMaxWidth().padding(top = 10.dp), gold = true, enabled = hours.isNotEmpty() && hours.size <= 12) {
                scope.launch { if (req("tips_cfg", JSONObject().put("enabled", on).put("hours", JSONArray(hours.sorted()))).ok) { St.toast = if (on) "Советы: ${hours.sorted().joinToString(", ") { "$it:00" }}" else "Советы выключены"; tick++ } }
            }
            cfg.optString("last").takeIf { it.isNotEmpty() }?.let { Text("Последний по расписанию: $it:00", style = F.small, modifier = Modifier.padding(top = 6.dp)) }
        }
        val list = tips.optJSONArray("tips") ?: JSONArray()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Всего ${list.length()} · включено ${(0 until list.length()).count { list.getJSONObject(it).optBoolean("active") }}. Очередь — кто дольше не отправлялся", style = F.small, modifier = Modifier.weight(1f))
            Text("+ Добавить", style = F.label.copy(color = C.gold, fontSize = 14.sp), modifier = Modifier.clickable { edit = JSONObject() }.padding(6.dp))
        }
        for (i in 0 until list.length()) list.getJSONObject(i).let { t -> TipRow(t, onEdit = { edit = t }, onToggle = {
            scope.launch { if (req("tip_set", JSONObject().put("id", t.optInt("id")).put("active", !t.optBoolean("active"))).ok) tick++ }
        }, onSend = {
            ask = "Отправить совет «${t.optString("title")}» всем сейчас?" to { scope.launch { if (req("tip_send", JSONObject().put("id", t.optInt("id"))).ok) { St.toast = "Совет отправлен"; tick++ } } }
        }) }

        Caps("Отправленные")
        val msgs = hist.optJSONArray("msgs") ?: JSONArray()
        val pers = hist.optJSONArray("personal") ?: JSONArray()
        Card(Modifier.fillMaxWidth()) {
            if (msgs.length() == 0 && pers.length() == 0) Text("Пока пусто", style = F.small)
            for (i in 0 until pers.length()) pers.getJSONObject(i).let { m -> HistRow("→ ${m.optString("to")}", m.optString("title"), m.optString("at")) }
            for (i in 0 until msgs.length()) msgs.getJSONObject(i).let { m -> HistRow(if (m.isNull("tip")) "всем" else "совет", m.optString("title"), m.optString("at")) }
        }
    }
    edit?.let { t -> TipEdit(t, { edit = null }) { args -> scope.launch { if (req("tip_set", args).ok) { St.toast = "Сохранено"; edit = null; tick++ } } } }
    ask?.let { (t, go) -> Confirm(t, { ask = null }) { ask = null; go() } }
}

@Composable
private fun HourChip(h: Int, on: Boolean, onClick: () -> Unit) {
    Text("%02d".format(h), style = F.label.copy(fontSize = 13.sp, color = if (on) C.bg else C.ink2),
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (on) C.gold else C.s2).clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 7.dp))
}

@Composable
private fun TipRow(t: JSONObject, onEdit: () -> Unit, onToggle: () -> Unit, onSend: () -> Unit) {
    val on = t.optBoolean("active")
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.s1).clickable(onClick = onEdit).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("#${t.optInt("id")}", style = F.small)
            Text(t.optString("title"), style = F.label.copy(fontSize = 14.sp, color = if (on) C.ink else C.ink3), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (!t.optString("image").let { it.isEmpty() || it == "null" }) Tag("картинка", C.ink2)
        }
        Text(t.optString("body"), style = F.small.copy(color = if (on) C.ink2 else C.ink3), maxLines = 2)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("отправлен ${t.optInt("sent")} раз" + (t.optString("last").takeIf { it.isNotEmpty() && it != "null" }?.let { " · ${it.take(16).replace('T', ' ')}" } ?: ""), style = F.small, modifier = Modifier.weight(1f))
            Text(if (on) "Выключить" else "Включить", style = F.label.copy(color = if (on) C.bad else C.ok, fontSize = 13.sp), modifier = Modifier.clickable(onClick = onToggle))
            Text("Отправить", style = F.label.copy(color = C.gold, fontSize = 13.sp), modifier = Modifier.clickable(onClick = onSend))
        }
    }
}

@Composable
private fun HistRow(who: String, title: String, at: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(at.take(16).replace('T', ' ').drop(5), style = F.small)
        Text(who, style = F.small.copy(color = C.gold2))
        Text(title, style = F.text.copy(fontSize = 13.sp), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TipEdit(t: JSONObject, close: () -> Unit, save: (JSONObject) -> Unit) {
    val isNew = !t.has("id")
    var title by remember { mutableStateOf(t.optString("title")) }
    var body by remember { mutableStateOf(t.optString("body")) }
    var image by remember { mutableStateOf(t.optString("image").takeIf { it != "null" } ?: "") }
    Dialog(close) {
        Column(Modifier.clip(RoundedCornerShape(22.dp)).background(C.s1).verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (isNew) "Новый совет" else "Совет #${t.optInt("id")}", style = F.h2)
            Field(title, "Заголовок", Modifier.fillMaxWidth()) { title = it.take(80) }
            Field(body, "Текст", Modifier.fillMaxWidth()) { body = it.take(300) }
            Field(image, "Картинка https://… (необязательно)", Modifier.fillMaxWidth()) { image = it.trim().take(500) }
            PushPreview(title, body, image)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn("Сохранить", Modifier.weight(1f), gold = true, enabled = title.isNotBlank() && body.isNotBlank() && (image.isBlank() || image.startsWith("https://"))) {
                    save(JSONObject().put("title", title.trim()).put("body", body.trim()).put("image", image).apply { if (!isNew) put("id", t.optInt("id")) })
                }
                Btn("Отмена", Modifier.weight(1f), onClick = close)
            }
        }
    }
}

/** Подарки по коду (сайт → kapsula://gift/КОД): включить, выключить, добавить. */
@Composable
fun Gifts() {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf(JSONArray()) }
    var tick by remember { mutableStateOf(0) }
    var code by remember { mutableStateOf("") }
    var item by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    LaunchedEffect(tick) { val r = req("gifts"); if (r.ok) list = r.arr() }
    Caps("Подарки по коду")
    Card(Modifier.fillMaxWidth()) {
        if (list.length() == 0) Text("Подарков нет", style = F.small)
        for (i in 0 until list.length()) list.getJSONObject(i).let { g ->
            val on = g.optBoolean("active")
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(g.optString("code"), style = F.label.copy(color = if (on) C.gold2 else C.ink3))
                    Text("${g.optString("title")} · ${g.optString("item")} · забрали ${g.optInt("claims")}", style = F.small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(if (on) "Выключить" else "Включить", style = F.label.copy(color = if (on) C.bad else C.ok, fontSize = 13.sp), modifier = Modifier.clickable {
                    scope.launch { if (req("gift_set", JSONObject().put("code", g.optString("code")).put("active", !on)).ok) tick++ }
                })
            }
        }
    }
    Field(code, "Код, например NY2027", Modifier.fillMaxWidth()) { code = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(24) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Field(item, "Предмет (fr_winter)", Modifier.weight(1f)) { item = it.trim().take(24) }
        Field(name, "Название", Modifier.weight(1f)) { name = it.take(40) }
    }
    Btn("Добавить подарок", Modifier.fillMaxWidth(), enabled = code.isNotBlank() && item.isNotBlank()) {
        scope.launch { if (req("gift_set", JSONObject().put("code", code).put("item", item).put("name", name).put("active", true)).ok) { St.toast = "Подарок $code включён"; code = ""; item = ""; name = ""; tick++ } }
    }
}
