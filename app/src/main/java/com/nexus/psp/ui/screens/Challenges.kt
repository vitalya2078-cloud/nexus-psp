package com.nexus.psp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.Cloud
import com.nexus.psp.data.Tokens
import com.nexus.psp.ui.kit.Bar
import com.nexus.psp.ui.kit.BtnKind
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.KButton
import com.nexus.psp.ui.kit.SolarIcon
import com.nexus.psp.ui.kit.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Прогресс испытания словами: «3 ч 20 мин из 5 ч», «2 из 4 дней», «1 из 2 консолей». */
internal fun challengeProgress(o: JSONObject): String {
    val p = o.optInt("progress"); val t = o.optInt("target")
    fun h(m: Int) = if (m >= 60) "${m / 60} ч" + (if (m % 60 > 0) " ${m % 60} мин" else "") else "$m мин"
    return when (o.optString("kind")) {
        "play_min" -> "${h(p)} из ${h(t)}"
        "days" -> "$p из $t дн."
        else -> "$p из $t"
    }
}

/** Сколько осталось до конца недели: «ещё 3 дня», «ещё 5 ч». */
internal fun weekLeft(until: String, now: Long = System.currentTimeMillis()): String? {
    val end = try { java.time.OffsetDateTime.parse(until).toInstant().toEpochMilli() } catch (e: Exception) { return null }
    val h = ((end - now) / 3_600_000L).coerceAtLeast(0)
    return if (h >= 48) "ещё ${h / 24} дн." else "ещё $h ч"
}

/** «Испытания недели» во вкладке «Моё»: три цели, прогресс и кнопка «Забрать». Без таймеров и штрафов. */
@Composable
internal fun WeekChallenges() {
    val scope = rememberCoroutineScope()
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(-1L) }
    LaunchedEffect(Cloud.code, Cloud.lastOk) { if (Cloud.code.isNotEmpty()) err = withContext(Dispatchers.IO) { Cloud.loadChallenges() } }
    val w = Cloud.week
    val items = w?.optJSONArray("items")?.let { a -> (0 until a.length()).map { a.getJSONObject(it) } } ?: emptyList()
    if (w == null && err == null) return
    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("ИСПЫТАНИЯ НЕДЕЛИ", style = T.caps, modifier = Modifier.weight(1f))
        w?.optString("until")?.let { weekLeft(it) }?.let { Text(it, style = T.small.copy(fontSize = 12.sp)) }
    }
    if (items.isEmpty()) { Text(err ?: "На этой неделе испытаний нет", style = T.small); return }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { o ->
            val id = o.optLong("id"); val done = o.optInt("progress") >= o.optInt("target"); val claimed = o.optBoolean("claimed")
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(K.s1).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SolarIcon(if (claimed) R.drawable.ph_check else R.drawable.sl_trophy, 20.dp, if (done) K.acc else K.ink2)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(o.optString("title"), style = T.label.copy(fontSize = 14.sp), maxLines = 1)
                    Bar(o.optInt("progress").toFloat() / o.optInt("target").coerceAtLeast(1), modifier = Modifier.fillMaxWidth())
                    Text(challengeProgress(o) + " · +${o.optInt("reward")} ${Tokens.word(o.optInt("reward"))}", style = T.small.copy(fontSize = 12.sp))
                }
                when {
                    claimed -> Text("Получено", style = T.small.copy(fontSize = 12.sp))
                    done -> KButton(if (busy == id) "…" else "Забрать", kind = BtnKind.Main, small = true, enabled = busy < 0) {
                        busy = id
                        scope.launch {
                            val (n, e) = withContext(Dispatchers.IO) { Cloud.claim(id) }
                            busy = -1
                            AppState.say(e ?: "+$n ${Tokens.word(n)} · ${o.optString("title")}")
                        }
                    }
                }
            }
        }
        Text("Прогресс считается после игры, при возвращении в KAPSULA. Новые испытания — каждый понедельник.", style = T.small.copy(fontSize = 11.sp))
    }
}
