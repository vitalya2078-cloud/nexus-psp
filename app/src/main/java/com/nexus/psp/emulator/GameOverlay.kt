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
import androidx.compose.ui.graphics.graphicsLayer
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
        AnimatedVisibility(ui.phase == "ready" && ui.paused && !ui.editing && !ui.genreAsk, enter = fadeIn(tween(160)), exit = fadeOut(tween(160))) {
            PauseMenu(a, ui, conName, conColor)
        }
        AnimatedVisibility(ui.phase == "ready" && ui.paused && ui.settings && !ui.editing, enter = fadeIn(tween(160)), exit = fadeOut(tween(160))) {
            SettingsPanel(a, ui)
        }
        if (ui.adding) AddButtonPanel(a)
        if (ui.editing && ui.picking == null && !ui.adding && !ui.presets) Box(Modifier.align(Alignment.TopCenter).padding(top = 12.dp)) { EditBar(a, ui) }
        if (ui.presets) PresetsPanel(a, ui)
        if (ui.genreAsk) PresetsPanel(a, ui, ask = true)
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
    val enter = com.nexus.psp.ui.rememberEnter(160)
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = .6f)).clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.width(380.dp).graphicsLayer { alpha = enter.value; val k = .96f + .04f * enter.value; scaleX = k; scaleY = k }.clip(RoundedCornerShape(20.dp)).background(K.s1)
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
    Opt("Пресеты кнопок", "Жанры, готовая раскладка игры, свои пресеты") { KButton("Открыть", small = true, enabled = ui.ctlMode == "touch") { a.openPresets() } }
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
    val enter = com.nexus.psp.ui.rememberEnter(200)
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var group by remember { mutableStateOf(0) }
    var cur by remember(key) { mutableStateOf(a.currentIcon(key)) }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)).clickable(remember { MutableInteractionSource() }, indication = null) { a.closePicker() }) {
        Column(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(.7f).widthIn(min = 500.dp).graphicsLayer { translationX = (1f - enter.value) * 56.dp.toPx(); alpha = enter.value }.background(K.bg)
                .clickable(remember { MutableInteractionSource() }, indication = null) {}
                .windowInsetsPadding(WindowInsets.displayCutout).padding(start = 20.dp, end = 20.dp, top = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Кнопка", style = T.h2, modifier = Modifier.weight(1f))
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(K.s2).press { a.closePicker() }, contentAlignment = Alignment.Center) { SolarIcon(R.drawable.ph_x, 18.dp) }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            @Suppress("UNUSED_VARIABLE") val t = ui.tick
            val multi = ui.pickingExtra.isNotEmpty()
            // что нажимает в игре (у «двух разом» и комбо — свой набор, задаётся при создании)
            if (!multi) {
                Text("НАЖИМАЕТ В ИГРЕ", style = T.caps, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val curKey = a.btnKey(key)
                    ControlLayout.PARTS.forEach { k ->
                        val on = k == curKey
                        Box(
                            Modifier.size(52.dp, 42.dp).clip(RoundedCornerShape(12.dp)).background(if (on) K.acc.copy(alpha = .18f) else K.s1)
                                .border(1.5.dp, if (on) K.acc else K.line, RoundedCornerShape(12.dp)).press { a.setBtnKey(key, k) },
                            contentAlignment = Alignment.Center,
                        ) { PartIcon(a, k, 22.dp) }
                    }
                }
            }
            Opt("Размер", null) {
                val steps = listOf(.8f, 1f, 1.25f, 1.5f)
                Seg(listOf("S", "M", "L", "XL"), steps.indexOfFirst { kotlin.math.abs(it - a.btnSize(key)) < .01f }.coerceAtLeast(1), small = true) { a.setBtnSize(key, steps[it]) }
            }
            // роль: обычная / нажал — переключил раскладку / пока держишь — другая раскладка
            val role = a.btnRole(key)
            val kind = when { role == null -> 0; role.startsWith("hold:") -> 2; else -> 1 }
            val others = ui.layouts.filter { it.id != ui.layId }
            Opt("Действие", when (kind) { 1 -> "Нажимает кнопку в игре и через миг переключает раскладку"; 2 -> "Пока палец на кнопке — другая раскладка"; else -> null }) {
                Seg(listOf("Обычная", "Переключает", "Пока держишь"), kind, small = true) { i ->
                    val target = (role?.substringAfter(":") ?: others.firstOrNull()?.id)
                    when {
                        i == 0 -> a.setBtnRole(key, null)
                        target == null -> a.toast("Сначала добавь вторую раскладку")
                        else -> a.setBtnRole(key, (if (i == 2) "hold:" else "switch:") + target)
                    }
                }
            }
            if (kind != 0) Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("На раскладку:", style = T.small)
                val tgt = role?.substringAfter(":")
                others.forEach { l -> com.nexus.psp.ui.kit.Chip(l.name, l.id == tgt || l.base == tgt) { a.setBtnRole(key, (if (kind == 2) "hold:" else "switch:") + l.id) } }
            }
            if (!multi) Opt("Турбо", "Пока держишь — кнопка быстро нажимается сама") { Toggle(ui.pickingTurbo) { a.setTurbo(key, it) } }
            if (ui.pickingIsExtra) Opt("Своя кнопка", ui.pickingExtra.ifEmpty { null }) { KButton("Удалить", icon = R.drawable.sl_trash, small = true, danger = true) { a.removeExtra(key) } }
            else Opt("Скрыть кнопку", "В игре не видна; в редакторе — полупрозрачная, чтобы вернуть") { Toggle(a.btnHidden(key)) { a.setBtnHidden(key, it) } }
            Row(Modifier.padding(top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("ЗНАЧОК", style = T.caps, modifier = Modifier.weight(1f))
                KButton("Как было", small = true) { a.setIcon(key, null); cur = ui.pickingDefault }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CtlIcons.GROUPS.forEachIndexed { i, (g, _) -> com.nexus.psp.ui.kit.Chip(g, group == i) { group = i } }
            }
            androidx.compose.foundation.layout.FlowRow(
                Modifier.padding(top = 8.dp),
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
    val enter = com.nexus.psp.ui.rememberEnter(200)
    var mode by remember { mutableStateOf(0) } // 0 одна, 1 две разом, 2 комбо
    val combo = mode == 2
    val keys = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)).clickable(remember { MutableInteractionSource() }, indication = null) { a.closeAdding() }) {
        Column(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(.7f).widthIn(min = 500.dp).graphicsLayer { translationX = (1f - enter.value) * 56.dp.toPx(); alpha = enter.value }.background(K.bg)
                .clickable(remember { MutableInteractionSource() }, indication = null) {}
                .windowInsetsPadding(WindowInsets.displayCutout).padding(start = 20.dp, end = 20.dp, top = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Новая кнопка", style = T.h2, modifier = Modifier.weight(1f))
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(K.s2).press { a.closeAdding() }, contentAlignment = Alignment.Center) { SolarIcon(R.drawable.ph_x, 18.dp) }
            }
            Seg(listOf("Одна", "Две разом", "Комбо"), mode) { mode = it; keys.clear() }
            Text(
                when (mode) {
                    0 -> "Обычная кнопка: выбери, что она нажимает в игре. Значок, размер и роль — потом, нажатием на неё в редакторе."
                    2 -> "Одно касание — кнопки нажимаются по очереди. Нажимай кнопки ниже в нужном порядке (до 8)."
                    else -> "Пока держишь — нажаты все выбранные кнопки сразу. Выбери 2–4 кнопки."
                },
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
                    val on = mode != 2 && k in keys
                    Box(
                        Modifier.size(64.dp, 52.dp).clip(RoundedCornerShape(12.dp)).background(if (on) K.acc.copy(alpha = .16f) else K.s1)
                            .border(1.5.dp, if (on) K.acc else K.line, RoundedCornerShape(12.dp))
                            .press {
                                if (mode == 0) { keys.clear(); keys.add(k) }
                                else if (combo) { if (keys.size < 8) keys.add(k) }
                                else if (k in keys) keys.remove(k) else if (keys.size < 4) keys.add(k)
                            },
                        contentAlignment = Alignment.Center,
                    ) { PartIcon(a, k, 26.dp) }
                }
            }
            KButton("Добавить кнопку", icon = R.drawable.ph_plus, kind = BtnKind.Main, enabled = keys.size >= (if (mode == 0) 1 else 2), modifier = Modifier.padding(bottom = 16.dp)) { a.addExtra(keys.toList(), combo) }
        }
    }
}

/** Плашка редактора кнопок: кнопки видны поверх игры, касание мимо них приходит сюда. */
@Composable
private fun EditBar(a: GameActivity, ui: GameActivity.UiState) {
    var layMenu by remember { mutableStateOf<String?>(null) }
    var addLay by remember { mutableStateOf(false) }
    val glass = Modifier.clip(RoundedCornerShape(22.dp)).background(Color.Black.copy(alpha = .78f)).border(1.dp, Color.White.copy(alpha = .15f), RoundedCornerShape(22.dp))
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            glass.padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SolarIcon(R.drawable.ph_move, 16.dp, Color(0xFFF2F1EC))
            Text("Перетащи или нажми на кнопку", style = T.label.copy(fontSize = 13.sp, color = Color(0xFFF2F1EC)))
            KButton("Кнопка", icon = R.drawable.ph_plus, small = true) { a.startAdding() }
            KButton("Пресеты", small = true) { a.openPresets() }
            KButton("Сбросить", small = true) { a.resetButtons() }
            KButton("Готово", icon = R.drawable.ph_check, kind = BtnKind.Main, small = true) { a.stopEditing() }
        }
        // раскладки игры: нажатие — открыть, повторное — переименовать/удалить; «+» — новая раскладка из шаблона
        if (ui.ctlMode != "classic") Row(glass.padding(horizontal = 6.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            ui.layouts.forEach { l -> com.nexus.psp.ui.kit.Chip(l.name, l.id == ui.layId) { if (l.id == ui.layId) layMenu = l.id else a.selectLayout(l.id) } }
            if (ui.layouts.size < 4) com.nexus.psp.ui.kit.Chip("Раскладка", icon = R.drawable.ph_plus) { addLay = true }
        }
    }
    layMenu?.let { id -> LayoutDialog(a, ui, id) { layMenu = null } }
    if (addLay) AddLayoutDialog(a) { addLay = false }
}

/** Переименовать или удалить раскладку. */
@Composable
private fun LayoutDialog(a: GameActivity, ui: GameActivity.UiState, id: String, onClose: () -> Unit) {
    val l = ui.layouts.firstOrNull { it.id == id } ?: return
    var name by remember(id) { mutableStateOf(l.name) }
    DialogBox(onClose) {
        Text("Раскладка", style = T.h3)
        com.nexus.psp.ui.screens.SearchField(name, "Название", Modifier.fillMaxWidth()) { name = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KButton("Сохранить", kind = BtnKind.Main, small = true) { a.renameLayout(id, name); onClose() }
            if (ui.layouts.size > 1) KButton("Удалить", icon = R.drawable.sl_trash, small = true, danger = true) { a.removeLayout(id); onClose() }
        }
    }
}

/** Новая раскладка: основа-шаблон (пешком, транспорт, прицел…). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AddLayoutDialog(a: GameActivity, onClose: () -> Unit) {
    DialogBox(onClose) {
        Text("Новая раскладка", style = T.h3)
        Text("Выбери основу — дальше её можно перенастроить", style = T.small)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Templates.BASES.forEach { (base, name) -> com.nexus.psp.ui.kit.Chip(name) { a.addLayout(base, name); onClose() } }
        }
    }
}

/** Небольшое окно поверх игры (затемнение по краям, закрывается касанием мимо). */
@Composable
private fun DialogBox(onClose: () -> Unit, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val enter = com.nexus.psp.ui.rememberEnter(160)
    androidx.compose.ui.window.Dialog(onClose) {
        Column(
            Modifier.width(440.dp).graphicsLayer { alpha = enter.value }.clip(RoundedCornerShape(20.dp)).background(K.s1).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp), content = content,
        )
    }
}

/** Пресеты: встроенный для этой игры, шаблоны жанров, свои (сохранить / применить / переименовать / удалить). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PresetsPanel(a: GameActivity, ui: GameActivity.UiState, ask: Boolean = false) {
    val enter = com.nexus.psp.ui.rememberEnter(200)
    var newName by remember { mutableStateOf("") }
    var rename by remember { mutableStateOf<String?>(null) }
    @Suppress("UNUSED_VARIABLE") val t = ui.tick
    val close = { if (ask) a.answerGenre(null) else a.closePresets() }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f)).clickable(remember { MutableInteractionSource() }, indication = null) { close() }) {
        Column(
            Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(.7f).widthIn(min = 500.dp).graphicsLayer { translationX = (1f - enter.value) * 56.dp.toPx(); alpha = enter.value }.background(K.bg)
                .clickable(remember { MutableInteractionSource() }, indication = null) {}
                .windowInsetsPadding(WindowInsets.displayCutout).padding(start = 20.dp, end = 20.dp, top = 14.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (ask) "Какой это жанр?" else "Пресеты кнопок", style = T.h2, modifier = Modifier.weight(1f))
                if (ask) KButton("Не сейчас", small = true) { a.answerGenre(null) }
                else Box(Modifier.size(36.dp).clip(RoundedCornerShape(18.dp)).background(K.s2).press { close() }, contentAlignment = Alignment.Center) { SolarIcon(R.drawable.ph_x, 18.dp) }
            }
            if (ask) Text("Подберу экранные кнопки под игру. Потом всё можно поменять: пауза → «Кнопки».", style = T.small)
            if (!ask && a.hasBuiltin()) Opt("Для этой игры", "Готовая раскладка KAPSULA «${ui.builtin ?: a.title}»") { KButton("Применить", kind = BtnKind.Main, small = true) { a.applyBuiltin() } }
            Text("ЖАНРЫ", style = T.caps, modifier = Modifier.padding(top = 4.dp))
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Templates.GENRES.forEach { g ->
                    Column(
                        Modifier.width(104.dp).clip(RoundedCornerShape(14.dp)).background(K.s1).border(1.dp, K.line, RoundedCornerShape(14.dp))
                            .press { if (ask) a.answerGenre(g.id) else a.applyGenre(g.id) }.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        val gc = GLYPH_COLOR[g.icon]
                        if (gc != null) GlyphIcon(g.icon, gc, 28.dp) else {
                            val ctx = androidx.compose.ui.platform.LocalContext.current
                            val id = remember(g.icon) { ctx.resources.getIdentifier("ctl_${g.icon}", "drawable", ctx.packageName) }
                            if (id != 0) SolarIcon(id, 28.dp, K.ink)
                        }
                        Text(g.name, style = T.label.copy(fontSize = 13.sp), maxLines = 1)
                        Text(g.layouts.joinToString(" · ") { it.name }, style = T.small.copy(fontSize = 10.sp), maxLines = 1)
                    }
                }
            }
            if (!ask) {
                Text("МОИ ПРЕСЕТЫ", style = T.caps, modifier = Modifier.padding(top = 8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.nexus.psp.ui.screens.SearchField(newName, "Название, например «Мои гонки»", Modifier.weight(1f)) { newName = it }
                    KButton("Сохранить текущие", icon = R.drawable.sl_save, small = true) { a.savePreset(newName); newName = "" }
                }
                val mine = a.myPresets()
                if (mine.isEmpty()) Text("Сохрани настроенные кнопки — и применяй их к другим играм одним нажатием.", style = T.small)
                mine.forEach { (n, _) ->
                    Opt(n, null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            KButton("Применить", kind = BtnKind.Main, small = true) { a.applyMyPreset(n) }
                            KButton(icon = R.drawable.sl_text, small = true) { rename = n }
                            KButton(icon = R.drawable.sl_trash, small = true, danger = true) { a.deletePreset(n) }
                        }
                    }
                }
            }
            Box(Modifier.height(16.dp))
        }
    }
    rename?.let { old ->
        var nn by remember(old) { mutableStateOf(old) }
        DialogBox({ rename = null }) {
            Text("Переименовать пресет", style = T.h3)
            com.nexus.psp.ui.screens.SearchField(nn, "Название", Modifier.fillMaxWidth()) { nn = it }
            KButton("Сохранить", kind = BtnKind.Main, small = true) { a.renamePreset(old, nn); rename = null }
        }
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
