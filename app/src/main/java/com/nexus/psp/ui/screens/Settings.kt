package com.nexus.psp.ui.screens

import android.app.ActivityManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.psp.R
import com.nexus.psp.data.AppState
import com.nexus.psp.data.AppState.S
import com.nexus.psp.data.PLATFORMS
import com.nexus.psp.ui.Full
import com.nexus.psp.ui.Nav
import com.nexus.psp.ui.OvHead
import com.nexus.psp.ui.Ov
import com.nexus.psp.ui.kit.AssetImage
import com.nexus.psp.ui.kit.K
import com.nexus.psp.ui.kit.KButton
import com.nexus.psp.ui.kit.Opt
import com.nexus.psp.ui.kit.Pill
import com.nexus.psp.ui.kit.Seg
import com.nexus.psp.ui.kit.SolarDuo
import com.nexus.psp.ui.kit.SolarIcon
import com.nexus.psp.ui.kit.StateCard
import com.nexus.psp.ui.kit.Sub
import com.nexus.psp.ui.kit.T
import com.nexus.psp.ui.kit.Toggle
import com.nexus.psp.ui.kit.press
import com.nexus.psp.ui.kit.tap
import org.json.JSONObject
import com.nexus.psp.data.CoreOpt
import com.nexus.psp.data.CoreOptions
import com.nexus.psp.data.CoreStore

/** Настройки (п. 21, 22, 27, 43, 47, 51, 53) + настройки ядра PSP по утверждённому макету. */
@Composable
fun SettingsScreen() {
    val nav = listOf(
        Triple("general", "Общие", R.drawable.sl_sliders), Triple("dl", "Игры и память", R.drawable.sl_folder),
        Triple("core", "Эмулятор PSP", R.drawable.sl_monitor), Triple("ctl", "Управление", R.drawable.sl_pad),
        Triple("bios", "BIOS", R.drawable.sl_chip), Triple("a11y", "Доступность", R.drawable.ph_hand), Triple("about", "О приложении", R.drawable.sl_info),
    )
    val tab = Nav.settingsTab
    Full {
        OvHead("Настройки")
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(Modifier.width(200.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                nav.forEach { (id, t, ic) ->
                    val on = tab == id
                    Row(
                        Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(12.dp)).background(if (on) K.s2 else androidx.compose.ui.graphics.Color.Transparent).tap { Nav.settingsTab = id }.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SolarIcon(ic, 20.dp, if (on) K.acc else K.ink2)
                        Text(t, style = T.label.copy(fontSize = 14.sp, color = if (on) K.ink else K.ink2))
                    }
                }
            }
            Column(Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                when (tab) {
                    "general" -> General()
                    "dl" -> Downloads()
                    "core" -> CoreSettings()
                    "ctl" -> Controls()
                    "bios" -> Bios()
                    "a11y" -> A11y()
                    else -> About()
                }
            }
        }
    }
}

@Composable
private fun General() {
    val th = listOf("dark", "oled", "light")
    Opt("Тема", "OLED — чистый чёрный для AMOLED-экранов") { Seg(listOf("Тёмная", "OLED", "Светлая"), th.indexOf(S.theme).coerceAtLeast(0)) { S.theme = th[it]; AppState.applyLook() } }
    Opt("Язык", null) { Seg(listOf("RU", "EN"), if (S.lang == "EN") 1 else 0) { S.lang = if (it == 1) "EN" else "RU"; if (it == 1) AppState.say("Английский перевод появится позже") } }
    Opt("Вибрация", "Отклик при выборе и важных действиях") { Toggle(S.vib) { S.vib = it } }
    Opt("Звуки интерфейса", "Тихий щелчок в карусели") { Toggle(S.sound) { S.sound = it } }
    Opt("Продолжать с места выхода", "При выходе из игры место сохраняется само, «Играть» возвращает туда же") { Toggle(S.autoResume) { S.autoResume = it } }
    Opt("Облачные сохранения", "Продолжай на другом телефоне (в планах)") { Toggle(S.cloud) { S.cloud = it } }
}

@Composable
private fun Downloads() {
    val ctx = LocalContext.current
    Opt("Папка с играми", com.nexus.psp.emulator.CoreFiles.gamesDir(ctx).absolutePath.replace("/storage/emulated/0/", "")) { Text("своя папка — позже", style = T.small) }
    Opt("Добавить игру", "Файл ISO, CSO, CHD или PBP — скопируется в папку KAPSULA") { KButton("Выбрать файл", R.drawable.ph_plus, small = true) { com.nexus.psp.ui.Act.addFile() } }
    Opt("Найти игры на телефоне", "Выбери папку — KAPSULA найдёт все образы в ней и вложенных папках") { KButton("Выбрать папку", R.drawable.sl_search, small = true) { com.nexus.psp.ui.Act.findGames() } }
    Opt("Обложки из интернета", "По серийному номеру диска, из открытой базы обложек. Сохраняются на телефоне") { Toggle(S.covers) { S.covers = it; if (it) AppState.byId.keys.forEach { id -> AppState.fetchMeta(id) } } }
    Opt("Обложки только по Wi-Fi", "Одна обложка — до 1 МБ") { Toggle(S.coversWifi) { S.coversWifi = it } }
    Opt("Обновить обложки", "Поискать заново для всех игр") { KButton("Обновить", small = true) { AppState.byId.keys.forEach { id -> AppState.fetchMeta(id, force = true) } } }
    Opt("Память", "Игры KAPSULA ${com.nexus.psp.data.size(AppState.usedMb())} · свободно ${com.nexus.psp.data.size(AppState.freeMb())}") { }
}

@Composable
private fun Controls() {
    Opt("Экранный геймпад", mapOf("touch" to "KAPSULA Touch", "classic" to "Классика", "pad" to "Геймпад")[S.ctlMode] + " · раскладка, размер, прозрачность") {
        KButton("Настроить", kind = com.nexus.psp.ui.kit.BtnKind.Main, small = true) { Nav.open(Ov.Pad) }
    }
    Opt("Скрывать кнопки", "Когда подключён геймпад") { Toggle(S.hide) { S.hide = it } }
    CoreRows(CoreOptions.INPUT)
    Opt("Ускорение", "Во сколько раз быстрее при включённом ускорении") {
        Seg(listOf("×2", "×3", "×4"), ((S.fastLevel.toIntOrNull() ?: 2) - 2).coerceIn(0, 2), small = true) { S.fastLevel = (it + 2).toString() }
    }
    Opt("Чувствительность руля наклоном", "Руль наклоном включается в игре: пауза → Настройки → Управление") {
        com.nexus.psp.ui.kit.Range(S.tiltSens.toIntOrNull() ?: 100, 50, 200, fill = false) { S.tiltSens = it.toString() }
    }
    Opt("Физический геймпад", "Кнопки передаются в ядро напрямую; «Mode» — меню паузы") { Text("автоматически", style = T.small) }
}

@Composable
private fun Bios() {
    StateCard("ic-bios.png", "BIOS — прошивка консоли.", "Для PS1 и PS2 понадобится файл BIOS с твоей приставки — KAPSULA проверит его по контрольной сумме. PSP работает без BIOS.")
    PLATFORMS.forEach { p ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AssetImage(p.conImg, Modifier.width(76.dp).height(44.dp))
            Box(Modifier.weight(1f)) {
                Opt("BIOS ${p.full}", if (!p.bios) "Не требуется" else "Понадобится, когда подключим ${p.short}") {
                    if (!p.bios) Pill("не нужен", icon = R.drawable.ph_check, color = K.ok) else Pill("скоро")
                }
            }
        }
    }
}

@Composable
private fun A11y() {
    Opt("Крупный текст", null) { Toggle(S.big) { S.big = it; AppState.applyLook() } }
    Opt("Высокий контраст", null) { Toggle(S.contrast) { S.contrast = it; AppState.applyLook() } }
    Opt("Уменьшить движение", "Без анимаций") { Toggle(S.calm) { S.calm = it } }
    Opt("Раскладка для левшей", "Зеркальные экранные кнопки") { Toggle(S.mirror) { S.mirror = it } }
}

@Composable
private fun About() {
    val ctx = LocalContext.current
    val ver = try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName } catch (e: Exception) { "" }
    Opt("KAPSULA", "Версия $ver · нативный интерфейс") { KButton("Знакомство заново", small = true) { S.onboarded = false; AppState.say("Знакомство покажется при следующем запуске") } }
    Opt("Эмулятор PSP", "Ядро KAPSULA") { }
    Opt("PS1 и PS2", "Будут подключены следующими") { }
    Opt("Конфиденциальность", "Данные хранятся только на телефоне") { }
    // авторы и тексты лицензий открытых компонентов — по условиям этих лицензий; на виду только нейтральная строка
    var legal by remember { mutableStateOf(false) }
    // незаметная серая ссылка внизу раздела
    Text(
        "Правовая информация", style = T.small.copy(fontSize = 11.sp, color = K.ink3),
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp).clip(RoundedCornerShape(6.dp)).press { legal = true }.padding(horizontal = 2.dp, vertical = 4.dp),
    )
    if (legal) LegalDialog { legal = false }
    // последняя ошибка (если приложение или игра падали) — чтобы прислать разработчику
    var crash by remember { mutableStateOf(com.nexus.psp.data.CrashLog.last(ctx)) }
    crash?.let { f ->
        val head = remember(f) { try { f.readLines().take(3).joinToString(" · ") { it.trim() }.take(140) } catch (e: Exception) { "" } }
        Opt("Последняя ошибка", head) {
            androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KButton("Скопировать", small = true) {
                    val cm = ctx.getSystemService(android.content.ClipboardManager::class.java)
                    cm?.setPrimaryClip(android.content.ClipData.newPlainText("KAPSULA", try { f.readText() } catch (e: Exception) { "" }))
                    AppState.say("Текст ошибки скопирован")
                }
                KButton("Очистить", small = true) { com.nexus.psp.data.CrashLog.clear(ctx); crash = null }
            }
        }
    }
}

// ---------- ядро PSP ----------

/** Общие настройки ядра для всех игр PSP. */
val GlobalCore = object : CoreStore {
    override fun vars(): JSONObject = try { JSONObject(S.coreVars) } catch (e: Exception) { JSONObject() }
    override fun set(k: String, v: String) { S.coreVars = vars().put(k, v).toString(); S.corePreset = "custom" }
    override var preset: String
        get() = S.corePreset
        set(value) { S.corePreset = value }
}

/** Пресет по мощности телефона: ОЗУ 8 ГБ+ — «Качество», 4 ГБ+ — «Баланс», иначе — «Батарея». */
fun recommended(ctx: Context): String {
    val mi = ActivityManager.MemoryInfo(); (ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(mi)
    val gb = mi.totalMem / (1024.0 * 1024 * 1024)
    return when { gb >= 7.5 -> "quality"; gb >= 3.5 -> "balance"; else -> "battery" }
}

@Composable
fun CoreRows(list: List<CoreOpt>, store: CoreStore = GlobalCore, onChange: (CoreOpt, String) -> Unit = { _, _ -> }) {
    var tick by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val o = androidx.compose.runtime.remember(tick, store) { store.vars() }
    list.forEach { c ->
        val cur = o.optString(c.key, c.def)
        Opt(c.title, c.desc, badge = if (c.restart) ({ Pill("после перезапуска игры", color = K.warn, bg = K.warn.copy(alpha = .14f)) }) else null) {
            Seg(c.labels, c.values.indexOf(cur).coerceAtLeast(0), small = true) { store.set(c.key, c.values[it]); tick++; onChange(c, c.values[it]) }
        }
    }
}

/** Блок «Качество» с пресетами — общий для экрана ядра и настроек игры. */
@Composable
fun CoreQuality(store: CoreStore = GlobalCore, onChange: (CoreOpt?, String) -> Unit = { _, _ -> }) {
    val ctx = LocalContext.current
    val rec = remember { recommended(ctx) }
    var tick by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val preset = androidx.compose.runtime.remember(tick, store) { store.preset }
    androidx.compose.runtime.LaunchedEffect(Unit) { if (store === GlobalCore && S.corePreset.isEmpty()) { store.applyPreset(rec); tick++ } }
    val names = mapOf("quality" to "Качество", "balance" to "Баланс", "battery" to "Батарея")
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(14.dp)).background(K.acc.copy(alpha = .1f)).border(1.dp, K.acc.copy(alpha = .3f), RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SolarDuo(R.drawable.sd_chip, 20.dp)
        Text("Подобрано под твой телефон: ${names[rec]}", style = T.label.copy(fontSize = 13.sp, color = K.ink2))
    }
    Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Max).padding(top = 18.dp, bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // строка 1 — разрешение (короткая, без переносов), строка 2 — смысл пресета
        listOf(
            Triple("quality", "1440×816", "Текстуры ×2, для мощных телефонов" to 4),
            Triple("balance", "960×544", "Чётко и плавно" to 3),
            Triple("battery", "480×272", "Как на PSP, дольше играть" to 1),
            Triple("custom", "Вручную", "Включится само, если поменяешь настройки" to 0),
        ).forEach { (id, l1, rest) ->
            val on = preset == id
            // отметка «Рекомендуем» — язычком на верхней границе карточки, вне потока текста: строки всех карточек на одном уровне
            Box(Modifier.weight(1f).fillMaxHeight()) {
                Column(
                    Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(if (on) K.acc.copy(alpha = .1f) else K.s1)
                        .border(1.5.dp, if (on) K.acc else K.line, RoundedCornerShape(14.dp))
                        .then(if (id != "custom") Modifier.press { store.applyPreset(id); tick++; onChange(null, id) } else Modifier)
                        .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(names[id] ?: "Своё", style = T.label.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (id == "custom" && !on) K.ink3 else K.ink))
                    Text(l1, style = T.label.copy(fontSize = 12.5.sp, color = if (on) K.ink else K.ink2), maxLines = 1)
                    Text(rest.first, style = T.small.copy(fontSize = 11.sp, lineHeight = 14.sp), modifier = Modifier.weight(1f))
                    Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        repeat(4) { i -> Box(Modifier.size(14.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(if (i < rest.second) (if (on) K.acc else K.ink2) else K.s3)) }
                    }
                }
                if (id == rec) Text(
                    "РЕКОМЕНДУЕМ", style = T.caps.copy(fontSize = 9.sp, color = K.bg), maxLines = 1,
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = (-10).dp, y = (-8).dp).clip(RoundedCornerShape(8.dp)).background(K.acc).padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
    Sub("Изображение")
    CoreRows(CoreOptions.QUALITY, store) { c, v -> tick++; onChange(c, v) }
}

@Composable
private fun CoreSettings() {
    var sec by rememberSaveable { mutableStateOf(0) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 6.dp)) {
        Seg(listOf("Качество", "Скорость", "Система", "Для опытных"), sec) { sec = it }
        Text("для всех игр PSP", style = T.small)
    }
    when (sec) {
        0 -> {
            val f = listOf("default" to "Обычный", "smooth" to "Сглаживание", "pixel" to "Пиксели", "lcd" to "ЖК", "crt" to "ЭЛТ")
            Opt("Фильтр картинки", "Для всех игр; в игре можно поменять на лету: пауза → Настройки") {
                Seg(f.map { it.second }, f.indexOfFirst { it.first == S.shader }.coerceAtLeast(0), small = true) { S.shader = f[it].first }
            }
            CoreQuality()
        }
        1 -> CoreRows(CoreOptions.SPEED)
        2 -> CoreRows(CoreOptions.SYSTEM)
        else -> {
            StateCard(null, null, "Эти параметры могут сломать игру. Не помогло — верни как было.", tint = K.warn) {
                KButton("Сбросить раздел", small = true) { val o = GlobalCore.vars(); CoreOptions.EXPERT.forEach { o.remove(it.key) }; S.coreVars = o.toString(); AppState.say("Раздел «Для опытных» сброшен") }
            }
            CoreRows(CoreOptions.EXPERT)
        }
    }
    Text("Изменения применяются при следующем запуске игры.", style = T.small, modifier = Modifier.padding(top = 10.dp))
}


/** Правовая информация: компоненты с открытым исходным кодом, авторы, лицензии (полные тексты — из assets/licenses). */
@Composable
private fun LegalDialog(onClose: () -> Unit) {
    val ctx = LocalContext.current
    var open by remember { mutableStateOf<String?>(null) }
    val parts = listOf(
        Triple("Ядро эмуляции PSP", "Основано на PPSSPP — Copyright © 2012– Henrik Rydgård and PPSSPP contributors. GNU GPL версии 2 или более поздней. Изменено командой KAPSULA.", "GPL-2.0.txt"),
        Triple("Фронтенд libretro", "LibretroDroid — Copyright © Filippo Scognamiglio. GNU GPL версии 3.", "GPL-3.0.txt"),
        Triple("Значки Solar", "© 480 Design. Creative Commons Attribution 4.0 (CC BY 4.0).", "LICENSE-solar.txt"),
        Triple("Значки Phosphor", "© Phosphor Icons. MIT License.", "LICENSE-phosphor.txt"),
        Triple("Шрифт Manrope", "© The Manrope Project Authors. SIL Open Font License 1.1.", "OFL-Manrope.txt"),
    )
    androidx.compose.ui.window.Dialog(onClose, androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth(.86f).fillMaxHeight(.9f).clip(RoundedCornerShape(22.dp)).background(K.s1).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (open == null) "Правовая информация" else open!!, style = T.h3, modifier = Modifier.weight(1f))
                if (open != null) { KButton("Назад", small = true) { open = null }; androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp)) }
                KButton(icon = R.drawable.ph_x, small = true) { onClose() }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val f = open
                if (f != null) {
                    val txt = remember(f) { try { ctx.assets.open("licenses/$f").bufferedReader().readText() } catch (e: Exception) { "Текст лицензии не найден" } }
                    Text(txt, style = T.small.copy(fontSize = 11.sp, lineHeight = 15.sp, color = K.ink2))
                } else {
                    Text("KAPSULA использует компоненты с открытым исходным кодом. Исходный код KAPSULA, включая изменения ядра: github.com/vitalya2078-cloud/nexus-psp", style = T.small.copy(color = K.ink2))
                    parts.forEach { (t, d, file) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).padding(end = 12.dp)) { Text(t, style = T.label); Text(d, style = T.small) }
                            KButton("Текст", small = true) { open = file }
                        }
                    }
                }
            }
        }
    }
}
