package com.nexus.psp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.AppState.S
import com.nexus.psp.data.CURATED
import com.nexus.psp.data.GAMES
import com.nexus.psp.data.Game
import com.nexus.psp.data.PL
import com.nexus.psp.data.plural
import com.nexus.psp.data.size
import com.nexus.psp.ui.ActButton
import com.nexus.psp.ui.Ambient
import com.nexus.psp.ui.ConChips
import com.nexus.psp.ui.Cover
import com.nexus.psp.ui.GameCard
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.Ov
import com.nexus.psp.ui.kit.AssetImage
import com.nexus.psp.ui.kit.Chip
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.KButton
import com.nexus.psp.ui.kit.StateCard
import com.nexus.psp.ui.kit.T
import kotlinx.coroutines.delay

/** Магазин: консоли, витрина «Новое», подборки, все игры (п. 5, 17). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoreScreen() {
    val l = GAMES.filter { AppState.inCon(it) }
    val all = if (S.fit) l.filter { AppState.lib[it.id]?.s == "ok" || it.size <= AppState.free() } else l
    LazyColumn(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { ConChips(GAMES) }
        if (S.con != "all") item { ConsoleHead(S.con, l.size) }
        if (!AppState.online) item { StateCard("es-offline.png", "Нет интернета", "Магазин недоступен, а установленные игры работают как обычно.") }
        item { Featured(l) }
        CURATED.forEach { c ->
            val ids = c.ids.mapNotNull { AppState.byId[it] }.filter { AppState.inCon(it) }
            if (ids.isNotEmpty()) item(key = c.t) { Shelf(c.t, c.d, ids) }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
                Text("Все игры", style = T.h3)
                Chip("Только то, что поместится", S.fit, icon = R.drawable.sl_chip) { S.fit = !S.fit }
                Text("свободно ${size(AppState.free())}", style = T.small)
            }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                all.forEach { GameCard(it, 150.dp) }
            }
        }
    }
}

@Composable
private fun ConsoleHead(con: String, n: Int) {
    val p = PL[con] ?: return
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.horizontalGradient(listOf(p.color.copy(alpha = .14f), Color.Transparent))).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        AssetImage(p.conImg, Modifier.height(90.dp).widthIn(max = 220.dp))
        Column {
            Text("Консоль", style = T.small)
            Text(p.full, style = T.h1)
            Text(plural(n, "игра", "игры", "игр") + " в магазине · " + (if (p.bios) "нужен BIOS" else "BIOS не нужен"), style = T.small)
        }
    }
}

/** Витрина «Новое» — листается сама раз в 7 секунд. */
@Composable
private fun Featured(l: List<Game>) {
    val news = l.filter { it.isNew }.ifEmpty { l }
    if (news.isEmpty()) return
    var idx by remember { mutableIntStateOf(0) }
    LaunchedEffect(news.size) { while (true) { delay(7000); idx++ } }
    val g = news[idx % news.size]
    LaunchedEffect(g.id) { if (Nav.tab == "store") Ambient.color = Color(g.pal[2]) }
    AnimatedContent(g, transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(400)) }, label = "feat") { fg ->
        Box(Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(22.dp))) {
            Cover(fg, 1400, 420, 5, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = .78f), Color.Black.copy(alpha = .1f)))))
            Column(Modifier.align(Alignment.CenterStart).padding(22.dp).widthIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("НОВОЕ · ${fg.plat.full.uppercase()}", style = T.caps.copy(color = Color(0xFFF2F1EC).copy(alpha = .7f)))
                Text(fg.t, style = T.h1.copy(color = Color(0xFFF2F1EC)))
                Text(fg.desc, style = T.body.copy(color = Color(0xFFF2F1EC).copy(alpha = .8f)), maxLines = 2)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                    ActButton(fg, small = true)
                    KButton("Подробнее", small = true, kind = com.nexus.psp.ui.kit.BtnKind.Glass) { Nav.open(Ov.Game(fg.id)) }
                }
            }
        }
    }
}

@Composable
private fun Shelf(title: String, desc: String, list: List<Game>) {
    Column {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = T.h3)
            Text(desc, style = T.small)
        }
        LazyRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(list, key = { it.id }) { GameCard(it, 140.dp) }
        }
    }
}
