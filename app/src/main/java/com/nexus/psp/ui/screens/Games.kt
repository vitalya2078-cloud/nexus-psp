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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.sp
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
        if (list.isEmpty()) { EmptyShelf(); return@Column }
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
                        Modifier.width(w).fillMaxHeight().graphicsLayer { scaleX = sc; scaleY = sc; alpha = if (on) 1f else .7f }
                            .clip(RoundedCornerShape(20.dp)).border(if (on) 2.dp else 1.dp, if (on) K.ink.copy(alpha = .9f) else K.line, RoundedCornerShape(20.dp))
                            .combinedClickable(remember { MutableInteractionSource() }, null, onLongClick = { Act.buzzLong(); Ctx.open(g.id) }) {
                                if (on) Act.play(ctx, g.id)
                                else scope.launch { state.animateScrollToItem(i) }
                            },
                    ) {
                        Cover(g, 300, 400, modifier = Modifier.fillMaxSize())
                        PlatformTag(g)
                        com.nexus.psp.ui.FormatBadge(g, Modifier.align(Alignment.BottomStart).padding(8.dp))
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
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Ellip(g.t, T.h1)
            Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                ConsolePill(g.plat.full, g.plat.color)
                if (e != null && e.play > 0) { Meta("Наиграно ${hrs(e.play)}"); Meta(e.last) } else Meta("Ещё не запускалась")
                Meta("${g.format} · ${size(g.size)}")
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

/**
 * Пустая полка (первый запуск): та же карусель, только из пустых капсул-слотов.
 * Центральный слот — «Добавь первую игру»; когда игры появятся, они встанут на эти места.
 */
@Composable
private fun EmptyShelf() {
    var help by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxSize().padding(start = 20.dp, end = 24.dp, top = 6.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
            val h = maxHeight; val w = minOf(h * .75f, maxWidth / 2.3f)
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                GhostSlot(w * .62f, w * .62f / .75f, .3f)
                GhostSlot(w, w / .75f, 1f, main = true) { Act.addFile() }
                GhostSlot(w * .62f, w * .62f / .75f, .3f)
            }
        }
        Column(Modifier.weight(1.1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Твоя полка пока пуста", style = T.h1.copy(fontSize = 28.sp))
            Text("Добавь образ своей игры с телефона — ISO, CSO, CHD или PBP. Сейчас работает PSP; PS1 и PS2 подключаем следующими.", style = T.body)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                KButton("Добавить игру", R.drawable.ph_plus, BtnKind.Main) { Act.addFile() }
                KButton("Найти на телефоне", R.drawable.sl_search) { Act.findGames() }
            }
            Text("Где взять игры?", style = T.label.copy(color = K.acc), modifier = Modifier.padding(top = 2.dp).press { help = true }.padding(vertical = 6.dp))
        }
    }
    if (help) WhereToGet { help = false }
}

/** Пустой слот-капсула с пунктирной обводкой. */
@Composable
private fun GhostSlot(w: androidx.compose.ui.unit.Dp, h: androidx.compose.ui.unit.Dp, a: Float, main: Boolean = false, onClick: (() -> Unit)? = null) {
    val line = K.ink.copy(alpha = if (main) .45f else .2f)
    Box(
        Modifier.width(w).height(h).graphicsLayer { alpha = a }.clip(RoundedCornerShape(20.dp)).background(K.s1.copy(alpha = .6f))
            .drawBehind {
                drawRoundRect(line, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(14f, 10f))), cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()))
            }
            .then(if (onClick != null) Modifier.press(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (main) Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(56.dp).clip(RoundedCornerShape(28.dp)).background(K.ink), contentAlignment = Alignment.Center) { SolarIcon(R.drawable.ph_plus, 26.dp, K.bg) }
            Text("Добавь\nпервую игру", style = T.label.copy(color = K.ink2, textAlign = androidx.compose.ui.text.style.TextAlign.Center), modifier = Modifier.padding(top = 12.dp))
        }
    }
}

/** «Где взять игры?» — честно и коротко. */
@Composable
fun WhereToGet(onClose: () -> Unit) {
    androidx.compose.ui.window.Dialog(onClose) {
        Column(Modifier.width(520.dp).clip(RoundedCornerShape(22.dp)).background(K.s1).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Где взять игры?", style = T.h2)
            Tip("Свои диски.", "Сделай образ (дамп) своего UMD-диска на прошитой PSP — получится файл ISO или CSO. Это законный способ играть в купленные игры.")
            Tip("Бесплатные homebrew-игры.", "Любительские игры, которые авторы раздают бесплатно — обычно в формате PBP.")
            Tip("Магазин KAPSULA.", "Скоро: игры от авторов, которые разрешили их распространять.")
            Text("KAPSULA не скачивает коммерческие игры и не помогает их искать.", style = T.small)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { KButton("Понятно", kind = BtnKind.Main, small = true) { onClose() } }
        }
    }
}

@Composable
private fun Tip(t: String, d: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.padding(top = 7.dp).size(6.dp).clip(RoundedCornerShape(3.dp)).background(K.acc))
        Column { Text(t, style = T.label); Text(d, style = T.body.copy(fontSize = 13.sp)) }
    }
}
