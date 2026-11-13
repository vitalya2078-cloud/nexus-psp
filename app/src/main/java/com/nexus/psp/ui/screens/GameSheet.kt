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
import com.nexus.psp.ui.kit.SolarDuo
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
        // фон игры за листом: виден в верхней части листа и плавно уходит в цвет фона
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(.94f)
                .offset { IntOffset(0, (drag.value + enter.value * 600).roundToInt()) }
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(K.bg),
        ) { com.nexus.psp.ui.GameBackdrop(g, Modifier.fillMaxSize(), strength = .6f) }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(.94f)
                .offset { IntOffset(0, (drag.value + enter.value * 600).roundToInt()) }
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).tap { }
                .windowInsetsPadding(WindowInsets.displayCutout),
        ) {
            // полоска-ручка: потянуть вниз — закрыть (фон игры — под листом, см. SheetBackdrop)
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
                    val tabs = listOf("about" to "Обзор", "shots" to "Скриншоты", "saves" to "Сохранения", "file" to "Файл")
                    Seg(tabs.map { it.second }, tabs.indexOfFirst { it.first == tab }.coerceAtLeast(0)) { tab = tabs[it].first }
                    when (tab) {
                        "about" -> About(g)
                        "saves" -> Saves(g)
                        "shots" -> Shots(g)
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
    KvGrid(listOf(
        Triple("Носитель", p.media, p.mediaImg), Triple("BIOS", if (p.bios) "Нужен BIOS ${p.short}" else "Не нужен", R.drawable.sd_chip),
        Triple("Управление", "Любое", R.drawable.sd_pad), Triple("Эмулятор", if (p.ready) "KAPSULA" else "Скоро", R.drawable.sd_monitor),
    ))
    if (!p.ready) StateCard("es-bios.png", "${p.full} пока не поддерживается.", "Ядро для ${p.short} подключим следующим — игра останется в библиотеке.")
    val m = AppState.meta[g.id]
    Sub("Обложка")
    Text(
        when {
            m?.cover?.name == "cover_user.jpg" -> "Своя обложка из галереи."
            m?.cover != null -> "Найдена в интернете по серийному номеру диска${m.serial?.let { " ($it)" } ?: ""}."
            !S.covers -> "Загрузка обложек выключена в настройках — сейчас обложка создаётся по названию."
            else -> "Обложку пока не нашли — показываем созданную по названию. Нужен интернет${if (S.coversWifi) " по Wi-Fi" else ""}."
        }, style = T.small,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        KButton("Найти заново", R.drawable.sl_search, small = true) { AppState.fetchMeta(g.id, force = true) }
        KButton("Из галереи", R.drawable.sl_folder, small = true) { com.nexus.psp.ui.Act.pickCover?.invoke(g.id) }
        if (m?.cover?.name == "cover_user.jpg") KButton("Убрать свою", small = true) { AppState.clearUserCover(g.id) }
    }
}

/** Блок сведений; в сетке все блоки одной ширины и высоты. */
@Composable
private fun KvCell(k: String, v: String, img: Any? = null, modifier: Modifier = Modifier) {
    // у каждого блока свой значок в одной и той же «ячейке» 28 dp — текст всех блоков начинается на одной вертикали
    Row(
        modifier.height(64.dp).clip(RoundedCornerShape(14.dp)).background(K.s1.copy(alpha = .85f)).padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        when (img) {
            is String -> AssetImage(img, Modifier.size(28.dp))
            is Int -> Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { SolarDuo(img, 24.dp) }
        }
        Column { Text(k.uppercase(), style = T.caps.copy(fontSize = 10.sp), maxLines = 1); Ellip(v, T.label) }
    }
}

/** Ровная сетка блоков: по cols в ряд, одинаковой ширины. */
@Composable
private fun KvGrid(cells: List<Triple<String, String, Any?>>, cols0: Int = 4) {
    // на узких экранах — по 2 в ряд: подписи и значения не обрезаются
    androidx.compose.foundation.layout.BoxWithConstraints {
    val cols = if (cols0 > 2 && maxWidth < 600.dp) 2 else cols0
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cells.chunked(cols).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (k, v, img) -> KvCell(k, v, img, Modifier.weight(1f)) }
                repeat(cols - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FileInfo(g: Game) {
    KvGrid(listOfNotNull(
        Triple("Формат", g.format, R.drawable.sd_list), Triple("Размер", size(g.size), R.drawable.sd_dl), Triple("Консоль", g.plat.full, R.drawable.sd_pad),
        AppState.meta[g.id]?.serial?.let { Triple("Серийный номер", it, R.drawable.sd_keyhole) },
    ))
    KvGrid(listOf(Triple("Файл", g.file, R.drawable.sd_folder)), cols0 = 1)
    Text(g.path.replace("/storage/emulated/0/", ""), style = T.small)
}

/** Скриншоты: кадр из игры и титульный экран (libretro), картинка из образа. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Shots(g: Game) {
    val m = AppState.meta[g.id]
    val shots = m?.shots.orEmpty()
    if (shots.isEmpty()) {
        StateCard(null, "Скриншотов пока нет", if (!S.covers) "Загрузка из интернета выключена в настройках." else "Найдём их вместе с обложкой, когда будет интернет${if (S.coversWifi) " по Wi-Fi" else ""}.") {
            KButton("Найти", R.drawable.sl_search, small = true) { AppState.fetchMeta(g.id, force = true) }
        }
        return
    }
    var open by remember { mutableStateOf<java.io.File?>(null) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        shots.forEach { f ->
            Box(Modifier.width(240.dp).aspectRatio(480f / 272f).clip(RoundedCornerShape(12.dp)).border(1.dp, K.line, RoundedCornerShape(12.dp)).tap { open = f }) {
                com.nexus.psp.ui.FileImage(f, Modifier.fillMaxSize())
            }
        }
    }
    open?.let { f ->
        androidx.compose.ui.window.Dialog({ open = null }, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .9f)).tap { open = null }, contentAlignment = Alignment.Center) {
                com.nexus.psp.ui.FileImage(f, Modifier.fillMaxWidth(.85f).aspectRatio(480f / 272f).clip(RoundedCornerShape(14.dp)))
            }
        }
    }
}

/** Сохранения: быстрый слот и слоты 1–3 с кадром и временем; «Продолжить» — запуск игры сразу с этого места. */
@Composable
private fun Saves(g: Game) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // 4 — место выхода (мгновенное продолжение), 0 — быстрое, 1–3 — свои
    val slots = listOf(4, 0, 1, 2, 3).map { it to AppState.stateFile(g, it) }
    if (slots.none { it.second.exists() }) {
        StateCard(null, "Сохранений пока нет", "В игре открой меню паузы («Назад» или кнопка паузы) — там четыре слота сохранений: быстрый и три своих. А при выходе из игры место сохраняется само.")
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        slots.forEach { (i, f) ->
            val has = f.exists()
            val thumb = java.io.File(f.path + ".jpg")
            Column(Modifier.weight(1f)) {
                Box(Modifier.fillMaxWidth().aspectRatio(480f / 272f).clip(RoundedCornerShape(12.dp)).background(K.s1).border(1.dp, K.line, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                    if (has && thumb.exists()) com.nexus.psp.ui.FileImage(thumb, Modifier.fillMaxSize())
                    else if (!has) SolarIcon(if (i == 4) R.drawable.sl_exit else R.drawable.ph_plus, 20.dp, K.ink3)
                }
                Text(when (i) { 4 -> "Место выхода"; 0 -> "Быстрое"; else -> "Слот $i" }, style = T.label, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
                Text(if (has) SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(Date(f.lastModified())) else "пусто", style = T.small)
                if (has) KButton("Продолжить", R.drawable.sb_play, small = true, modifier = Modifier.padding(top = 6.dp)) { com.nexus.psp.ui.Act.play(ctx, g.id, i) }
            }
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

/** Настройки конкретной игры (п. 9): свои или как для всех; те же разделы, что в паузе игры. */
@Composable
fun GameSettingsScreen(id: String) {
    val g = AppState.byId[id] ?: run { Nav.back(); return }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val cfg = remember(id) { com.nexus.psp.data.GameConfig(ctx, id) }
    var own by remember(id) { mutableStateOf(cfg.own) }
    var tick by remember { mutableStateOf(0) }
    val store = remember(id, tick) { cfg.store { } }
    com.nexus.psp.ui.Full {
        com.nexus.psp.ui.OvHead("${g.t} — настройки", back = true)
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Opt("Свои настройки для этой игры", if (own) "Изменения ниже — только для «${g.t}»" else "Сейчас игра использует общие настройки PSP") {
                Toggle(own) { on ->
                    own = on
                    if (on) cfg.edit { o -> o.put("own", true); if (!o.has("vars")) o.put("vars", com.nexus.psp.ui.screens.GlobalCore.vars()); if (!o.has("preset")) o.put("preset", AppState.S.corePreset) }
                    else cfg.clear()
                    tick++
                }
            }
            if (own) {
                CoreQuality(store)
                Sub("Скорость"); CoreRows(com.nexus.psp.data.CoreOptions.SPEED, store)
                Sub("Система"); CoreRows(com.nexus.psp.data.CoreOptions.SYSTEM, store)
                Sub("Управление")
                val modes = listOf("touch", "classic", "pad")
                var mode by remember(id, tick) { mutableStateOf(cfg.read().optString("ctlMode", S.ctlMode)) }
                Opt("Режим кнопок", null) { Seg(listOf("Touch", "Классика", "Геймпад"), modes.indexOf(mode).coerceAtLeast(0)) { mode = modes[it]; cfg.edit { o -> o.put("ctlMode", mode) } } }
                var fps by remember(id, tick) { mutableStateOf(cfg.read().optBoolean("fps", S.fps)) }
                Opt("Счётчик кадров", null) { Toggle(fps) { fps = it; cfg.edit { o -> o.put("fps", it) } } }
                Text("То же можно менять прямо в игре: пауза → «Настройки».", style = T.small, modifier = Modifier.padding(vertical = 10.dp))
            } else StateCard(null, null, "Общие настройки PSP — в «Настройки → Эмулятор PSP». Включи переключатель выше, чтобы задать этой игре свои: например, ×3 для одной игры и «Батарея» для другой.", tint = K.acc)
        }
    }
}
