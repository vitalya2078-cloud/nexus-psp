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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.AppState.S
import com.nexus.psp.data.Game
import com.nexus.psp.data.hrs
import com.nexus.psp.data.size
import com.nexus.psp.ui.ActButton
import com.nexus.psp.ui.Cover
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.Ov
import com.nexus.psp.ui.kit.AssetImage
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/** Карточка игры — лист снизу. Тянуть вниз за верхнюю полоску — закрыть. Без лишней шапки: вся высота — под информацию. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameSheet(id: String) {
    val g = AppState.byId[id] ?: run { Nav.back(); return }
    val e = AppState.lib[id]
    var tab by rememberSaveable(id) { mutableStateOf("about") }
    var askDelete by remember { mutableStateOf(false) }
    val drag = remember { Animatable(0f) }
    val enter = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(id) { enter.snapTo(1f); enter.animateTo(0f, tween(240)) }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)).tap { Nav.back() })
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(.94f)
                .offset { IntOffset(0, (drag.value + enter.value * 600).roundToInt()) }
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(K.bg).tap { }
                .windowInsetsPadding(WindowInsets.displayCutout),
        ) {
            // полоска-ручка: потянуть вниз — закрыть
            Box(
                Modifier.fillMaxWidth().height(18.dp).pointerInput(id) {
                    detectVerticalDragGestures(onDragEnd = { scope.launch { if (drag.value > 140f) Nav.back() else drag.animateTo(0f) } }) { _, dy ->
                        scope.launch { drag.snapTo((drag.value + dy).coerceAtLeast(0f)) }
                    }
                },
                contentAlignment = Alignment.Center,
            ) { Box(Modifier.size(40.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(K.ink3)) }
            Row(Modifier.weight(1f).padding(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                Box(Modifier.width(136.dp).aspectRatio(3f / 4f).clip(RoundedCornerShape(16.dp)).border(1.dp, K.line, RoundedCornerShape(16.dp))) {
                    Cover(g, 300, 400, modifier = Modifier.fillMaxSize())
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Pill(g.plat.full, dot = g.plat.color); Pill(g.format); Pill(size(g.size))
                                if (e != null && e.play > 0) Pill(hrs(e.play), icon = R.drawable.sl_clock)
                            }
                            Text(g.t, style = T.h1.copy(fontSize = 26.sp))
                        }
                        KButton(icon = R.drawable.ph_x, small = true) { Nav.back() }
                    }
                    ChipRow {
                        AppState.cols.forEach { (c, ids) -> Chip(c, id in ids, icon = if (id in ids) R.drawable.ph_check else R.drawable.ph_plus) { AppState.toggleCol(c, id) } }
                        Chip("Избранное", AppState.favs[id] == true, icon = if (AppState.favs[id] == true) R.drawable.sb_heart else R.drawable.sl_heart) { AppState.toggleFav(id) }
                    }
                    val tabs = listOf("about" to "Обзор", "saves" to "Сохранения", "file" to "Файл")
                    Seg(tabs.map { it.second }, tabs.indexOfFirst { it.first == tab }.coerceAtLeast(0)) { tab = tabs[it].first }
                    when (tab) {
                        "about" -> About(g)
                        "saves" -> Saves(g)
                        else -> FileInfo(g)
                    }
                    Box(Modifier.height(12.dp))
                }
            }
            // нижняя панель действий
            Row(
                Modifier.fillMaxWidth().background(K.s1).padding(horizontal = 22.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Ellip(g.t, T.label)
                    Text(if (e != null && e.play > 0) "Последний запуск: ${e.last}" else "Ещё не запускалась", style = T.small)
                }
                KButton(icon = R.drawable.sl_sliders) { Nav.open(Ov.GSet(id)) }
                KButton(icon = R.drawable.sl_trash) { askDelete = true }
                ActButton(g)
            }
        }
        if (askDelete) Confirm("Удалить «${g.t}» с телефона?", "Файл игры удалится из папки KAPSULA. Сохранения внутри игры останутся.", "Удалить", { askDelete = false }) { askDelete = false; Nav.back(); AppState.uninstall(id) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun About(g: Game) {
    val p = g.plat
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        KvCell("Носитель", p.media, p.mediaImg)
        KvCell("BIOS", if (p.bios) "Нужен BIOS ${p.short}" else "Не нужен")
        KvCell("Управление", "Экран или геймпад")
        KvCell("Эмулятор", if (p.ready) "PPSSPP" else "Скоро")
    }
    if (!p.ready) StateCard("es-bios.png", "${p.full} пока не поддерживается.", "Ядро для ${p.short} подключим следующим — игра останется в библиотеке.")
    Text("Описание и обложку можно будет выбрать позже — сейчас обложка создаётся по названию файла.", style = T.small)
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
private fun FileInfo(g: Game) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        KvCell("Файл", g.file); KvCell("Формат", g.format); KvCell("Размер", size(g.size)); KvCell("Консоль", g.plat.full)
    }
    Text(g.path.replace("/storage/emulated/0/", ""), style = T.small)
}

/** Сохранения: настоящий быстрый слот из меню паузы. */
@Composable
private fun Saves(g: Game) {
    val f = AppState.stateFile(g)
    if (!f.exists()) {
        StateCard(null, "Сохранений пока нет", "В игре открой меню паузы («Назад» или кнопка паузы) и нажми «Сохранить» — здесь появится быстрое сохранение.")
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.width(200.dp).aspectRatio(480f / 272f).clip(RoundedCornerShape(12.dp))) { Cover(g, 480, 272, 30, Modifier.fillMaxSize()) }
        Column {
            Text("Быстрое сохранение", style = T.label)
            Text(SimpleDateFormat("d MMMM, HH:mm", Locale("ru")).format(Date(f.lastModified())), style = T.small)
            Text("«Загрузить» — в меню паузы игры", style = T.small)
        }
    }
}

/** Подтверждение опасного действия. */
@Composable
fun Confirm(title: String, text: String, ok: String, onCancel: () -> Unit, onOk: () -> Unit) {
    androidx.compose.ui.window.Dialog(onCancel) {
        Column(Modifier.width(440.dp).clip(RoundedCornerShape(22.dp)).background(K.s1).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = T.h3)
            Text(text, style = T.body)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                KButton("Отмена", small = true) { onCancel() }
                KButton(ok, small = true, danger = true) { onOk() }
            }
        }
    }
}

/** Настройки конкретной игры (п. 9). */
@Composable
fun GameSettingsScreen(id: String) {
    val g = AppState.byId[id] ?: run { Nav.back(); return }
    com.nexus.psp.ui.Full {
        com.nexus.psp.ui.OvHead("${g.t} — настройки", back = true)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            StateCard(null, null, "Пока эти настройки общие для всех игр PSP. Отдельные настройки для каждой игры появятся следующим обновлением.", tint = K.acc)
            CoreQuality()
            Opt("Управление", "Режим экранных кнопок") {
                val modes = listOf("touch", "classic", "pad")
                Seg(listOf("Touch", "Классика", "Геймпад"), modes.indexOf(S.ctlMode).coerceAtLeast(0)) { S.ctlMode = modes[it] }
            }
            Opt("Счётчик кадров", null) { Toggle(S.fps) { S.fps = it } }
        }
    }
}
