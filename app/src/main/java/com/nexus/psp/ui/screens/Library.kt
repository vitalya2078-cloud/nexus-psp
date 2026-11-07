package com.nexus.psp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.AppState.S
import com.nexus.psp.data.Game
import com.nexus.psp.data.PLATFORMS
import com.nexus.psp.data.plural
import com.nexus.psp.ui.Act
import com.nexus.psp.ui.Full
import com.nexus.psp.ui.GameCard
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.kit.Chip
import com.nexus.psp.ui.kit.ChipRow
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.KButton
import com.nexus.psp.ui.kit.SolarIcon
import com.nexus.psp.ui.kit.StateCard
import com.nexus.psp.ui.kit.Sub
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.tap
import kotlinx.coroutines.delay

/** Поле поиска в стиле KAPSULA. */
@Composable
fun SearchField(value: String, hint: String, modifier: Modifier = Modifier, autoFocus: Boolean = false, onDone: () -> Unit = {}, onChange: (String) -> Unit) {
    val fr = remember { FocusRequester() }
    if (autoFocus) LaunchedEffect(Unit) { delay(120); fr.requestFocus() }
    Row(
        modifier.height(42.dp).clip(RoundedCornerShape(21.dp)).background(K.s2).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SolarIcon(R.drawable.sl_search, 18.dp, K.ink3)
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(hint, style = T.label.copy(color = K.ink3, fontSize = 14.sp))
            BasicTextField(
                value, onChange, singleLine = true, textStyle = T.label.copy(fontSize = 14.sp), cursorBrush = SolidColor(K.acc),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search), keyboardActions = KeyboardActions(onSearch = { onDone() }),
                modifier = Modifier.fillMaxWidth().focusRequester(fr),
            )
        }
        if (value.isNotEmpty()) SolarIcon(R.drawable.ph_x, 16.dp, K.ink3, Modifier.tap { onChange("") })
    }
}

/** Вся библиотека: поиск, фильтры (консоль, статус, коллекции), сортировка, сетка (п. 2, 19). */
@Composable
fun LibraryScreen() {
    var showNew by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var q by rememberSaveable { mutableStateOf("") }
    val ord = mapOf("Недавние" to 0, "По названию" to 1, "По времени игры" to 2)
    val list = AppState.lib.keys.mapNotNull { AppState.byId[it] }.filter { AppState.inCon(it) }.filter { g ->
        when (S.lf) {
            "Избранное" -> AppState.favs[g.id] == true
            "Не запускались" -> (AppState.lib[g.id]?.play ?: 0) == 0
            "Есть сохранение" -> (0..3).any { AppState.stateFile(g, it).exists() }
            else -> true
        } && (S.lc.isEmpty() || (AppState.cols[S.lc]?.contains(g.id) == true)) && (q.isBlank() || g.t.contains(q.trim(), true))
    }.let { l ->
        when (ord[S.sort]) {
            1 -> l.sortedBy { it.t }
            2 -> l.sortedByDescending { AppState.lib[it.id]?.play ?: 0 }
            else -> l.sortedByDescending { AppState.lib[it.id]?.t ?: 0 }
        }
    }
    val mine = AppState.lib.keys.mapNotNull { AppState.byId[it] }
    Full {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Библиотека", style = T.h2)
            Text(plural(list.size, "игра", "игры", "игр"), style = T.small)
            SearchField(q, "Поиск в библиотеке", Modifier.weight(1f)) { q = it }
            KButton("Свой файл", R.drawable.sl_folder, small = true) { Act.addFile() }
            KButton(S.sort, R.drawable.sl_list, small = true) { val so = ord.keys.toList(); S.sort = so[(so.indexOf(S.sort) + 1) % so.size] }
            KButton(icon = R.drawable.ph_x) { Nav.back() }
        }
        ChipRow(Modifier.padding(horizontal = 16.dp).padding(bottom = 10.dp)) {
            Chip("Все", S.con == "all", count = mine.size) { S.con = "all" }
            PLATFORMS.forEach { p -> Chip(p.short, S.con == p.id, dot = p.color, count = mine.count { it.p == p.id }) { S.con = p.id } }
            Sep()
            listOf("Избранное", "Не запускались", "Есть сохранение").forEach { f -> Chip(f, S.lf == f) { S.lf = if (S.lf == f) "Все" else f } }
            Sep()
            AppState.cols.forEach { (c, ids) -> Chip(c, S.lc == c, icon = R.drawable.sl_folder, count = ids.size) { S.lc = if (S.lc == c) "" else c } }
            if (showNew) {
                SearchField(newName, "Название коллекции", Modifier.width(200.dp), autoFocus = true, onDone = { if (AppState.newCol(newName)) { S.lc = newName.trim(); newName = ""; showNew = false } }) { newName = it }
                Chip("Создать", icon = R.drawable.ph_check) { if (AppState.newCol(newName)) { S.lc = newName.trim(); newName = ""; showNew = false } else AppState.say("Введи название коллекции") }
            } else Chip("Коллекция", icon = R.drawable.ph_plus) { showNew = true }
        }
        if (list.isEmpty()) Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val reset: @Composable () -> Unit = { KButton("Сбросить фильтры", small = true) { S.lf = "Все"; S.lc = ""; S.con = "all"; q = "" } }
            when {
                mine.isEmpty() -> StateCard("es-empty.png", "Библиотека пуста", "Добавь образ игры с телефона — ISO, CSO, CHD или PBP.") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { KButton("Найти на телефоне", R.drawable.sl_search, small = true) { Act.findGames() }; KButton("Добавить игру", R.drawable.ph_plus, com.nexus.psp.ui.kit.BtnKind.Main, small = true) { Act.addFile() } }
                }
                q.isNotBlank() -> StateCard("es-nothing.png", "Нет игры «${q.trim()}»", "Проверь название или сбрось фильтры.", action = reset)
                S.lc.isNotEmpty() && (AppState.cols[S.lc]?.isEmpty() != false) -> StateCard("es-empty.png", "Коллекция «${S.lc}» пустая", "Удерживай обложку игры → «Добавить в «${S.lc}»», или открой карточку игры и нажми на название коллекции.")
                S.lf == "Избранное" -> StateCard("es-empty.png", "В избранном пусто", "Открой карточку игры и нажми «Избранное» — она появится здесь.")
                else -> StateCard("es-nothing.png", "Ничего не нашлось", "По выбранным фильтрам игр нет.", action = reset)
            }
        } else LazyVerticalGrid(
            GridCells.Adaptive(150.dp), Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(18.dp),
        ) { items(list, key = { it.id }) { GameCard(it, null, mine = true) } }
    }
}

@Composable
private fun Sep() = Box(Modifier.width(1.dp).height(20.dp).background(K.line2))

/** Поиск по своей библиотеке (п. 8): недавние запросы, консоли; пусто — подсказка и «Добавить игру». */
@Composable
fun SearchScreen() {
    var q by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(q) { if (q.isNotBlank()) { delay(1200); AppState.addRecent(q) } }
    fun match(g: Game, t: String) = (g.t + " " + g.file + " " + g.plat.full + " " + g.plat.short).contains(t, true)
    Full {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SearchField(q, "Название игры или консоль", Modifier.weight(1f), autoFocus = true, onDone = { AppState.addRecent(q) }) { q = it }
            KButton(icon = R.drawable.ph_x) { Nav.back() }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val t = q.trim()
            val all = AppState.byId.values.toList()
            if (t.isEmpty()) {
                val rec = AppState.recentList()
                if (rec.isNotEmpty()) {
                    Sub("Недавние запросы")
                    ChipRow { rec.forEach { r -> Chip(r, icon = R.drawable.sl_clock) { q = r } }; Chip("Очистить") { S.recent = "" } }
                }
                Sub("Консоли")
                ChipRow { PLATFORMS.forEach { p -> Chip(p.full, dot = p.color) { q = p.short } } }
                if (all.isEmpty()) StateCard("es-empty.png", "Искать пока не в чем", "Поиск работает по твоей библиотеке. Добавь первую игру.") {
                    KButton("Добавить игру", R.drawable.ph_plus, com.nexus.psp.ui.kit.BtnKind.Main, small = true) { Act.addFile() }
                } else Text("Ищи по названию игры, имени файла или консоли.", style = T.small, modifier = Modifier.padding(top = 8.dp))
            } else {
                val mine = all.filter { match(it, t) }
                if (mine.isEmpty()) StateCard("es-nothing.png", "Такой игры у тебя нет", "По запросу «$t» ничего в библиотеке. Если файл есть на телефоне — добавь его.") {
                    KButton("Добавить игру", R.drawable.ph_plus, small = true) { Act.addFile() }
                } else { Sub("В библиотеке · ${mine.size}"); Grid(mine) }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun Grid(l: List<Game>) =
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) { l.forEach { GameCard(it, 150.dp, mine = true) } }
