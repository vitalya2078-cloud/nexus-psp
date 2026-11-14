package com.nexus.psp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.data.AppState
import com.nexus.psp.data.Game
import com.nexus.psp.emulator.GamePresets
import com.nexus.psp.emulator.Guide
import com.nexus.psp.emulator.Templates
import com.nexus.psp.ui.Cover
import com.nexus.psp.ui.Full
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.OvHead
import com.nexus.psp.ui.Ov
import com.nexus.psp.ui.kit.CtlIcon
import com.nexus.psp.ui.kit.GuidePanel
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.Mascot
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.press
import org.json.JSONObject

/** Настройки кнопок игры: свои (из её настроек) → встроенная раскладка из базы → обычный геймпад. */
fun ctlOfGame(c: android.content.Context, g: Game): JSONObject? =
    com.nexus.psp.data.GameConfig(c, g.id).read().optJSONObject("ctl") ?: GamePresets.find(g.t, AppState.meta[g.id]?.title)?.second

/** Ярлык на обложке: «★ Своя» — у игры своя раскладка из базы, иначе значок жанра. */
@Composable
private fun GuideBadge(g: Game, modifier: Modifier) {
    val title2 = AppState.meta[g.id]?.title
    val own = GamePresets.ownOf(g.t, title2)
    val genre = GamePresets.genreOf(g.t, title2)?.let { Templates.genre(it) }
    Row(
        modifier.clip(RoundedCornerShape(9.dp)).background(Color(0xE6101116)).padding(horizontal = 7.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (own) { Text("★", style = T.num.copy(fontSize = 11.sp, color = K.warn)); Text("Своя", style = T.num.copy(fontSize = 11.sp, color = K.warn)) }
        else CtlIcon(genre?.icon ?: "fb", 14.dp, Color(0xFFB8B7B1))
    }
}

/** Вкладка «Управление»: твои игры, поиск по базе раскладок, основы и своя раскладка. */
@Composable
fun ControlsTab() {
    var all by rememberSaveable { mutableStateOf(false) }
    var q by rememberSaveable { mutableStateOf("") }
    val games = AppState.byId.values.sortedByDescending { AppState.lib[it.id]?.t ?: 0 }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // небольшие обложки: колонок столько, сколько влезает по ~110 dp (на телефоне в альбомной — 7)
        val cols = ((maxWidth - 40.dp) / 112.dp).toInt().coerceIn(4, 9)
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text("ТВОИ ИГРЫ", style = T.caps) }
            if (games.isEmpty()) item {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(K.s1).padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Mascot("search", 72.dp)
                    Column { Text("Игр пока нет", style = T.h3); Text("Добавь игру — здесь появится её управление. Или найди любую ниже.", style = T.body.copy(fontSize = 13.sp)) }
                }
            } else {
                val shown = if (all) games else games.take(cols * 2)
                shown.chunked(cols).forEach { row ->
                    item(key = "r" + row.first().id) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { g -> GameTile(g, Modifier.weight(1f)) }
                            repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                if (games.size > cols * 2) item {
                    Text(if (all) "Свернуть" else "Все игры (${games.size})", style = T.label.copy(color = K.acc),
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).press { all = !all }.padding(vertical = 4.dp, horizontal = 2.dp))
                }
            }
            item { Text("НАЙТИ ИГРУ", style = T.caps, modifier = Modifier.padding(top = 8.dp)) }
            item { SearchField(q, "Название игры — посмотреть её управление", Modifier.fillMaxWidth()) { q = it } }
            if (q.trim().length >= 2) {
                val found = GamePresets.entries.filter { it.name.contains(q.trim(), true) }.take(12)
                if (found.isEmpty()) item { Text("Такой игры в базе нет — у неё будет раскладка по жанру, который выберешь при первом запуске.", style = T.small) }
                found.forEach { e -> item(key = "f" + e.name) { FoundRow(e) } }
            }
            item { Text("ОСНОВЫ", style = T.caps, modifier = Modifier.padding(top = 8.dp)) }
            item { Basics(if (maxWidth > 600.dp) 3 else 2) }
            item { Text("СВОЯ РАСКЛАДКА", style = T.caps, modifier = Modifier.padding(top = 8.dp)) }
            item { OwnSteps() }
        }
    }
}

@Composable
private fun GameTile(g: Game, modifier: Modifier) {
    Column(modifier.press { Nav.open(Ov.Guide(id = g.id)) }, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(12.dp)).border(1.dp, K.line, RoundedCornerShape(12.dp))) {
            Cover(g, 300, 400, modifier = Modifier.fillMaxSize())
            GuideBadge(g, Modifier.align(Alignment.BottomEnd).padding(6.dp))
        }
        Text(g.t, style = T.small.copy(fontSize = 12.sp, color = K.ink2), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FoundRow(e: GamePresets.Entry) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(K.s1).press { Nav.open(Ov.Guide(preset = e.name)) }.padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CtlIcon(Templates.genre(e.genre)?.icon ?: "fb", 20.dp, K.ink2)
        Text(e.name, style = T.label.copy(fontSize = 14.sp), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (e.own) "★ Своя" else (Templates.genre(e.genre)?.name ?: ""), style = T.num.copy(fontSize = 12.sp, color = if (e.own) K.warn else K.ink3))
    }
}

// ---------- основы: шесть карточек со схемами ----------
private class Basic(val title: String, val text: String, val draw: DrawScope.(Color, Color) -> Unit)

private val BASICS = listOf(
    Basic("Стик", "Коснись левой половины — стик появится под пальцем") { ink, acc ->
        val c = Offset(size.width * .5f, size.height * .55f); val r = size.minDimension * .32f
        drawCircle(ink.copy(alpha = .18f), r, c); drawCircle(ink.copy(alpha = .5f), r, c, style = Stroke(2.dp.toPx()))
        drawCircle(acc.copy(alpha = .35f), r * .45f, Offset(c.x + r * .38f, c.y - r * .2f)); drawCircle(acc, r * .45f, Offset(c.x + r * .38f, c.y - r * .2f), style = Stroke(2.dp.toPx()))
    },
    Basic("Камера свайпом", "Веди пальцем справа — камера крутится. Два касания — назад") { ink, acc ->
        val w = 2.dp.toPx(); val y = size.height * .55f
        drawRoundRect(ink.copy(alpha = .14f), Offset(size.width * .14f, size.height * .2f), Size(size.width * .72f, size.height * .66f), androidx.compose.ui.geometry.CornerRadius(14f), style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
        drawLine(acc, Offset(size.width * .3f, y), Offset(size.width * .7f, y), w * 1.4f)
        drawLine(acc, Offset(size.width * .3f, y), Offset(size.width * .36f, y - 9f), w * 1.4f); drawLine(acc, Offset(size.width * .3f, y), Offset(size.width * .36f, y + 9f), w * 1.4f)
        drawLine(acc, Offset(size.width * .7f, y), Offset(size.width * .64f, y - 9f), w * 1.4f); drawLine(acc, Offset(size.width * .7f, y), Offset(size.width * .64f, y + 9f), w * 1.4f)
    },
    Basic("Панель «⋯»", "Карта, меню и другое редкое — не мешают на экране") { ink, acc ->
        val c = Offset(size.width * .5f, size.height * .62f); val r = size.minDimension * .2f
        drawCircle(ink.copy(alpha = .5f), r, c, style = Stroke(2.dp.toPx()))
        for (i in -1..1) drawCircle(acc, r * .16f, Offset(c.x + i * r * .45f, c.y))
        drawRoundRect(ink.copy(alpha = .18f), Offset(size.width * .22f, size.height * .14f), Size(size.width * .56f, size.height * .22f), androidx.compose.ui.geometry.CornerRadius(20f))
    },
    Basic("Смена раскладки", "Пешком, в машине, бой — у каждого режима свои кнопки") { ink, acc ->
        val w = 2.dp.toPx(); val s = Size(size.width * .26f, size.height * .42f)
        drawRoundRect(ink.copy(alpha = .5f), Offset(size.width * .14f, size.height * .3f), s, androidx.compose.ui.geometry.CornerRadius(10f), style = Stroke(w))
        drawRoundRect(acc, Offset(size.width * .6f, size.height * .3f), s, androidx.compose.ui.geometry.CornerRadius(10f), style = Stroke(w))
        val y = size.height * .51f; drawLine(acc, Offset(size.width * .43f, y), Offset(size.width * .57f, y), w); drawLine(acc, Offset(size.width * .57f, y), Offset(size.width * .52f, y - 7f), w); drawLine(acc, Offset(size.width * .57f, y), Offset(size.width * .52f, y + 7f), w)
    },
    Basic("Одна вместо двух", "Сочетания вроде «R + □» собраны в одну кнопку") { ink, acc ->
        val r = size.minDimension * .15f; val y = size.height * .55f
        drawCircle(ink.copy(alpha = .5f), r, Offset(size.width * .26f, y), style = Stroke(2.dp.toPx())); drawCircle(ink.copy(alpha = .5f), r, Offset(size.width * .46f, y), style = Stroke(2.dp.toPx()))
        drawCircle(acc.copy(alpha = .3f), r * 1.3f, Offset(size.width * .74f, y)); drawCircle(acc, r * 1.3f, Offset(size.width * .74f, y), style = Stroke(2.dp.toPx()))
    },
    Basic("Удержание", "Некоторые кнопки работают, пока держишь палец") { ink, acc ->
        val c = Offset(size.width * .5f, size.height * .55f); val r = size.minDimension * .28f
        drawCircle(ink.copy(alpha = .2f), r, c, style = Stroke(4.dp.toPx()))
        drawArc(acc, -90f, 250f, false, Offset(c.x - r, c.y - r), Size(r * 2, r * 2), style = Stroke(4.dp.toPx()))
        drawCircle(acc.copy(alpha = .6f), r * .3f, c)
    },
)

@Composable
private fun Basics(cols: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BASICS.chunked(cols).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { b ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(K.s1).border(1.dp, K.line, RoundedCornerShape(18.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val ink = K.ink; val acc = K.acc
                        Canvas(Modifier.fillMaxWidth().height(70.dp)) { b.draw(this, ink, acc) }
                        Text(b.title, style = T.h3.copy(fontSize = 15.sp))
                        Text(b.text, style = T.small.copy(fontSize = 12.sp, lineHeight = 16.sp))
                    }
                }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun OwnSteps() {
    val steps = listOf("В игре открой паузу → «Кнопки»", "Двигай кнопки, меняй размер и значок", "Собери свою кнопку из нескольких — «Добавить»")
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(K.s1).border(1.dp, K.line, RoundedCornerShape(18.dp)).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        steps.forEachIndexed { i, s ->
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(28.dp).clip(CircleShape).background(K.acc.copy(alpha = .18f)), contentAlignment = Alignment.Center) { Text("${i + 1}", style = T.num.copy(color = K.acc)) }
                Text(s, style = T.small.copy(fontSize = 12.5.sp, lineHeight = 16.sp, color = K.ink2))
            }
        }
    }
}

/** Экран гайда для игры из библиотеки или из базы раскладок. */
@Composable
fun GuideScreen(id: String?, preset: String?) {
    val ctx = LocalContext.current
    val g = id?.let { AppState.byId[it] }
    val title = g?.t ?: preset ?: "Управление"
    val plat = g?.p ?: "psp"
    val ctl = remember(id, preset) { if (g != null) ctlOfGame(ctx, g) else preset?.let { GamePresets.byName(it) } }
    val modes = remember(id, preset) { Guide.modes(ctl, plat) }
    Full {
        OvHead(title, back = true)
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth > 700.dp
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
                item { GuidePanel(modes, ctl, plat, wide) }
                if (g != null && ctl == null) item {
                    Text("Своей раскладки у игры нет: при первом запуске KAPSULA спросит жанр и подберёт кнопки.", style = T.small, textAlign = TextAlign.Start, modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
    }
}
