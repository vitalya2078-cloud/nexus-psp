package com.nexus.psp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.Cloud
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.Ov
import com.nexus.psp.ui.kit.BtnKind
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.KButton
import com.nexus.psp.ui.kit.Mascot
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.Toggle
import com.nexus.psp.ui.kit.press
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Зелёный «в игре». */
internal val LiveGreen = Color(0xFF8FC9A3)

/** Подпись «во что играет»: текст и идёт ли игра сейчас; null — ничего не известно (или скрыто). */
internal fun playLine(p: JSONObject?): Pair<String, Boolean>? {
    val t = p?.optString("t").orEmpty(); if (t.isEmpty()) return null
    val live = p!!.optBoolean("live")
    val pl = com.nexus.psp.data.PL[p.optString("p")]?.short
    val at = try { java.time.OffsetDateTime.parse(p.optString(if (live) "since" else "at")).toInstant().toEpochMilli() } catch (e: Exception) { 0L }
    val m = if (at > 0) ((System.currentTimeMillis() - at) / 60000).toInt().coerceAtLeast(0) else -1
    val ago = when {
        m < 0 -> null
        live -> if (m < 60) "$m мин" else "${m / 60} ч ${m % 60} мин"
        m < 1 -> "только что"; m < 60 -> "$m мин назад"; m < 1440 -> "${m / 60} ч назад"; else -> "${m / 1440} дн назад"
    }
    return listOfNotNull((if (live) "Играет: " else "Недавно: ") + t, pl, ago).joinToString(" · ") to live
}

/** Вкладка «Друзья» в «Моё»: добавить по ID, заявки, список друзей, «скрывать, во что я играю». */
@Composable
fun FriendsPane() {
    val scope = rememberCoroutineScope()
    var id by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { err = withContext(Dispatchers.IO) { Cloud.loadFriends() } }
    fun act(op: String, to: String, after: () -> Unit = {}) {
        if (busy) return
        busy = true
        scope.launch {
            val msg = withContext(Dispatchers.IO) { Cloud.friend(op, to) }
            busy = false; AppState.say(msg); after()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // добавить по ID
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(K.s1).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SearchField(id, "ID друга, например 7Q4M2K", Modifier.weight(1f), icon = R.drawable.sl_user,
                onDone = { if (id.length >= 6) act("add", id) { id = "" } }) { v -> id = v.uppercase().filter { it.isLetterOrDigit() }.take(8) }
            KButton("Добавить", R.drawable.ph_plus, BtnKind.Main, small = true) { if (id.length >= 6) act("add", id) { id = "" } else AppState.say("ID — 6 букв и цифр, например 7Q4M2K") }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (Cloud.shortId.isNotEmpty()) "Твой ID — ${Cloud.shortId}. Дай его друзьям, чтобы они тебя добавили" else "Твой ID появится, когда аккаунт создастся",
                style = T.small, modifier = Modifier.weight(1f))
            Text("Скрывать, во что я играю", style = T.small)
            Toggle(Cloud.hidePlay) { Cloud.hidePlaying(it) }
        }
        val f = Cloud.friends
        when {
            f == null && err != null -> FriendsEmpty("wait", err!!)
            f == null -> Text("Загружаю…", style = T.body)
            else -> {
                val inc = f.optJSONArray("incoming") ?: JSONArray()
                val out = f.optJSONArray("outgoing") ?: JSONArray()
                val fr = f.optJSONArray("friends") ?: JSONArray()
                if (inc.length() > 0) {
                    FriendsCaps("Хотят дружить · ${inc.length()}")
                    Cards(inc) { o -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        KButton("Принять", kind = BtnKind.Main, small = true) { act("accept", o.optString("short_id")) }
                        KButton(icon = R.drawable.ph_x, small = true) { act("decline", o.optString("short_id")) }
                    } }
                }
                FriendsCaps(if (fr.length() > 0) "Друзья · ${fr.length()}" else "Друзья")
                if (fr.length() == 0) FriendsEmpty("hug", "Пока никого. Добавь друга по ID — и здесь будет видно, во что он играет")
                else Cards(fr, null)
                if (out.length() > 0) {
                    FriendsCaps("Ждут ответа · ${out.length()}")
                    Cards(out) { o -> KButton("Отменить", small = true) { act("cancel", o.optString("short_id")) } }
                }
            }
        }
    }
}

@Composable
private fun FriendsCaps(t: String) = Text(t.uppercase(), style = T.caps, modifier = Modifier.padding(top = 8.dp))

@Composable
private fun FriendsEmpty(pose: String, text: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(K.s1).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Mascot(pose, 64.dp); Text(text, style = T.body, modifier = Modifier.weight(1f))
    }
}

/** Карточки по две в ряд. */
@Composable
private fun Cards(a: JSONArray, actions: (@Composable (JSONObject) -> Unit)?) {
    val list = (0 until a.length()).map { a.getJSONObject(it) }
    list.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { o -> FriendCard(o, Modifier.weight(1f), actions) }
            if (row.size == 1) Box(Modifier.weight(1f))
        }
    }
}

@Composable
fun FriendCard(o: JSONObject, modifier: Modifier = Modifier, actions: (@Composable (JSONObject) -> Unit)? = null) {
    val nick = o.optString("nick").ifBlank { "Игрок" }
    val admin = o.optBoolean("is_admin")
    val id = o.optString("short_id")
    val play = playLine(o.optJSONObject("playing"))
    Row(modifier.clip(RoundedCornerShape(18.dp)).background(K.s1).press { Nav.open(Ov.Player(id)) }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box {
            AvatarOf(o, nick, 54.dp, admin)
            if (play?.second == true) Box(Modifier.align(Alignment.BottomEnd).size(14.dp).clip(CircleShape).background(K.s1).padding(2.dp).clip(CircleShape).background(LiveGreen))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PlayerName(nick, admin, T.label.copy(fontSize = 15.sp), "gold", Modifier.weight(1f, fill = false))
                if (admin) PixelCrown(14.dp)
            }
            Text("ID $id · Ур. ${o.optInt("level", 1)}", style = T.small.copy(fontSize = 11.5.sp), maxLines = 1)
            play?.let { (t, live) -> Text(t, style = T.small.copy(fontSize = 12.sp, color = if (live) LiveGreen else K.ink2), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        actions?.invoke(o)
    }
}

/** Кнопки дружбы в чужом профиле. */
@Composable
fun FriendActions(id: String) {
    if (id.isEmpty() || id == Cloud.shortId) return
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var sure by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (Cloud.friends == null) withContext(Dispatchers.IO) { Cloud.loadFriends() } }
    fun act(op: String) {
        if (busy) return
        busy = true
        scope.launch { val m = withContext(Dispatchers.IO) { Cloud.friend(op, id) }; busy = false; sure = false; AppState.say(m) }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        when (Cloud.relation(id)) {
            "friend" -> {
                Text("В друзьях", style = T.label.copy(color = LiveGreen))
                if (sure) { KButton("Точно удалить", small = true, danger = true) { act("remove") }; KButton("Нет", small = true) { sure = false } }
                else KButton("Удалить из друзей", small = true) { sure = true }
            }
            "in" -> { KButton("Принять заявку", R.drawable.ph_plus, BtnKind.Main, small = true) { act("accept") }; KButton("Отклонить", small = true) { act("decline") } }
            "out" -> { Text("Заявка отправлена", style = T.label); KButton("Отменить", small = true) { act("cancel") } }
            else -> KButton("Добавить в друзья", R.drawable.ph_plus, BtnKind.Main, small = true) { act("add") }
        }
    }
}
