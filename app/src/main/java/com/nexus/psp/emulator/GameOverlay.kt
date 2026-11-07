package com.nexus.psp.emulator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.R
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nexus.psp.data.CoreOptions
import com.nexus.psp.ui.kit.BtnKind
import com.nexus.psp.ui.kit.KButton
import com.nexus.psp.ui.kit.Opt
import com.nexus.psp.ui.kit.Range
import com.nexus.psp.ui.kit.Seg
import com.nexus.psp.ui.kit.Toggle
import com.nexus.psp.ui.screens.CoreQuality
import com.nexus.psp.ui.screens.CoreRows
import com.nexus.psp.ui.kit.ActionTile
import com.nexus.psp.ui.kit.ConsolePill
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.SolarIcon
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.press

private val CONSOLE = mapOf("psp" to ("PlayStation Portable" to K.psp), "ps1" to ("PlayStation" to K.ps1), "ps2" to ("PlayStation 2" to K.ps2))

/** Всё, что поверх картинки игры: заставка загрузки, меню паузы, ошибка, всплывающая подсказка. */
@Composable
fun GameOverlay(a: GameActivity, ui: GameActivity.UiState) {
    val (conName, conColor) = CONSOLE[a.platform] ?: CONSOLE.getValue("psp")
    Box(Modifier.fillMaxSize()) {
        // заставка, пока ядро не нарисовало первый кадр
        AnimatedVisibility(ui.phase == "loading", enter = fadeIn(), exit = fadeOut(tween(500))) {
            Boot(a.title, conName, conColor, a.bgFile)
        }
        AnimatedVisibility(ui.phase == "ready" && ui.paused && !ui.editing, enter = fadeIn(tween(160)), exit = fadeOut(tween(160))) {
            PauseMenu(a, ui, conName, conColor)
        }
        AnimatedVisibility(ui.phase == "ready" && ui.paused && ui.settings && !ui.editing, enter = fadeIn(tween(160)), exit = fadeOut(tween(160))) {
            SettingsPanel(a, ui)
        }
        if (ui.adding) AddButtonPanel(a)
        if (ui.editing && ui.picking == null && !ui.adding) Box(Modifier.align(Alignment.TopCenter).padding(top = 12.dp)) { EditBar(a) }
        ui.picking?.let { key -> IconPicker(a, ui, key) }
        // счётчик кадров
        if (ui.showFps && ui.phase == "ready" && !ui.paused) Text(
            "${ui.fps} FPS",
            style = T.num.copy(fontSize = 12.sp, color = if (ui.fps >= 55) Color(0xFF9BD49B) else if (ui.fps >= 28) Color(0xFFE5C07B) else Color(0xFFD98A8A)),
            modifier = Modifier.align(Alignment.TopStart).windowInsetsPadding(WindowInsets.displayCutout).padding(10.dp)
                .clip(RoundedCornerShape(8.dp)).background(Color.Black.copy(alpha = .55f)).padding(horizontal = 8.dp, vertical = 3.dp),
        )
        if (ui.phase == "error") ErrorScreen(ui.error) { a.exitGame() }
        AnimatedVisibility(ui.toast.isNotEmpty(), Modifier.align(Alignment.TopCenter).padding(top = 14.dp), enter = fadeIn(), exit = fadeOut()) {
            Text(
                ui.toast,
                style = T.label.copy(color = K.bg),
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(K.ink).padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}

@Composable
private fun Boot(title: String, con: String, color: Color, bg: java.io.File?) {
    val pulse by rememberInfiniteTransition(label = "boot").animateFloat(.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "p")
    Box(Modifier.fillMaxSize().background(Color.Black)) {
    // фон игры, пока стартует ядро
    if (bg != null) {
        com.nexus.psp.ui.FileImage(bg, Modifier.fillMaxSize(), alpha = .55f)
        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Black.copy(alpha = .35f), Color.Black.copy(alpha = .55f), Color.Black.copy(alpha = .9f)))))
    }
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // капсула с «экраном» — тот же знак, что в веб-заставке PSP
        Box(Modifier.width(150.dp).height(62.dp).border(2.dp, color.copy(alpha = pulse), RoundedCornerShape(31.dp)), contentAlignment = Alignment.Center) {
            Box(Modifier.width(54.dp).height(38.dp).border(2.dp, color.copy(alpha = pulse), RoundedCornerShape(3.dp)))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = T.h2.copy(fontSize = 20.sp))
        Spacer(Modifier.height(6.dp))
        Text(con.uppercase(), style = T.caps.copy(color = K.ink3))
    }
}
}

@Composable
private fun PauseMenu(a: GameActivity, ui: GameActivity.UiState, con: String, color: Color) {
    var ask by remember { mutableStateOf<GameActivity.Slot?>(null) }
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(0f to Color.Black.copy(alpha = .35f), .45f to Color.Black.copy(alpha = .8f), 1f to Color.Black.copy(alpha = .95f)))
            // касание пустого места не уходит в игру
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.displayCutout).padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ConsolePill(con, color)
                if (ui.canUndo) Row(
                    Modifier.clip(RoundedCornerShape(16.dp)).background(K.acc.copy(alpha = .2f)).press { a.undoLoad() }.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SolarIcon(R.drawable.sl_left, 16.dp, K.ink)
                    Text("Отменить загрузку", style = T.label.copy(fontSize = 13.sp))
                }
            }
            Text(a.title, style = T.h1, maxLines = 1)
            // все плитки помещаются в ширину экрана: делят её поровну (не шире 124 dp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val m = Modifier.weight(1f).widthIn(max = 124.dp)
                ActionTile(R.drawable.sb_play, "Продолжить", main = true, sub = "В игру", modifier = m) { a.setPaused(false) }
                ActionTile(R.drawable.sl_save, "Сохранить", sub = "Быстрый слот", modifier = m) { a.saveQuick() }
                ActionTile(R.drawable.sl_cam, "Скриншот", sub = "В галерею", modifier = m) { a.screenshot() }
                ActionTile(R.drawable.sl_sliders, "Настройки", sub = "Поверх игры", modifier = m) { ui.settings = true }
                ActionTile(R.drawable.sl_pad, "Кнопки", enabled = ui.ctlMode != "pad", sub = if (ui.ctlMode == "pad") "Геймпад" else "Места, значки", modifier = m) { a.startEditing() }
                ActionTile(R.drawable.sl_bolt, "Ускорение", on = ui.fast, sub = if (ui.fast) "×2 · вкл." else "Выключено", modifier = m) { a.toggleFast() }
                ActionTile(R.drawable.sl_exit, "Выйти", danger = true, sub = "В библиотеку", modifier = m) { a.exitGame() }
            }
            Text("СОХРАНЕНИЯ", style = T.caps.copy(color = Color(0xFF8E9099)), modifier = Modifier.padding(top = 4.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val list = ui.slots.ifEmpty { (0..3).map { GameActivity.Slot(it, false, 0, null) } }
                list.forEach { sl -> SlotCard(sl) { if (sl.exists) ask = sl else a.saveSlot(sl.i) } }
            }
        }
        ask?.let { sl -> SlotDialog(sl, onDismiss = { ask = null }, onLoad = { ask = null; a.loadSlot(sl.i) }, onSave = { ask = null; a.saveSlot(sl.i) }, onDelete = { ask = null; a.deleteSlot(sl.i) }) }
    }
}

private fun slotName(i: Int) = if (i == 0) "Быстрое" else "Слот $i"

private fun ago(t: Long): String {
    val m = ((System.currentTimeMillis() - t) / 60000).coerceAtLeast(0)
    return when {
        m < 1 -> "только что"
        m < 60 -> "$m мин назад"
        m < 60 * 24 -> java.text.SimpleDateFormat("сегодня, HH:mm", java.util.Locale("ru")).format(java.util.Date(t))
        else -> java.text.SimpleDateFormat("d MMM, HH:mm", java.util.Locale("ru")).format(java.util.Date(t))
    }
}

/** Карточка слота: кадр игры, название, время. Все четыре — одного размера. */
@Composable
private fun SlotCard(sl: GameActivity.Slot, onClick: () -> Unit) {
    Column(
        Modifier.width(150.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF131419))
            .border(1.dp, Color.White.copy(alpha = .08f), RoundedCornerShape(14.dp)).press(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().height(84.dp).background(Color(0xFF0B0C0F)), contentAlignment = Alignment.Center) {
            val th = sl.thumb
            if (sl.exists && th != null) com.nexus.psp.ui.FileImage(th, Modifier.fillMaxSize())
            else SolarIcon(if (sl.exists) R.drawable.sl_save else R.drawable.ph_plus, 22.dp, Color(0xFF8E9099))
        }
        Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text(slotName(sl.i), style = T.label.copy(fontSize = 13.sp, color = Color(0xFFF2F1EC)), maxLines = 1)
            Text(if (sl.exists) ago(sl.time) else "пусто · сохранить", style = T.small.copy(fontSize = 11.sp, color = Color(0xFF8E9099)), maxLines = 1)
        }
    }
}

@Composable
private fun SlotDialog(sl: GameActivity.Slot, onDismiss: () -> Unit, onLoad: () -> Unit, onSave: () -> Unit, onDelete: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = .6f)).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.width(380.dp).clip(RoundedCornerShape(20.dp)).background(K.s1)
                .clickable(remember { MutableInteractionSource() }, indication = null) {}.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                val th = sl.thumb
                Box(Modifier.width(120.dp).height(68.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black)) {
                    if (th != null) com.nexus.psp.ui.FileImage(th, Modifier.fillMaxSize())
                }
                Column {
                    Text(slotName(sl.i), style = T.h2.copy(fontSize = 18.sp))
                    Text(ago(sl.time), style = T.small)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KButton("Загрузить", icon = R.drawable.sl_load, kind = BtnKind.Main, small = true, onClick = onLoad)
                KButton("Перезаписать", icon = R.drawable.sl_save, small = true, onClick = onSave)
                KButton("Удалить", icon = R.drawable.sl_trash, small = true, danger = true, onClick = onDelete)
            }
            Text("Перед загрузкой текущий момент сохранится отдельно — загрузку можно отменить.", style = T.small.copy(fontSize = 11.5.sp))
        }
    }
}

/** Настройки поверх игры: живые изменения ядра, управление, звук. */
@Composable
private fun SettingsPanel(a: GameActivity, ui: GameActivity.UiState) {
    var tab by remember { mutableStateOf(0) }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = .45f)).clickable(remember { MutableInteractionSource() }, indication = null) { ui.settings = false },
    ) {
        Column(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(.66f).widthIn(min = 480.dp).background(K.bg)
                .clickable(remember { MutableInteractionSource() }, indication = null) {}
                .windowInsetsPadding(WindowInsets.displayCutout).padding(start = 20.dp, end = 20.dp, top = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Настройки", style = T.h2, modifier = Modifier.weight(1f))
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(K.s2).press { ui.settings = false }, contentAlignment = Alignment.Center) {
                    SolarIcon(R.drawable.ph_x, 18.dp)
                }
            }
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Seg(listOf("Только для этой игры", "Для всех игр"), if (ui.scopeGame) 0 else 1, small = true) { ui.scopeGame = it == 0 }
            }
            Text(
                if (ui.scopeGame) "Изменения запомнятся для «${a.title}» и не тронут другие игры"
                else "Изменения станут общими для всех игр, у которых нет своих настроек",
                style = T.small.copy(fontSize = 11.5.sp), modifier = Modifier.padding(top = 6.dp),
            )
            Row(Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState())) {
                Seg(listOf("Качество", "Скорость", "Управление", "Звук"), tab) { tab = it }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 4.dp, bottom = 16.dp)) {
                key(ui.scopeGame) {
                    when (tab) {
                        0 -> CoreQuality(a.coreStore)
                        1 -> {
                            CoreRows(CoreOptions.SPEED, a.coreStore)
                            Opt("Счётчик кадров", "Число кадров в секунду в углу экрана") { Toggle(ui.showFps) { a.setFps(it) } }
                        }
                        2 -> Controls(a, ui)
                        else -> Opt("Громкость", "Громкость звука телефона для игр") { Range(ui.volume, 0, 100) { a.setVolume(it) } }
                    }
                }
            }
            if (ui.restartNeeded) Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(14.dp)).background(K.warn.copy(alpha = .14f)).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SolarIcon(R.drawable.sl_info, 18.dp, K.warn)
                Text("Часть настроек заработает после перезапуска. Место в игре сохранится.", style = T.small.copy(fontSize = 12.sp, color = K.ink2), modifier = Modifier.weight(1f))
                KButton("Применить", kind = BtnKind.Main, small = true) { a.applyRestart() }
            }
        }
    }
}

@Composable
private fun Controls(a: GameActivity, ui: GameActivity.UiState) {
    val modes = listOf("touch" to "Экранные", "classic" to "Классика", "pad" to "Геймпад")
    Opt("Кнопки", "Экранные — под игру, классика — как на PSP, геймпад — без экранных кнопок") {
        Seg(modes.map { it.second }, modes.indexOfFirst { it.first == ui.ctlMode }.coerceAtLeast(0), small = true) { a.setCtlMode(modes[it].first) }
    }
    Opt("Прозрачность кнопок", null) { Range(ui.op, 15, 100) { a.setOpacity(it) } }
    Opt("Размер кнопок", null) { Range(ui.sc, 70, 140) { a.setSize(it) } }
    Opt("Для левшей", "Зеркальная раскладка") { Toggle(ui.mirror) { a.setMirror(it) } }
    Opt("Положение кнопок", "Перетащи кнопки пальцем прямо поверх игры") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KButton("Сбросить", small = true) { a.resetButtons() }
            KButton("Переставить", icon = R.drawable.ph_move, kind = BtnKind.Main, small = true, enabled = ui.ctlMode != "pad") { a.startEditing() }
        }
    }
}

/** Значок-фигура кнопки действия (как на экранных кнопках). */
@Composable
fun GlyphIcon(g: String, color: Color, size: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val w = this.size.minDimension; val c = androidx.compose.ui.geometry.Offset(w / 2, w / 2); val a = w * .36f
        val st = androidx.compose.ui.graphics.drawscope.Stroke(width = w * .1f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        when (g) {
            "ft" -> drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(c.x, c.y - a); lineTo(c.x + a * 1.05f, c.y + a * .72f); lineTo(c.x - a * 1.05f, c.y + a * .72f); close() }, color, style = st)
            "fr" -> drawCircle(color, a * .82f, c, style = st)
            "fb" -> { val q = a * .74f; drawLine(color, c.copy(c.x - q, c.y - q), c.copy(c.x + q, c.y + q), w * .1f, androidx.compose.ui.graphics.StrokeCap.Round); drawLine(color, c.copy(c.x + q, c.y - q), c.copy(c.x - q, c.y + q), w * .1f, androidx.compose.ui.graphics.StrokeCap.Round) }
            else -> { val q = a * .7f; drawRoundRect(color, androidx.compose.ui.geometry.Offset(c.x - q, c.y - q), androidx.compose.ui.geometry.Size(q * 2, q * 2), androidx.compose.ui.geometry.CornerRadius(q * .14f), style = st) }
        }
    }
}

private val GLYPH_COLOR = mapOf("ft" to Color(0xFFD9B36C), "fr" to Color(0xFF7FA7D9), "fb" to Color(0xFF8FC9A3), "fl" to Color(0xFFD98A8A))

/** Выбор значка для кнопки KAPSULA Touch + турбо. Запоминается для игры или для всех (как остальные настройки). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun IconPicker(a: GameActivity, ui: GameActivity.UiState, key: String) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var group by remember { mutableStateOf(0) }
    var cur by remember(key) { mutableStateOf(a.currentIcon(key)) }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)).clickable(remember { MutableInteractionSource() }, indication = null) { a.closePicker() }) {
        Column(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(.7f).widthIn(min = 500.dp).background(K.bg)
                .clickable(remember { MutableInteractionSource() }, indication = null) {}
                .windowInsetsPadding(WindowInsets.displayCutout).padding(start = 20.dp, end = 20.dp, top = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Значок кнопки", style = T.h2, modifier = Modifier.weight(1f))
                KButton("Как было", small = true) { a.setIcon(key, null); cur = ui.pickingDefault }
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(K.s2).press { a.closePicker() }, contentAlignment = Alignment.Center) { SolarIcon(R.drawable.ph_x, 18.dp) }
            }
            if (ui.pickingExtra.isNotEmpty()) Opt("Своя кнопка", ui.pickingExtra) { KButton("Удалить", icon = R.drawable.sl_trash, small = true, danger = true) { a.removeExtra(key) } }
            else Opt("Турбо", "Пока держишь — кнопка быстро нажимается сама") { Toggle(ui.pickingTurbo) { a.setTurbo(key, it) } }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CtlIcons.GROUPS.forEachIndexed { i, (g, _) -> com.nexus.psp.ui.kit.Chip(g, group == i) { group = i } }
            }
            androidx.compose.foundation.layout.FlowRow(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CtlIcons.GROUPS[group].second.forEach { (name, label) ->
                    val on = name == cur
                    Column(
                        Modifier.width(76.dp).clip(RoundedCornerShape(14.dp)).background(if (on) K.acc.copy(alpha = .16f) else K.s1)
                            .border(1.5.dp, if (on) K.acc else K.line, RoundedCornerShape(14.dp))
                            .press { a.setIcon(key, name); cur = name }.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val gc = GLYPH_COLOR[name]
                        if (gc != null) GlyphIcon(name, gc, 30.dp)
                        else {
                            val id = remember(name) { ctx.resources.getIdentifier("ctl_$name", "drawable", ctx.packageName) }
                            if (id != 0) SolarIcon(id, 30.dp, K.ink)
                        }
                        Text(label, style = T.small.copy(fontSize = 11.sp, color = K.ink2), maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Значок части своей кнопки: фигура, стрелка или надпись. */
@Composable
private fun PartIcon(a: GameActivity, k: String, size: androidx.compose.ui.unit.Dp) {
    val gc = GLYPH_COLOR[k]
    if (gc != null) GlyphIcon(k, gc, size)
    else Text(a.partName(k), style = T.label.copy(fontSize = (size.value * .5f).sp, color = K.ink), maxLines = 1)
}

/** Новая кнопка: «две разом» (все вместе, пока держишь) или комбо (по очереди одним касанием). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AddButtonPanel(a: GameActivity) {
    var combo by remember { mutableStateOf(false) }
    val keys = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)).clickable(remember { MutableInteractionSource() }, indication = null) { a.closeAdding() }) {
        Column(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(.7f).widthIn(min = 500.dp).background(K.bg)
                .clickable(remember { MutableInteractionSource() }, indication = null) {}
                .windowInsetsPadding(WindowInsets.displayCutout).padding(start = 20.dp, end = 20.dp, top = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Новая кнопка", style = T.h2, modifier = Modifier.weight(1f))
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(K.s2).press { a.closeAdding() }, contentAlignment = Alignment.Center) { SolarIcon(R.drawable.ph_x, 18.dp) }
            }
            Seg(listOf("Две разом", "Комбо"), if (combo) 1 else 0) { combo = it == 1; keys.clear() }
            Text(
                if (combo) "Одно касание — кнопки нажимаются по очереди. Нажимай кнопки ниже в нужном порядке (до 8)."
                else "Пока держишь — нажаты все выбранные кнопки сразу. Выбери 2–4 кнопки.",
                style = T.small,
            )
            // что получилось
            Row(
                Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp)).background(K.s1).border(1.dp, K.line, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (keys.isEmpty()) Text("Пока пусто", style = T.small)
                keys.forEachIndexed { i, k ->
                    if (i > 0) Text(if (combo) "→" else "+", style = T.label.copy(color = K.ink3))
                    PartIcon(a, k, 26.dp)
                }
                Spacer(Modifier.weight(1f))
                if (combo && keys.isNotEmpty()) KButton("Стереть", small = true) { keys.removeAt(keys.lastIndex) }
            }
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ControlLayout.PARTS.forEach { k ->
                    val on = !combo && k in keys
                    Box(
                        Modifier.size(64.dp, 52.dp).clip(RoundedCornerShape(12.dp)).background(if (on) K.acc.copy(alpha = .16f) else K.s1)
                            .border(1.5.dp, if (on) K.acc else K.line, RoundedCornerShape(12.dp))
                            .press {
                                if (combo) { if (keys.size < 8) keys.add(k) }
                                else if (k in keys) keys.remove(k) else if (keys.size < 4) keys.add(k)
                            },
                        contentAlignment = Alignment.Center,
                    ) { PartIcon(a, k, 26.dp) }
                }
            }
            KButton("Добавить кнопку", icon = R.drawable.ph_plus, kind = BtnKind.Main, enabled = keys.size >= 2, modifier = Modifier.padding(bottom = 16.dp)) { a.addExtra(keys.toList(), combo) }
        }
    }
}

/** Плашка редактора кнопок: кнопки видны поверх игры, касание мимо них приходит сюда. */
@Composable
private fun EditBar(a: GameActivity) {
    Row(
        Modifier.clip(RoundedCornerShape(22.dp)).background(Color.Black.copy(alpha = .75f)).border(1.dp, Color.White.copy(alpha = .15f), RoundedCornerShape(22.dp))
            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SolarIcon(R.drawable.ph_move, 16.dp, Color(0xFFF2F1EC))
        Text("Перетащи или нажми на кнопку", style = T.label.copy(fontSize = 13.sp, color = Color(0xFFF2F1EC)))
        KButton("Кнопка", icon = R.drawable.ph_plus, small = true) { a.startAdding() }
        KButton("Сбросить", small = true) { a.resetButtons() }
        KButton("Готово", icon = R.drawable.ph_check, kind = BtnKind.Main, small = true) { a.stopEditing() }
    }
}

@Composable
private fun ErrorScreen(msg: String, onExit: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SolarIcon(R.drawable.sl_alert, 40.dp, K.warn)
        Spacer(Modifier.height(14.dp))
        Text("Игра не запустилась", style = T.h2)
        Spacer(Modifier.height(6.dp))
        Text(msg, style = T.body.copy(color = K.ink2, fontWeight = FontWeight.Medium), textAlign = TextAlign.Center, modifier = Modifier.width(420.dp))
        Spacer(Modifier.height(18.dp))
        Box(Modifier.clip(RoundedCornerShape(22.dp)).background(K.ink).press(onClick = onExit).padding(horizontal = 22.dp, vertical = 12.dp)) {
            Text("Выйти", style = T.label.copy(color = K.bg))
        }
    }
}
