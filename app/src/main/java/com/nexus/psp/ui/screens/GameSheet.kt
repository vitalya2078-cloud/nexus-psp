package com.nexus.psp.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.AppState.S
import com.nexus.psp.data.GAMES
import com.nexus.psp.data.Game
import com.nexus.psp.data.hrs
import com.nexus.psp.data.size
import com.nexus.psp.ui.ActButton
import com.nexus.psp.ui.Cover
import com.nexus.psp.ui.GameCard
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.Ov
import com.nexus.psp.ui.kit.AssetImage
import com.nexus.psp.ui.kit.BtnKind
import com.nexus.psp.ui.kit.Chip
import com.nexus.psp.ui.kit.ChipRow
import com.nexus.psp.ui.kit.Ellip
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.KButton
import com.nexus.psp.ui.kit.Opt
import com.nexus.psp.ui.kit.Pill
import com.nexus.psp.ui.kit.Seg
import com.nexus.psp.ui.kit.SolarIcon
import com.nexus.psp.ui.kit.StateCard
import com.nexus.psp.ui.kit.Sub
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.Toggle
import com.nexus.psp.ui.kit.tap
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Демо: найден ли BIOS (как BIOS = { ps1: true, ps2: false } в прототипе). */
val BIOS = mapOf("ps1" to true, "ps2" to false)

/** Карточка игры — лист снизу: обложка, пилюли, коллекции, вкладки, нижняя панель действий (п. 3, 18, 9). Свайп вниз — закрыть. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameSheet(id: String) {
    val g = AppState.byId[id] ?: run { Nav.back(); return }
    val s = AppState.stat(id).k
    val e = AppState.lib[id]
    var tab by rememberSaveable(id) { mutableStateOf("about") }
    if (tab == "saves" && s != "ok") tab = "about"
    val drag = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val enter = remember { Animatable(1f) }
    LaunchedEffect(id) { enter.snapTo(1f); enter.animateTo(0f, tween(260)) }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)).tap { Nav.back() })
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(.94f)
                .offset { IntOffset(0, (drag.value + enter.value * 600).roundToInt()) }
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(K.bg).tap { }
                .windowInsetsPadding(WindowInsets.displayCutout),
        ) {
            // шапка с размытой обложкой; тянется вниз — закрыть
            Box(
                Modifier.fillMaxWidth().height(96.dp).pointerInput(id) {
                    detectVerticalDragGestures(
                        onDragEnd = { scope.launch { if (drag.value > 160f) { Nav.back() } else drag.animateTo(0f) } },
                    ) { _, dy -> scope.launch { drag.snapTo((drag.value + dy).coerceAtLeast(0f)) } }
                },
            ) {
                Cover(g, 1400, 300, 3, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, K.bg))))
                Box(Modifier.align(Alignment.TopCenter).padding(top = 8.dp).size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = .5f)))
                KButton(icon = R.drawable.ph_x, kind = BtnKind.Glass, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) { Nav.back() }
            }
            Row(Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                Box(Modifier.width(150.dp).aspectRatio(3f / 4f).clip(RoundedCornerShape(16.dp)).border(1.dp, K.line, RoundedCornerShape(16.dp))) {
                    Cover(g, 300, 400, modifier = Modifier.fillMaxSize())
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill(g.plat.full, dot = g.plat.color); Pill(g.genre); Pill(g.year); Pill(g.kind)
                        if (s != "none") Pill(AppState.stat(id).lbl)
                    }
                    Text(g.t, style = T.h1.copy(fontSize = 28.sp))
                    if (s == "ok" && e != null) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Pill(hrs(e.play), icon = R.drawable.sl_clock); Pill("${e.saves} сохр.", icon = R.drawable.sl_save)
                    }
                    if (s != "none") ChipRow {
                        AppState.cols.forEach { (c, ids) -> Chip(c, id in ids, icon = if (id in ids) R.drawable.ph_check else R.drawable.ph_plus) { AppState.toggleCol(c, id) } }
                        Chip("Избранное", AppState.favs[id] == true, icon = if (AppState.favs[id] == true) R.drawable.sb_heart else R.drawable.sl_heart) { AppState.toggleFav(id) }
                    }
                    val tabs = listOf("about" to "Обзор", "shots" to "Скриншоты", "info" to "Детали") + (if (s == "ok") listOf("saves" to "Сохранения") else emptyList())
                    Seg(tabs.map { it.second }, tabs.indexOfFirst { it.first == tab }.coerceAtLeast(0)) { tab = tabs[it].first }
                    when (tab) {
                        "about" -> About(g)
                        "shots" -> FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf(13, 26, 39, 52).forEach { k -> Box(Modifier.width(240.dp).aspectRatio(480f / 272f).clip(RoundedCornerShape(12.dp))) { Cover(g, 480, 272, k, Modifier.fillMaxSize()) } }
                        }
                        "saves" -> Saves(g)
                        else -> Kv(listOf("Платформа" to g.plat.full, "Жанр" to g.genre, "Год" to g.year, "Размер" to size(g.size), "Тип" to g.kind, "Языки" to "RU, EN", "Регион" to "EU", "Формат" to if (g.real) g.path!!.substringAfterLast('.').uppercase() else if (g.p == "ps1") "CHD" else "ISO"))
                    }
                    Box(Modifier.height(16.dp))
                }
            }
            // нижняя панель действий
            Row(
                Modifier.fillMaxWidth().background(K.s1).padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Ellip(g.t, T.label)
                    Text("${g.plat.short} · ${size(g.size)}" + (if (s != "none") " · " + AppState.stat(id).lbl else ""), style = T.small)
                }
                if (s == "none") KButton("Добавить и установить", R.drawable.sl_dl) { AppState.lib[id] = com.nexus.psp.data.Lib("own"); AppState.install(id) }
                if (s == "ok") {
                    KButton(icon = R.drawable.sl_sliders) { Nav.open(Ov.GSet(id)) }
                    KButton(icon = R.drawable.sl_trash) { Nav.back(); AppState.uninstall(id) }
                }
                ActButton(g)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun About(g: Game) {
    Text(g.desc, style = T.body.copy(fontSize = 15.sp, lineHeight = 23.sp))
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        g.feat.forEach { f -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.size(5.dp).clip(CircleShape).background(g.plat.color)); Text(f, style = T.body) } }
    }
    Sub("Что нужно для игры")
    val p = g.plat
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        KvCell("Носитель", p.media, p.mediaImg)
        KvCell("Размер", size(g.size))
        KvCell("BIOS", if (p.bios) (if (BIOS[g.p] == true) "Найден" else "Нужен BIOS ${p.short}") else "Не нужен")
        KvCell("Управление", "Экран или геймпад")
        KvCell("Игроки", if (Regex("двоих|Дуэли|Кооператив|Мультиплеер|Хот-сит").containsMatchIn(g.feat.joinToString(" "))) "1–2" else "1")
    }
    if (p.bios && BIOS[g.p] != true) StateCard("es-bios.png", "Нужен BIOS ${p.short}.", "Без него игра не запустится. Добавь свой файл BIOS в настройках — там есть инструкция.")
    if (!g.real) {
        Sub("Совместимость")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) { (1..3).forEach { n -> Box(Modifier.size(16.dp, 5.dp).clip(RoundedCornerShape(3.dp)).background(if (n <= g.compat) K.ok else K.s3)) } }
            Text((if (g.compat == 3) "Работает отлично" else "Играбельно, бывают просадки") + " · ${g.rev} отзывов", style = T.small.copy(color = K.ink2))
        }
        Sub("Похожие игры")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(GAMES.filter { it.id != g.id && (it.genre == g.genre || it.p == g.p) }.take(6), key = { it.id }) { GameCard(it, 120.dp, withAct = false) }
        }
    }
}

@Composable
private fun KvCell(k: String, v: String, img: String? = null) {
    Row(
        Modifier.clip(RoundedCornerShape(14.dp)).background(K.s1).padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (img != null) AssetImage(img, Modifier.size(32.dp))
        Column { Text(k.uppercase(), style = T.caps.copy(fontSize = 10.sp)); Text(v, style = T.label) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Kv(rows: List<Pair<String, String>>) =
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { rows.forEach { KvCell(it.first, it.second) } }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Saves(g: Game) {
    val n = AppState.lib[g.id]?.saves ?: 0
    val when_ = listOf("5 минут назад", "вчера", "3 дня назад", "неделю назад")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(minOf(n, 4)) { i ->
            Column(Modifier.width(200.dp)) {
                Box(Modifier.fillMaxWidth().aspectRatio(480f / 272f).clip(RoundedCornerShape(12.dp))) { Cover(g, 480, 272, 30 + i, Modifier.fillMaxSize()) }
                Text(if (i == 0) "Быстрое сохранение" else "Слот ${i + 1}", style = T.label, modifier = Modifier.padding(top = 6.dp))
                Text(when_[i], style = T.small)
            }
        }
        Column(Modifier.width(200.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(480f / 272f).clip(RoundedCornerShape(12.dp)).border(1.dp, K.line2, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                SolarIcon(R.drawable.ph_plus, 22.dp, K.ink3)
            }
            Text("Пустой слот", style = T.label, modifier = Modifier.padding(top = 6.dp))
            Text("Сохрани из меню паузы", style = T.small)
        }
    }
}

/** Настройки конкретной игры (п. 9): пресет автора, разрешение, управление. */
@Composable
fun GameSettingsScreen(id: String) {
    val g = AppState.byId[id] ?: run { Nav.back(); return }
    com.nexus.psp.ui.Full {
        com.nexus.psp.ui.OvHead("${g.t} — настройки", back = true)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            StateCard(null, "Применён пресет автора каталога.", "Проверенные настройки для этой игры. Изменения ниже применяются к играм PSP; отдельные настройки на игру — следующим обновлением.", tint = K.acc)
            CoreQuality()
            Opt("Управление", "Режим экранных кнопок") {
                val modes = listOf("touch", "classic", "pad")
                Seg(listOf("Touch", "Классика", "Геймпад"), modes.indexOf(S.ctlMode).coerceAtLeast(0)) { S.ctlMode = modes[it] }
            }
            Opt("Счётчик кадров", null) { Toggle(S.fps) { S.fps = it } }
            Opt("Читы", "Появятся позже") { Text("скоро", style = T.small) }
        }
    }
}
