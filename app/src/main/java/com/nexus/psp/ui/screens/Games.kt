package com.nexus.psp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.AppState.S
import com.nexus.psp.data.Game
import com.nexus.psp.data.hrs
import com.nexus.psp.data.size
import com.nexus.psp.ui.Act
import com.nexus.psp.ui.ActButton
import com.nexus.psp.ui.Ambient
import com.nexus.psp.ui.ConChips
import com.nexus.psp.ui.Cover
import com.nexus.psp.ui.Ctx
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.Ov
import com.nexus.psp.ui.PlatformTag
import com.nexus.psp.ui.StatusBadge
import com.nexus.psp.ui.kit.AssetImage
import com.nexus.psp.ui.kit.Bar
import com.nexus.psp.ui.kit.BtnKind
import com.nexus.psp.ui.kit.ConsolePill
import com.nexus.psp.ui.kit.Ellip
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.KButton
import com.nexus.psp.ui.kit.SolarIcon
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.press
import kotlinx.coroutines.launch

/** «Игры»: карусель моей библиотеки, игра в фокусе — по центру, под ней информация и действия (п. 1). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GamesScreen() {
    val list = AppState.myList()
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            ConChips(AppState.lib.keys.mapNotNull { AppState.byId[it] }, Modifier.weight(1f))
            KButton("Вся библиотека", R.drawable.sl_grid, small = true) { Nav.open(Ov.Library) }
        }
        if (list.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                AssetImage("es-empty.png", Modifier.size(140.dp))
                Text("Библиотека пуста", style = T.h1)
                Text("Добавь игры в магазине или свой файл — они появятся здесь.", style = T.body, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KButton("Открыть магазин", kind = BtnKind.Main) { Nav.go("store") }
                    KButton("Свой файл", R.drawable.sl_folder) { Act.addFile() }
                }
            }
            return@Column
        }
        Carousel(list)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Carousel(list: List<Game>) {
    val ctx = LocalContext.current
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val focus by remember(list) {
        derivedStateOf {
            val li = state.layoutInfo
            val c = (li.viewportStartOffset + li.viewportEndOffset) / 2
            li.visibleItemsInfo.minByOrNull { kotlin.math.abs(it.offset + it.size / 2 - c) }?.index ?: 0
        }
    }
    val fg = list.getOrNull(focus.coerceAtMost(list.lastIndex))
    LaunchedEffect(fg?.id) { fg?.let { Ambient.color = Color(it.pal[2]); Act.buzz() } }
    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxWidth().weight(1f)) {
            val h = maxHeight * .92f
            val w = h * .75f
            val side = (maxWidth - w) / 2
            LazyRow(
                state = state, flingBehavior = rememberSnapFlingBehavior(state),
                contentPadding = PaddingValues(horizontal = side), horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize(),
            ) {
                itemsIndexed(list, key = { _, g -> g.id }) { i, g ->
                    val on = i == focus
                    val sc by animateFloatAsState(if (on) 1f else .8f, tween(220), label = "cc")
                    val e = AppState.lib[g.id]
                    Box(
                        Modifier.width(w).fillMaxHeight().graphicsLayer { scaleX = sc; scaleY = sc; alpha = if (on) 1f else .7f; if (e?.s == "own") { alpha *= .75f } }
                            .clip(RoundedCornerShape(20.dp)).border(if (on) 2.dp else 1.dp, if (on) K.ink.copy(alpha = .9f) else K.line, RoundedCornerShape(20.dp))
                            .combinedClickable(remember { MutableInteractionSource() }, null, onLongClick = { Act.buzzLong(); Ctx.open(g.id) }) {
                                if (on) { if (e?.s == "ok") Act.play(ctx, g.id) else Nav.open(Ov.Game(g.id)) }
                                else scope.launch { state.animateScrollToItem(i) }
                            },
                    ) {
                        Cover(g, 300, 400, modifier = Modifier.fillMaxSize())
                        PlatformTag(g)
                        if (e?.s != "ok") StatusBadge(g.id, Modifier.align(Alignment.BottomStart).padding(8.dp))
                        if (e != null && e.s in setOf("dl", "pause", "q")) Bar(e.pr / 100f, if (e.s == "dl") K.acc else K.warn, 4.dp, Modifier.align(Alignment.BottomCenter))
                    }
                }
                item {
                    Box(
                        Modifier.width(w * .7f).fillMaxHeight(.7f).clip(RoundedCornerShape(20.dp)).background(K.s1).border(1.dp, K.line, RoundedCornerShape(20.dp)).press { Nav.open(Ov.Library) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SolarIcon(R.drawable.sl_grid, 26.dp, K.ink2)
                            Text("Вся\nбиблиотека", style = T.label.copy(color = K.ink2), modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
        if (fg != null) AnimatedContent(fg.id, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, label = "info") { id ->
            AppState.byId[id]?.let { Info(it) }
        }
    }
}

@Composable
private fun Info(g: Game) {
    val e = AppState.lib[g.id]
    val s = AppState.stat(g.id)
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Ellip(g.t, T.h1)
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ConsolePill(g.plat.full, g.plat.color)
                when (s.k) {
                    "ok" -> { Meta("Наиграно ${hrs(e!!.play)}"); Meta(e.last); Meta("${e.saves} сохр.") }
                    "own" -> { Meta("В библиотеке · не установлена"); Meta(size(g.size)) }
                    else -> { Meta(if (s.k == "dl") "Загружается · ${s.lbl}" else s.lbl); Meta(size(g.size)) }
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        ActButton(g)
        Spacer(Modifier.width(8.dp))
        KButton(icon = R.drawable.sl_info) { Nav.open(Ov.Game(g.id)) }
    }
}

@Composable
private fun Meta(t: String) = Text(t, style = T.small.copy(color = K.ink2))
