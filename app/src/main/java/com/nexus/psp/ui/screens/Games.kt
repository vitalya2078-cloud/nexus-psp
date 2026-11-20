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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.widthIn
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

/** Игры главного экрана: выбранная во вкладке «Игры ▾» консоль (all — все мои игры), недавние — сверху. */
fun shelfGames(): List<Game> {
    val sh = S.shelf
    return AppState.lib.keys.mapNotNull { AppState.byId[it] }.filter { sh == "all" || it.p == sh }.sortedByDescending { AppState.lib[it.id]?.t ?: 0 }
}

/** «Игры» — «Ретро-меню»: слева список игр выбранной консоли, справа крупная обложка, данные, «Играть» и маскот. */
@Composable
fun GamesScreen() {
    val list = shelfGames()
    // добавили игру другой консоли — показываем все игры, чтобы новая не «пропала»
    val n = AppState.lib.size
    var seen by remember { mutableStateOf(n) }
    LaunchedEffect(n) {
        if (n > seen) AppState.lib.entries.maxByOrNull { it.value.t }?.key?.let { AppState.byId[it] }?.let { if (S.shelf != "all" && it.p != S.shelf) S.shelf = "all" }
        seen = n
    }
    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout)) {
        when {
            list.isNotEmpty() -> RetroMenu(list)
            S.shelf == "all" -> EmptyShelf()
            else -> ShelfEmpty(S.shelf)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RetroMenu(list: List<Game>) {
    val ctx = LocalContext.current
    var sel by androidx.compose.runtime.saveable.rememberSaveable(S.shelf) { mutableStateOf(list.first().id) }
    val fg = list.firstOrNull { it.id == sel } ?: list.first()
    LaunchedEffect(fg.id) { Ambient.color = AppState.meta[fg.id]?.color?.let { c -> Color(c) } ?: Color(fg.pal[2]); Ambient.game = fg }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { Ambient.game = null } }
    val state = rememberLazyListState()
    BoxWithConstraints(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp)) {
        val listW = (maxWidth * .4f).coerceIn(220.dp, 380.dp)
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            // список: мини-обложка + название; выбранная — подсвечена, второе касание — играть
            LazyColumn(
                state = state,
                modifier = Modifier.width(listW).fillMaxHeight().clip(RoundedCornerShape(22.dp)).background(K.s1.copy(alpha = .78f))
                    .border(1.dp, if (K.evening) K.acc.copy(alpha = .16f) else K.line, RoundedCornerShape(22.dp)),
                contentPadding = PaddingValues(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(list, key = { it.id }) { g ->
                    val on = g.id == fg.id
                    Row(
                        Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (on) K.acc.copy(alpha = .16f) else Color.Transparent)
                            .border(1.5.dp, if (on) K.acc.copy(alpha = .7f) else Color.Transparent, RoundedCornerShape(16.dp))
                            .combinedClickable(remember { MutableInteractionSource() }, androidx.compose.material3.ripple(color = K.ink),
                                onLongClick = { Act.buzzLong(); Ctx.open(g.id) }) {
                                if (on) Act.play(ctx, g.id) else { sel = g.id; Act.buzz() }
                            }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.width(34.dp).height(44.dp).clip(RoundedCornerShape(7.dp))) { Cover(g, 68, 88, modifier = Modifier.fillMaxSize()) }
                        Column(Modifier.weight(1f)) {
                            Ellip(g.t, T.label.copy(fontSize = 14.5.sp, color = if (on) K.ink else K.ink2).let { if (K.evening) it.copy(fontFamily = com.nexus.psp.ui.kit.Balsamiq, fontSize = 15.sp) else it })
                            if (S.shelf == "all") Text(g.plat.short, style = T.small.copy(fontSize = 11.sp), maxLines = 1)
                        }
                        if (on) Text("▶", style = T.label.copy(color = K.acc, fontSize = 15.sp))
                    }
                }
                item { MenuRow(R.drawable.ph_plus, "Добавить игру") { Act.addFile() } }
                item { MenuRow(R.drawable.sl_grid, "Вся библиотека") { Nav.open(Ov.Library) } }
            }
            // выбранная игра
            AnimatedContent(fg.id, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) }, label = "info", modifier = Modifier.weight(1f).fillMaxHeight()) { id ->
                AppState.byId[id]?.let { Detail(it) }
            }
        }
        // маскот — справа внизу, с приветствием по времени суток (один раз за запуск)
        HomeBuddy(Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun MenuRow(icon: Int, text: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).press(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(34.dp), contentAlignment = Alignment.Center) { SolarIcon(icon, 20.dp, K.acc) }
        Text(text, style = T.label.copy(color = K.ink2))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Detail(g: Game) {
    val e = AppState.lib[g.id]
    val ctx = LocalContext.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val coverH = maxHeight * .94f
        val coverW = minOf(coverH * .75f, maxWidth * .44f)
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(
                Modifier.width(coverW).height(coverW / .75f).clip(RoundedCornerShape(18.dp))
                    .border(if (K.evening) 3.dp else 1.5.dp, if (K.evening) K.ink.copy(alpha = .85f) else K.ink.copy(alpha = .5f), RoundedCornerShape(18.dp))
                    .combinedClickable(remember { MutableInteractionSource() }, null, onLongClick = { Act.buzzLong(); Ctx.open(g.id) }) { Act.play(ctx, g.id) },
            ) {
                Cover(g, 300, 400, modifier = Modifier.fillMaxSize())
                PlatformTag(g)
                com.nexus.psp.ui.FormatBadge(g, Modifier.align(Alignment.BottomStart).padding(8.dp))
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    ConsolePill(g.plat.short, g.plat.color)
                    Meta("${g.format} · ${size(g.size)}")
                }
                Ellip(g.t, T.h1, lines = 2)
                Meta(if (e != null && e.play > 0) "Наиграно ${hrs(e.play)} · ${e.last}" else "Ещё не запускалась")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    ActButton(g)
                    KButton("Карточка") { Nav.open(Ov.Game(g.id)) }
                }
            }
        }
    }
}

/** Маскот главного экрана: приветствие (поза и облачко по времени суток), потом — спокойный, живой. */
@Composable
private fun HomeBuddy(modifier: Modifier) {
    if (!S.mascot) return
    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    var pose by remember { mutableStateOf(if (com.nexus.psp.ui.kit.Buddy.greeted) "idle" else com.nexus.psp.ui.kit.Buddy.greetPose(hour)) }
    var bubble by remember { mutableStateOf(if (com.nexus.psp.ui.kit.Buddy.greeted) null else com.nexus.psp.ui.kit.Buddy.greetText(hour, S.nick)) }
    LaunchedEffect(Unit) {
        if (com.nexus.psp.ui.kit.Buddy.greeted) return@LaunchedEffect
        com.nexus.psp.ui.kit.Buddy.greeted = true
        kotlinx.coroutines.delay(5000); bubble = null
        // ночью продолжает спать, в остальное время — обычная поза
        if (pose != "sleep") { kotlinx.coroutines.delay(600); pose = "idle" }
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.animation.AnimatedVisibility(bubble != null, enter = fadeIn(tween(300)), exit = fadeOut(tween(400))) {
            Text(
                bubble ?: "", maxLines = 2,
                style = T.label.copy(fontSize = 14.sp, color = K.ink).let { if (K.evening) it.copy(fontFamily = com.nexus.psp.ui.kit.Balsamiq, fontSize = 15.sp) else it },
                modifier = Modifier.widthIn(max = 160.dp).clip(RoundedCornerShape(16.dp)).background(K.s2.copy(alpha = .95f))
                    .border(1.dp, K.acc.copy(alpha = .4f), RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
        com.nexus.psp.ui.kit.LiveMascot(pose, 96.dp) {
            // касание: смех (внутри LiveMascot); спящий — просыпается
            if (pose == "sleep") pose = "idle"
        }
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
            Text("Полка пока пустая", style = T.h1.copy(fontSize = 28.sp))
            Text("Закинь образ своей игры — для PSP, PS1, PS2, Sega и других консолей, или Java-игру. Проще всего — положить файлы в папку ${com.nexus.psp.emulator.GameLibrary.FOLDER} в памяти телефона: игры появятся сами. Консоль выбирается во вкладке «Игры ▾».", style = T.body)
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

/** Полка выбранной консоли пуста: что ей нужно и кнопки добавления. */
@Composable
private fun ShelfEmpty(id: String) {
    val p = com.nexus.psp.data.PL[id] ?: run { S.shelf = "all"; return }
    Row(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        AssetImage(p.conImg, Modifier.width(220.dp).height(160.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${p.short}: пока нет игр", style = T.h1)
            Text("Форматы: " + p.exts.joinToString(", ") { it.uppercase() } + if (p.biosLevel == 2) ". Нужен BIOS — подскажу, где его указать." else ".", style = T.body)
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KButton("Добавить игру", R.drawable.ph_plus, BtnKind.Main) { Act.addFile() }
                KButton("Найти на телефоне", R.drawable.sl_search) { Act.findGames() }
                if (p.bios) KButton("BIOS", R.drawable.sl_chip) { Nav.settingsTab = "bios"; Nav.open(Ov.Settings) }
            }
        }
        com.nexus.psp.ui.kit.Mascot("wait", 110.dp)
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
            Tip("Другие консоли.", "Картриджи и диски Sega, Atari, NEC, SNK и Bandai — тоже своими образами. Если консоли нужен BIOS, KAPSULA подскажет.")
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
